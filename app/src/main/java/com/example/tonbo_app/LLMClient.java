package com.example.tonbo_app;

import android.content.Context;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * LLM Client for intelligent conversation
 * Supports DeepSeek and GLM-4-Flash APIs
 */
public class LLMClient {
    private static final String TAG = "LLMClient";
    
    private static LLMClient instance;
    private OkHttpClient httpClient;
    private Gson gson;
    private LLMConfig config;
    private Context context;
    
    // API endpoints
    private static final String DEEPSEEK_API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final String ZHIPU_API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
    
    /**
     * Get singleton instance
     */
    public static synchronized LLMClient getInstance(Context context) {
        if (instance == null) {
            instance = new LLMClient(context);
        }
        return instance;
    }
    
    private LLMClient(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new Gson();
        this.config = new LLMConfig(context);
        
        // Initialize HTTP client with timeout
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    }
    
    /**
     * Check if LLM is enabled
     */
    public boolean isEnabled() {
        return config.isEnabled();
    }
    
    /**
     * Get current provider
     */
    public String getProvider() {
        return config.getProvider();
    }
    
    /**
     * Send chat message to LLM
     * @param message User input text
     * @param history Conversation history (can be null)
     * @param callback Response callback
     */
    public void sendChatMessage(String message, List<ConversationManager.ConversationTurn> history, ChatCallback callback) {
        if (!config.isEnabled()) {
            callback.onError("LLM is disabled");
            return;
        }
        
        String provider = config.getProvider();
        String apiKey = config.getApiKey();
        
        if (apiKey == null || apiKey.isEmpty()) {
            callback.onError("API key not configured");
            return;
        }
        
        // Build request based on provider
        if ("deepseek".equals(provider)) {
            sendDeepSeekRequest(message, history, apiKey, callback);
        } else if ("zhipu".equals(provider)) {
            sendZhipuRequest(message, history, apiKey, callback);
        } else {
            callback.onError("Unknown provider: " + provider);
        }
    }
    
    // Store message and history for retry
    private String retryMessage;
    private List<ConversationManager.ConversationTurn> retryHistory;
    private ChatCallback retryCallback;
    
    /**
     * Send request to DeepSeek API
     */
    private void sendDeepSeekRequest(String message, List<ConversationManager.ConversationTurn> history, 
                                     String apiKey, ChatCallback callback) {
        // Store for potential retry
        retryMessage = message;
        retryHistory = history;
        retryCallback = callback;
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "deepseek-chat");
        requestBody.addProperty("temperature", 0.7);
        requestBody.addProperty("max_tokens", 1000);
        
        // Build messages array
        JsonArray messages = new JsonArray();
        
        // Add system message
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        String systemPrompt = buildSystemPrompt();
        systemMsg.addProperty("content", systemPrompt);
        messages.add(systemMsg);
        
        // Add conversation history
        if (history != null && !history.isEmpty()) {
            for (ConversationManager.ConversationTurn turn : history) {
                // User message
                JsonObject userMsg = new JsonObject();
                userMsg.addProperty("role", "user");
                userMsg.addProperty("content", turn.userInput);
                messages.add(userMsg);
                
                // Assistant message
                if (turn.assistantResponse != null && !turn.assistantResponse.isEmpty()) {
                    JsonObject assistantMsg = new JsonObject();
                    assistantMsg.addProperty("role", "assistant");
                    assistantMsg.addProperty("content", turn.assistantResponse);
                    messages.add(assistantMsg);
                }
            }
        }
        
        // Add current user message
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", message);
        messages.add(userMsg);
        
        requestBody.add("messages", messages);
        
        RequestBody body = RequestBody.create(
            requestBody.toString(),
            MediaType.parse("application/json; charset=utf-8")
        );
        
        Request request = new Request.Builder()
            .url(DEEPSEEK_API_URL)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer " + apiKey)
            .build();
        
        sendRequest(request, "deepseek", message, history, callback);
    }
    
    /**
     * Send request to Zhipu (GLM-4-Flash) API
     */
    private void sendZhipuRequest(String message, List<ConversationManager.ConversationTurn> history,
                                  String apiKey, ChatCallback callback) {
        // Store for potential retry
        retryMessage = message;
        retryHistory = history;
        retryCallback = callback;
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "glm-4-flash");
        requestBody.addProperty("temperature", 0.7);
        requestBody.addProperty("max_tokens", 1000);
        
        // Build messages array
        JsonArray messages = new JsonArray();
        
        // Add system message
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        String systemPrompt = buildSystemPrompt();
        systemMsg.addProperty("content", systemPrompt);
        messages.add(systemMsg);
        
        // Add conversation history
        if (history != null && !history.isEmpty()) {
            for (ConversationManager.ConversationTurn turn : history) {
                JsonObject userMsg = new JsonObject();
                userMsg.addProperty("role", "user");
                userMsg.addProperty("content", turn.userInput);
                messages.add(userMsg);
                
                if (turn.assistantResponse != null && !turn.assistantResponse.isEmpty()) {
                    JsonObject assistantMsg = new JsonObject();
                    assistantMsg.addProperty("role", "assistant");
                    assistantMsg.addProperty("content", turn.assistantResponse);
                    messages.add(assistantMsg);
                }
            }
        }
        
        // Add current user message
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", message);
        messages.add(userMsg);
        
        requestBody.add("messages", messages);
        
        RequestBody body = RequestBody.create(
            requestBody.toString(),
            MediaType.parse("application/json; charset=utf-8")
        );
        
        // Zhipu API uses different auth format
        Request request = new Request.Builder()
            .url(ZHIPU_API_URL)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer " + apiKey)
            .build();
        
        sendRequest(request, "zhipu", message, history, callback);
    }
    
    /**
     * Build system prompt based on current language
     */
    private String buildSystemPrompt() {
        // Get current language
        android.content.SharedPreferences prefs = context.getSharedPreferences(
            "AppSettings", Context.MODE_PRIVATE);
        String language = prefs.getString("current_language", "cantonese");
        
        if ("cantonese".equals(language)) {
            return "你是一個友善的語音助手，專為視障人士設計。請用廣東話回應，回答要簡潔自然，適合語音播報。";
        } else if ("mandarin".equals(language)) {
            return "你是一個友善的語音助手，專為視障人士設計。請用普通話回應，回答要簡潔自然，適合語音播報。";
        } else {
            return "You are a friendly voice assistant designed for visually impaired users. Please respond in English, keep answers concise and natural, suitable for voice broadcast.";
        }
    }
    
    /**
     * Send HTTP request
     */
    private void sendRequest(Request request, String currentProvider, String message,
                             List<ConversationManager.ConversationTurn> history,
                             ChatCallback callback) {
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "LLM API request failed", e);
                callback.onError("Network error: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorMsg = "HTTP " + response.code() + ": " + response.message();
                    String errorBody = response.body() != null ? response.body().string() : "";
                    Log.e(TAG, errorMsg + " - " + errorBody);
                    
                    // Auto-switch to GLM-4-Flash if DeepSeek has errors
                    if ("deepseek".equals(currentProvider) && (response.code() == 402 || response.code() == 401)) {
                        String reason = response.code() == 402 ? "餘額不足" : "API key 無效";
                        Log.w(TAG, "DeepSeek API " + reason + "，自動切換到 GLM-4-Flash");
                        config.useGLM4Flash();
                        // Retry with GLM-4-Flash using stored message and history
                        String retryMsg = retryMessage != null ? retryMessage : message;
                        List<ConversationManager.ConversationTurn> retryHist = retryHistory != null ? retryHistory : history;
                        sendZhipuRequest(retryMsg, retryHist, config.getApiKey(), callback);
                        return;
                    }

                    callback.onError(errorMsg);
                    return;
                }
                try {
                    String responseBody = response.body().string();
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                    
                    // Parse response (both APIs use similar format)
                    String aiResponse = null;
                    if (jsonResponse.has("choices")) {
                        JsonArray choices = jsonResponse.getAsJsonArray("choices");
                        if (choices.size() > 0) {
                            JsonObject choice = choices.get(0).getAsJsonObject();
                            if (choice.has("message")) {
                                JsonObject message = choice.getAsJsonObject("message");
                                if (message.has("content")) {
                                    aiResponse = message.get("content").getAsString();
                                }
                            }
                        }
                    }
                    
                    if (aiResponse != null && !aiResponse.isEmpty()) {
                        Log.d(TAG, "LLM response received: " + aiResponse);
                        // Clear retry data on success
                        retryMessage = null;
                        retryHistory = null;
                        retryCallback = null;
                        callback.onResponse(aiResponse);
                    } else {
                        Log.w(TAG, "Empty response from LLM");
                        callback.onError("Empty response from server");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse LLM response", e);
                    callback.onError("Failed to parse response: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Test connection to LLM API
     */
    public void testConnection(ConnectionCallback callback) {
        if (!config.isEnabled()) {
            callback.onResult(false, "LLM is disabled");
            return;
        }
        
        sendChatMessage("你好", null, new ChatCallback() {
            @Override
            public void onResponse(String response) {
                callback.onResult(true, "Connection successful");
            }
            
            @Override
            public void onError(String error) {
                callback.onResult(false, "Connection failed: " + error);
            }
        });
    }
    
    /**
     * Chat callback interface
     */
    public interface ChatCallback {
        void onResponse(String response);
        void onError(String error);
    }
    
    /**
     * Connection test callback interface
     */
    public interface ConnectionCallback {
        void onResult(boolean success, String message);
    }
    
    /**
     * Release resources
     */
    public void release() {
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
        }
    }
}

