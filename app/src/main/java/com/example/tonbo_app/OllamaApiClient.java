package com.example.tonbo_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Ollama API 客戶端
 * 用於與本地或遠程的 Ollama 服務進行通信
 */
public class OllamaApiClient {
    private static final String TAG = "OllamaApiClient";
    private static final String PREFS_NAME = "ollama_config";
    private static final String KEY_API_URL = "ollama_api_url";
    private static final String KEY_MODEL = "ollama_model";
    
    // 默認配置
    private static final String DEFAULT_API_URL = "http://localhost:11434";
    // 推薦使用更強大的模型以獲得類似DeepSeek的效果
    // 可選模型：qwen2.5:7b, qwen2.5:14b, deepseek-r1:7b, llama3.1:8b 等
    private static final String DEFAULT_MODEL = "qwen2.5:7b"; // 支持中文的模型
    
    private OkHttpClient httpClient;
    private Gson gson;
    private String apiUrl;
    private String model;
    private Context context;
    
    public interface OllamaCallback {
        void onResponse(String response);
        void onError(String error);
    }
    
    public OllamaApiClient(Context context) {
        this.context = context;
        this.gson = new Gson();
        
        // 創建 HTTP 客戶端，設置超時
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
        
        // 從 SharedPreferences 加載配置
        loadConfig();
    }
    
    /**
     * 從 SharedPreferences 加載配置
     */
    private void loadConfig() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.apiUrl = prefs.getString(KEY_API_URL, DEFAULT_API_URL);
        this.model = prefs.getString(KEY_MODEL, DEFAULT_MODEL);
        Log.d(TAG, "加載配置 - API URL: " + apiUrl + ", Model: " + model);
    }
    
    /**
     * 保存配置到 SharedPreferences
     */
    public void saveConfig(String apiUrl, String model) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putString(KEY_API_URL, apiUrl)
            .putString(KEY_MODEL, model)
            .apply();
        
        this.apiUrl = apiUrl;
        this.model = model;
        Log.d(TAG, "保存配置 - API URL: " + apiUrl + ", Model: " + model);
    }
    
    /**
     * 獲取當前 API URL
     */
    public String getApiUrl() {
        return apiUrl;
    }
    
    /**
     * 獲取當前模型
     */
    public String getModel() {
        return model;
    }
    
    /**
     * 生成回應（異步）
     * @param userMessage 用戶消息
     * @param language 語言（用於設置系統提示）
     * @param callback 回調接口
     */
    public void generateResponse(String userMessage, String language, OllamaCallback callback) {
        generateResponse(userMessage, language, null, callback);
    }
    
    /**
     * 生成回應（異步，帶對話上下文）
     * @param userMessage 用戶消息
     * @param language 語言（用於設置系統提示）
     * @param conversationContext 對話上下文（最近幾輪對話）
     * @param callback 回調接口
     */
    public void generateResponse(String userMessage, String language, String conversationContext, OllamaCallback callback) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            callback.onError("用戶消息為空");
            return;
        }
        
        // 構建系統提示（根據語言）
        String systemPrompt = buildSystemPrompt(language);
        
        // 構建完整的提示（包含上下文）
        // 使用類似DeepSeek的提示格式
        String fullPrompt;
        if (conversationContext != null && !conversationContext.trim().isEmpty()) {
            // 如果有上下文，使用更結構化的格式
            fullPrompt = conversationContext + "\n\n---\n\n當前對話：\n用戶: " + userMessage + "\n助手:";
        } else {
            // 沒有上下文時，直接使用用戶消息
            fullPrompt = userMessage;
        }
        
        // 構建請求 JSON
        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("model", model);
        requestJson.addProperty("prompt", fullPrompt);
        requestJson.addProperty("system", systemPrompt);
        requestJson.addProperty("stream", false); // 不使用流式輸出
        
        // 添加模型參數以提升回答質量（類似DeepSeek）
        requestJson.addProperty("temperature", 0.7); // 平衡創造性和準確性
        requestJson.addProperty("top_p", 0.9); // 核採樣，提高回答質量
        requestJson.addProperty("top_k", 40); // Top-K採樣
        requestJson.addProperty("repeat_penalty", 1.1); // 減少重複
        requestJson.addProperty("num_predict", 200); // 最大生成長度
        
        // 構建請求
        String url = apiUrl + "/api/generate";
        RequestBody body = RequestBody.create(
            requestJson.toString(),
            MediaType.parse("application/json; charset=utf-8")
        );
        
        Request request = new Request.Builder()
            .url(url)
            .post(body)
            .build();
        
        // 異步執行請求
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Ollama API 請求失敗", e);
                callback.onError("無法連接到 Ollama 服務: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "未知錯誤";
                    Log.e(TAG, "Ollama API 響應錯誤: " + response.code() + " - " + errorBody);
                    callback.onError("API 錯誤: " + response.code() + " - " + errorBody);
                    return;
                }
                
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Ollama API 響應: " + responseBody);
                    
                    // 解析 JSON 響應
                    JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                    
                    if (jsonResponse.has("response")) {
                        String aiResponse = jsonResponse.get("response").getAsString();
                        callback.onResponse(aiResponse.trim());
                    } else {
                        callback.onError("響應格式錯誤: 缺少 'response' 字段");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析 Ollama API 響應失敗", e);
                    callback.onError("解析響應失敗: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * 構建系統提示（根據語言）
     * 優化為類似DeepSeek的專業、智能、準確的AI助手風格
     */
    private String buildSystemPrompt(String language) {
        switch (language) {
            case "english":
                return "You are an intelligent AI assistant similar to DeepSeek, designed to provide accurate, helpful, and thoughtful responses. " +
                       "Your responses should be: " +
                       "1. Accurate and well-informed - provide correct information based on knowledge. " +
                       "2. Clear and concise - explain complex topics in an understandable way. " +
                       "3. Context-aware - understand the conversation history and respond accordingly. " +
                       "4. Helpful and practical - offer actionable advice when appropriate. " +
                       "5. Natural and conversational - maintain a friendly tone while being professional. " +
                       "Keep responses concise (under 100 words) but comprehensive. " +
                       "For visually impaired users, be clear, descriptive, and considerate. " +
                       "Think step by step when needed, and provide reasoning when helpful.";
            case "mandarin":
                return "你是一個智能AI助手，類似DeepSeek，旨在提供準確、有幫助且深思熟慮的回答。 " +
                       "你的回答應該： " +
                       "1. 準確且信息豐富 - 基於知識提供正確信息。 " +
                       "2. 清晰簡潔 - 用易懂的方式解釋複雜話題。 " +
                       "3. 上下文感知 - 理解對話歷史並相應回應。 " +
                       "4. 實用且可操作 - 在適當時候提供可行建議。 " +
                       "5. 自然且對話式 - 保持友好語調同時專業。 " +
                       "保持簡潔（100字以內）但全面。 " +
                       "對於視障用戶，要清晰、描述性且體貼。 " +
                       "需要時逐步思考，並在有用時提供推理過程。";
            case "cantonese":
            default:
                return "你係一個智能AI助手，類似DeepSeek，旨在提供準確、有幫助且深思熟慮嘅回答。 " +
                       "你嘅回答應該： " +
                       "1. 準確且信息豐富 - 基於知識提供正確信息。 " +
                       "2. 清晰簡潔 - 用易懂嘅方式解釋複雜話題。 " +
                       "3. 上下文感知 - 理解對話歷史並相應回應。 " +
                       "4. 實用且可操作 - 在適當時候提供可行建議。 " +
                       "5. 自然且對話式 - 保持友好語調同時專業。 " +
                       "保持簡潔（100字以內）但全面。 " +
                       "對於視障用戶，要清晰、描述性且體貼。 " +
                       "需要時逐步思考，並在有用時提供推理過程。";
        }
    }
    
    /**
     * 測試連接
     * @param callback 回調接口
     */
    public void testConnection(OllamaCallback callback) {
        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("model", model);
        requestJson.addProperty("prompt", "Hello");
        requestJson.addProperty("stream", false);
        
        String url = apiUrl + "/api/generate";
        RequestBody body = RequestBody.create(
            requestJson.toString(),
            MediaType.parse("application/json; charset=utf-8")
        );
        
        Request request = new Request.Builder()
            .url(url)
            .post(body)
            .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("連接失敗: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    callback.onResponse("連接成功");
                } else {
                    callback.onError("連接失敗: HTTP " + response.code());
                }
            }
        });
    }
}

