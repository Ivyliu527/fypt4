package com.example.tonbo_app;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 語音命令控制頁面
 */
public class VoiceCommandActivity extends BaseAccessibleActivity {
    
    private static final String TAG = "VoiceCommandActivity";
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 200;
    
    private VoiceCommandManager voiceCommandManager;
    private VoiceAIAssistant aiAssistant;
    private StreamingVoiceAI streamingAI; // 高級即時對話AI
    private Button listenButton;
    private TextView statusText;
    private TextView commandText;
    private TextView hintText;
    private TextView pageTitle;
    private TextView availableCommandsTitle;
    
    private boolean isListening = false;
    private boolean useStreamingMode = false; // 是否使用連續對話模式
    
    // 聊天記錄導航
    private int currentHistoryIndex = -1; // 當前查看的記錄索引（-1表示未在查看記錄）
    private List<ConversationManager.ConversationTurn> chatHistory = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_command);
        
        // 獲取語言設置
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("language")) {
            currentLanguage = intent.getStringExtra("language");
        }
        
        initViews();
        initVoiceCommandManager();
        checkPermissions();
        
        // 頁面標題播報
        announcePageTitle();
    }
    
    @Override
    protected void announcePageTitle() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            String cantoneseText = "語音AI助手頁面。你可以同我聊天，或者說出指令控制應用，例如：打開環境識別、讀文件、緊急求助等。" +
                    "你可以說「查看聊天記錄」來回顧之前的對話，說「上一句」或「下一句」來瀏覽記錄。" +
                    "點擊中間的按鈕開始對話。長按按鈕可以切換到連續對話模式，無需每次點擊即可持續對話。";
            String englishText = "Voice AI Assistant page. You can chat with me or speak commands to control the app, such as: open environment, read document, emergency help. " +
                    "You can say 'view chat history' to review previous conversations, say 'previous message' or 'next message' to navigate. " +
                    "Tap the center button to start conversation. Long press the button to switch to continuous conversation mode for hands-free continuous dialogue.";
            ttsManager.speak(cantoneseText, englishText, true);
        }, 500);
    }
    
    private void initViews() {
        listenButton = findViewById(R.id.listenButton);
        statusText = findViewById(R.id.statusText);
        commandText = findViewById(R.id.commandText);
        hintText = findViewById(R.id.hintText);
        pageTitle = findViewById(R.id.pageTitle);
        availableCommandsTitle = findViewById(R.id.availableCommandsTitle);
        
        // 設置監聽按鈕
        listenButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            toggleListening();
        });
        
        // 長按切換連續對話模式
        listenButton.setOnLongClickListener(v -> {
            vibrationManager.vibrateLongPress();
            toggleStreamingMode();
            return true;
        });
        
        // 返回按鈕
        android.widget.ImageButton backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                handleBackPressed();
            });
        }
        
        // 根據當前語言更新界面文字
        updateLanguageUI();
        
        updateUI(false);
    }
    
    /**
     * 更新語言UI
     */
    private void updateLanguageUI() {
        if (pageTitle != null) {
            pageTitle.setText(getLocalizedString("voice_command_title"));
        }
        
        if (statusText != null) {
            statusText.setText(getLocalizedString("listening_status"));
        }
        
        if (availableCommandsTitle != null) {
            availableCommandsTitle.setText(getLocalizedString("available_commands"));
        }
        
        if (hintText != null) {
            hintText.setText(getLocalizedString("commands_list"));
        }
    }
    
    /**
     * 根據當前語言獲取本地化字符串
     */
    private String getLocalizedString(String key) {
        String currentLang = LocaleManager.getInstance(this).getCurrentLanguage();
        
        switch (key) {
            case "voice_command_title":
                if ("english".equals(currentLang)) {
                    return "Voice Assistant";
                } else if ("mandarin".equals(currentLang)) {
                    return "语音助手";
                } else {
                    return "語音助手";
                }
            case "listening_status":
                if ("english".equals(currentLang)) {
                    return "Click to Start";
                } else if ("mandarin".equals(currentLang)) {
                    return "点击开始";
                } else {
                    return "點擊開始";
                }
            case "listening_active":
                if ("english".equals(currentLang)) {
                    return "Listening...";
                } else if ("mandarin".equals(currentLang)) {
                    return "正在监听...";
                } else {
                    return "正在監聽...";
                }
            case "available_commands":
                if ("english".equals(currentLang)) {
                    return "Available Commands";
                } else if ("mandarin".equals(currentLang)) {
                    return "可用指令";
                } else {
                    return "可用指令";
                }
            case "commands_list":
                if ("english".equals(currentLang)) {
                    return "• Environment Recognition: Open environment, Look around\n" +
                           "• Document Assistant: Read document, Scan document\n" +
                           "• Find Items: Find items, Find things\n" +
                           "• Live Assistance: Live assistance, Help\n" +
                           "• Emergency: Emergency, Help me\n" +
                           "• System Settings: Settings, Open settings\n" +
                           "• Language Switch: Switch language, Change language\n" +
                           "• Return Home: Go home, Home\n" +
                           "• Control: Start detection, Stop detection, Describe environment\n" +
                           "• Utilities: What time is it, Repeat, Volume up/down\n" +
                           "• Chat History: View chat history, Previous message, Next message, Repeat last message, Clear history\n" +
                           "• Continuous Commands: Say multiple commands, e.g. \"Open environment then start detection\"";
                } else if ("mandarin".equals(currentLang)) {
                    return "• 环境识别：打开环境识别、看看周围\n" +
                           "• 阅读助手：读文件、扫描文件\n" +
                           "• 寻找物品：找东西、寻找物品\n" +
                           "• 即时协助：即时协助、帮助\n" +
                           "• 紧急求助：紧急求助、救命\n" +
                           "• 系统设置：设置、打开设置\n" +
                           "• 语言切换：切换语言、转换语言\n" +
                           "• 返回主页：返回主页、主页\n" +
                           "• 控制命令：开始检测、停止检测、描述环境\n" +
                           "• 实用功能：现在几点、重复、增大/减小音量\n" +
                           "• 聊天记录：查看聊天记录、上一句、下一句、重复上一句、清除记录\n" +
                           "• 连续命令：一次说多个命令，例如「打开环境识别然后开始检测」";
                } else {
                    return "• 環境識別：打開環境識別、睇下周圍\n" +
                           "• 閱讀助手：讀文件、掃描文件\n" +
                           "• 尋找物品：搵嘢、尋找物品\n" +
                           "• 即時協助：即時協助、幫手\n" +
                           "• 緊急求助：緊急求助、救命\n" +
                           "• 系統設定：設定、打開設定\n" +
                           "• 語言切換：轉語言、切換語言\n" +
                           "• 返回主頁：返回主頁、主頁\n" +
                           "• 控制命令：開始檢測、停止檢測、描述環境\n" +
                           "• 實用功能：現在幾點、重複、增大/減小音量\n" +
                           "• 聊天記錄：查看聊天記錄、上一句、下一句、重複上一句、清除記錄\n" +
                           "• 連續命令：一次說多個命令，例如「打開環境識別然後開始檢測」";
                }
            default:
                return getString(R.string.app_name);
        }
    }
    
    private void initVoiceCommandManager() {
        voiceCommandManager = VoiceCommandManager.getInstance(this);
        voiceCommandManager.setLanguage(currentLanguage);
        
        // 初始化AI助手
        aiAssistant = new VoiceAIAssistant(this);
        aiAssistant.setLanguage(currentLanguage);
        
        // 初始化高級即時對話AI
        initStreamingAI();
        
        // 啟用 ASRManager 並設置引擎
        setupASRManager();
        
        voiceCommandManager.setCommandListener(new VoiceCommandManager.ExtendedVoiceCommandListener() {
            @Override
            public void onCommandRecognized(String command, String originalText) {
                runOnUiThread(() -> {
                    if ("continuous_commands".equals(command)) {
                        // 連續命令將通過 onContinuousCommandsRecognized 處理
                        return;
                    }
                    vibrationManager.vibrateSuccess();
                    String recognizedText = "english".equals(currentLanguage) ? 
                        "Recognized: " + originalText : 
                        ("mandarin".equals(currentLanguage) ? "识别到: " : "識別到: ") + originalText;
                    commandText.setText(recognizedText);
                    executeCommand(command, originalText);
                });
            }
            
            @Override
            public void onContinuousCommandsRecognized(List<VoiceCommandManager.CommandPair> commands, String originalText) {
                runOnUiThread(() -> {
                    vibrationManager.vibrateSuccess();
                    String recognizedText = "english".equals(currentLanguage) ? 
                        "Recognized continuous commands: " + originalText :
                        ("mandarin".equals(currentLanguage) ? "识别到连续命令: " : "識別到連續命令: ") + originalText;
                    commandText.setText(recognizedText);
                    executeContinuousCommands(commands, originalText);
                });
            }
            
            @Override
            public void onListeningStarted() {
                runOnUiThread(() -> {
                    updateUI(true);
                    statusText.setText(getLocalizedString("listening_active"));
                    String announceText = "english".equals(currentLanguage) ? 
                        "Start listening, please speak your command" :
                        ("mandarin".equals(currentLanguage) ? "开始监听，请说出指令" : "開始監聽，請說出指令");
                    announceInfo(announceText);
                });
            }
            
            @Override
            public void onListeningStopped() {
                runOnUiThread(() -> {
                    updateUI(false);
                    statusText.setText(getLocalizedString("listening_status"));
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    vibrationManager.vibrateError();
                    String errorMessage = getLocalizedErrorMessage(error);
                    statusText.setText(errorMessage);
                    announceError(errorMessage);
                    updateUI(false);
                });
            }
            
            @Override
            public void onPartialResult(String partialText) {
                runOnUiThread(() -> {
                    String recognizingText = "english".equals(currentLanguage) ? 
                        "Recognizing: " + partialText :
                        ("mandarin".equals(currentLanguage) ? "识别中: " : "識別中: ") + partialText;
                    commandText.setText(recognizingText);
                });
            }
            
            @Override
            public void onTextRecognized(String text) {
                runOnUiThread(() -> {
                    // 識別到非命令的語音，使用AI助手處理
                    vibrationManager.vibrateSuccess();
                    
                    // 更新AI助手的語言
                    aiAssistant.setLanguage(currentLanguage);
                    
                    // 檢查是否為命令（雙重檢查）
                    String command = voiceCommandManager.checkIfCommand(text);
                    if (command != null) {
                        // 實際上是命令，執行命令
                        executeCommand(command, text);
                        return;
                    }
                    
                    // 顯示用戶說的話
                    String recognizedText = "english".equals(currentLanguage) ? 
                        "You: " + text :
                        ("mandarin".equals(currentLanguage) ? "您: " : "您: ") + text;
                    commandText.setText(recognizedText);
                    
                    // 使用AI助手處理對話（異步版本）
                    aiAssistant.processInputAsync(text, new VoiceAIAssistant.AssistantResponseCallback() {
                        @Override
                        public void onResponse(VoiceAIAssistant.AssistantResponse response) {
                            runOnUiThread(() -> {
                                // 播報AI助手回應
                                if (response != null && response.response != null && !response.response.isEmpty()) {
                                    // 延遲一點再播報回應，讓用戶聽到識別結果
                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        announceInfo(response.response);
                                    }, 500);
                                } else {
                                    // 如果無法生成回應，播報識別到的文本
                                    String announceText = "english".equals(currentLanguage) ? 
                                        "You said: " + text :
                                        ("mandarin".equals(currentLanguage) ? "您說: " : "您說: ") + text;
                                    announceInfo(announceText);
                                }
                            });
                        }
                    });
                });
            }
        });
    }
    
    /**
     * 初始化高級即時對話AI
     */
    private void initStreamingAI() {
        streamingAI = new StreamingVoiceAI(this);
        streamingAI.setLanguage(currentLanguage);
        streamingAI.setCallback(new StreamingVoiceAI.StreamingAICallback() {
            @Override
            public void onPartialText(String partialText) {
                runOnUiThread(() -> {
                    // 實時顯示識別中的文本
                    String recognizingText = "english".equals(currentLanguage) ? 
                        "Recognizing: " + partialText :
                        ("mandarin".equals(currentLanguage) ? "识别中: " : "識別中: ") + partialText;
                    commandText.setText(recognizingText);
                    commandText.setTextColor(getResources().getColor(android.R.color.holo_blue_light));
                });
            }
            
            @Override
            public void onFinalText(String finalText) {
                runOnUiThread(() -> {
                    vibrationManager.vibrateSuccess();
                    // 顯示最終識別結果
                    String recognizedText = "english".equals(currentLanguage) ? 
                        "You: " + finalText :
                        ("mandarin".equals(currentLanguage) ? "您: " : "您: ") + finalText;
                    commandText.setText(recognizedText);
                    commandText.setTextColor(getResources().getColor(android.R.color.white));
                });
            }
            
            @Override
            public void onAIResponse(String response) {
                runOnUiThread(() -> {
                    // 顯示AI回應
                    String aiText = "english".equals(currentLanguage) ? 
                        "AI: " + response :
                        ("mandarin".equals(currentLanguage) ? "AI: " : "AI: ") + response;
                    commandText.setText(aiText);
                    commandText.setTextColor(getResources().getColor(android.R.color.holo_green_light));
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    vibrationManager.vibrateError();
                    String errorMessage = getLocalizedErrorMessage(error);
                    statusText.setText(errorMessage);
                    announceError(errorMessage);
                    updateUI(false);
                });
            }
            
            @Override
            public void onListeningStateChanged(boolean isListening) {
                runOnUiThread(() -> {
                    VoiceCommandActivity.this.isListening = isListening;
                    updateUI(isListening);
                    if (isListening) {
                        statusText.setText(getLocalizedString("listening_active"));
                        String announceText = "english".equals(currentLanguage) ? 
                            "Continuous conversation mode active, please speak" :
                            ("mandarin".equals(currentLanguage) ? "连续对话模式已激活，请说话" : "連續對話模式已激活，請說話");
                        announceInfo(announceText);
                    } else {
                        statusText.setText(getLocalizedString("listening_status"));
                    }
                });
            }
        });
    }
    
    /**
     * 切換連續對話模式
     */
    private void toggleStreamingMode() {
        useStreamingMode = !useStreamingMode;
        
        // 如果正在監聽，先停止
        if (isListening) {
            if (useStreamingMode) {
                stopListening();
                // 延遲啟動連續對話模式
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    startStreamingConversation();
                }, 500);
            } else {
                stopStreamingConversation();
                // 延遲啟動普通模式
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    startListening();
                }, 500);
            }
        }
        
        // 播報模式切換
        String modeText = useStreamingMode ? 
            ("english".equals(currentLanguage) ? "Switched to continuous conversation mode" :
             ("mandarin".equals(currentLanguage) ? "已切换到连续对话模式" : "已切換到連續對話模式")) :
            ("english".equals(currentLanguage) ? "Switched to normal mode" :
             ("mandarin".equals(currentLanguage) ? "已切换到普通模式" : "已切換到普通模式"));
        announceInfo(modeText);
        
        // 更新按鈕提示
        updateModeHint();
    }
    
    /**
     * 更新模式提示
     */
    private void updateModeHint() {
        String hint = useStreamingMode ?
            ("english".equals(currentLanguage) ? "Long press to switch to normal mode" :
             ("mandarin".equals(currentLanguage) ? "长按切换到普通模式" : "長按切換到普通模式")) :
            ("english".equals(currentLanguage) ? "Long press to switch to continuous conversation mode" :
             ("mandarin".equals(currentLanguage) ? "长按切换到连续对话模式" : "長按切換到連續對話模式"));
        
        if (hintText != null) {
            String currentHint = hintText.getText().toString();
            // 保留原有提示，添加模式提示
            if (!currentHint.contains("長按") && !currentHint.contains("长按") && !currentHint.contains("Long press")) {
                hintText.setText(currentHint + "\n\n" + hint);
            }
        }
    }
    
    /**
     * 開始連續對話
     */
    private void startStreamingConversation() {
        if (streamingAI == null) {
            initStreamingAI();
        }
        
        if (streamingAI != null) {
            streamingAI.setLanguage(currentLanguage);
            streamingAI.startContinuousConversation();
        }
    }
    
    /**
     * 停止連續對話
     */
    private void stopStreamingConversation() {
        if (streamingAI != null) {
            streamingAI.stopConversation();
        }
    }
    
    /**
     * 設置 ASRManager
     * 檢查可用的 ASR 引擎並啟用最合適的引擎
     */
    private void setupASRManager() {
        // 獲取可用的 ASR 引擎列表
        List<ASRManager.ASREngine> availableEngines = voiceCommandManager.getAvailableASREngines();
        
        if (availableEngines == null || availableEngines.isEmpty()) {
            Log.w(TAG, "沒有可用的 ASR 引擎，將使用原生 SpeechRecognizer");
            return;
        }
        
        Log.d(TAG, "可用的 ASR 引擎: " + availableEngines);
        
        // 優先選擇順序：SHERPA_ONNX > FUNASR > ANDROID_NATIVE
        ASRManager.ASREngine selectedEngine = null;
        
        if (availableEngines.contains(ASRManager.ASREngine.SHERPA_ONNX)) {
            selectedEngine = ASRManager.ASREngine.SHERPA_ONNX;
            Log.d(TAG, "選擇 sherpa-onnx 引擎");
        } else if (availableEngines.contains(ASRManager.ASREngine.FUNASR)) {
            selectedEngine = ASRManager.ASREngine.FUNASR;
            Log.d(TAG, "選擇 FunASR 引擎");
        } else if (availableEngines.contains(ASRManager.ASREngine.ANDROID_NATIVE)) {
            // 注意：AndroidNativeASR 目前返回錯誤，所以跳過
            // selectedEngine = ASRManager.ASREngine.ANDROID_NATIVE;
            Log.w(TAG, "Android Native ASR 目前不可用，跳過");
        }
        
        if (selectedEngine != null) {
            // 設置 ASR 引擎
            voiceCommandManager.setASREngine(selectedEngine);
            
            // 啟用 ASRManager
            voiceCommandManager.setUseASRManager(true);
            
            String engineName = selectedEngine == ASRManager.ASREngine.SHERPA_ONNX ? "sherpa-onnx" :
                               selectedEngine == ASRManager.ASREngine.FUNASR ? "FunASR" : "未知";
            
            Log.d(TAG, "已啟用 ASRManager，使用引擎: " + engineName);
        } else {
            Log.w(TAG, "沒有可用的 ASR 引擎，將使用原生 SpeechRecognizer");
        }
    }
    
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_RECORD_AUDIO);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                String message = "english".equals(currentLanguage) ? 
                    "Microphone permission granted" :
                    ("mandarin".equals(currentLanguage) ? "录音权限已授予" : "錄音權限已授予");
                announceInfo(message);
            } else {
                String message = "english".equals(currentLanguage) ? 
                    "Microphone permission needed to use voice commands" :
                    ("mandarin".equals(currentLanguage) ? "需要录音权限才能使用语音命令" : "需要錄音權限才能使用語音命令");
                announceError(message);
            }
        }
    }
    
    private void toggleListening() {
        if (isListening) {
            stopListening();
        } else {
            startListening();
        }
    }
    
    private void startListening() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            String message = "english".equals(currentLanguage) ? 
                "Please grant microphone permission first" :
                ("mandarin".equals(currentLanguage) ? "请先授予录音权限" : "請先授予錄音權限");
            announceError(message);
            checkPermissions();
            return;
        }
        
        // 根據模式選擇啟動方式
        if (useStreamingMode) {
            startStreamingConversation();
        } else {
            isListening = true;
            voiceCommandManager.startListening();
        }
    }
    
    private void stopListening() {
        isListening = false;
        
        // 根據模式選擇停止方式
        if (useStreamingMode) {
            stopStreamingConversation();
        } else {
            voiceCommandManager.stopListening();
        }
        
        updateUI(false);
    }
    
    private void updateUI(boolean listening) {
        isListening = listening;
        if (listening) {
            listenButton.setText("⏸️");
            listenButton.setContentDescription(getLocalizedString("listening_active"));
            statusText.setText(getLocalizedString("listening_active"));
            statusText.setTextColor(getResources().getColor(android.R.color.holo_green_light));
        } else {
            listenButton.setText("🎤");
            listenButton.setContentDescription(getLocalizedString("listening_status"));
            statusText.setText(getLocalizedString("listening_status"));
            statusText.setTextColor(getResources().getColor(android.R.color.white));
        }
    }
    
    private String lastCommand = null;
    private String lastCommandText = null;
    private Handler commandHandler = new Handler(Looper.getMainLooper());
    
    /**
     * 執行連續命令
     */
    private void executeContinuousCommands(List<VoiceCommandManager.CommandPair> commands, String originalText) {
        if (commands == null || commands.isEmpty()) {
            String message = "english".equals(currentLanguage) ? 
                "No commands to execute" :
                ("mandarin".equals(currentLanguage) ? "没有可执行的命令" : "沒有可執行的命令");
            announceError(message);
            return;
        }
        
        // 播報識別到的連續命令數量
        String cantoneseText = "識別到 " + commands.size() + " 個命令，將按順序執行";
        String englishText = "Recognized " + commands.size() + " commands, will execute in order";
        ttsManager.speak(cantoneseText, englishText, true);
        
        // 延遲執行，讓用戶聽到提示
        commandHandler.postDelayed(() -> {
            executeCommandSequence(commands, 0);
        }, 1500);
    }
    
    /**
     * 按順序執行命令序列
     */
    private void executeCommandSequence(List<VoiceCommandManager.CommandPair> commands, int index) {
        if (index >= commands.size()) {
            // 所有命令執行完成
            String cantoneseText = "所有命令已執行完成";
            String englishText = "All commands executed";
            ttsManager.speak(cantoneseText, englishText, false);
            return;
        }
        
        VoiceCommandManager.CommandPair commandPair = commands.get(index);
        String command = commandPair.command;
        String originalText = commandPair.originalText;
        
        // 播報當前執行的命令（最後一個命令不播報，直接執行）
        if (index < commands.size() - 1) {
            String cantoneseText = "執行第 " + (index + 1) + " 個命令：" + originalText;
            String englishText = "Executing command " + (index + 1) + ": " + originalText;
            ttsManager.speak(cantoneseText, englishText, false);
        }
        
        // 執行命令
        performCommand(command, originalText);
        
        // 延遲執行下一個命令（給前一個命令時間完成）
        int delay = getCommandDelay(command);
        commandHandler.postDelayed(() -> {
            executeCommandSequence(commands, index + 1);
        }, delay);
    }
    
    /**
     * 根據命令類型獲取執行延遲時間（毫秒）
     */
    private int getCommandDelay(String command) {
        // 導航類命令需要更多時間（Activity切換）
        if (command.startsWith("open_") || "go_home".equals(command) || "go_back".equals(command)) {
            return 2000; // 2秒
        }
        // 緊急命令需要立即執行
        if ("emergency".equals(command)) {
            return 1000; // 1秒
        }
        // 其他命令
        return 1500; // 1.5秒
    }
    
    private void executeCommand(String command, String originalText) {
        Log.d(TAG, "執行命令: " + command);
        
        // 保存最後一個命令（用於重複功能）
        lastCommand = command;
        lastCommandText = originalText;
        
        // 命令確認（重要命令需要確認）
        if (needsConfirmation(command)) {
            confirmCommand(command, originalText);
            return;
        }
        
        // 直接執行命令
        performCommand(command, originalText);
    }
    
    /**
     * 判斷命令是否需要確認
     */
    private boolean needsConfirmation(String command) {
        // 重要操作需要確認
        return "emergency".equals(command) || 
               "switch_language".equals(command) ||
               "go_home".equals(command);
    }
    
    /**
     * 確認命令執行
     */
    private void confirmCommand(String command, String originalText) {
        String confirmText = getCommandConfirmText(command);
        announceInfo(confirmText);
        
        // 延遲執行，給用戶時間取消
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            performCommand(command, originalText);
        }, 2000); // 2秒後執行
    }
    
    /**
     * 獲取命令確認文本
     */
    private String getCommandConfirmText(String command) {
        if ("english".equals(currentLanguage)) {
            switch (command) {
                case "emergency":
                    return "Emergency alert will be triggered in 2 seconds";
                case "switch_language":
                    return "Language will be switched in 2 seconds";
                case "go_home":
                    return "Returning to home in 2 seconds";
                default:
                    return "Command will be executed in 2 seconds";
            }
        } else if ("mandarin".equals(currentLanguage)) {
            switch (command) {
                case "emergency":
                    return "緊急警報將在2秒後觸發";
                case "switch_language":
                    return "語言將在2秒後切換";
                case "go_home":
                    return "將在2秒後返回主頁";
                default:
                    return "命令將在2秒後執行";
            }
        } else {
            switch (command) {
                case "emergency":
                    return "緊急警報將在2秒後觸發";
                case "switch_language":
                    return "語言將在2秒後切換";
                case "go_home":
                    return "將在2秒後返回主頁";
                default:
                    return "命令將在2秒後執行";
            }
        }
    }
    
    /**
     * 執行命令
     */
    private void performCommand(String command, String originalText) {
        switch (command) {
            case "open_environment":
                String envNav = "english".equals(currentLanguage) ? 
                    "Opening environment recognition" :
                    ("mandarin".equals(currentLanguage) ? "正在打开环境识别" : "正在打開環境識別");
                announceNavigation(envNav);
                // 使用與主頁面相同的 RealAIDetectionActivity
                startActivity(new Intent(this, RealAIDetectionActivity.class).putExtra("language", currentLanguage));
                break;
                
            case "open_document":
                String docNav = "english".equals(currentLanguage) ? 
                    "Opening document assistant" :
                    ("mandarin".equals(currentLanguage) ? "正在打开阅读助手" : "正在打開閱讀助手");
                announceNavigation(docNav);
                startActivity(new Intent(this, DocumentCurrencyActivity.class).putExtra("language", currentLanguage));
                break;
                
            case "open_find":
                String findNav = "english".equals(currentLanguage) ? 
                    "Opening find items" :
                    ("mandarin".equals(currentLanguage) ? "正在打开寻找物品" : "正在打開尋找物品");
                announceNavigation(findNav);
                startActivity(new Intent(this, FindItemsActivity.class).putExtra("language", currentLanguage));
                break;
                
            case "open_assistance":
                String assistNav = "english".equals(currentLanguage) ? 
                    "Opening live assistance" :
                    ("mandarin".equals(currentLanguage) ? "正在打开即时协助" : "正在打開即時協助");
                announceNavigation(assistNav);
                startActivity(new Intent(this, InstantAssistanceActivity.class).putExtra("language", currentLanguage));
                break;
                
            case "emergency":
                String emergencyText = "english".equals(currentLanguage) ? 
                    "Triggering emergency alert" :
                    ("mandarin".equals(currentLanguage) ? "触发紧急求助" : "觸發緊急求助");
                announceInfo(emergencyText);
                // 直接撥打999，不顯示選擇對話框
                List<String> emergency999 = new java.util.ArrayList<>();
                emergency999.add("999");
                EmergencyManager.getInstance(this).triggerEmergencyAlert(emergency999);
                break;
                
            case "go_home":
                String homeNav = "english".equals(currentLanguage) ? 
                    "Returning to home" :
                    ("mandarin".equals(currentLanguage) ? "返回主页" : "返回主頁");
                announceNavigation(homeNav);
                Intent homeIntent = new Intent(this, MainActivity.class);
                homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(homeIntent);
                finish();
                break;
                
            case "go_back":
                String backText = "english".equals(currentLanguage) ? 
                    "Going back" :
                    ("mandarin".equals(currentLanguage) ? "返回上一页" : "返回上一頁");
                announceInfo(backText);
                finish();
                break;
                
            case "switch_language":
                switchLanguage();
                break;
                
            case "open_settings":
                String settingsNav = "english".equals(currentLanguage) ? 
                    "Opening system settings" :
                    ("mandarin".equals(currentLanguage) ? "正在打开系统设置" : "正在打開系統設定");
                announceNavigation(settingsNav);
                startActivity(new Intent(this, SettingsActivity.class).putExtra("language", currentLanguage));
                break;
                
            case "tell_time":
                tellTime();
                break;
                
            case "stop_listening":
                String stopText = "english".equals(currentLanguage) ? 
                    "Stopped listening" :
                    ("mandarin".equals(currentLanguage) ? "停止监听" : "停止監聽");
                announceInfo(stopText);
                stopListening();
                break;
                
            // 環境識別控制命令
            case "start_detection":
                if (isInEnvironmentActivity()) {
                    sendBroadcastToEnvironment("start_detection");
                    String startText = "english".equals(currentLanguage) ? 
                        "Starting environment detection" :
                        ("mandarin".equals(currentLanguage) ? "开始环境检测" : "開始環境檢測");
                    announceInfo(startText);
                } else {
                    String needOpenText = "english".equals(currentLanguage) ? 
                        "Please open environment recognition first" :
                        ("mandarin".equals(currentLanguage) ? "请先打开环境识别功能" : "請先打開環境識別功能");
                    announceInfo(needOpenText);
                }
                break;
                
            case "stop_detection":
                if (isInEnvironmentActivity()) {
                    sendBroadcastToEnvironment("stop_detection");
                    String stopDetText = "english".equals(currentLanguage) ? 
                        "Stopped environment detection" :
                        ("mandarin".equals(currentLanguage) ? "停止环境检测" : "停止環境檢測");
                    announceInfo(stopDetText);
                } else {
                    String needOpenText2 = "english".equals(currentLanguage) ? 
                        "Please open environment recognition first" :
                        ("mandarin".equals(currentLanguage) ? "请先打开环境识别功能" : "請先打開環境識別功能");
                    announceInfo(needOpenText2);
                }
                break;
                
            case "describe_environment":
                if (isInEnvironmentActivity()) {
                    sendBroadcastToEnvironment("describe_environment");
                    String describeText = "english".equals(currentLanguage) ? 
                        "Describing environment" :
                        ("mandarin".equals(currentLanguage) ? "正在描述环境" : "正在描述環境");
                    announceInfo(describeText);
                } else {
                    String needOpenText3 = "english".equals(currentLanguage) ? 
                        "Please open environment recognition first" :
                        ("mandarin".equals(currentLanguage) ? "请先打开环境识别功能" : "請先打開環境識別功能");
                    announceInfo(needOpenText3);
                }
                break;
                
            // 控制命令
            case "repeat":
                if (lastCommand != null) {
                    String repeatText = "english".equals(currentLanguage) ? 
                        "Repeat: " + lastCommandText :
                        ("mandarin".equals(currentLanguage) ? "重复执行: " : "重複執行: ") + lastCommandText;
                    announceInfo(repeatText);
                    performCommand(lastCommand, lastCommandText);
                } else {
                    String noRepeatText = "english".equals(currentLanguage) ? 
                        "No command to repeat" :
                        ("mandarin".equals(currentLanguage) ? "没有可重复的命令" : "沒有可重複的命令");
                    announceInfo(noRepeatText);
                }
                break;
                
            case "volume_up":
                adjustVolume(true);
                break;
                
            case "volume_down":
                adjustVolume(false);
                break;
                
            // 聊天記錄命令
            case "view_chat_history":
                viewChatHistory();
                break;
                
            case "previous_message":
                navigateToPreviousMessage();
                break;
                
            case "next_message":
                navigateToNextMessage();
                break;
                
            case "repeat_last_message":
                repeatLastMessage();
                break;
                
            case "clear_chat_history":
                clearChatHistory();
                break;
                
            default:
                announceError("未知命令: " + command);
                suggestSimilarCommands(originalText);
                break;
        }
    }
    
    /**
     * 查看聊天記錄（語音播報）
     */
    private void viewChatHistory() {
        // 獲取聊天記錄
        if (aiAssistant != null) {
            chatHistory = aiAssistant.getConversationManager().getAllHistory();
        }
        
        if (chatHistory == null || chatHistory.isEmpty()) {
            String noHistoryText = "english".equals(currentLanguage) ? 
                "No chat history yet" :
                ("mandarin".equals(currentLanguage) ? "还没有聊天记录" : "還沒有聊天記錄");
            announceInfo(noHistoryText);
            currentHistoryIndex = -1;
            return;
        }
        
        // 設置索引到最後一條記錄
        currentHistoryIndex = chatHistory.size() - 1;
        
        // 播報記錄總數和當前位置
        String summaryText;
        if ("english".equals(currentLanguage)) {
            summaryText = "Chat history: " + chatHistory.size() + " messages. " +
                         "Currently viewing message " + (currentHistoryIndex + 1) + " of " + chatHistory.size() + ". " +
                         "Say 'previous message' or 'next message' to navigate.";
        } else if ("mandarin".equals(currentLanguage)) {
            summaryText = "聊天記錄共有 " + chatHistory.size() + " 條消息。 " +
                         "當前查看第 " + (currentHistoryIndex + 1) + " 條，共 " + chatHistory.size() + " 條。 " +
                         "說「上一句」或「下一句」來瀏覽。";
        } else {
            summaryText = "聊天記錄共有 " + chatHistory.size() + " 條消息。 " +
                         "當前查看第 " + (currentHistoryIndex + 1) + " 條，共 " + chatHistory.size() + " 條。 " +
                         "說「上一句」或「下一句」來瀏覽。";
        }
        
        announceInfo(summaryText);
        
        // 延遲播報當前記錄
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            announceCurrentMessage();
        }, 2000);
    }
    
    /**
     * 播報當前查看的消息
     */
    private void announceCurrentMessage() {
        if (currentHistoryIndex < 0 || currentHistoryIndex >= chatHistory.size()) {
            return;
        }
        
        ConversationManager.ConversationTurn turn = chatHistory.get(currentHistoryIndex);
        
        // 構建播報文本
        String messageText;
        if ("english".equals(currentLanguage)) {
            messageText = "Message " + (currentHistoryIndex + 1) + ". " +
                         "You said: " + turn.userInput + ". " +
                         "Assistant replied: " + (turn.assistantResponse != null ? turn.assistantResponse : "No response");
        } else if ("mandarin".equals(currentLanguage)) {
            messageText = "第 " + (currentHistoryIndex + 1) + " 條消息。 " +
                         "您說：" + turn.userInput + "。 " +
                         "助手回復：" + (turn.assistantResponse != null ? turn.assistantResponse : "無回應");
        } else {
            messageText = "第 " + (currentHistoryIndex + 1) + " 條消息。 " +
                         "您說：" + turn.userInput + "。 " +
                         "助手回復：" + (turn.assistantResponse != null ? turn.assistantResponse : "無回應");
        }
        
        announceInfo(messageText);
    }
    
    /**
     * 導航到上一條消息
     */
    private void navigateToPreviousMessage() {
        if (chatHistory == null || chatHistory.isEmpty()) {
            viewChatHistory(); // 如果沒有加載記錄，先加載
            return;
        }
        
        if (currentHistoryIndex < 0) {
            // 如果還沒開始查看，從最後一條開始
            currentHistoryIndex = chatHistory.size() - 1;
        } else if (currentHistoryIndex > 0) {
            currentHistoryIndex--;
        } else {
            // 已經是第一條
            String firstMessageText = "english".equals(currentLanguage) ? 
                "This is the first message" :
                ("mandarin".equals(currentLanguage) ? "这是第一条消息" : "這是第一條消息");
            announceInfo(firstMessageText);
            return;
        }
        
        announceCurrentMessage();
    }
    
    /**
     * 導航到下一條消息
     */
    private void navigateToNextMessage() {
        if (chatHistory == null || chatHistory.isEmpty()) {
            viewChatHistory(); // 如果沒有加載記錄，先加載
            return;
        }
        
        if (currentHistoryIndex < 0) {
            // 如果還沒開始查看，從第一條開始
            currentHistoryIndex = 0;
        } else if (currentHistoryIndex < chatHistory.size() - 1) {
            currentHistoryIndex++;
        } else {
            // 已經是最後一條
            String lastMessageText = "english".equals(currentLanguage) ? 
                "This is the last message" :
                ("mandarin".equals(currentLanguage) ? "这是最后一条消息" : "這是最後一條消息");
            announceInfo(lastMessageText);
            return;
        }
        
        announceCurrentMessage();
    }
    
    /**
     * 重複上一條消息
     */
    private void repeatLastMessage() {
        if (aiAssistant != null) {
            List<ConversationManager.ConversationTurn> history = 
                aiAssistant.getConversationManager().getAllHistory();
            
            if (history == null || history.isEmpty()) {
                String noHistoryText = "english".equals(currentLanguage) ? 
                    "No previous message" :
                    ("mandarin".equals(currentLanguage) ? "没有上一条消息" : "沒有上一條消息");
                announceInfo(noHistoryText);
                return;
            }
            
            ConversationManager.ConversationTurn lastTurn = history.get(history.size() - 1);
            
            String repeatText;
            if ("english".equals(currentLanguage)) {
                repeatText = "Last message. You said: " + lastTurn.userInput + ". " +
                           "Assistant replied: " + (lastTurn.assistantResponse != null ? lastTurn.assistantResponse : "No response");
            } else if ("mandarin".equals(currentLanguage)) {
                repeatText = "上一條消息。您說：" + lastTurn.userInput + "。 " +
                           "助手回復：" + (lastTurn.assistantResponse != null ? lastTurn.assistantResponse : "無回應");
            } else {
                repeatText = "上一條消息。您說：" + lastTurn.userInput + "。 " +
                           "助手回復：" + (lastTurn.assistantResponse != null ? lastTurn.assistantResponse : "無回應");
            }
            
            announceInfo(repeatText);
        }
    }
    
    /**
     * 清除聊天記錄
     */
    private void clearChatHistory() {
        if (aiAssistant != null) {
            aiAssistant.getConversationManager().clearHistory();
            chatHistory.clear();
            currentHistoryIndex = -1;
            
            String clearedText = "english".equals(currentLanguage) ? 
                "Chat history cleared" :
                ("mandarin".equals(currentLanguage) ? "聊天记录已清除" : "聊天記錄已清除");
            announceInfo(clearedText);
        }
    }
    
    /**
     * 檢查是否在環境識別頁面
     */
    private boolean isInEnvironmentActivity() {
        // 使用 RealAIDetectionActivity 的靜態方法檢查
        return RealAIDetectionActivity.isActive();
    }
    
    /**
     * 發送廣播到環境識別頁面
     */
    private void sendBroadcastToEnvironment(String action) {
        Intent broadcast = new Intent("com.example.tonbo_app.VOICE_COMMAND");
        broadcast.putExtra("action", action);
        sendBroadcast(broadcast);
    }
    
    /**
     * 調整音量
     */
    private void adjustVolume(boolean increase) {
        // 這裡可以調用TTSManager調整音量
        if (ttsManager != null) {
            String message;
            if ("english".equals(currentLanguage)) {
                message = increase ? "Volume increased" : "Volume decreased";
            } else if ("mandarin".equals(currentLanguage)) {
                message = increase ? "音量已增大" : "音量已減小";
            } else {
                message = increase ? "音量已增大" : "音量已減小";
            }
            announceInfo(message);
        }
    }
    
    /**
     * 獲取本地化錯誤消息
     */
    private String getLocalizedErrorMessage(String error) {
        if ("english".equals(currentLanguage)) {
            switch (error) {
                case "未識別的命令":
                case "Command not recognized":
                    return "Command not recognized";
                case "需要錄音權限":
                case "Permission needed":
                    return "Microphone permission needed";
                case "語音識別器初始化失敗":
                case "Speech recognizer initialization failed":
                    return "Speech recognition unavailable";
                case "沒有匹配結果":
                case "No match found":
                    return "No match found. Please try again";
                case "網絡錯誤":
                case "Network error":
                    return "Network error. Please check connection";
                default:
                    return error;
            }
        } else if ("mandarin".equals(currentLanguage)) {
            switch (error) {
                case "未識別的命令":
                    return "未識別的命令";
                case "需要錄音權限":
                    return "需要錄音權限";
                case "語音識別器初始化失敗":
                    return "語音識別不可用";
                case "沒有匹配結果":
                    return "沒有匹配結果，請重試";
                case "網絡錯誤":
                    return "網絡錯誤，請檢查連接";
                default:
                    return error;
            }
        } else {
            return error;
        }
    }
    
    /**
     * 建議相似命令
     */
    private void suggestSimilarCommands(String unrecognizedText) {
        // 智能建議：根據輸入文本推測可能的命令
        String suggestion;
        if ("english".equals(currentLanguage)) {
            suggestion = "Command not recognized: \"" + unrecognizedText + "\". " +
                        "You can say: open environment, read document, emergency help, go home, etc.";
        } else if ("mandarin".equals(currentLanguage)) {
            suggestion = "未識別的命令：\"" + unrecognizedText + "\"。您可以說：打開環境識別、讀文件、緊急求助、返回主頁等";
        } else {
            suggestion = "未識別的命令：\"" + unrecognizedText + "\"。您可以說：打開環境識別、讀文件、緊急求助、返回主頁等";
        }
        announceInfo(suggestion);
    }
    
    private void switchLanguage() {
        switch (currentLanguage) {
            case "cantonese":
                currentLanguage = "english";
                ttsManager.changeLanguage("english");
                voiceCommandManager.setLanguage("english");
                if (aiAssistant != null) {
                    aiAssistant.setLanguage("english");
                }
                if (streamingAI != null) {
                    streamingAI.setLanguage("english");
                }
                announceInfo("Switched to English");
                break;
            case "english":
                currentLanguage = "mandarin";
                ttsManager.changeLanguage("mandarin");
                voiceCommandManager.setLanguage("mandarin");
                if (aiAssistant != null) {
                    aiAssistant.setLanguage("mandarin");
                }
                if (streamingAI != null) {
                    streamingAI.setLanguage("mandarin");
                }
                announceInfo("已切換到普通話");
                break;
            case "mandarin":
            default:
                currentLanguage = "cantonese";
                ttsManager.changeLanguage("cantonese");
                voiceCommandManager.setLanguage("cantonese");
                if (aiAssistant != null) {
                    aiAssistant.setLanguage("cantonese");
                }
                if (streamingAI != null) {
                    streamingAI.setLanguage("cantonese");
                }
                announceInfo("已切換到廣東話");
                break;
        }
    }
    
    private void tellTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String currentTime = sdf.format(new Date());
        
        String cantoneseText = "現在時間是" + currentTime;
        String englishText = "Current time is " + currentTime;
        
        ttsManager.speak(cantoneseText, englishText, true);
    }
    
    @Override
    protected void onDestroy() {
        if (voiceCommandManager != null) {
            voiceCommandManager.stopListening();
        }
        if (streamingAI != null) {
            streamingAI.release();
        }
        if (ttsManager != null) {
            ttsManager.stopSpeaking();
        }
        super.onDestroy();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // 暫停時停止監聽
        if (isListening) {
            stopListening();
        }
    }
}
