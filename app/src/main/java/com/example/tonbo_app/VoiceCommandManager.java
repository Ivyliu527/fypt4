package com.example.tonbo_app;

import android.Manifest;
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
 * 語音命令管理器
 * 處理語音識別和命令執行
 */
public class VoiceCommandManager {
    private static final String TAG = "VoiceCommandManager";
    private static final int REQUEST_CODE_SPEECH_INPUT = 1000;
    
    private BaseAccessibleActivity activity;
    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;
    
    // 支持的語音命令
    private static final String[] VOICE_COMMANDS = {
        "環境識別", "閱讀助手", "尋找物品", "即時協助",
        "緊急求助", "系統設定", "語言切換", "返回主頁",
        "開始檢測", "停止檢測", "切換語言", "打開設置",
        "語音命令", "幫助", "退出"
    };
    
    // 英文命令對應
    private static final String[] ENGLISH_COMMANDS = {
        "environment recognition", "document assistant", "find items", "live assistance",
        "emergency help", "system settings", "language switch", "go home",
        "start detection", "stop detection", "switch language", "open settings",
        "voice command", "help", "exit"
    };
    
    public VoiceCommandManager(BaseAccessibleActivity activity) {
        this.activity = activity;
        initializeSpeechRecognizer();
    }
    
    private void initializeSpeechRecognizer() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, 
                new String[]{Manifest.permission.RECORD_AUDIO}, 
                REQUEST_CODE_SPEECH_INPUT);
            return;
        }
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d(TAG, "語音識別準備就緒");
                activity.announceInfo("請說出您的命令");
                activity.vibrationManager.vibrateNotification();
            }
            
            @Override
            public void onBeginningOfSpeech() {
                Log.d(TAG, "開始語音識別");
                activity.announceInfo("正在聆聽...");
            }
            
            @Override
            public void onRmsChanged(float rmsdB) {
                // 音量變化，可以用於視覺反饋
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
                
                String errorMessage = getErrorMessage(error);
                activity.announceError(errorMessage);
                activity.vibrationManager.vibrateError();
            }
            
            @Override
            public void onResults(Bundle results) {
                Log.d(TAG, "語音識別結果");
                isListening = false;
                
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String command = matches.get(0).toLowerCase();
                    Log.d(TAG, "識別到的命令: " + command);
                    
                    executeCommand(command);
                } else {
                    activity.announceError("未識別到語音命令");
                    activity.vibrationManager.vibrateError();
                }
            }
            
            @Override
            public void onPartialResults(Bundle partialResults) {
                // 部分結果
            }
            
            @Override
            public void onEvent(int eventType, Bundle params) {
                // 事件處理
            }
        });
    }
    
    /**
     * 開始語音識別
     */
    public void startVoiceRecognition() {
        if (isListening) {
            activity.announceInfo("語音識別正在進行中");
            return;
        }
        
        if (speechRecognizer == null) {
            activity.announceError("語音識別未初始化");
            return;
        }
        
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            
            // 根據當前語言設置識別語言
            String currentLanguage = activity.localeManager.getCurrentLanguage();
            if ("english".equals(currentLanguage)) {
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH);
            } else if ("mandarin".equals(currentLanguage)) {
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.SIMPLIFIED_CHINESE);
            } else {
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-HK"); // 粵語
            }
            
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "請說出您的命令");
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            
            speechRecognizer.startListening(intent);
            isListening = true;
            
            Log.d(TAG, "開始語音識別");
            
        } catch (Exception e) {
            Log.e(TAG, "語音識別啟動失敗: " + e.getMessage());
            activity.announceError("語音識別啟動失敗");
            activity.vibrationManager.vibrateError();
        }
    }
    
    /**
     * 停止語音識別
     */
    public void stopVoiceRecognition() {
        if (speechRecognizer != null && isListening) {
            speechRecognizer.stopListening();
            isListening = false;
            Log.d(TAG, "停止語音識別");
        }
    }
    
    /**
     * 執行語音命令
     */
    private void executeCommand(String command) {
        Log.d(TAG, "執行命令: " + command);
        
        // 震動反饋
        activity.vibrationManager.vibrateSuccess();
        
        // 根據命令執行相應操作
        if (command.contains("環境識別") || command.contains("environment")) {
            activity.announceInfo("正在啟動環境識別");
            ((MainActivity) activity).startEnvironmentActivity();
        } else if (command.contains("閱讀助手") || command.contains("document")) {
            activity.announceInfo("正在啟動閱讀助手");
            ((MainActivity) activity).startDocumentCurrencyActivity();
        } else if (command.contains("尋找物品") || command.contains("find")) {
            activity.announceInfo("正在啟動尋找物品");
            ((MainActivity) activity).startFindItemsActivity();
        } else if (command.contains("即時協助") || command.contains("assistance")) {
            activity.announceInfo("即時協助功能開發中");
        } else if (command.contains("緊急求助") || command.contains("emergency")) {
            activity.announceInfo("正在啟動緊急求助設置");
            ((MainActivity) activity).openEmergencySettings();
        } else if (command.contains("系統設定") || command.contains("settings")) {
            activity.announceInfo("正在啟動系統設定");
            ((MainActivity) activity).openSettings();
        } else if (command.contains("語言切換") || command.contains("language")) {
            activity.announceInfo("正在切換語言");
            ((MainActivity) activity).toggleLanguage();
        } else if (command.contains("返回主頁") || command.contains("home")) {
            activity.announceInfo("返回主頁");
            activity.finish();
        } else if (command.contains("幫助") || command.contains("help")) {
            activity.announceInfo("語音命令幫助：您可以說出環境識別、閱讀助手、尋找物品、緊急求助、系統設定、語言切換等命令");
        } else {
            activity.announceError("未識別的命令，請說出有效的命令");
            activity.vibrationManager.vibrateError();
        }
    }
    
    /**
     * 獲取錯誤信息
     */
    private String getErrorMessage(int error) {
        switch (error) {
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
                return "未識別到語音";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "語音識別器忙碌";
            case SpeechRecognizer.ERROR_SERVER:
                return "服務器錯誤";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "語音超時";
            default:
                return "語音識別錯誤";
        }
    }
    
    /**
     * 檢查是否正在聆聽
     */
    public boolean isListening() {
        return isListening;
    }
    
    /**
     * 釋放資源
     */
    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        isListening = false;
        Log.d(TAG, "語音命令管理器已釋放");
    }
}