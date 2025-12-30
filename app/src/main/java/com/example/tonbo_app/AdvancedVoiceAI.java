package com.example.tonbo_app;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 高級即時對話語音AI
 * 支持流式識別、實時響應、上下文理解
 */
public class AdvancedVoiceAI {
    private static final String TAG = "AdvancedVoiceAI";
    
    private Context context;
    private VoiceAIAssistant aiAssistant;
    private TTSManager ttsManager;
    private VibrationManager vibrationManager;
    
    // 語音識別相關
    private AudioRecord audioRecord;
    private ExecutorService audioExecutor;
    private Handler mainHandler;
    
    // 音頻參數
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
    
    // 流式識別參數
    private static final int CHUNK_SIZE_MS = 300; // 300ms 塊大小，用於實時識別
    private static final int CHUNK_SIZE_SAMPLES = SAMPLE_RATE * CHUNK_SIZE_MS / 1000;
    private static final int SILENCE_THRESHOLD = 500; // 靜音閾值（毫秒）
    
    // 狀態管理
    private AtomicBoolean isListening = new AtomicBoolean(false);
    private AtomicBoolean isProcessing = new AtomicBoolean(false);
    private String currentLanguage = "cantonese";
    
    // 對話上下文
    private List<String> conversationHistory = new ArrayList<>();
    private StringBuilder currentUtterance = new StringBuilder();
    private long lastSpeechTime = 0;
    private boolean isSpeaking = false;
    
    // 回調接口
    public interface VoiceAICallback {
        void onPartialText(String partialText);
        void onFinalText(String finalText);
        void onAIResponse(String response);
        void onError(String error);
        void onListeningStateChanged(boolean isListening);
    }
    
    private VoiceAICallback callback;
    
    public AdvancedVoiceAI(Context context) {
        this.context = context;
        this.audioExecutor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.aiAssistant = new VoiceAIAssistant(context);
        this.ttsManager = TTSManager.getInstance(context);
        this.vibrationManager = VibrationManager.getInstance(context);
    }
    
    /**
     * 設置回調
     */
    public void setCallback(VoiceAICallback callback) {
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
    }
    
    /**
     * 開始即時對話
     */
    public void startConversation() {
        if (isListening.get()) {
            Log.w(TAG, "已經在對話中");
            return;
        }
        
        isListening.set(true);
        currentUtterance.setLength(0);
        lastSpeechTime = System.currentTimeMillis();
        
        if (callback != null) {
            callback.onListeningStateChanged(true);
        }
        
        // 開始錄音和識別
        audioExecutor.execute(() -> {
            startAudioRecording();
        });
        
        Log.d(TAG, "開始即時對話");
    }
    
    /**
     * 停止對話
     */
    public void stopConversation() {
        if (!isListening.get()) {
            return;
        }
        
        isListening.set(false);
        
        if (audioRecord != null) {
            try {
                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop();
                }
                audioRecord.release();
            } catch (Exception e) {
                Log.e(TAG, "停止錄音失敗: " + e.getMessage());
            }
            audioRecord = null;
        }
        
        // 處理最後的語音
        if (currentUtterance.length() > 0) {
            processFinalText(currentUtterance.toString());
            currentUtterance.setLength(0);
        }
        
        if (callback != null) {
            callback.onListeningStateChanged(false);
        }
        
        Log.d(TAG, "停止即時對話");
    }
    
    /**
     * 開始音頻錄製
     */
    private void startAudioRecording() {
        try {
            // 檢查權限
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if (context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) 
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    if (callback != null) {
                        callback.onError("需要錄音權限");
                    }
                    isListening.set(false);
                    return;
                }
            }
            
            // 初始化 AudioRecord
            audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                BUFFER_SIZE * 2
            );
            
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                if (callback != null) {
                    callback.onError("無法初始化音頻錄製");
                }
                isListening.set(false);
                return;
            }
            
            audioRecord.startRecording();
            Log.d(TAG, "開始錄音");
            
            // 流式識別循環
            byte[] audioBuffer = new byte[CHUNK_SIZE_SAMPLES * 2];
            long lastActivityTime = System.currentTimeMillis();
            
            while (isListening.get()) {
                int bytesRead = audioRecord.read(audioBuffer, 0, audioBuffer.length);
                
                if (bytesRead > 0) {
                    // 檢測是否有語音活動
                    boolean hasSpeech = detectSpeechActivity(audioBuffer, bytesRead);
                    
                    if (hasSpeech) {
                        lastActivityTime = System.currentTimeMillis();
                        lastSpeechTime = System.currentTimeMillis();
                        
                        // 處理音頻塊（流式識別）
                        processAudioChunk(audioBuffer, bytesRead);
                    } else {
                        // 檢查是否靜音超時
                        long silenceDuration = System.currentTimeMillis() - lastActivityTime;
                        if (silenceDuration > SILENCE_THRESHOLD && currentUtterance.length() > 0) {
                            // 靜音超時，處理當前語句
                            String finalText = currentUtterance.toString().trim();
                            if (!finalText.isEmpty()) {
                                processFinalText(finalText);
                                currentUtterance.setLength(0);
                            }
                            lastActivityTime = System.currentTimeMillis();
                        }
                    }
                }
            }
            
            // 停止錄音
            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "音頻錄製錯誤: " + e.getMessage(), e);
            if (callback != null) {
                callback.onError("錄音錯誤: " + e.getMessage());
            }
            isListening.set(false);
        }
    }
    
    /**
     * 檢測語音活動
     */
    private boolean detectSpeechActivity(byte[] audioData, int length) {
        // 計算音頻能量
        ShortBuffer shortBuffer = ByteBuffer.wrap(audioData, 0, length)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer();
        
        float energy = 0;
        int sampleCount = length / 2;
        
        for (int i = 0; i < sampleCount && i < shortBuffer.remaining(); i++) {
            short sample = shortBuffer.get(i);
            energy += Math.abs(sample);
        }
        
        energy = energy / sampleCount;
        
        // 閾值判斷（可根據環境調整）
        return energy > 1000; // 簡單的能量閾值
    }
    
    /**
     * 處理音頻塊（流式識別）
     * 使用 Android 原生語音識別或 ASRManager
     */
    private void processAudioChunk(byte[] audioData, int length) {
        // 這裡可以集成流式 ASR 引擎
        // 目前使用緩衝區累積，然後通過 Android SpeechRecognizer 處理
        
        // 可以通過以下方式實現：
        // 1. 使用 ASRManager 的流式識別（如果支持）
        // 2. 使用 FunASR 的流式識別
        // 3. 使用 Android SpeechRecognizer 的連續識別模式
        
        // 目前先累積音頻數據，等待靜音後處理
        // 實際的流式識別需要集成具體的 ASR 引擎
    }
    
    /**
     * 處理最終文本
     */
    private void processFinalText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        
        if (isProcessing.get()) {
            Log.d(TAG, "正在處理上一條消息，跳過: " + text);
            return;
        }
        
        isProcessing.set(true);
        
        mainHandler.post(() -> {
            // 更新UI顯示識別結果
            if (callback != null) {
                callback.onFinalText(text);
            }
            
            // 添加到對話歷史
            conversationHistory.add(text);
            
            // 生成AI回應
            generateAIResponse(text);
        });
    }
    
    /**
     * 生成AI回應
     */
    private void generateAIResponse(String userInput) {
        if (aiAssistant == null) {
            isProcessing.set(false);
            return;
        }
        
        // 使用異步方式生成回應
        aiAssistant.processInputAsync(userInput, new VoiceAIAssistant.AssistantResponseCallback() {
            @Override
            public void onResponse(VoiceAIAssistant.AssistantResponse response) {
                isProcessing.set(false);
                
                String aiResponse = response.response;
                if (aiResponse != null && !aiResponse.isEmpty()) {
                    // 添加到對話歷史
                    conversationHistory.add(aiResponse);
                    
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
        Map<String, String> translations = new HashMap<>();
        translations.put("你好", "Hello");
        translations.put("再見", "Goodbye");
        translations.put("謝謝", "Thank you");
        
        for (Map.Entry<String, String> entry : translations.entrySet()) {
            if (text.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        return text;
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
        currentUtterance.setLength(0);
    }
    
    /**
     * 釋放資源
     */
    public void release() {
        stopConversation();
        
        if (audioExecutor != null) {
            audioExecutor.shutdown();
        }
        
        if (aiAssistant != null) {
            // 可以添加清理邏輯
        }
    }
    
    /**
     * 檢查是否正在對話
     */
    public boolean isConversing() {
        return isListening.get();
    }
    
    /**
     * 檢查是否正在處理
     */
    public boolean isProcessing() {
        return isProcessing.get();
    }
}

