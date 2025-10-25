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
        
        // 設置監聽按鈕
        listenButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            toggleListening();
        });

        // 返回按鈕
        Button backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                vibrationManager.vibrateClick();
                finish();
            });
        }
        
        updateUI(false);
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
                    statusText.setText("錯誤: " + error);
                    announceError(error);
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
            listenButton.setContentDescription(getString(R.string.listening_active));
            statusText.setText(getString(R.string.listening_active));
            statusText.setTextColor(getResources().getColor(android.R.color.holo_green_light));
        } else {
            listenButton.setText("🎤");
            listenButton.setContentDescription(getString(R.string.listening_status));
            statusText.setText(getString(R.string.listening_status));
            statusText.setTextColor(getResources().getColor(android.R.color.white));
        }
    }
    
    private void executeCommand(String command, String originalText) {
        Log.d(TAG, "執行命令: " + command);
        
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
                announceInfo("尋找物品功能開發中");
                break;
                
            case "open_assistance":
                announceInfo("即時協助功能開發中");
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
                
            default:
                announceError("未知命令");
                break;
        }
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
