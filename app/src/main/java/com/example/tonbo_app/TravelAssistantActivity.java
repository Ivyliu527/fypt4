package com.example.tonbo_app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

/**
 * 出行協助Activity
 * 提供出行導航、路線規劃、交通信息等功能
 */
public class TravelAssistantActivity extends BaseAccessibleActivity {
    private static final String TAG = "TravelAssistant";
    
    private TextView pageTitle;
    private TextView voiceStatusTitle;
    private TextView voiceStatusText;
    private Button startTravelVoiceButton;
    private Button emergencyButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_travel_assistant);
        
        initViews();
        setupButtons();
        announcePageTitle();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    
    private void initViews() {
        pageTitle = findViewById(R.id.page_title);
        voiceStatusTitle = findViewById(R.id.voice_status_title);
        voiceStatusText = findViewById(R.id.voice_status_text);
        startTravelVoiceButton = findViewById(R.id.start_travel_voice_button);
        emergencyButton = findViewById(R.id.emergency_button);
        
        // 設置返回按鈕
        android.widget.ImageButton backButton = findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                handleBackPressed();
            });
        }
        
        // 根據當前語言更新界面文字
        updateLanguageUI();
    }
    
    private void setupButtons() {
        // 開始出行：直接跳轉 StartTravelActivity
        startTravelVoiceButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            Intent intent = new Intent(this, StartTravelActivity.class);
            intent.putExtra("language", currentLanguage);
            startActivity(intent);
        });
        
        // 聯繫志願者
        emergencyButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            updateVoiceStatus(getString(R.string.voice_status_emergency));
            EmergencyManager.getInstance(this).triggerEmergencyAlert();
        });
    }
    
    /**
     * 更新語音狀態提示
     */
    private void updateVoiceStatus(String status) {
        if (voiceStatusText != null) {
            voiceStatusText.setText(status);
            // 為視障用戶播報狀態（避免重複播報，只在關鍵狀態變化時播報）
        }
    }
    
    /**
     * 更新語言UI
     */
    private void updateLanguageUI() {
        if (pageTitle != null) {
            pageTitle.setText(getString(R.string.travel_assistant_title));
        }
        
        if (voiceStatusTitle != null) {
            voiceStatusTitle.setText(getString(R.string.voice_status_title));
        }
        
        if (voiceStatusText != null) {
            voiceStatusText.setText(getString(R.string.voice_status_ready));
        }
        
        if (startTravelVoiceButton != null) {
            startTravelVoiceButton.setText(getString(R.string.start_travel_voice));
        }
        
        if (emergencyButton != null) {
            emergencyButton.setText(getString(R.string.contact_volunteer));
        }
    }
    

    @Override
    protected void announcePageTitle() {
        String currentLang = LocaleManager.getInstance(this).getCurrentLanguage();
        switch (currentLang) {
            case "english":
                ttsManager.speak(null, "Travel Assistant. Start travel, contact volunteer.", true);
                break;
            case "mandarin":
                ttsManager.speak("出行协助。开始出行、联系志愿者。", null, true);
                break;
            case "cantonese":
            default:
                ttsManager.speak("出行協助。開始出行、聯繫志願者。", "Travel Assistant. Start travel, contact volunteer.", true);
                break;
        }
    }
    
    private String getEnglishDescription() {
        return "This feature provides travel assistance including navigation, route planning, traffic information, weather updates, and emergency location sharing.";
    }
    
    private String getSimplifiedChineseDescription() {
        return "出行助手功能，提供导航、路线规划、交通信息、天气更新和紧急位置分享等服务。";
    }
    
    @Override
    protected void startEnvironmentActivity() {
        // 重寫父類方法，避免語音命令衝突
        startEnvironmentRecognition();
    }
    
    /**
     * 啟動環境識別（自動開始檢測）
     */
    private void startEnvironmentRecognition() {
        Log.d(TAG, "啟動環境識別，自動開始檢測");
        
        // 播報提示
        String announcement = getString(R.string.environment_recognition_starting);
        if (ttsManager != null) {
            ttsManager.speak(announcement, announcement, true);
        }
        
        // 啟動環境識別 Activity，並傳遞自動開始標記
        Intent intent = new Intent(this, RealAIDetectionActivity.class);
        intent.putExtra("language", currentLanguage);
        intent.putExtra("auto_start", true); // 標記自動開始檢測
        startActivity(intent);
    }
    
    @Override
    protected void startDocumentCurrencyActivity() {
        // 重寫父類方法，避免語音命令衝突
    }
    
    @Override
    protected void startFindItemsActivity() {
        // 重寫父類方法，避免語音命令衝突
    }
    
    @Override
    protected void startSettingsActivity() {
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra("language", currentLanguage);
        startActivity(intent);
    }
    
    @Override
    protected void handleEmergencyCommand() {
        announceInfo(getString(R.string.emergency_location_feature_coming_soon));
    }
    
    @Override
    protected void goToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("language", currentLanguage);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
    
    @Override
    protected void handleLanguageSwitch() {
        announceInfo(getString(R.string.language_switch_feature_coming_soon));
    }
    
    @Override
    protected void stopCurrentOperation() {
        announceInfo(getString(R.string.operation_stopped));
    }
}
