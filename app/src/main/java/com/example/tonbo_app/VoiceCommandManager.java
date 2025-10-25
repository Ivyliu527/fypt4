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

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 語音命令管理器
 * 負責語音識別和命令解析
 */
public class VoiceCommandManager {
    
    private static final String TAG = "VoiceCommandManager";
    private static final int REQUEST_CODE_SPEECH_INPUT = 1000;
    private static VoiceCommandManager instance;
    
    private BaseAccessibleActivity activity;
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private boolean isListening = false;
    
    // 命令映射表 - 廣東話
    private Map<String, String> cantoneseCommands = new HashMap<>();
    // 命令映射表 - 英文
    private Map<String, String> englishCommands = new HashMap<>();
    // 命令映射表 - 普通話
    private Map<String, String> mandarinCommands = new HashMap<>();
    
    // 當前語言
    private String currentLanguage = "cantonese";
    
    /**
     * 語音命令回調接口
     */
    public interface VoiceCommandListener {
        void onCommandRecognized(String command, String originalText);
        void onListeningStarted();
        void onListeningStopped();
        void onError(String error);
        void onPartialResult(String partialText);
    }
    
    private VoiceCommandListener commandListener;
    
    public VoiceCommandManager(BaseAccessibleActivity activity) {
        this.activity = activity;
        initializeCommands();
        initializeSpeechRecognizer();
    }
    
    public static synchronized VoiceCommandManager getInstance(BaseAccessibleActivity activity) {
        if (instance == null) {
            instance = new VoiceCommandManager(activity);
        }
        return instance;
    }
    
    /**
     * 初始化命令映射表
     */
    private void initializeCommands() {
        // 廣東話命令
        cantoneseCommands.put("打開環境識別", "open_environment");
        cantoneseCommands.put("環境識別", "open_environment");
        cantoneseCommands.put("睇下周圍", "open_environment");
        
        cantoneseCommands.put("打開閱讀助手", "open_document");
        cantoneseCommands.put("閱讀助手", "open_document");
        cantoneseCommands.put("讀文件", "open_document");
        cantoneseCommands.put("掃描文件", "open_document");
        
        cantoneseCommands.put("尋找物品", "open_find");
        cantoneseCommands.put("搵嘢", "open_find");
        
        cantoneseCommands.put("即時協助", "open_assistance");
        cantoneseCommands.put("幫手", "open_assistance");
        
        cantoneseCommands.put("緊急求助", "emergency");
        cantoneseCommands.put("救命", "emergency");
        cantoneseCommands.put("幫我", "emergency");
        
        cantoneseCommands.put("返回主頁", "go_home");
        cantoneseCommands.put("主頁", "go_home");
        cantoneseCommands.put("返回", "go_back");
        
        cantoneseCommands.put("切換語言", "switch_language");
        cantoneseCommands.put("轉語言", "switch_language");
        
        cantoneseCommands.put("打開設定", "open_settings");
        cantoneseCommands.put("設定", "open_settings");
        
        cantoneseCommands.put("現在幾點", "tell_time");
        cantoneseCommands.put("幾點", "tell_time");
        
        cantoneseCommands.put("停止", "stop_listening");
        cantoneseCommands.put("收聲", "stop_listening");
        
        // 英文命令
        englishCommands.put("open environment", "open_environment");
        englishCommands.put("environment recognition", "open_environment");
        englishCommands.put("look around", "open_environment");
        
        englishCommands.put("open document", "open_document");
        englishCommands.put("document assistant", "open_document");
        englishCommands.put("read document", "open_document");
        englishCommands.put("scan document", "open_document");
        
        englishCommands.put("find items", "open_find");
        englishCommands.put("find object", "open_find");
        
        englishCommands.put("live assistance", "open_assistance");
        englishCommands.put("help", "open_assistance");
        
        englishCommands.put("emergency", "emergency");
        englishCommands.put("help me", "emergency");
        
        englishCommands.put("go home", "go_home");
        englishCommands.put("home", "go_home");
        englishCommands.put("go back", "go_back");
        
        englishCommands.put("switch language", "switch_language");
        englishCommands.put("change language", "switch_language");
        
        englishCommands.put("open settings", "open_settings");
        englishCommands.put("settings", "open_settings");
        
        englishCommands.put("what time is it", "tell_time");
        englishCommands.put("tell me the time", "tell_time");
        
        englishCommands.put("stop", "stop_listening");
        englishCommands.put("stop listening", "stop_listening");
        
        // 普通話命令
        mandarinCommands.put("打開環境識別", "open_environment");
        mandarinCommands.put("環境識別", "open_environment");
        mandarinCommands.put("看看周圍", "open_environment");
        
        mandarinCommands.put("打開閱讀助手", "open_document");
        mandarinCommands.put("閱讀助手", "open_document");
        mandarinCommands.put("讀文件", "open_document");
        mandarinCommands.put("掃描文件", "open_document");
        
        mandarinCommands.put("尋找物品", "open_find");
        mandarinCommands.put("找東西", "open_find");
        
        mandarinCommands.put("即時協助", "open_assistance");
        mandarinCommands.put("幫助", "open_assistance");
        
        mandarinCommands.put("緊急求助", "emergency");
        mandarinCommands.put("救命", "emergency");
        mandarinCommands.put("幫我", "emergency");
        
        mandarinCommands.put("返回主頁", "go_home");
        mandarinCommands.put("主頁", "go_home");
        mandarinCommands.put("返回", "go_back");
        
        mandarinCommands.put("切換語言", "switch_language");
        mandarinCommands.put("轉換語言", "switch_language");
        
        mandarinCommands.put("打開設置", "open_settings");
        mandarinCommands.put("設置", "open_settings");
        
        mandarinCommands.put("現在幾點", "tell_time");
        mandarinCommands.put("幾點了", "tell_time");
        
        mandarinCommands.put("停止", "stop_listening");
        mandarinCommands.put("停", "stop_listening");
        
        Log.d(TAG, "命令映射表初始化完成");
    }
    
    /**
     * 初始化語音識別器
     */
    private void initializeSpeechRecognizer() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, 
                new String[]{Manifest.permission.RECORD_AUDIO}, 
                REQUEST_CODE_SPEECH_INPUT);
            return;
        }
        
        if (SpeechRecognizer.isRecognitionAvailable(activity)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    Log.d(TAG, "準備接收語音");
                    isListening = true;
                    activity.announceInfo("請說出您的命令");
                    activity.vibrationManager.vibrateNotification();
                    if (commandListener != null) {
                        commandListener.onListeningStarted();
                    }
                }
                
                @Override
                public void onBeginningOfSpeech() {
                    Log.d(TAG, "開始說話");
                    activity.announceInfo("正在聆聽...");
                }
                
                @Override
                public void onRmsChanged(float rmsdB) {
                    // 音量變化
                }
                
                @Override
                public void onBufferReceived(byte[] buffer) {
                    // 接收緩衝區數據
                }
                
                @Override
                public void onEndOfSpeech() {
                    Log.d(TAG, "說話結束");
                    isListening = false;
                }
                
                @Override
                public void onError(int error) {
                    Log.e(TAG, "語音識別錯誤: " + getErrorText(error));
                    isListening = false;
                    String errorMessage = getErrorText(error);
                    activity.announceError(errorMessage);
                    activity.vibrationManager.vibrateError();
                    if (commandListener != null) {
                        commandListener.onError(errorMessage);
                        commandListener.onListeningStopped();
                    }
                }
                
                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String recognizedText = matches.get(0);
                        Log.d(TAG, "識別結果: " + recognizedText);
                        processCommand(recognizedText);
                    }
                    isListening = false;
                    if (commandListener != null) {
                        commandListener.onListeningStopped();
                    }
                }
                
                @Override
                public void onPartialResults(Bundle partialResults) {
                    ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String partialText = matches.get(0);
                        Log.d(TAG, "部分識別結果: " + partialText);
                        if (commandListener != null) {
                            commandListener.onPartialResult(partialText);
                        }
                    }
                }
                
                @Override
                public void onEvent(int eventType, Bundle params) {
                    // 其他事件
                }
            });
            
            Log.d(TAG, "語音識別器初始化成功");
        } else {
            Log.e(TAG, "設備不支持語音識別");
        }
    }
    
    /**
     * 設置語言
     */
    public void setLanguage(String language) {
        this.currentLanguage = language;
        Log.d(TAG, "語言已設置為: " + language);
    }
    
    /**
     * 開始監聽語音命令
     */
    public void startVoiceRecognition() {
        if (speechRecognizer == null) {
            initializeSpeechRecognizer();
        }
        
        if (isListening) {
            Log.w(TAG, "已經在監聽中");
            activity.announceInfo("語音識別正在進行中");
            return;
        }
        
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        
        // 根據當前語言設置識別語言
        Locale locale;
        switch (currentLanguage) {
            case "english":
                locale = Locale.ENGLISH;
                break;
            case "mandarin":
                locale = Locale.SIMPLIFIED_CHINESE;
                break;
            case "cantonese":
            default:
                locale = new Locale("zh", "HK");
                break;
        }
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, locale);
        
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        
        speechRecognizer.startListening(recognizerIntent);
        Log.d(TAG, "開始監聽語音命令 - 語言: " + currentLanguage);
    }
    
    /**
     * 停止監聽
     */
    public void stopVoiceRecognition() {
        if (speechRecognizer != null && isListening) {
            speechRecognizer.stopListening();
            isListening = false;
            Log.d(TAG, "停止監聽");
        }
    }
    
    /**
     * 處理命令
     */
    private void processCommand(String recognizedText) {
        String command = matchCommand(recognizedText.toLowerCase());
        
        if (command != null) {
            Log.d(TAG, "匹配到命令: " + command);
            executeCommand(command, recognizedText);
            if (commandListener != null) {
                commandListener.onCommandRecognized(command, recognizedText);
            }
        } else {
            Log.d(TAG, "未匹配到命令: " + recognizedText);
            activity.announceError("未識別的命令，請說出有效的命令");
            activity.vibrationManager.vibrateError();
            if (commandListener != null) {
                commandListener.onError("未識別的命令");
            }
        }
    }
    
    /**
     * 執行語音命令
     */
    private void executeCommand(String command, String originalText) {
        Log.d(TAG, "執行命令: " + command);
        
        // 震動反饋
        activity.vibrationManager.vibrateSuccess();
        
        // 根據命令執行相應操作
        switch (command) {
            case "open_environment":
                activity.announceInfo("正在啟動環境識別");
                ((MainActivity) activity).startEnvironmentActivity();
                break;
            case "open_document":
                activity.announceInfo("正在啟動閱讀助手");
                ((MainActivity) activity).startDocumentCurrencyActivity();
                break;
            case "open_find":
                activity.announceInfo("正在啟動尋找物品");
                ((MainActivity) activity).startFindItemsActivity();
                break;
            case "open_assistance":
                activity.announceInfo("即時協助功能開發中");
                break;
            case "emergency":
                activity.announceInfo("正在啟動緊急求助設置");
                ((MainActivity) activity).openEmergencySettings();
                break;
            case "open_settings":
                activity.announceInfo("正在啟動系統設定");
                ((MainActivity) activity).openSettings();
                break;
            case "switch_language":
                activity.announceInfo("正在切換語言");
                ((MainActivity) activity).toggleLanguage();
                break;
            case "go_home":
                activity.announceInfo("返回主頁");
                activity.finish();
                break;
            case "go_back":
                activity.announceInfo("返回上一頁");
                activity.finish();
                break;
            case "tell_time":
                activity.announceInfo("時間功能開發中");
                break;
            case "stop_listening":
                activity.announceInfo("停止語音識別");
                stopVoiceRecognition();
                break;
            default:
                activity.announceError("未識別的命令，請說出有效的命令");
                activity.vibrationManager.vibrateError();
                break;
        }
    }
    
    /**
     * 匹配命令
     */
    private String matchCommand(String text) {
        Map<String, String> commandMap;
        
        switch (currentLanguage) {
            case "english":
                commandMap = englishCommands;
                break;
            case "mandarin":
                commandMap = mandarinCommands;
                break;
            case "cantonese":
            default:
                commandMap = cantoneseCommands;
                break;
        }
        
        // 精確匹配
        for (Map.Entry<String, String> entry : commandMap.entrySet()) {
            if (text.contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }
        
        return null;
    }
    
    /**
     * 設置命令監聽器
     */
    public void setCommandListener(VoiceCommandListener listener) {
        this.commandListener = listener;
    }
    
    /**
     * 是否正在監聽
     */
    public boolean isListening() {
        return isListening;
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
                return "沒有匹配結果";
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
     * 釋放資源
     */
    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
            Log.d(TAG, "語音識別器已釋放");
        }
        isListening = false;
    }
}