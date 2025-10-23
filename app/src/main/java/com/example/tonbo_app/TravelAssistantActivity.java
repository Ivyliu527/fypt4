package com.example.tonbo_app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 出行協助Activity
 * 提供出行導航、路線規劃、交通信息等功能
 */
public class TravelAssistantActivity extends BaseAccessibleActivity {
    private static final String TAG = "TravelAssistant";
    
    private TextView statusText;
    private Button navigationButton;
    private Button routePlanningButton;
    private Button trafficInfoButton;
    private Button weatherButton;
    private Button emergencyLocationButton;
    private LinearLayout controlButtons;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_travel_assistant);
        
        initViews();
        setupButtons();
        announcePageTitle();
    }
    
    private void initViews() {
        statusText = findViewById(R.id.status_text);
        navigationButton = findViewById(R.id.navigation_button);
        routePlanningButton = findViewById(R.id.route_planning_button);
        trafficInfoButton = findViewById(R.id.traffic_info_button);
        weatherButton = findViewById(R.id.weather_button);
        emergencyLocationButton = findViewById(R.id.emergency_location_button);
        controlButtons = findViewById(R.id.control_buttons);
        
        // 設置返回按鈕
        Button backButton = findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                vibrationManager.vibrateClick();
                announceInfo(getString(R.string.going_back_to_home));
                finish();
            });
        }
    }
    
    private void setupButtons() {
        // 導航功能
        navigationButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            announceInfo(getString(R.string.navigation_feature_coming_soon));
            // TODO: 實現導航功能
        });
        
        // 路線規劃
        routePlanningButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            announceInfo(getString(R.string.route_planning_feature_coming_soon));
            // TODO: 實現路線規劃功能
        });
        
        // 交通信息
        trafficInfoButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            announceInfo(getString(R.string.traffic_info_feature_coming_soon));
            // TODO: 實現交通信息功能
        });
        
        // 天氣信息
        weatherButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            announceInfo(getString(R.string.weather_info_feature_coming_soon));
            // TODO: 實現天氣信息功能
        });
        
        // 緊急位置分享
        emergencyLocationButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            handleEmergencyLocation();
        });
    }
    
    private void handleEmergencyLocation() {
        announceInfo(getString(R.string.emergency_location_feature_coming_soon));
        // TODO: 實現緊急位置分享功能
        // 1. 獲取當前GPS位置
        // 2. 發送位置信息給緊急聯絡人
        // 3. 播報當前位置信息
    }
    
    @Override
    protected void announcePageTitle() {
        String title = getString(R.string.travel_assistant_title);
        String description = getString(R.string.travel_assistant_description);
        String fullAnnouncement = title + "。" + description;
        
        Log.d(TAG, "🔊 播報頁面標題: " + fullAnnouncement);
        
        // 根據當前語言播報
        String currentLang = LocaleManager.getInstance(this).getCurrentLanguage();
        switch (currentLang) {
            case "english":
                ttsManager.speak(null, "Travel Assistant. " + getEnglishDescription(), true);
                break;
            case "mandarin":
                ttsManager.speak(getSimplifiedChineseDescription(), null, true);
                break;
            case "cantonese":
            default:
                ttsManager.speak(fullAnnouncement, null, true);
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
