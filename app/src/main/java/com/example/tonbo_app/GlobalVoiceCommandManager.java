package com.example.tonbo_app;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;

/**
 * 全局語音命令管理器
 * 在任何頁面都可以使用語音命令控制應用
 */
public class GlobalVoiceCommandManager {
    private static final String TAG = "GlobalVoiceManager";
    
    private Context context;
    private SpeechRecognizer speechRecognizer;
    private TTSManager ttsManager;
    private boolean isListening = false;
    private VoiceCommandCallback callback;
    
    // 語音識別重試機制
    private int retryCount = 0;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000;
    private long lastErrorTime = 0;
    private static final long ERROR_COOLDOWN_MS = 2000;
    
    // 語音命令接口
    public interface VoiceCommandCallback {
        void onCommandRecognized(String command);
        void onVoiceError(String error);
    }
    
    public GlobalVoiceCommandManager(Context context, TTSManager ttsManager) {
        this.context = context;
        this.ttsManager = ttsManager;
        initializeSpeechRecognizer();
    }
    
    public void setCallback(VoiceCommandCallback callback) {
        this.callback = callback;
    }
    
    private void initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    Log.d(TAG, "語音識別準備就緒");
                    isListening = true;
                }

                @Override
                public void onBeginningOfSpeech() {
                    Log.d(TAG, "開始語音識別");
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                    // 音量變化，可用於視覺反饋
                }

                @Override
                public void onBufferReceived(byte[] buffer) {
                    // 音頻緩衝區接收
                }

                @Override
                public void onEndOfSpeech() {
                    Log.d(TAG, "語音識別結束");
                    isListening = false;
                }

                @Override
                public void onError(int error) {
                    Log.e(TAG, "語音識別錯誤: " + error);
                    isListening = false;
                    handleSpeechError(error);
                }

                @Override
                public void onResults(Bundle results) {
                    Log.d(TAG, "語音識別結果");
                    isListening = false;
                    processRecognitionResults(results);
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    // 部分結果，可用於實時顯示
                }

                @Override
                public void onEvent(int eventType, Bundle params) {
                    // 其他事件
                }
            });
        } else {
            Log.e(TAG, "設備不支持語音識別");
        }
    }
    
    public void startListening(VoiceCommandCallback callback) {
        this.callback = callback;
        
        // 檢查語音識別權限
        if (!checkMicrophonePermission()) {
            Log.e(TAG, "麥克風權限不足");
            if (callback != null) {
                callback.onVoiceError("需要麥克風權限才能使用語音命令");
            }
            return;
        }
        
        if (speechRecognizer == null) {
            Log.e(TAG, "語音識別器未初始化");
            if (callback != null) {
                callback.onVoiceError("語音識別不可用");
            }
            return;
        }
        
        if (isListening) {
            Log.w(TAG, "語音識別正在進行中，先停止當前識別");
            stopListening();
            // 等待一小段時間讓識別器完全停止
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, getCurrentLanguage());
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3); // 增加結果數量
            intent.putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, true); // 啟用置信度分數
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000); // 3秒靜音後結束
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500); // 1.5秒可能完成靜音
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000); // 最小語音長度1秒
            
            speechRecognizer.startListening(intent);
            isListening = true;
            
            // 播放開始聆聽的語音提示
            announceListeningStart();
            
            Log.d(TAG, "語音識別已啟動，語言: " + getCurrentLanguage());
            
        } catch (Exception e) {
            Log.e(TAG, "啟動語音識別失敗: " + e.getMessage());
            isListening = false;
            if (callback != null) {
                callback.onVoiceError("語音識別啟動失敗");
            }
        }
    }
    
    private boolean checkMicrophonePermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
                == PackageManager.PERMISSION_GRANTED;
    }
    
    public void stopListening() {
        if (speechRecognizer != null && isListening) {
            speechRecognizer.stopListening();
            isListening = false;
            Log.d(TAG, "停止語音識別");
        }
    }
    
    private void processRecognitionResults(Bundle results) {
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        
        if (matches != null && !matches.isEmpty()) {
            String recognizedText = matches.get(0).toLowerCase().trim();
            Log.d(TAG, "識別到的語音: " + recognizedText);
            
            // 處理語音命令
            String command = processVoiceCommand(recognizedText);
            
            if (callback != null) {
                callback.onCommandRecognized(command);
            }
            
            // 播放確認語音
            announceCommandRecognized(command);
            
        } else {
            Log.w(TAG, "未識別到語音");
            if (callback != null) {
                callback.onVoiceError("未識別到語音");
            }
        }
    }
    
    private String processVoiceCommand(String recognizedText) {
        // 根據當前語言處理語音命令
        String currentLang = getCurrentLanguage();
        
        if (currentLang.startsWith("en")) {
            return processEnglishCommand(recognizedText);
        } else if (currentLang.startsWith("zh-CN")) {
            return processMandarinCommand(recognizedText);
        } else {
            return processCantoneseCommand(recognizedText);
        }
    }
    
    private String processEnglishCommand(String text) {
        if (text.contains("environment") || text.contains("environment recognition")) {
            return "environment";
        } else if (text.contains("document") || text.contains("reading assistant")) {
            return "document";
        } else if (text.contains("voice command") || text.contains("voice control")) {
            return "voice_command";
        } else if (text.contains("find items") || text.contains("find item")) {
            return "find_items";
        } else if (text.contains("live assistance") || text.contains("help")) {
            return "live_assistance";
        } else if (text.contains("emergency") || text.contains("emergency help")) {
            return "emergency";
        } else if (text.contains("home") || text.contains("go home") || text.contains("back home")) {
            return "home";
        } else if (text.contains("settings") || text.contains("setting")) {
            return "settings";
        } else if (text.contains("language") || text.contains("switch language")) {
            return "language";
        } else if (text.contains("time") || text.contains("what time")) {
            return "time";
        } else if (text.contains("stop") || text.contains("cancel")) {
            return "stop";
        }
        return "unknown";
    }
    
    private String processMandarinCommand(String text) {
        if (text.contains("环境识别") || text.contains("环境")) {
            return "environment";
        } else if (text.contains("阅读助手") || text.contains("文档")) {
            return "document";
        } else if (text.contains("语音命令") || text.contains("语音控制")) {
            return "voice_command";
        } else if (text.contains("寻找物品") || text.contains("找东西")) {
            return "find_items";
        } else if (text.contains("即时协助") || text.contains("帮助")) {
            return "live_assistance";
        } else if (text.contains("紧急求助") || text.contains("紧急")) {
            return "emergency";
        } else if (text.contains("主页") || text.contains("回家") || text.contains("返回")) {
            return "home";
        } else if (text.contains("设置") || text.contains("设定")) {
            return "settings";
        } else if (text.contains("语言") || text.contains("切换语言")) {
            return "language";
        } else if (text.contains("时间") || text.contains("几点")) {
            return "time";
        } else if (text.contains("停止") || text.contains("取消")) {
            return "stop";
        }
        return "unknown";
    }
    
    private String processCantoneseCommand(String text) {
        if (text.contains("環境識別") || text.contains("環境")) {
            return "environment";
        } else if (text.contains("閱讀助手") || text.contains("文件")) {
            return "document";
        } else if (text.contains("語音命令") || text.contains("語音控制")) {
            return "voice_command";
        } else if (text.contains("尋找物品") || text.contains("搵嘢")) {
            return "find_items";
        } else if (text.contains("即時協助") || text.contains("幫助")) {
            return "live_assistance";
        } else if (text.contains("緊急求助") || text.contains("緊急")) {
            return "emergency";
        } else if (text.contains("主頁") || text.contains("返屋企") || text.contains("返回")) {
            return "home";
        } else if (text.contains("設定") || text.contains("設置")) {
            return "settings";
        } else if (text.contains("語言") || text.contains("切換語言")) {
            return "language";
        } else if (text.contains("時間") || text.contains("幾點")) {
            return "time";
        } else if (text.contains("停止") || text.contains("取消")) {
            return "stop";
        }
        return "unknown";
    }
    
    private void handleSpeechError(int error) {
        String currentLang = LocaleManager.getInstance(context).getCurrentLanguage();
        String errorMessage;
        boolean shouldRetry = false;
        boolean shouldAutoRetry = false;
        
        long currentTime = System.currentTimeMillis();
        
        // 檢查錯誤冷卻期
        if (currentTime - lastErrorTime < ERROR_COOLDOWN_MS) {
            Log.d(TAG, "錯誤冷卻期中，跳過處理");
            return;
        }
        lastErrorTime = currentTime;
        
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                errorMessage = getLocalizedErrorMessage("audio_error", currentLang);
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                errorMessage = getLocalizedErrorMessage("client_error", currentLang);
                shouldRetry = true;
                shouldAutoRetry = true;
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                errorMessage = getLocalizedErrorMessage("permission_error", currentLang);
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                errorMessage = getLocalizedErrorMessage("network_error", currentLang);
                shouldRetry = true;
                shouldAutoRetry = true;
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                errorMessage = getLocalizedErrorMessage("network_timeout", currentLang);
                shouldRetry = true;
                shouldAutoRetry = true;
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                errorMessage = getLocalizedErrorMessage("no_match", currentLang);
                shouldRetry = true;
                shouldAutoRetry = true;
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                errorMessage = getLocalizedErrorMessage("recognizer_busy", currentLang);
                shouldRetry = true;
                shouldAutoRetry = true;
                break;
            case SpeechRecognizer.ERROR_SERVER:
                errorMessage = getLocalizedErrorMessage("server_error", currentLang);
                shouldRetry = true;
                shouldAutoRetry = true;
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                errorMessage = getLocalizedErrorMessage("speech_timeout", currentLang);
                shouldRetry = true;
                shouldAutoRetry = true;
                break;
            default:
                errorMessage = getLocalizedErrorMessage("unknown_error", currentLang);
                shouldRetry = true;
                shouldAutoRetry = true;
                break;
        }
        
        Log.e(TAG, "語音識別錯誤: " + errorMessage + " (錯誤代碼: " + error + ")");
        
        // 重置聆聽狀態
        isListening = false;
        
        if (callback != null) {
            callback.onVoiceError(errorMessage);
        }
        
        // 播放錯誤語音
        ttsManager.speak(null, errorMessage, true);
        
        // 智能重試機制
        if (shouldRetry && retryCount < MAX_RETRY_ATTEMPTS) {
            retryCount++;
            
            if (shouldAutoRetry) {
                // 自動重試
                Log.d(TAG, "自動重試語音識別 (" + retryCount + "/" + MAX_RETRY_ATTEMPTS + ")");
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (callback != null) {
                        startListening(callback);
                    }
                }, RETRY_DELAY_MS);
            } else {
                // 手動重試提示
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    String retryMessage = getRetryMessage(currentLang);
                    ttsManager.speak(null, retryMessage, true);
                }, 2000);
            }
        } else if (retryCount >= MAX_RETRY_ATTEMPTS) {
            // 達到最大重試次數
            Log.w(TAG, "達到最大重試次數，停止重試");
            retryCount = 0;
            
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                String maxRetryMessage = getMaxRetryMessage(currentLang);
                ttsManager.speak(null, maxRetryMessage, true);
            }, 2000);
        }
    }
    
    /**
     * 獲取重試提示消息
     */
    private String getRetryMessage(String currentLang) {
        switch (currentLang) {
            case "english":
                return "Please speak clearly and try again";
            case "mandarin":
                return "請清晰說話並重試";
            case "cantonese":
            default:
                return "請清晰說話並重試";
        }
    }
    
    /**
     * 獲取最大重試次數消息
     */
    private String getMaxRetryMessage(String currentLang) {
        switch (currentLang) {
            case "english":
                return "Maximum retry attempts reached. Please try again later";
            case "mandarin":
                return "已達到最大重試次數，請稍後再試";
            case "cantonese":
            default:
                return "已達到最大重試次數，請稍後再試";
        }
    }
    
    private String getLocalizedErrorMessage(String errorType, String currentLang) {
        switch (errorType) {
            case "audio_error":
                switch (currentLang) {
                    case "english": return "Audio error, please check microphone";
                    case "mandarin": return "音频错误，请检查麦克风";
                    default: return "音頻錯誤，請檢查麥克風";
                }
            case "client_error":
                switch (currentLang) {
                    case "english": return "Client error";
                    case "mandarin": return "客户端错误";
                    default: return "客戶端錯誤";
                }
            case "permission_error":
                switch (currentLang) {
                    case "english": return "Permission denied, please allow microphone permission in settings";
                    case "mandarin": return "权限不足，请在设置中允许麦克风权限";
                    default: return "權限不足，請在設定中允許麥克風權限";
                }
            case "network_error":
                switch (currentLang) {
                    case "english": return "Network error, please check network connection";
                    case "mandarin": return "网络错误，请检查网络连接";
                    default: return "網絡錯誤，請檢查網絡連接";
                }
            case "network_timeout":
                switch (currentLang) {
                    case "english": return "Network timeout, please retry";
                    case "mandarin": return "网络超时，请重试";
                    default: return "網絡超時，請重試";
                }
            case "no_match":
                switch (currentLang) {
                    case "english": return "No speech recognized, please retry";
                    case "mandarin": return "未识别到语音，请重试";
                    default: return "未識別到語音，請重試";
                }
            case "recognizer_busy":
                switch (currentLang) {
                    case "english": return "Speech recognizer busy, please retry later";
                    case "mandarin": return "语音识别器忙碌，请稍后重试";
                    default: return "語音識別器忙碌，請稍後重試";
                }
            case "server_error":
                switch (currentLang) {
                    case "english": return "Server error, please retry";
                    case "mandarin": return "服务器错误，请重试";
                    default: return "服務器錯誤，請重試";
                }
            case "speech_timeout":
                switch (currentLang) {
                    case "english": return "Speech timeout, please retry";
                    case "mandarin": return "语音超时，请重试";
                    default: return "語音超時，請重試";
                }
            case "unknown_error":
            default:
                switch (currentLang) {
                    case "english": return "Unknown error";
                    case "mandarin": return "未知错误";
                    default: return "未知錯誤";
                }
        }
    }
    
    private void announceListeningStart() {
        String currentLang = LocaleManager.getInstance(context).getCurrentLanguage();
        String message;
        
        switch (currentLang) {
            case "english":
                message = "Listening for voice commands";
                break;
            case "mandarin":
                message = "正在聆听语音命令";
                break;
            case "cantonese":
            default:
                message = "開始聆聽語音命令";
                break;
        }
        
        ttsManager.speak(null, message, true);
    }
    
    private void announceCommandRecognized(String command) {
        String currentLang = LocaleManager.getInstance(context).getCurrentLanguage();
        String message;
        
        // 重置重試計數
        retryCount = 0;
        
        switch (currentLang) {
            case "english":
                message = "Command recognized: " + command;
                break;
            case "mandarin":
                message = "已识别命令: " + command;
                break;
            case "cantonese":
            default:
                message = "已識別命令: " + command;
                break;
        }
        
        ttsManager.speak(null, message, true);
    }
    
    private String getCurrentLanguage() {
        // 從LocaleManager獲取當前語言
        String currentLang = LocaleManager.getInstance(context).getCurrentLanguage();
        
        switch (currentLang) {
            case "english":
                return Locale.ENGLISH.toString();
            case "mandarin":
                return Locale.SIMPLIFIED_CHINESE.toString();
            case "cantonese":
            default:
                return Locale.TRADITIONAL_CHINESE.toString();
        }
    }
    
    public boolean isListening() {
        return isListening;
    }
    
    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        isListening = false;
        Log.d(TAG, "全局語音命令管理器已銷毀");
    }
}
