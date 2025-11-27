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
import java.util.Date;
import java.util.Locale;

/**
 * 語音命令控制頁面
 */
public class VoiceCommandActivity extends BaseAccessibleActivity {
    
    private static final String TAG = "VoiceCommandActivity";
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 200;
    
    private VoiceCommandManager voiceCommandManager;
    private Button listenButton;
    private TextView statusText;
    private TextView commandText;
    private TextView hintText;
    private TextView pageTitle;
    private TextView availableCommandsTitle;
    
    private boolean isListening = false;
    
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
            String cantoneseText = "語音命令頁面。你可以說出指令控制應用，例如：打開環境識別、讀文件、緊急求助等。" +
                    "點擊中間的按鈕開始監聽。";
            String englishText = "Voice Command page. You can speak commands to control the app, such as: open environment, read document, emergency help. " +
                    "Tap the center button to start listening.";
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
                    return "Voice Command";
                } else if ("mandarin".equals(currentLang)) {
                    return "语音命令";
                } else {
                    return "語音命令";
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
                           "• Utilities: What time is it, Repeat, Volume up/down";
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
                           "• 实用功能：现在几点、重复、增大/减小音量";
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
                           "• 實用功能：現在幾點、重複、增大/減小音量";
                }
            default:
                return getString(R.string.app_name);
        }
    }
    
    private void initVoiceCommandManager() {
        voiceCommandManager = VoiceCommandManager.getInstance(this);
        voiceCommandManager.setLanguage(currentLanguage);
        voiceCommandManager.setCommandListener(new VoiceCommandManager.VoiceCommandListener() {
            @Override
            public void onCommandRecognized(String command, String originalText) {
                runOnUiThread(() -> {
                    vibrationManager.vibrateSuccess();
                    commandText.setText("識別到: " + originalText);
                    executeCommand(command, originalText);
                });
            }
            
            @Override
            public void onListeningStarted() {
                runOnUiThread(() -> {
                    updateUI(true);
                    statusText.setText("正在監聽...");
                    announceInfo("開始監聽，請說出指令");
                });
            }
            
            @Override
            public void onListeningStopped() {
                runOnUiThread(() -> {
                    updateUI(false);
                    statusText.setText("點擊開始");
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
                    commandText.setText("識別中: " + partialText);
                });
            }
        });
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
                announceInfo("錄音權限已授予");
            } else {
                announceError("需要錄音權限才能使用語音命令");
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
            announceError("請先授予錄音權限");
            checkPermissions();
            return;
        }
        
        isListening = true;
        voiceCommandManager.startListening();
    }
    
    private void stopListening() {
        isListening = false;
        voiceCommandManager.stopListening();
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
                announceNavigation("正在打開環境識別");
                startActivity(new Intent(this, EnvironmentActivity.class).putExtra("language", currentLanguage));
                break;
                
            case "open_document":
                announceNavigation("正在打開閱讀助手");
                startActivity(new Intent(this, DocumentCurrencyActivity.class).putExtra("language", currentLanguage));
                break;
                
            case "open_find":
                announceNavigation("正在打開尋找物品");
                startActivity(new Intent(this, FindItemsActivity.class).putExtra("language", currentLanguage));
                break;
                
            case "open_assistance":
                announceNavigation("正在打開即時協助");
                startActivity(new Intent(this, InstantAssistanceActivity.class).putExtra("language", currentLanguage));
                break;
                
            case "emergency":
                announceInfo("觸發緊急求助");
                EmergencyManager.getInstance(this).triggerEmergencyAlert();
                break;
                
            case "go_home":
                announceNavigation("返回主頁");
                Intent homeIntent = new Intent(this, MainActivity.class);
                homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(homeIntent);
                finish();
                break;
                
            case "go_back":
                announceInfo("返回上一頁");
                finish();
                break;
                
            case "switch_language":
                switchLanguage();
                break;
                
            case "open_settings":
                announceNavigation("正在打開系統設定");
                startActivity(new Intent(this, SettingsActivity.class).putExtra("language", currentLanguage));
                break;
                
            case "tell_time":
                tellTime();
                break;
                
            case "stop_listening":
                announceInfo("停止監聽");
                stopListening();
                break;
                
            // 環境識別控制命令
            case "start_detection":
                if (isInEnvironmentActivity()) {
                    sendBroadcastToEnvironment("start_detection");
                    announceInfo("開始環境檢測");
                } else {
                    announceInfo("請先打開環境識別功能");
                }
                break;
                
            case "stop_detection":
                if (isInEnvironmentActivity()) {
                    sendBroadcastToEnvironment("stop_detection");
                    announceInfo("停止環境檢測");
                } else {
                    announceInfo("請先打開環境識別功能");
                }
                break;
                
            case "describe_environment":
                if (isInEnvironmentActivity()) {
                    sendBroadcastToEnvironment("describe_environment");
                    announceInfo("正在描述環境");
                } else {
                    announceInfo("請先打開環境識別功能");
                }
                break;
                
            // 控制命令
            case "repeat":
                if (lastCommand != null) {
                    announceInfo("重複執行: " + lastCommandText);
                    performCommand(lastCommand, lastCommandText);
                } else {
                    announceInfo("沒有可重複的命令");
                }
                break;
                
            case "volume_up":
                adjustVolume(true);
                break;
                
            case "volume_down":
                adjustVolume(false);
                break;
                
            default:
                announceError("未知命令: " + command);
                suggestSimilarCommands(originalText);
                break;
        }
    }
    
    /**
     * 檢查是否在環境識別頁面
     */
    private boolean isInEnvironmentActivity() {
        // 簡單檢查：可以通過廣播或Activity狀態來判斷
        // 這裡簡化處理，實際可以通過ActivityManager檢查
        return false; // 暫時返回false，需要時可以改進
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
            // 假設TTSManager有調整音量的方法
            String message = increase ? "音量已增大" : "音量已減小";
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
                announceInfo("Switched to English");
                break;
            case "english":
                currentLanguage = "mandarin";
                ttsManager.changeLanguage("mandarin");
                voiceCommandManager.setLanguage("mandarin");
                announceInfo("已切換到普通話");
                break;
            case "mandarin":
            default:
                currentLanguage = "cantonese";
                ttsManager.changeLanguage("cantonese");
                voiceCommandManager.setLanguage("cantonese");
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
        super.onDestroy();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (isListening) {
            stopListening();
        }
    }
}
