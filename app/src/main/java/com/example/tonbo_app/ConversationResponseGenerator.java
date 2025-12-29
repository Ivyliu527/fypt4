package com.example.tonbo_app;

import android.util.Log;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 對話回應生成器
 * 根據用戶的語音輸入生成智能回應
 */
public class ConversationResponseGenerator {
    private static final String TAG = "ConversationResponseGenerator";
    
    private String currentLanguage = "cantonese";
    private Random random = new Random();
    
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
     * 設置語言
     */
    public void setLanguage(String language) {
        this.currentLanguage = language;
    }
    
    /**
     * 初始化回應模板
     */
    private void initializeResponses() {
        // 粵語回應
        cantoneseResponses.put("天氣", new String[]{
            "係啊，今日天氣真係好好", "天氣真係唔錯", "係啊，天氣幾好"
        });
        cantoneseResponses.put("好", new String[]{
            "係啊，真係好好", "我都覺得係", "同意"
        });
        cantoneseResponses.put("開心", new String[]{
            "聽到你開心我都好開心", "開心就好", "保持開心"
        });
        cantoneseResponses.put("累", new String[]{
            "辛苦你啦，記得休息下", "要好好休息", "注意身體"
        });
        cantoneseResponses.put("謝謝", new String[]{
            "唔使客氣", "應該嘅", "隨時可以幫你"
        });
        cantoneseResponses.put("你好", new String[]{
            "你好，有咩可以幫到你", "你好，我係你的助手", "你好，很高興為你服務"
        });
        cantoneseResponses.put("再見", new String[]{
            "再見，有需要隨時叫我", "再見，保重", "再見，祝你一切順利"
        });
        
        // 普通話回應
        mandarinResponses.put("天氣", new String[]{
            "是啊，今天天氣真的很好", "天氣真不錯", "是啊，天氣挺好的"
        });
        mandarinResponses.put("好", new String[]{
            "是啊，真的很好", "我也覺得是", "同意"
        });
        mandarinResponses.put("開心", new String[]{
            "聽到你開心我也很開心", "開心就好", "保持開心"
        });
        mandarinResponses.put("累", new String[]{
            "辛苦了，記得休息一下", "要好好休息", "注意身體"
        });
        mandarinResponses.put("謝謝", new String[]{
            "不客氣", "應該的", "隨時可以幫你"
        });
        mandarinResponses.put("你好", new String[]{
            "你好，有什麼可以幫你的", "你好，我是你的助手", "你好，很高興為你服務"
        });
        mandarinResponses.put("再見", new String[]{
            "再見，有需要隨時叫我", "再見，保重", "再見，祝你一切順利"
        });
        
        // 英語回應
        englishResponses.put("weather", new String[]{
            "Yes, the weather is really nice today", "The weather is great", "Yes, the weather is good"
        });
        englishResponses.put("good", new String[]{
            "Yes, it's really good", "I think so too", "Agreed"
        });
        englishResponses.put("happy", new String[]{
            "I'm glad to hear you're happy", "That's great", "Stay happy"
        });
        englishResponses.put("tired", new String[]{
            "You've worked hard, remember to rest", "Take good care", "Take care of yourself"
        });
        englishResponses.put("thank", new String[]{
            "You're welcome", "My pleasure", "I'm here to help anytime"
        });
        englishResponses.put("hello", new String[]{
            "Hello, how can I help you", "Hello, I'm your assistant", "Hello, nice to serve you"
        });
        englishResponses.put("bye", new String[]{
            "Goodbye, call me anytime you need", "Goodbye, take care", "Goodbye, all the best"
        });
    }
    
    /**
     * 生成回應
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
            genericResponses.put("default", "我聽到了，有咩可以幫你");
            // 如果用戶話語較長，嘗試提取關鍵信息
            if (userText.length() > 5) {
                // 提取最後幾個字作為回應基礎
                String lastPart = userText.length() > 10 ? 
                    userText.substring(userText.length() - 10) : userText;
                return "係啊，" + lastPart;
            }
        } else if (currentLanguage.equals("mandarin")) {
            genericResponses.put("default", "我聽到了，有什麼可以幫你");
            if (userText.length() > 5) {
                String lastPart = userText.length() > 10 ? 
                    userText.substring(userText.length() - 10) : userText;
                return "是啊，" + lastPart;
            }
        } else {
            genericResponses.put("default", "I heard you, how can I help");
            if (userText.length() > 5) {
                String lastPart = userText.length() > 10 ? 
                    userText.substring(userText.length() - 10) : userText;
                return "Yes, " + lastPart;
            }
        }
        
        return genericResponses.get("default");
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

