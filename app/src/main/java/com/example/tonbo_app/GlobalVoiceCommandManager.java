package com.example.tonbo_app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.Toast;

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
        
        if (speechRecognizer == null) {
            Log.e(TAG, "語音識別器未初始化");
            if (callback != null) {
                callback.onVoiceError("語音識別不可用");
            }
            return;
        }
        
        if (isListening) {
            Log.w(TAG, "語音識別正在進行中");
            return;
        }
        
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, getCurrentLanguage());
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            
            speechRecognizer.startListening(intent);
            
            // 播放開始聆聽的語音提示
            announceListeningStart();
            
        } catch (Exception e) {
            Log.e(TAG, "啟動語音識別失敗: " + e.getMessage());
            if (callback != null) {
                callback.onVoiceError("語音識別啟動失敗");
            }
        }
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
        String errorMessage;
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                errorMessage = "音頻錯誤";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                errorMessage = "客戶端錯誤";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                errorMessage = "權限不足";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                errorMessage = "網絡錯誤";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                errorMessage = "網絡超時";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                errorMessage = "未識別到語音";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                errorMessage = "語音識別器忙碌";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                errorMessage = "服務器錯誤";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                errorMessage = "語音超時";
                break;
            default:
                errorMessage = "未知錯誤";
                break;
        }
        
        Log.e(TAG, "語音識別錯誤: " + errorMessage);
        
        if (callback != null) {
            callback.onVoiceError(errorMessage);
        }
        
        // 播放錯誤語音
        ttsManager.speak(null, errorMessage, true);
    }
    
    private void announceListeningStart() {
        String message = "開始聆聽語音命令";
        ttsManager.speak(null, message, true);
    }
    
    private void announceCommandRecognized(String command) {
        String message = "已識別命令: " + command;
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
