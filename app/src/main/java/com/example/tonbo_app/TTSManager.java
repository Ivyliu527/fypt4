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
        Log.d(TAG, "🔊 ensureTTSInitialized 被調用 - textToSpeech: " + (textToSpeech != null) + ", isInitializing: " + isInitializing);
        if (textToSpeech == null && !isInitializing) {
            Log.d(TAG, "🔊 TTS未初始化，開始初始化");
            initTTS();
        } else if (textToSpeech != null) {
            Log.d(TAG, "🔊 TTS已存在，無需重新初始化");
        } else {
            Log.d(TAG, "🔊 TTS正在初始化中，等待完成");
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
        Log.d(TAG, "🔊 開始初始化TTS引擎...");
        Log.d(TAG, "🔊 當前語言設置: " + currentLanguage);
        
        textToSpeech = new TextToSpeech(context, status -> {
            isInitializing = false;
            Log.d(TAG, "🔊 TTS初始化回調，狀態: " + status);
            if (status == TextToSpeech.SUCCESS) {
                setLanguage(currentLanguage);
                isInitialized = true;
                Log.d(TAG, "✅ TTS初始化成功，語言: " + currentLanguage);
            } else {
                Log.e(TAG, "❌ TTS初始化失敗，狀態: " + status);
                isInitialized = false;
            }
        });
    }
    
    private void setLanguage(String language) {
        if (textToSpeech == null) {
            Log.w(TAG, "❌ textToSpeech為空，無法設置語言");
            return;
        }
        
        Log.d(TAG, "🔊 設置TTS語言: " + language);
        int result = TextToSpeech.LANG_MISSING_DATA;
        
        switch (language) {
            case "cantonese":
                // 優先使用香港廣東話 (zh-HK)
                Log.d(TAG, "🔊 嘗試設置廣東話 (zh-HK)");
                result = textToSpeech.setLanguage(cantoneseLocale);
                Log.d(TAG, "🔊 廣東話設置結果: " + result);
                
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "⚠️ 廣東話不支持，嘗試使用台灣國語");
                    result = textToSpeech.setLanguage(Locale.TAIWAN);
                    Log.d(TAG, "🔊 台灣國語設置結果: " + result);
                    
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w(TAG, "⚠️ 台灣國語不支持，使用繁體中文");
                        result = textToSpeech.setLanguage(Locale.TRADITIONAL_CHINESE);
                        Log.d(TAG, "🔊 繁體中文設置結果: " + result);
                    }
                }
                break;
                
            case "english":
                Log.d(TAG, "🔊 設置英文");
                result = textToSpeech.setLanguage(Locale.ENGLISH);
                Log.d(TAG, "🔊 英文設置結果: " + result);
                break;
                
            case "mandarin":
            default:
                Log.d(TAG, "🔊 設置普通話/簡體中文");
                result = textToSpeech.setLanguage(Locale.SIMPLIFIED_CHINESE);
                Log.d(TAG, "🔊 簡體中文設置結果: " + result);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "⚠️ 簡體中文不支持，使用繁體中文");
                    result = textToSpeech.setLanguage(Locale.TRADITIONAL_CHINESE);
                    Log.d(TAG, "🔊 繁體中文設置結果: " + result);
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
        Log.d(TAG, "🔊 TTS speak 被調用 - cantoneseText: " + cantoneseText + ", englishText: " + englishText + ", priority: " + priority);
        Log.d(TAG, "🔊 當前語言: " + currentLanguage + ", TTS初始化狀態: " + isInitialized);
        Log.d(TAG, "🔊 textToSpeech對象: " + (textToSpeech != null ? "存在" : "為空"));
        
        // 強制確保TTS已初始化
        ensureTTSInitialized();
        
        String textToSpeak = currentLanguage.equals("english") ?
                (englishText != null ? englishText : cantoneseText) :
                (cantoneseText != null ? cantoneseText : englishText);
        
        Log.d(TAG, "🔊 選擇的語音文本: " + textToSpeak);
        
        // 如果TTS未初始化，等待初始化完成
        if (!isInitialized || textToSpeech == null) {
            Log.w(TAG, "TTS未初始化，等待初始化完成後播放");
            Log.d(TAG, "🔊 延遲播放語音: " + textToSpeak);
            
            // 使用更長的延遲時間，確保TTS完全初始化
            handler.postDelayed(() -> {
                Log.d(TAG, "🔊 重試播放語音: " + textToSpeak);
                Log.d(TAG, "🔊 重試時TTS狀態: " + isInitialized + ", textToSpeech: " + (textToSpeech != null));
                speak(cantoneseText, englishText, priority);
            }, 2000); // 增加到2秒
            return;
        }
        
        if (textToSpeak != null && !textToSpeak.trim().isEmpty()) {
            if (priority) {
                // 優先播放，停止當前語音並立即播放
                Log.d(TAG, "🔊 優先播放語音: " + textToSpeak);
                textToSpeech.stop();
                speechQueue.clear();
                int result = textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "priority_speech");
                Log.d(TAG, "🔊 TTS speak 結果: " + result + " (SUCCESS=" + TextToSpeech.SUCCESS + ", ERROR=" + TextToSpeech.ERROR + ")");
                
                if (result == TextToSpeech.ERROR) {
                    Log.e(TAG, "❌ TTS播放失敗！");
                } else if (result == TextToSpeech.SUCCESS) {
                    Log.d(TAG, "✅ TTS播放成功");
                } else {
                    Log.w(TAG, "⚠️ TTS播放結果未知: " + result);
                }
            } else {
                // 加入隊列播放
                Log.d(TAG, "🔊 加入隊列播放: " + textToSpeak);
                speechQueue.offer(textToSpeak);
                if (!isSpeaking) {
                    playNextInQueue();
                }
            }
        } else {
            Log.w(TAG, "❌ 語音文本為空，無法播放");
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
            Log.d(TAG, "語言已切換到: " + language);
        } else {
            Log.w(TAG, "TTS未初始化，無法切換語言");
        }
    }
    
    public void speakPageTitle(String pageName) {
        Log.d(TAG, "🔊 speakPageTitle 被調用，頁面名稱: " + pageName);
        String cantoneseText = "當前頁面：" + pageName;
        String englishText = "Current page: " + pageName;
        Log.d(TAG, "🔊 廣東話文本: " + cantoneseText);
        Log.d(TAG, "🔊 英文文本: " + englishText);
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
