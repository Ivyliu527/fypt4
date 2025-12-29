package com.example.tonbo_app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 對話回應生成器
 * 根據用戶的語音輸入生成智能回應
 * 支持 Ollama AI API 和關鍵詞匹配兩種模式
 */
public class ConversationResponseGenerator {
    private static final String TAG = "ConversationResponseGenerator";
    
    private String currentLanguage = "cantonese";
    private Random random = new Random();
    private Context context;
    private OllamaApiClient ollamaClient;
    private boolean useOllama = false; // 是否使用 Ollama API
    private ConversationManager conversationManager; // 用於獲取對話上下文
    
    // 關鍵詞到回應模板的映射（粵語）
    private Map<String, String[]> cantoneseResponses = new HashMap<>();
    // 關鍵詞到回應模板的映射（普通話）
    private Map<String, String[]> mandarinResponses = new HashMap<>();
    // 關鍵詞到回應模板的映射（英語）
    private Map<String, String[]> englishResponses = new HashMap<>();
    
    public ConversationResponseGenerator() {
        initializeResponses();
    }
    
    /**
     * 帶 Context 的構造函數（用於 Ollama API）
     */
    public ConversationResponseGenerator(Context context) {
        this.context = context;
        initializeResponses();
        
        // 初始化 Ollama 客戶端
        if (context != null) {
            try {
                ollamaClient = new OllamaApiClient(context);
                useOllama = true;
                Log.d(TAG, "Ollama API 客戶端已初始化");
            } catch (Exception e) {
                Log.e(TAG, "初始化 Ollama API 客戶端失敗", e);
                useOllama = false;
            }
        }
    }
    
    /**
     * 設置是否使用 Ollama API
     */
    public void setUseOllama(boolean useOllama) {
        this.useOllama = useOllama && ollamaClient != null;
        Log.d(TAG, "Ollama API 使用狀態: " + this.useOllama);
    }
    
    /**
     * 獲取 Ollama 客戶端（用於配置）
     */
    public OllamaApiClient getOllamaClient() {
        return ollamaClient;
    }
    
    /**
     * 設置語言
     */
    public void setLanguage(String language) {
        this.currentLanguage = language;
    }
    
    /**
     * 設置對話管理器（用於獲取上下文）
     */
    public void setConversationManager(ConversationManager manager) {
        this.conversationManager = manager;
    }
    
    /**
     * 獲取對話上下文（用於 Ollama API）
     * 類似DeepSeek的多輪對話上下文管理
     */
    private String getConversationContext() {
        if (conversationManager == null) {
            return null;
        }
        
        // 獲取最近5輪對話作為上下文（增加上下文長度以提升理解）
        List<ConversationManager.ConversationTurn> recentHistory = 
            conversationManager.getRecentHistory(5);
        
        if (recentHistory == null || recentHistory.isEmpty()) {
            return null;
        }
        
        // 構建更清晰的上下文格式
        StringBuilder context = new StringBuilder();
        context.append("以下是之前的對話歷史：\n\n");
        
        for (int i = 0; i < recentHistory.size(); i++) {
            ConversationManager.ConversationTurn turn = recentHistory.get(i);
            context.append("[").append(i + 1).append("] 用戶: ").append(turn.userInput).append("\n");
            if (turn.assistantResponse != null && !turn.assistantResponse.isEmpty()) {
                context.append("    助手: ").append(turn.assistantResponse).append("\n");
            }
            context.append("\n");
        }
        
        context.append("請基於以上對話歷史，理解上下文並給出合適的回應。");
        
        return context.toString().trim();
    }
    
    /**
     * 後處理回應，使其更自然
     */
    private String postProcessResponse(String response) {
        if (response == null || response.isEmpty()) {
            return response;
        }
        
        // 移除可能的AI標記或格式
        response = response.trim();
        
        // 移除常見的AI開頭詞
        String[] aiPrefixes = currentLanguage.equals("english") ? 
            new String[]{"As an AI", "I'm an AI", "As a language model"} :
            new String[]{"作為AI", "我是一個AI", "作為語言模型", "我係一個AI"};
        
        for (String prefix : aiPrefixes) {
            if (response.startsWith(prefix)) {
                // 找到第一個句號後開始
                int periodIndex = response.indexOf("。");
                if (periodIndex < 0) periodIndex = response.indexOf(".");
                if (periodIndex >= 0 && periodIndex < response.length() - 1) {
                    response = response.substring(periodIndex + 1).trim();
                }
            }
        }
        
        // 確保回應長度適中（允許更長的回應，類似DeepSeek）
        // 如果超過200字，在合適的位置截斷
        if (response.length() > 200) {
            // 嘗試在句號處截斷
            int lastPeriod = response.lastIndexOf("。", 180);
            if (lastPeriod < 0) lastPeriod = response.lastIndexOf(".", 180);
            if (lastPeriod < 0) lastPeriod = response.lastIndexOf("，", 180);
            if (lastPeriod < 0) lastPeriod = response.lastIndexOf(",", 180);
            
            if (lastPeriod > 0 && lastPeriod < 190) {
                response = response.substring(0, lastPeriod + 1);
            } else {
                // 如果找不到合適的截斷點，在180字處截斷
                response = response.substring(0, 177) + "...";
            }
        }
        
        return response;
    }
    
    /**
     * 初始化回應模板
     */
    private void initializeResponses() {
        // 粵語回應（更自然的表達）
        cantoneseResponses.put("天氣", new String[]{
            "係啊，今日天氣真係好好，好適合出街", "天氣真係唔錯，心情都好好", "係啊，天氣幾好，你有冇出街啊？"
        });
        cantoneseResponses.put("好", new String[]{
            "係啊，真係好好", "我都覺得係，真係唔錯", "同意，我都係咁諗"
        });
        cantoneseResponses.put("開心", new String[]{
            "聽到你開心我都好開心，希望你一直保持開心", "開心就好，保持呢個狀態", "好開心聽到你咁講"
        });
        cantoneseResponses.put("累", new String[]{
            "辛苦你啦，記得休息下，身體緊要", "要好好休息，唔好太勉強自己", "注意身體，累就休息下"
        });
        cantoneseResponses.put("謝謝", new String[]{
            "唔使客氣，應該嘅", "唔使多謝，有需要隨時搵我", "隨時可以幫你，唔使客氣"
        });
        cantoneseResponses.put("你好", new String[]{
            "你好，有咩可以幫到你？", "你好，我係你的助手，有咩可以幫手？", "你好，好高興同你傾計"
        });
        cantoneseResponses.put("再見", new String[]{
            "再見，有需要隨時叫我", "再見，保重，有咩事隨時搵我", "再見，祝你一切順利"
        });
        
        // 普通話回應（更自然的表達）
        mandarinResponses.put("天氣", new String[]{
            "是啊，今天天氣真的很好，很適合出門", "天氣真不錯，心情也變好了", "是啊，天氣挺好的，你有出門嗎？"
        });
        mandarinResponses.put("好", new String[]{
            "是啊，真的很好", "我也覺得是，真的不錯", "同意，我也是這麼想的"
        });
        mandarinResponses.put("開心", new String[]{
            "聽到你開心我也很開心，希望你一直保持開心", "開心就好，保持這個狀態", "很高興聽到你這麼說"
        });
        mandarinResponses.put("累", new String[]{
            "辛苦了，記得休息一下，身體重要", "要好好休息，別太勉強自己", "注意身體，累了就休息一下"
        });
        mandarinResponses.put("謝謝", new String[]{
            "不客氣，應該的", "不用謝，有需要隨時找我", "隨時可以幫你，不用客氣"
        });
        mandarinResponses.put("你好", new String[]{
            "你好，有什麼可以幫你的？", "你好，我是你的助手，有什麼可以幫忙的？", "你好，很高興和你聊天"
        });
        mandarinResponses.put("再見", new String[]{
            "再見，有需要隨時叫我", "再見，保重，有什麼事隨時找我", "再見，祝你一切順利"
        });
        
        // 英語回應（更自然的表達）
        englishResponses.put("weather", new String[]{
            "Yes, the weather is really nice today, perfect for going out", "The weather is great, it makes me feel good too", "Yes, the weather is good, did you go out?"
        });
        englishResponses.put("good", new String[]{
            "Yes, it's really good", "I think so too, it's really nice", "Agreed, I feel the same way"
        });
        englishResponses.put("happy", new String[]{
            "I'm glad to hear you're happy, I hope you stay happy", "That's great, keep that feeling", "So happy to hear that"
        });
        englishResponses.put("tired", new String[]{
            "You've worked hard, remember to rest, your health matters", "Take good care, don't push yourself too hard", "Take care of yourself, rest when you're tired"
        });
        englishResponses.put("thank", new String[]{
            "You're welcome, my pleasure", "No problem, feel free to ask anytime", "Anytime, don't hesitate to ask"
        });
        englishResponses.put("hello", new String[]{
            "Hello, how can I help you?", "Hello, I'm your assistant, what can I do for you?", "Hello, nice to chat with you"
        });
        englishResponses.put("bye", new String[]{
            "Goodbye, call me anytime you need", "Goodbye, take care, feel free to reach out", "Goodbye, all the best"
        });
    }
    
    /**
     * 生成回應（同步版本，使用關鍵詞匹配）
     * @param userText 用戶輸入的文本
     * @return 回應文本，如果無法生成則返回 null
     */
    public String generateResponse(String userText) {
        if (userText == null || userText.trim().isEmpty()) {
            return null;
        }
        
        String lowerText = userText.toLowerCase().trim();
        Map<String, String[]> responses = getResponsesForLanguage();
        
        // 檢查是否包含關鍵詞
        for (Map.Entry<String, String[]> entry : responses.entrySet()) {
            String keyword = entry.getKey();
            if (lowerText.contains(keyword)) {
                String[] templates = entry.getValue();
                if (templates != null && templates.length > 0) {
                    // 隨機選擇一個回應模板
                    String template = templates[random.nextInt(templates.length)];
                    
                    // 嘗試提取用戶話語中的關鍵信息並融入回應
                    String response = enhanceResponse(template, userText, keyword);
                    
                    Log.d(TAG, "生成回應: " + response + " (關鍵詞: " + keyword + ")");
                    return response;
                }
            }
        }
        
        // 如果沒有匹配的關鍵詞，生成通用回應
        return generateGenericResponse(userText);
    }
    
    /**
     * 生成回應（異步版本，使用 Ollama API）
     * @param userText 用戶輸入的文本
     * @param callback 回調接口
     */
    public void generateResponseAsync(String userText, ResponseCallback callback) {
        if (userText == null || userText.trim().isEmpty()) {
            callback.onResponse(null);
            return;
        }
        
        // 如果啟用了 Ollama 且客戶端可用，使用 Ollama API
        if (useOllama && ollamaClient != null) {
            Log.d(TAG, "使用 Ollama API 生成回應");
            
            // 獲取對話上下文（如果有的話）
            String context = getConversationContext();
            
            ollamaClient.generateResponse(userText, currentLanguage, context, new OllamaApiClient.OllamaCallback() {
                @Override
                public void onResponse(String response) {
                    Log.d(TAG, "Ollama API 回應: " + response);
                    // 後處理：確保回應自然
                    String processedResponse = postProcessResponse(response);
                    callback.onResponse(processedResponse);
                }
                
                @Override
                public void onError(String error) {
                    Log.w(TAG, "Ollama API 錯誤，回退到關鍵詞匹配: " + error);
                    // 回退到關鍵詞匹配
                    String fallbackResponse = generateResponse(userText);
                    callback.onResponse(fallbackResponse);
                }
            });
        } else {
            // 使用關鍵詞匹配
            Log.d(TAG, "使用關鍵詞匹配生成回應");
            String response = generateResponse(userText);
            callback.onResponse(response);
        }
    }
    
    /**
     * 回應回調接口
     */
    public interface ResponseCallback {
        void onResponse(String response);
    }
    
    /**
     * 增強回應，將用戶的話語融入回應中
     * 例如：用戶說「今天天氣很好」，回應「今天天氣真的很好」
     */
    private String enhanceResponse(String template, String userText, String keyword) {
        // 如果模板已經包含完整回應，直接返回
        if (template.length() > 15) {
            return template;
        }
        
        // 嘗試構建更自然的回應
        if (currentLanguage.equals("cantonese")) {
            // 粵語：在用戶話語基礎上添加肯定詞
            if (userText.contains("天氣")) {
                if (userText.contains("好")) {
                    // 「今天天氣很好」 -> 「今天天氣真的很好」
                    return userText.replace("天氣很好", "天氣真係好好")
                                   .replace("天氣好", "天氣真係好");
                }
            }
            if (userText.contains("好") && !userText.contains("真")) {
                // 添加「真係」
                return userText.replace("好", "真係好");
            }
        } else if (currentLanguage.equals("mandarin")) {
            // 普通話：在用戶話語基礎上添加肯定詞
            if (userText.contains("天氣")) {
                if (userText.contains("好")) {
                    // 「今天天氣很好」 -> 「今天天氣真的很好」
                    return userText.replace("天氣很好", "天氣真的很好")
                                   .replace("天氣好", "天氣真的很好");
                }
            }
            if (userText.contains("好") && !userText.contains("真的")) {
                // 添加「真的」
                return userText.replace("很好", "真的很好")
                               .replace("好", "真的很好");
            }
        } else {
            // 英語：類似處理
            if (userText.contains("weather") && userText.contains("good")) {
                return userText.replace("good", "really good")
                               .replace("nice", "really nice");
            }
        }
        
        // 如果無法增強，返回模板
        return template;
    }
    
    /**
     * 生成通用回應（當沒有匹配關鍵詞時）
     */
    private String generateGenericResponse(String userText) {
        Map<String, String> genericResponses = new HashMap<>();
        
        if (currentLanguage.equals("cantonese")) {
            // 更自然的通用回應
            String[] naturalResponses = {
                "我明白，繼續講", "係啊，我聽到", "嗯，我理解", "好，繼續"
            };
            if (userText.length() > 5) {
                // 嘗試融入用戶的話語，但更自然
                String lastPart = userText.length() > 15 ? 
                    userText.substring(userText.length() - 15) : userText;
                return "係啊，" + lastPart + "，我明白";
            }
            return naturalResponses[random.nextInt(naturalResponses.length)];
        } else if (currentLanguage.equals("mandarin")) {
            String[] naturalResponses = {
                "我明白，继续说吧", "是啊，我听到了", "嗯，我理解", "好，继续"
            };
            if (userText.length() > 5) {
                String lastPart = userText.length() > 15 ? 
                    userText.substring(userText.length() - 15) : userText;
                return "是啊，" + lastPart + "，我明白";
            }
            return naturalResponses[random.nextInt(naturalResponses.length)];
        } else {
            String[] naturalResponses = {
                "I understand, go on", "Yes, I heard you", "I see, continue", "Okay, go ahead"
            };
            if (userText.length() > 5) {
                String lastPart = userText.length() > 15 ? 
                    userText.substring(userText.length() - 15) : userText;
                return "Yes, " + lastPart + ", I understand";
            }
            return naturalResponses[random.nextInt(naturalResponses.length)];
        }
    }
    
    /**
     * 獲取當前語言的回應映射
     */
    private Map<String, String[]> getResponsesForLanguage() {
        switch (currentLanguage) {
            case "english":
                return englishResponses;
            case "mandarin":
                return mandarinResponses;
            case "cantonese":
            default:
                return cantoneseResponses;
        }
    }
    
    /**
     * 檢查是否應該生成回應
     * 某些情況下可能不需要回應（例如用戶只是在思考）
     */
    public boolean shouldRespond(String userText) {
        if (userText == null || userText.trim().isEmpty()) {
            return false;
        }
        
        // 如果太短（可能是誤識別）
        if (userText.trim().length() < 2) {
            return false;
        }
        
        // 如果只是語氣詞
        String[] fillerWords = currentLanguage.equals("english") ? 
            new String[]{"um", "uh", "ah", "oh"} :
            new String[]{"嗯", "啊", "哦", "呃"};
        
        for (String filler : fillerWords) {
            if (userText.trim().equalsIgnoreCase(filler)) {
                return false;
            }
        }
        
        return true;
    }
}

