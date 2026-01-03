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
    
    // Speech queue management
    private ConcurrentLinkedQueue<String> speechQueue = new ConcurrentLinkedQueue<>();
    private Handler handler = new Handler(Looper.getMainLooper());
    
    private TTSManager(Context context) {
        this.context = context.getApplicationContext();
        cantoneseLocale = new Locale("zh", "HK");
        // Don't initialize TTS in constructor, wait for first use
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
    
    /**
     * Force initialize TTS engine
     */
    public void forceInitialize() {
        Log.d(TAG, "🔊 Force initialize TTS engine");
        ensureTTSInitialized();
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
                // Prioritize Hong Kong Cantonese (zh-HK)
                Log.d(TAG, "🔊 Try to set Cantonese (zh-HK)");
                result = textToSpeech.setLanguage(cantoneseLocale);
                Log.d(TAG, "🔊 Cantonese setting result: " + result);
                
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "⚠️ Cantonese not supported, try Taiwan Mandarin");
                    result = textToSpeech.setLanguage(Locale.TAIWAN);
                    Log.d(TAG, "🔊 Taiwan Mandarin setting result: " + result);
                    
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w(TAG, "⚠️ Taiwan Mandarin not supported, use Traditional Chinese");
                        result = textToSpeech.setLanguage(Locale.TRADITIONAL_CHINESE);
                        Log.d(TAG, "🔊 Traditional Chinese setting result: " + result);
                    }
                }
                break;
                
            case "english":
                Log.d(TAG, "🔊 Set English");
                result = textToSpeech.setLanguage(Locale.ENGLISH);
                Log.d(TAG, "🔊 English setting result: " + result);
                break;
                
            case "mandarin":
            default:
                Log.d(TAG, "🔊 Set Mandarin/Simplified Chinese");
                result = textToSpeech.setLanguage(Locale.SIMPLIFIED_CHINESE);
                Log.d(TAG, "🔊 Simplified Chinese setting result: " + result);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "⚠️ Simplified Chinese not supported, use Traditional Chinese");
                    result = textToSpeech.setLanguage(Locale.TRADITIONAL_CHINESE);
                    Log.d(TAG, "🔊 Traditional Chinese setting result: " + result);
                }
                break;
        }
        
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e(TAG, "❌ Language not supported: " + language);
        } else {
            Log.d(TAG, "✅ TTS language set successfully: " + language);
        }
    }
    
    // Set language silently, don't announce language switch
    public void setLanguageSilently(String language) {
        currentLanguage = language;
        setLanguage(language);
        Log.d(TAG, "Language silently set to: " + language);
    }
    
    /**
     * Set speech rate
     */
    public void setSpeechRate(float rate) {
        if (textToSpeech != null) {
            textToSpeech.setSpeechRate(rate);
            Log.d(TAG, "Speech rate set to: " + rate);
        }
    }
    
    /**
     * Set speech pitch
     */
    public void setSpeechPitch(float pitch) {
        if (textToSpeech != null) {
            textToSpeech.setPitch(pitch);
            Log.d(TAG, "Speech pitch set to: " + pitch);
        }
    }
    
    /**
     * Set speech volume (Note: Android TTS has no direct volume control)
     */
    public void setSpeechVolume(float volume) {
        if (textToSpeech != null) {
            // Android TTS volume is relative to system media volume
            // Here we record the setting, actual volume control needs to be done through system volume
            Log.d(TAG, "Speech volume set to: " + volume + " (need to adjust system media volume)");
        }
    }
    
    /**
     * Stop current speech playback
     */
    public void stop() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            Log.d(TAG, "Speech playback stopped");
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
        
        // Force ensure TTS is initialized
        ensureTTSInitialized();
        
        // Select corresponding text based on current language
        String textToSpeak;
        if ("english".equals(currentLanguage)) {
            // English mode: use English text
            textToSpeak = englishText != null ? englishText : cantoneseText;
        } else if ("mandarin".equals(currentLanguage)) {
            // Mandarin mode: use Chinese text (cantoneseText parameter contains Chinese labels in Mandarin mode)
            textToSpeak = cantoneseText != null ? cantoneseText : englishText;
        } else {
            // Cantonese mode: use Chinese text
            textToSpeak = cantoneseText != null ? cantoneseText : englishText;
        }
        
        Log.d(TAG, "🔊 Selected speech text: " + textToSpeak);
        
        // Ensure TTS language setting matches current language (set before playback)
        if (textToSpeech != null && isInitialized) {
            setLanguage(currentLanguage);
        }
        
        // If TTS not initialized, wait for initialization to complete
        if (!isInitialized || textToSpeech == null) {
            Log.w(TAG, "TTS not initialized, waiting for initialization to complete before playback");
            Log.d(TAG, "🔊 Delayed speech playback: " + textToSpeak);
            
            // Use longer delay time to ensure TTS is fully initialized
            handler.postDelayed(() -> {
                Log.d(TAG, "🔊 Retry speech playback: " + textToSpeak);
                Log.d(TAG, "🔊 TTS status on retry: " + isInitialized + ", textToSpeech: " + (textToSpeech != null));
                speak(cantoneseText, englishText, priority);
            }, 2000); // Increased to 2 seconds
            return;
        }
        
        if (textToSpeak != null && !textToSpeak.trim().isEmpty()) {
            if (priority) {
                // Priority playback, stop current speech and play immediately
                Log.d(TAG, "🔊 Priority speech playback: " + textToSpeak);
                textToSpeech.stop();
                speechQueue.clear();
                int result = textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "priority_speech");
                Log.d(TAG, "🔊 TTS speak result: " + result + " (SUCCESS=" + TextToSpeech.SUCCESS + ", ERROR=" + TextToSpeech.ERROR + ")");
                
                if (result == TextToSpeech.ERROR) {
                    Log.e(TAG, "❌ TTS playback failed!");
                } else if (result == TextToSpeech.SUCCESS) {
                    Log.d(TAG, "✅ TTS playback successful");
                } else {
                    Log.w(TAG, "⚠️ TTS playback result unknown: " + result);
                }
            } else {
                // Add to queue for playback
                Log.d(TAG, "🔊 Add to queue for playback: " + textToSpeak);
                speechQueue.offer(textToSpeak);
                if (!isSpeaking) {
                    playNextInQueue();
                }
            }
        } else {
            Log.w(TAG, "❌ Speech text is empty, cannot play");
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
            
            // Estimate playback time and schedule next speech
            int estimatedDuration = Math.max(textToSpeak.length() * 100, 2000); // At least 2 seconds
            handler.postDelayed(() -> playNextInQueue(), estimatedDuration);
        }
    }
    
    public void changeLanguage(String language) {
        currentLanguage = language;
        
        // Clear current speech queue to avoid delays
        stopSpeaking();
        
        // Ensure TTS is initialized
        ensureTTSInitialized();
        
        if (isInitialized && textToSpeech != null) {
            setLanguage(language);
            Log.d(TAG, "Language switched to: " + language);
        } else {
            Log.w(TAG, "TTS not initialized, cannot switch language");
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
        // No longer completely close TTS, only stop playback
        // Keep TTS instance so it can continue to be used when Activity is recreated
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
        speechQueue.clear();
        isSpeaking = false;
        Log.d(TAG, "TTS playback stopped");
    }
    
    public void forceShutdown() {
        // Only call this method when app truly exits
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
        speechQueue.clear();
        isInitialized = false;
        isInitializing = false;
        isSpeaking = false;
        Log.d(TAG, "TTS completely closed");
    }
}
