package com.example.tonbo_app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MainActivity extends BaseAccessibleActivity {
    private RecyclerView recyclerView;
    private FunctionAdapter adapter;
    private LinearLayout emergencyButton;
    private EmergencyManager emergencyManager;
    private final ArrayList<HomeFunction> functionList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化緊急管理器
        emergencyManager = EmergencyManager.getInstance(this);
        
        initViews();
        setupFunctionList();
        setupRecyclerView();
        
        // 設置無障礙內容描述
        setupAccessibilityContent();
        
        // 初始化語言按鈕狀態
        initializeLanguageButton();
    }
    
    @Override
    protected void announcePageTitle() {
        // 播報頁面標題和功能列表
        String cantoneseText = "瞳伴主頁。歡迎使用智能視覺助手。" +
                "當前有五個主要功能：環境識別、閱讀助手、語音命令、尋找物品、即時協助。" +
                "右上角有三個按鈕：緊急設置、系統設定、語言切換。" +
                "底部有緊急求助按鈕，長按三秒發送求助信息。" +
                "請點擊選擇功能或使用語音命令控制。";
        String englishText = "Tonbo Home. Welcome to the smart visual assistant. " +
                "Five main functions available: Environment Recognition, Document Assistant, Voice Command, Find Items, Live Assistance. " +
                "Three buttons on top right: Emergency Settings, System Settings, Language Switch. " +
                "Emergency button at bottom, long press for 3 seconds to send help request. " +
                "Please tap to select function or use voice command control.";
        ttsManager.speak(cantoneseText, englishText, true);
    }
    
    private void setupAccessibilityContent() {
        // 設置緊急按鈕的無障礙內容
        emergencyButton.setContentDescription(getString(R.string.emergency_button_long_press_info));
        
        // 設置語言按鈕的無障礙內容
        Button languageButton = findViewById(R.id.languageButton);
        if (languageButton != null) {
            String languageDesc = getLanguageDescription(currentLanguage);
            languageButton.setContentDescription(getString(R.string.language_button_desc_prefix) + languageDesc + getString(R.string.language_button_desc_suffix));
        }
        
        // 設置應用標題的無障礙內容
        TextView appTitle = findViewById(R.id.appTitle);
        appTitle.setContentDescription(getString(R.string.app_title_accessibility));
        
        // 設置功能選擇標題的無障礙內容
        TextView functionTitle = findViewById(R.id.functionSelectionTitle);
        functionTitle.setContentDescription(getString(R.string.function_selection_accessibility));
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        emergencyButton = findViewById(R.id.emergencyButton);

        // 設置緊急按鈕
        emergencyButton.setOnLongClickListener(v -> {
            emergencyManager.triggerEmergencyAlert();
            return true;
        });

        emergencyButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            announceInfo("這是緊急求助按鈕，請長按三秒發送求助信息。點擊右上角紅色緊急按鈕可配置緊急聯絡人");
        });

        // 緊急求助設置按鈕
        Button emergencySettingsButton = findViewById(R.id.emergencySettingsButton);
        if (emergencySettingsButton != null) {
            emergencySettingsButton.setOnClickListener(v -> {
                vibrationManager.vibrateClick();
                openEmergencySettings();
            });
        }

        // 系統設定按鈕
        Button systemSettingsButton = findViewById(R.id.systemSettingsButton);
        if (systemSettingsButton != null) {
            systemSettingsButton.setOnClickListener(v -> {
                vibrationManager.vibrateClick();
                openSettings();
            });
        }

        // 語言切換按鈕
        Button languageButton = findViewById(R.id.languageButton);
        if (languageButton != null) {
            languageButton.setOnClickListener(v -> {
                vibrationManager.vibrateClick();
                toggleLanguage();
            });
            
            // 添加長按事件作為備選
            languageButton.setOnLongClickListener(v -> {
                vibrationManager.vibrateLongPress();
                toggleLanguage();
                return true;
            });
        } else {
            announceError("語言切換按鈕未找到");
        }
        
        // 為所有按鈕提供觸控反饋
        provideTouchFeedback(emergencyButton);
        // 注意：languageButton已經有自己的點擊事件，不需要provideTouchFeedback
    }


    private void toggleLanguage() {
        switch (currentLanguage) {
            case "cantonese":
                currentLanguage = "english";
                break;
            case "english":
                currentLanguage = "mandarin";
                break;
            case "mandarin":
            default:
                currentLanguage = "cantonese";
                break;
        }
        
        // 保存語言設置
        localeManager.setLanguage(this, currentLanguage);
        
        // 更新TTS語言
        ttsManager.changeLanguage(currentLanguage);
        
        // 重新創建Activity以應用新語言
        recreate();
    }
    
    private String getLanguageDescription(String language) {
        switch (language) {
            case "cantonese": return getString(R.string.language_cantonese_desc);
            case "english": return getString(R.string.language_english_desc);
            case "mandarin": return getString(R.string.language_mandarin_desc);
            default: return getString(R.string.language_english_desc);
        }
    }

    private void updateLanguageButton() {
        Button languageButton = findViewById(R.id.languageButton);
        if (languageButton != null) {
            String buttonText = getLanguageButtonText(currentLanguage);
            languageButton.setText(buttonText);
        }
    }
    
    private void updateLanguageButtonDescription() {
        Button languageButton = findViewById(R.id.languageButton);
        if (languageButton != null) {
            String languageDesc = getLanguageDescription(currentLanguage);
            languageButton.setContentDescription(getString(R.string.language_button_desc_prefix) + languageDesc + getString(R.string.language_button_desc_suffix));
        }
    }
    
    private String getLanguageButtonText(String language) {
        switch (language) {
            case "cantonese": return getString(R.string.language_button_cantonese);
            case "english": return getString(R.string.language_button_english);
            case "mandarin": return getString(R.string.language_button_mandarin);
            default: return getString(R.string.language_button_cantonese);
        }
    }
    
    private void initializeLanguageButton() {
        Button languageButton = findViewById(R.id.languageButton);
        if (languageButton != null) {
            // 根據當前語言設置初始按鈕文字
            String buttonText = getLanguageButtonText(currentLanguage);
            languageButton.setText(buttonText);
            
            String languageDesc = getLanguageDescription(currentLanguage);
            languageButton.setContentDescription(getString(R.string.language_button_desc_prefix) + languageDesc + getString(R.string.language_button_desc_suffix));
        }
    }

    private void setupFunctionList() {
        functionList.clear(); // 清空列表，避免重複添加
        functionList.add(new HomeFunction(
            "environment",
            getString(R.string.function_environment), 
            getString(R.string.desc_environment), 
            R.drawable.ic_environment));
        functionList.add(new HomeFunction(
            "document",
            getString(R.string.function_document), 
            getString(R.string.desc_document), 
            R.drawable.ic_scan));
        functionList.add(new HomeFunction(
            "voice_command",
            getString(R.string.function_voice_command), 
            getString(R.string.desc_voice_command), 
            R.drawable.ic_voice_command));
        functionList.add(new HomeFunction(
            "find_items",
            getString(R.string.function_find_items), 
            getString(R.string.desc_find_items), 
            R.drawable.ic_search));
        functionList.add(new HomeFunction(
            "live_assistance",
            getString(R.string.function_live_assistance), 
            getString(R.string.desc_live_assistance), 
            R.drawable.ic_assistance));
    }

    private void setupRecyclerView() {
        adapter = new FunctionAdapter(functionList, new FunctionAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(HomeFunction function) {
                vibrationManager.vibrateClick();

                // 使用當前語言播報（名稱已經是正確語言）
                String announcement = (currentLanguage.equals("english") ? "Starting " : "正在啟動") + function.getName();
                ttsManager.speak(announcement, announcement, true);

                // 根據功能ID啟動相應頁面
                handleFunctionClick(function.getId());
            }

            @Override
            public void onItemFocus(HomeFunction function) {
                vibrationManager.vibrateFocus();
                String cantoneseText = "當前焦點：" + function.getName() + "，" + function.getDescription();
                String englishText = "Current focus: " + function.getName() + ", " + function.getDescription();
                ttsManager.speak(cantoneseText, englishText);
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
    }

    private void handleFunctionClick(String functionId) {
        switch (functionId) {
            case "environment":
                startEnvironmentActivity();
                break;
            case "document":
                startDocumentCurrencyActivity();
                break;
            case "voice_command":
                startVoiceCommandActivity();
                break;
            case "find_items":
                announceInfo(getString(R.string.function_find_items) + "功能開發中");
                break;
            case "live_assistance":
                announceInfo(getString(R.string.function_live_assistance) + "功能開發中");
                break;
        }
    }

    private void startEnvironmentActivity() {
        try {
            Intent intent = new Intent(MainActivity.this, EnvironmentActivity.class);
            intent.putExtra("language", currentLanguage);
            announceNavigation("正在進入環境識別頁面");
            startActivity(intent);
        } catch (Exception e) {
            announceError("環境識別功能暫不可用");
        }
    }

    private void startDocumentCurrencyActivity() {
        try {
            Intent intent = new Intent(MainActivity.this, DocumentCurrencyActivity.class);
            intent.putExtra("language", currentLanguage);
            announceNavigation(getString(R.string.document_assistant_announcement));
            startActivity(intent);
        } catch (Exception e) {
            announceError(getString(R.string.document_assistant_unavailable));
        }
    }
    
    private void startVoiceCommandActivity() {
        try {
            Intent intent = new Intent(MainActivity.this, VoiceCommandActivity.class);
            intent.putExtra("language", currentLanguage);
            announceNavigation("正在進入語音命令頁面");
            startActivity(intent);
        } catch (Exception e) {
            announceError("語音命令功能暫不可用");
            Log.e("MainActivity", "打開語音命令失敗: " + e.getMessage());
        }
    }

    private String getEnglishFunctionName(String chineseName) {
        switch (chineseName) {
            case "環境識別": return "Environment Recognition";
            case "閱讀助手": return "Document Assistant";
            case "語音命令": return "Voice Command";
            case "尋找物品": return "Find Items";
            case "即時協助": return "Live Assistance";
            default: return chineseName;
        }
    }

    private String getEnglishDescription(String chineseDescription) {
        switch (chineseDescription) {
            case "描述周圍環境和物體": return "Describe surroundings and objects";
            case "掃描文件和識別貨幣": return "Scan documents and recognize currency";
            case "語音控制應用功能": return "Voice control app functions";
            case "尋找標記的個人物品": return "Find marked personal items";
            case "視訊連線志工協助": return "Video call with volunteers";
            default: return chineseDescription;
        }
    }
    
    private void openSettings() {
        try {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            intent.putExtra("language", currentLanguage);
            announceNavigation(getString(R.string.settings_announcement));
            startActivity(intent);
        } catch (Exception e) {
            announceError(getString(R.string.settings_unavailable));
            Log.e("MainActivity", "打開系統設定失敗: " + e.getMessage());
        }
    }
    
    private void openEmergencySettings() {
        try {
            Intent intent = new Intent(MainActivity.this, EmergencySettingsActivity.class);
            intent.putExtra("language", currentLanguage);
            startActivity(intent);
        } catch (Exception e) {
            announceError(getString(R.string.emergency_settings_unavailable));
            Log.e("MainActivity", "打開緊急設置失敗: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理資源
        if (ttsManager != null) {
            ttsManager.shutdown();
        }
    }
}