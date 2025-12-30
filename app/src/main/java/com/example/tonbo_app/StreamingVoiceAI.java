package com.example.tonbo_app;

import android.content.Context;
import android.content.Intent;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

/**
 * 流式語音AI對話系統
 * 支持連續對話、實時識別、即時響應
 */
public class StreamingVoiceAI {
    private static final String TAG = "StreamingVoiceAI";
    
    private Context context;
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private VoiceAIAssistant aiAssistant;
    private TTSManager ttsManager;
    private VibrationManager vibrationManager;
    
    private Handler mainHandler;
    private boolean isListening = false;
    private boolean isProcessing = false;
    private String currentLanguage = "cantonese";
    
    // 對話歷史
    private List<String> conversationHistory = new ArrayList<>();
    
    // 回調接口
    public interface StreamingAICallback {
        void onPartialText(String partialText);
        void onFinalText(String finalText);
        void onAIResponse(String response);
        void onError(String error);
        void onListeningStateChanged(boolean isListening);
    }
    
    private StreamingAICallback callback;
    
    public StreamingVoiceAI(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.aiAssistant = new VoiceAIAssistant(context);
        this.ttsManager = TTSManager.getInstance(context);
        this.vibrationManager = VibrationManager.getInstance(context);
        
        initializeSpeechRecognizer();
    }
    
    /**
     * 初始化語音識別器
     */
    private void initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true); // 啟用部分結果
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
            
            // 設置連續識別模式
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500);
        }
    }
    
    /**
     * 設置回調
     */
    public void setCallback(StreamingAICallback callback) {
        this.callback = callback;
    }
    
    /**
     * 設置語言
     */
    public void setLanguage(String language) {
        this.currentLanguage = language;
        if (aiAssistant != null) {
            aiAssistant.setLanguage(language);
        }
        if (ttsManager != null) {
            ttsManager.changeLanguage(language);
        }
        
        // 更新識別語言
        if (recognizerIntent != null) {
            String locale = getLocaleForLanguage(language);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale);
        }
    }
    
    /**
     * 獲取語言代碼
     */
    private String getLocaleForLanguage(String language) {
        switch (language) {
            case "cantonese":
                return "zh-HK";
            case "mandarin":
                return "zh-CN";
            case "english":
                return "en-US";
            default:
                return "zh-HK";
        }
    }
    
    /**
     * 開始連續對話
     */
    public void startContinuousConversation() {
        if (isListening) {
            Log.w(TAG, "已經在對話中");
            return;
        }
        
        if (speechRecognizer == null) {
            initializeSpeechRecognizer();
        }
        
        if (speechRecognizer == null) {
            if (callback != null) {
                callback.onError("語音識別不可用");
            }
            return;
        }
        
        isListening = true;
        isProcessing = false;
        
        // 設置識別監聽器
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d(TAG, "準備接收語音");
                if (callback != null) {
                    callback.onListeningStateChanged(true);
                }
            }
            
            @Override
            public void onBeginningOfSpeech() {
                Log.d(TAG, "開始說話");
                isProcessing = true;
            }
            
            @Override
            public void onRmsChanged(float rmsdB) {
                // 音量變化，可用於視覺反饋
            }
            
            @Override
            public void onBufferReceived(byte[] buffer) {
                // 接收緩衝區數據
            }
            
            @Override
            public void onEndOfSpeech() {
                Log.d(TAG, "說話結束");
            }
            
            @Override
            public void onError(int error) {
                Log.e(TAG, "語音識別錯誤: " + getErrorText(error));
                isListening = false;
                isProcessing = false;
                
                if (callback != null) {
                    callback.onError(getErrorText(error));
                    callback.onListeningStateChanged(false);
                }
                
                // 自動重啟（某些錯誤可以重試）
                if (error == SpeechRecognizer.ERROR_NO_MATCH || 
                    error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    // 延遲後重啟
                    mainHandler.postDelayed(() -> {
                        if (!isListening) {
                            startContinuousConversation();
                        }
                    }, 1000);
                }
            }
            
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String recognizedText = matches.get(0);
                    Log.d(TAG, "識別結果: " + recognizedText);
                    
                    // 處理最終識別結果
                    processRecognizedText(recognizedText);
                }
                
                isProcessing = false;
                
                // 自動繼續監聽（連續對話模式）
                if (isListening) {
                    mainHandler.postDelayed(() -> {
                        if (isListening && speechRecognizer != null) {
                            speechRecognizer.startListening(recognizerIntent);
                        }
                    }, 500);
                }
            }
            
            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String partialText = matches.get(0);
                    Log.d(TAG, "部分結果: " + partialText);
                    
                    // 實時顯示部分結果
                    if (callback != null) {
                        callback.onPartialText(partialText);
                    }
                }
            }
            
            @Override
            public void onEvent(int eventType, Bundle params) {
                // 其他事件
            }
        });
        
        // 開始監聽
        speechRecognizer.startListening(recognizerIntent);
        Log.d(TAG, "開始連續對話");
    }
    
    /**
     * 停止對話
     */
    public void stopConversation() {
        isListening = false;
        
        if (speechRecognizer != null) {
            try {
                speechRecognizer.stopListening();
            } catch (Exception e) {
                Log.e(TAG, "停止監聽失敗: " + e.getMessage());
            }
        }
        
        if (callback != null) {
            callback.onListeningStateChanged(false);
        }
        
        Log.d(TAG, "停止連續對話");
    }
    
    /**
     * 處理識別到的文本
     */
    private void processRecognizedText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        
        mainHandler.post(() -> {
            // 更新UI顯示識別結果
            if (callback != null) {
                callback.onFinalText(text);
            }
            
            // 添加到對話歷史
            conversationHistory.add("用戶: " + text);
            
            // 生成AI回應
            generateAIResponse(text);
        });
    }
    
    /**
     * 生成AI回應
     */
    private void generateAIResponse(String userInput) {
        if (aiAssistant == null) {
            return;
        }
        
        // 使用異步方式生成回應
        aiAssistant.processInputAsync(userInput, new VoiceAIAssistant.AssistantResponseCallback() {
            @Override
            public void onResponse(VoiceAIAssistant.AssistantResponse response) {
                String aiResponse = response.response;
                if (aiResponse != null && !aiResponse.isEmpty()) {
                    // 添加到對話歷史
                    conversationHistory.add("AI: " + aiResponse);
                    
                    // 通過回調返回回應
                    if (callback != null) {
                        callback.onAIResponse(aiResponse);
                    }
                    
                    // 語音播報回應
                    speakResponse(aiResponse);
                }
            }
        });
    }
    
    /**
     * 語音播報回應
     */
    private void speakResponse(String response) {
        if (ttsManager == null || response == null || response.isEmpty()) {
            return;
        }
        
        // 停止當前播報（如果有的話）
        ttsManager.stopSpeaking();
        
        // 播報新回應
        String englishText = translateToEnglish(response);
        ttsManager.speak(response, englishText, true);
        
        // 震動反饋
        vibrationManager.vibrateNotification();
    }
    
    /**
     * 簡單的翻譯（用於TTS）
     */
    private String translateToEnglish(String text) {
        // 簡單的關鍵詞翻譯
        if (text.contains("你好")) return "Hello";
        if (text.contains("再見")) return "Goodbye";
        if (text.contains("謝謝")) return "Thank you";
        return text;
    }
    
    /**
     * 獲取錯誤文本
     */
    private String getErrorText(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "音頻錯誤";
            case SpeechRecognizer.ERROR_CLIENT:
                return "客戶端錯誤";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "權限不足";
            case SpeechRecognizer.ERROR_NETWORK:
                return "網絡錯誤";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "網絡超時";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "無法識別";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "識別器忙碌";
            case SpeechRecognizer.ERROR_SERVER:
                return "服務器錯誤";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "語音超時";
            default:
                return "未知錯誤";
        }
    }
    
    /**
     * 獲取對話歷史
     */
    public List<String> getConversationHistory() {
        return new ArrayList<>(conversationHistory);
    }
    
    /**
     * 清除對話歷史
     */
    public void clearHistory() {
        conversationHistory.clear();
    }
    
    /**
     * 釋放資源
     */
    public void release() {
        stopConversation();
        
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }
    
    /**
     * 檢查是否正在對話
     */
    public boolean isConversing() {
        return isListening;
    }
}

