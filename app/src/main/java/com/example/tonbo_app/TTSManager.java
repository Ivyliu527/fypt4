package com.example.tonbo_app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TTSManager {
    private static final String TAG = "TTSManager";
    private static TTSManager instance;
    
    private TextToSpeech textToSpeech;
    private Context context;
    private String currentLanguage = "english";
    private Locale cantoneseLocale;
    private boolean isInitialized = false;
    private boolean isSpeaking = false;
    private boolean isInitializing = false;
    
    // 語音隊列管理
    private ConcurrentLinkedQueue<String> speechQueue = new ConcurrentLinkedQueue<>();
    private Handler handler = new Handler(Looper.getMainLooper());
    
    private TTSManager(Context context) {
        this.context = context.getApplicationContext();
        cantoneseLocale = new Locale("zh", "HK");
        // 不在構造函數中初始化TTS，等待第一次使用時初始化
    }
    
    private void ensureTTSInitialized() {
        if (textToSpeech == null && !isInitializing) {
            initTTS();
        }
    }
    
    public static synchronized TTSManager getInstance(Context context) {
        if (instance == null) {
            instance = new TTSManager(context);
        }
        return instance;
    }
    
    private void initTTS() {
        if (isInitializing) {
            Log.d(TAG, "TTS正在初始化中...");
            return;
        }
        
        isInitializing = true;
        Log.d(TAG, "開始初始化TTS引擎...");
        
        textToSpeech = new TextToSpeech(context, status -> {
            isInitializing = false;
            if (status == TextToSpeech.SUCCESS) {
                setLanguage(currentLanguage);
                isInitialized = true;
                Log.d(TAG, "✅ TTS初始化成功");
            } else {
                Log.e(TAG, "❌ TTS初始化失敗");
                isInitialized = false;
            }
        });
    }
    
    private void setLanguage(String language) {
        if (textToSpeech == null) return;
        
        int result = TextToSpeech.LANG_MISSING_DATA;
        
        switch (language) {
            case "cantonese":
                // 優先使用香港廣東話 (zh-HK)
                result = textToSpeech.setLanguage(cantoneseLocale);
                
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "⚠️ 廣東話不支持，嘗試使用台灣國語");
                    result = textToSpeech.setLanguage(Locale.TAIWAN);
                    
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w(TAG, "⚠️ 台灣國語不支持，使用繁體中文");
                        result = textToSpeech.setLanguage(Locale.TRADITIONAL_CHINESE);
                    }
                }
                break;
                
            case "english":
                result = textToSpeech.setLanguage(Locale.ENGLISH);
                break;
                
            case "mandarin":
            default:
                result = textToSpeech.setLanguage(Locale.SIMPLIFIED_CHINESE);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    result = textToSpeech.setLanguage(Locale.TRADITIONAL_CHINESE);
                }
                break;
        }
        
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e(TAG, "❌ 語言不支持: " + language);
        } else {
            Log.d(TAG, "✅ TTS語言設置成功: " + language);
        }
    }
    
    // 靜默設置語言，不播報語言切換信息
    public void setLanguageSilently(String language) {
        currentLanguage = language;
        setLanguage(language);
        Log.d(TAG, "語言已靜默設置為: " + language);
    }
    
    /**
     * 設置語音速度
     */
    public void setSpeechRate(float rate) {
        if (textToSpeech != null) {
            textToSpeech.setSpeechRate(rate);
            Log.d(TAG, "語音速度已設置為: " + rate);
        }
    }
    
    /**
     * 設置語音音調
     */
    public void setSpeechPitch(float pitch) {
        if (textToSpeech != null) {
            textToSpeech.setPitch(pitch);
            Log.d(TAG, "語音音調已設置為: " + pitch);
        }
    }
    
    /**
     * 設置語音音量（注意：Android TTS沒有直接的音量控制）
     */
    public void setSpeechVolume(float volume) {
        if (textToSpeech != null) {
            // Android TTS音量是相對於系統媒體音量的比例
            // 這裡記錄設定，實際音量控制需要通過系統音量實現
            Log.d(TAG, "語音音量已設置為: " + volume + "（需要調整系統媒體音量）");
        }
    }
    
    /**
     * 停止當前語音播報
     */
    public void stop() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            Log.d(TAG, "語音播報已停止");
        }
    }
    
    public void speakWelcomeMessage() {
        String welcomeText = "瞳伴應用已啟動。歡迎使用智能視覺助手。" +
                "當前有四個主要功能：環境識別、閱讀助手、尋找物品、即時協助。" +
                "請點擊或滑動選擇功能。底部有緊急求助按鈕，長按三秒可發送求助信息。" +
                "如需切換語言，請點擊右上角的語言按鈕。";
        
        String englishText = "Tonbo application started. Welcome to the smart vision assistant. " +
                "Four main functions available: Environment Recognition, Document Assistant, Find Items, Live Assistance. " +
                "Tap or swipe to select function. Emergency help button at bottom, long press for 3 seconds to send help request. " +
                "To switch language, tap the language button on top right.";
        
        speak(welcomeText, englishText, true);
    }
    
    public void speak(String cantoneseText, String englishText) {
        speak(cantoneseText, englishText, false);
    }
    
    public void speak(String cantoneseText, String englishText, boolean priority) {
        // 確保TTS已初始化
        ensureTTSInitialized();
        
        if (!isInitialized || textToSpeech == null) {
            Log.w(TAG, "TTS未初始化，語音將在初始化後播放");
            // 延遲播放，等待TTS初始化
            handler.postDelayed(() -> speak(cantoneseText, englishText, priority), 500);
            return;
        }
        
        String textToSpeak = currentLanguage.equals("english") ?
                (englishText != null ? englishText : cantoneseText) :
                (cantoneseText != null ? cantoneseText : englishText);
        
        if (textToSpeak != null && !textToSpeak.trim().isEmpty()) {
            if (priority) {
                // 優先播放，停止當前語音並立即播放
                textToSpeech.stop();
                speechQueue.clear();
                textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "priority_speech");
            } else {
                // 加入隊列播放
                speechQueue.offer(textToSpeak);
                if (!isSpeaking) {
                    playNextInQueue();
                }
            }
        }
    }
    
    private void playNextInQueue() {
        if (speechQueue.isEmpty()) {
            isSpeaking = false;
            return;
        }
        
        String textToSpeak = speechQueue.poll();
        if (textToSpeak != null) {
            isSpeaking = true;
            textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "queue_speech");
            
            // 估算播放時間並安排下一段語音
            int estimatedDuration = Math.max(textToSpeak.length() * 100, 2000); // 至少2秒
            handler.postDelayed(() -> playNextInQueue(), estimatedDuration);
        }
    }
    
    public void changeLanguage(String language) {
        currentLanguage = language;
        
        // 清除當前語音隊列，避免延誤
        stopSpeaking();
        
        // 確保TTS已初始化
        ensureTTSInitialized();
        
        if (isInitialized && textToSpeech != null) {
            setLanguage(language);
            
            // 立即播放語言切換確認，不使用延遲
            switch (language) {
                case "cantonese":
                    speak("已切換到廣東話", "Switched to Cantonese", true);
                    break;
                case "english":
                    speak("已切換到英文", "Switched to English", true);
                    break;
                case "mandarin":
                    speak("已切換到普通話", "Switched to Mandarin", true);
                    break;
            }
        } else {
            Log.w(TAG, "TTS未初始化，無法切換語言");
        }
    }
    
    public void speakPageTitle(String pageName) {
        String cantoneseText = "當前頁面：" + pageName;
        String englishText = "Current page: " + pageName;
        speak(cantoneseText, englishText, true);
    }
    
    public void speakNavigationHint(String hint) {
        String cantoneseText = "提示：" + hint;
        String englishText = "Hint: " + hint;
        speak(cantoneseText, englishText, false);
    }
    
    public void speakError(String error) {
        String cantoneseText = "錯誤：" + error;
        String englishText = "Error: " + error;
        speak(cantoneseText, englishText, true);
    }
    
    public void speakSuccess(String message) {
        String cantoneseText = "成功：" + message;
        String englishText = "Success: " + message;
        speak(cantoneseText, englishText, true);
    }
    
    public void stopSpeaking() {
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
        speechQueue.clear();
        isSpeaking = false;
    }
    
    public void pauseSpeaking() {
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }
    
    public void resumeSpeaking() {
        if (!speechQueue.isEmpty() && !isSpeaking) {
            playNextInQueue();
        }
    }
    
    public String getCurrentLanguage() {
        return currentLanguage;
    }
    
    public boolean isSpeaking() {
        return isSpeaking;
    }
    
    public void shutdown() {
        // 不再完全關閉TTS，只停止播放
        // 保留TTS實例以便Activity重新創建時可以繼續使用
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
        speechQueue.clear();
        isSpeaking = false;
        Log.d(TAG, "TTS播放已停止");
    }
    
    public void forceShutdown() {
        // 只在應用真正退出時才調用此方法
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
        speechQueue.clear();
        isInitialized = false;
        isInitializing = false;
        isSpeaking = false;
        Log.d(TAG, "TTS已完全關閉");
    }
}
