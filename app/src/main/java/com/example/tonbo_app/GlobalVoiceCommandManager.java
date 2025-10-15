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
    private boolean isRecognizerBusy = false;
    private VoiceCommandCallback callback;
    
    // 語音識別重試機制 - 增強版
    private int retryCount = 0;
    private static final int MAX_RETRY_ATTEMPTS = 5; // 增加重試次數
    private static final long RETRY_DELAY_MS = 500; // 減少重試延遲
    private long lastErrorTime = 0;
    private static final long ERROR_COOLDOWN_MS = 1000; // 減少錯誤冷卻期
    
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
                    Log.d(TAG, "語音識別準備就緒 - 語言: " + getCurrentLanguage());
                    isListening = true;
                    isRecognizerBusy = false;
                }

                @Override
                public void onBeginningOfSpeech() {
                    Log.d(TAG, "開始語音識別 - 檢測到語音輸入");
                    isRecognizerBusy = true;
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                    // 音量變化，用於調試和語音檢測
                    if (rmsdB > 0) {
                        Log.d(TAG, "音量變化: " + rmsdB + " dB");
                        
                        // 語音檢測邏輯
                        if (rmsdB > 5.0f) {
                            Log.d(TAG, "檢測到語音輸入，音量: " + rmsdB + " dB");
                        } else if (rmsdB < 1.0f) {
                            Log.d(TAG, "音量過低，可能無法識別");
                        }
                    }
                }

                @Override
                public void onBufferReceived(byte[] buffer) {
                    // 音頻緩衝區接收
                }

                @Override
                public void onEndOfSpeech() {
                    Log.d(TAG, "語音識別結束");
                    isListening = false;
                    isRecognizerBusy = false;
                }

                @Override
                public void onError(int error) {
                    Log.e(TAG, "語音識別錯誤: " + error);
                    isListening = false;
                    isRecognizerBusy = false;
                    handleSpeechError(error);
                }

                @Override
                public void onResults(Bundle results) {
                    Log.d(TAG, "語音識別結果");
                    isListening = false;
                    isRecognizerBusy = false;
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
            handleMicrophonePermissionMissing();
            return;
        }
        
        if (speechRecognizer == null) {
            Log.e(TAG, "語音識別器未初始化");
            if (callback != null) {
                callback.onVoiceError("語音識別不可用");
            }
            return;
        }
        
        // 檢查語音識別服務連接狀態
        if (!isRecognitionServiceConnected()) {
            Log.e(TAG, "語音識別服務未連接");
            if (callback != null) {
                callback.onVoiceError("語音識別服務未連接，請稍後重試");
            }
            return;
        }
        
        if (isListening || isRecognizerBusy) {
            Log.w(TAG, "語音識別器忙碌，先停止當前識別");
            stopListening();
            // 等待更長時間讓識別器完全停止
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // 如果仍然忙碌，重置識別器
            if (isRecognizerBusy) {
                Log.w(TAG, "識別器仍然忙碌，重置識別器");
                resetRecognizer();
            }
        }
        
        try {
            // 移除預熱機制以避免連接問題
            // performVoiceRecognitionWarmup();
            
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, getCurrentLanguage());
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            
            // 根據語言優化語音識別參數
            optimizeSpeechRecognitionParams(intent);
            
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
        boolean hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
                == PackageManager.PERMISSION_GRANTED;
        Log.d(TAG, "麥克風權限檢查: " + (hasPermission ? "已授予" : "未授予"));
        return hasPermission;
    }
    
    /**
     * 處理麥克風權限缺失
     */
    private void handleMicrophonePermissionMissing() {
        Log.w(TAG, "處理麥克風權限缺失");
        
        // 獲取本地化錯誤消息
        String errorMessage = getLocalizedPermissionErrorMessage();
        
        // 通知回調
        if (callback != null) {
            callback.onVoiceError(errorMessage);
        }
        
        // 播放語音提示
        if (ttsManager != null) {
            ttsManager.speak(null, errorMessage, true);
        }
        
        // 提供權限請求指導
        providePermissionGuidance();
    }
    
    /**
     * 獲取本地化的權限錯誤消息
     */
    private String getLocalizedPermissionErrorMessage() {
        String currentLang = LocaleManager.getInstance(context).getCurrentLanguage();
        switch (currentLang) {
            case "english":
                return "Microphone permission required for voice commands. Please grant permission in settings.";
            case "mandarin":
                return "語音命令需要麥克風權限，請在設置中授予權限。";
            case "cantonese":
            default:
                return "語音命令需要麥克風權限，請喺設置度授予權限。";
        }
    }
    
    /**
     * 提供權限請求指導
     */
    private void providePermissionGuidance() {
        String currentLang = LocaleManager.getInstance(context).getCurrentLanguage();
        String guidanceMessage;
        
        switch (currentLang) {
            case "english":
                guidanceMessage = "To enable voice commands, go to Settings > Apps > Tonbo App > Permissions > Microphone and enable it.";
                break;
            case "mandarin":
                guidanceMessage = "要啟用語音命令，請前往設置 > 應用 > Tonbo應用 > 權限 > 麥克風並啟用它。";
                break;
            case "cantonese":
            default:
                guidanceMessage = "要啟用語音命令，請前往設置 > 應用 > Tonbo應用 > 權限 > 麥克風並啟用它。";
                break;
        }
        
        Log.i(TAG, "權限指導: " + guidanceMessage);
        
        // 延遲播放指導消息
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (ttsManager != null) {
                ttsManager.speak(null, guidanceMessage, true);
            }
        }, 3000); // 3秒後播放指導消息
    }
    
    public void stopListening() {
        if (speechRecognizer != null && (isListening || isRecognizerBusy)) {
            try {
                speechRecognizer.stopListening();
                speechRecognizer.cancel();
                isListening = false;
                isRecognizerBusy = false;
                Log.d(TAG, "語音識別已停止");
            } catch (Exception e) {
                Log.e(TAG, "停止語音識別失敗: " + e.getMessage());
                // 如果停止失敗，重置識別器
                resetRecognizer();
            }
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
                Log.w(TAG, "未識別到語音，將自動重試");
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                errorMessage = getLocalizedErrorMessage("recognizer_busy", currentLang);
                shouldRetry = true;
                shouldAutoRetry = true;
                isRecognizerBusy = true;
                Log.w(TAG, "語音識別器忙碌，將重置後重試");
                
                // 立即重置識別器
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    resetRecognizer();
                });
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
            
            // 智能重試延遲：根據錯誤類型和重試次數調整
            long progressiveDelay = calculateRetryDelay(error, retryCount);
            
            if (shouldAutoRetry) {
                // 自動重試
                Log.d(TAG, "自動重試語音識別 (" + retryCount + "/" + MAX_RETRY_ATTEMPTS + ")，延遲: " + progressiveDelay + "ms");
                
                // 如果是識別器忙碌錯誤，先重置識別器並增加延遲
                if (isRecognizerBusy) {
                    Log.d(TAG, "識別器忙碌，先重置識別器");
                    resetRecognizer();
                    // 識別器忙碌時增加額外延遲
                    progressiveDelay += 1000;
                }
                
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (callback != null) {
                        startListening(callback);
                    }
                }, progressiveDelay);
            } else {
                // 手動重試提示
                Log.d(TAG, "手動重試語音識別 (" + retryCount + "/" + MAX_RETRY_ATTEMPTS + ")，延遲: " + progressiveDelay + "ms");
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    String retryMessage = getRetryMessage(currentLang);
                    ttsManager.speak(null, retryMessage, true);
                }, progressiveDelay);
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
    
    /**
     * 重置語音識別器 - 增強版
     */
    private void resetRecognizer() {
        Log.d(TAG, "重置語音識別器");
        
        try {
            // 停止當前識別
            if (speechRecognizer != null) {
                try {
                    speechRecognizer.stopListening();
                    speechRecognizer.cancel();
                } catch (Exception e) {
                    Log.w(TAG, "停止識別器時出現異常: " + e.getMessage());
                }
                
                try {
                    speechRecognizer.destroy();
                } catch (Exception e) {
                    Log.w(TAG, "銷毀識別器時出現異常: " + e.getMessage());
                }
                
                speechRecognizer = null;
            }
            
            // 重置狀態
            isListening = false;
            isRecognizerBusy = false;
            retryCount = 0;
            
            // 等待一段時間讓服務完全釋放
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // 檢查語音識別服務可用性
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.e(TAG, "語音識別服務不可用");
                return;
            }
            
            // 重新初始化識別器
            initializeSpeechRecognizer();
            
            // 再次等待讓識別器完全初始化
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            Log.d(TAG, "語音識別器重置完成");
            
        } catch (Exception e) {
            Log.e(TAG, "重置語音識別器失敗: " + e.getMessage());
        }
    }
    
    /**
     * 語音識別預熱 - 提高識別成功率
     */
    private void performVoiceRecognitionWarmup() {
        Log.d(TAG, "開始語音識別預熱");
        
        // 檢查語音識別器狀態
        if (speechRecognizer == null) {
            Log.w(TAG, "語音識別器未初始化，跳過預熱");
            return;
        }
        
        // 進行一次快速的語音識別測試
        try {
            Intent warmupIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            warmupIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            warmupIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, getCurrentLanguage());
            warmupIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
            warmupIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            warmupIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1000);
            warmupIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 100);
            
            // 快速啟動和停止，進行預熱
            speechRecognizer.startListening(warmupIntent);
            
            // 100ms後停止預熱
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (isListening) {
                    speechRecognizer.stopListening();
                    Log.d(TAG, "語音識別預熱完成");
                }
            }, 100);
            
        } catch (Exception e) {
            Log.w(TAG, "語音識別預熱失敗: " + e.getMessage());
        }
    }
    
    /**
     * 根據語言優化語音識別參數
     */
    private void optimizeSpeechRecognitionParams(Intent intent) {
        String currentLang = getCurrentLanguage();
        
        // 通用參數
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10);
        intent.putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, true);
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false);
        intent.putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, new String[]{currentLang});
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getListeningPrompt());
        
        if (currentLang.contains("en")) {
            // 英文優化參數
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 6000); // 6秒靜音
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500); // 2.5秒可能完成
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 300); // 0.3秒最小長度
            
            // 英文特定優化
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US");
            intent.putExtra("android.speech.extra.DICTATION_MODE", false);
            
            Log.d(TAG, "應用英文語音識別優化參數");
            
        } else if (currentLang.contains("zh")) {
            // 中文優化參數
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 8000); // 8秒靜音
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000); // 3秒可能完成
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 200); // 0.2秒最小長度
            
            // 中文特定優化
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, currentLang);
            intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, currentLang);
            intent.putExtra("android.speech.extra.DICTATION_MODE", true);
            
            Log.d(TAG, "應用中文語音識別優化參數");
        } else {
            // 默認參數
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 7000);
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2800);
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 250);
            
            Log.d(TAG, "應用默認語音識別參數");
        }
    }
    
    /**
     * 獲取聆聽提示詞
     */
    private String getListeningPrompt() {
        String currentLang = LocaleManager.getInstance(context).getCurrentLanguage();
        switch (currentLang) {
            case "english":
                return "Please speak clearly, I'm listening...";
            case "mandarin":
                return "請清晰說話，我正在聆聽...";
            case "cantonese":
            default:
                return "請清晰咁講，我喺度聽緊...";
        }
    }
    
    public boolean isListening() {
        return isListening;
    }
    
    /**
     * 檢查識別器是否忙碌
     */
    public boolean isRecognizerBusy() {
        return isRecognizerBusy;
    }
    
    /**
     * 獲取識別器狀態信息
     */
    public String getRecognizerStatus() {
        return "Listening: " + isListening + ", Busy: " + isRecognizerBusy + ", Retry: " + retryCount;
    }
    
    /**
     * 檢查語音識別服務連接狀態
     */
    private boolean isRecognitionServiceConnected() {
        if (speechRecognizer == null) {
            return false;
        }
        
        try {
            // 嘗試檢查識別器狀態
            return SpeechRecognizer.isRecognitionAvailable(context);
        } catch (Exception e) {
            Log.w(TAG, "檢查識別服務連接失敗: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 語音識別診斷
     */
    public void performVoiceRecognitionDiagnostics() {
        Log.d(TAG, "開始語音識別診斷");
        
        // 檢查麥克風權限
        boolean hasPermission = checkMicrophonePermission();
        Log.d(TAG, "麥克風權限: " + (hasPermission ? "已授予" : "未授予"));
        
        if (!hasPermission) {
            Log.w(TAG, "⚠️ 麥克風權限未授予 - 這是語音識別失敗的主要原因");
            providePermissionGuidance();
            return;
        }
        
        // 檢查語音識別可用性
        boolean isAvailable = SpeechRecognizer.isRecognitionAvailable(context);
        Log.d(TAG, "語音識別服務: " + (isAvailable ? "可用" : "不可用"));
        
        if (!isAvailable) {
            Log.w(TAG, "⚠️ 語音識別服務不可用 - 請檢查系統設置");
        }
        
        // 檢查識別器狀態
        boolean isConnected = isRecognitionServiceConnected();
        Log.d(TAG, "識別器連接: " + (isConnected ? "已連接" : "未連接"));
        
        // 檢查當前語言
        String currentLang = getCurrentLanguage();
        Log.d(TAG, "當前識別語言: " + currentLang);
        
        // 檢查識別器忙碌狀態
        Log.d(TAG, "識別器狀態: " + getRecognizerStatus());
        
        // 檢查重試次數
        Log.d(TAG, "當前重試次數: " + retryCount + "/" + MAX_RETRY_ATTEMPTS);
        
        // 綜合診斷結果
        if (hasPermission && isAvailable && isConnected) {
            Log.i(TAG, "✅ 語音識別系統狀態正常");
        } else {
            Log.w(TAG, "❌ 語音識別系統存在問題，請檢查上述項目");
        }
        
        Log.d(TAG, "語音識別診斷完成");
    }
    
    /**
     * 檢查並請求麥克風權限
     */
    public boolean checkAndRequestMicrophonePermission() {
        boolean hasPermission = checkMicrophonePermission();
        
        if (!hasPermission) {
            Log.i(TAG, "麥克風權限未授予，嘗試請求權限");
            // 注意：這裡需要Activity來處理權限請求
            // 在GlobalVoiceCommandManager中我們只能檢查，不能直接請求
            // 實際的權限請求需要在Activity中處理
        }
        
        return hasPermission;
    }
    
    /**
     * 計算智能重試延遲
     */
    private long calculateRetryDelay(int error, int retryCount) {
        long baseDelay = RETRY_DELAY_MS;
        
        switch (error) {
            case SpeechRecognizer.ERROR_NO_MATCH:
                // 未識別到語音，增加延遲讓用戶有時間說話
                baseDelay = 2000 + (retryCount * 1000); // 2秒起，每次增加1秒
                break;
                
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                // 識別器忙碌，需要更長延遲
                baseDelay = 1500 + (retryCount * 500); // 1.5秒起，每次增加0.5秒
                break;
                
            case SpeechRecognizer.ERROR_NETWORK:
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                // 網絡錯誤，適度延遲
                baseDelay = 1000 + (retryCount * 500); // 1秒起，每次增加0.5秒
                break;
                
            default:
                // 其他錯誤，使用漸進式延遲
                baseDelay = RETRY_DELAY_MS * retryCount;
                break;
        }
        
        // 限制最大延遲時間
        return Math.min(baseDelay, 5000);
    }
    
    /**
     * 測試語音識別功能
     */
    public void testVoiceRecognition() {
        Log.d(TAG, "開始語音識別測試");
        
        // 檢查權限
        if (!checkMicrophonePermission()) {
            Log.e(TAG, "麥克風權限不足，無法測試語音識別");
            return;
        }
        
        // 檢查語音識別器
        if (speechRecognizer == null) {
            Log.e(TAG, "語音識別器未初始化，無法測試");
            return;
        }
        
        // 檢查設備支持
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "設備不支持語音識別");
            return;
        }
        
        Log.d(TAG, "語音識別測試條件滿足，開始測試");
        
        // 播放測試提示
        String testMessage = getTestMessage();
        ttsManager.speak(null, testMessage, true);
        
        // 延遲啟動語音識別
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            startListening(new VoiceCommandCallback() {
                @Override
                public void onCommandRecognized(String command) {
                    Log.d(TAG, "語音識別測試成功: " + command);
                    ttsManager.speak(null, "語音識別測試成功，識別到：" + command, true);
                }
                
                @Override
                public void onVoiceError(String error) {
                    Log.e(TAG, "語音識別測試失敗: " + error);
                    ttsManager.speak(null, "語音識別測試失敗：" + error, true);
                }
            });
        }, 2000); // 2秒後開始測試
    }
    
    /**
     * 獲取測試提示消息
     */
    private String getTestMessage() {
        String currentLang = LocaleManager.getInstance(context).getCurrentLanguage();
        switch (currentLang) {
            case "english":
                return "Voice recognition test starting. Please say something clearly in 2 seconds.";
            case "mandarin":
                return "語音識別測試開始，請在2秒後清晰說話";
            case "cantonese":
            default:
                return "語音識別測試開始，請喺2秒後清晰咁講";
        }
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
