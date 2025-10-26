package com.example.tonbo_app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;

public class MainActivity extends BaseAccessibleActivity {
    private ViewPager2 viewPager;
    private TextView pageIndicator;
    private FunctionPagerAdapter pagerAdapter;
    private LinearLayout emergencyButton;
    private EmergencyManager emergencyManager;
    private ArrayList<ArrayList<HomeFunction>> functionPages = new ArrayList<>();
    private FunctionListFragment.OnFunctionClickListener functionClickListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化緊急管理器
        emergencyManager = EmergencyManager.getInstance(this);
        
        // 確保currentLanguage正確初始化
        if (currentLanguage == null) {
            currentLanguage = localeManager.getCurrentLanguage();
        }
        
        initViews();
        setupFunctionList();
        setupViewPager();
        
        // 設置無障礙內容描述
        setupAccessibilityContent();
        
        // 初始化語言按鈕狀態
        initializeLanguageButton();
        
        // 更新語言UI
        updateLanguageUI();
    }
    
    @Override
    protected void announcePageTitle() {
        // 播報頁面標題和功能列表
        String cantoneseText = "瞳伴主頁。歡迎使用智能視覺助手。" +
                "當前有七個主要功能：環境識別、文檔助手、語音命令、尋找物品、即時協助、出行協助、手勢管理。" +
                "右上角有三個按鈕：緊急設置、系統設定、語言切換。" +
                "底部有緊急求助按鈕，長按三秒發送求助信息。" +
                "請點擊選擇功能或使用語音命令控制。";
        String englishText = "Tonbo Home. Welcome to the smart visual assistant. " +
                "Seven main functions available: Environment Recognition, Document Assistant, Voice Command, Find Items, Live Assistance, Travel Assistant, Gesture Management. " +
                "Three buttons on top right: Emergency Settings, System Settings, Language Switch. " +
                "Emergency button at bottom, long press for 3 seconds to send help request. " +
                "Please tap to select function or use voice command control.";
        String mandarinText = "瞳伴主页。欢迎使用智能视觉助手。" +
                "当前有七个主要功能：环境识别、文档助手、语音命令、寻找物品、即时协助、出行协助、手势管理。" +
                "右上角有三个按钮：紧急设置、系统设定、语言切换。" +
                "底部有紧急求助按钮，长按三秒发送求助信息。" +
                "请点击选择功能或使用语音命令控制。";
        
        String[] texts = {cantoneseText, englishText, mandarinText};
        if ("english".equals(currentLanguage)) {
            ttsManager.speak(texts[1], null, true);
        } else if ("mandarin".equals(currentLanguage)) {
            ttsManager.speak(texts[2], null, true);
        } else {
            ttsManager.speak(texts[0], texts[1], true);
        }
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
        viewPager = findViewById(R.id.viewPager);
        pageIndicator = findViewById(R.id.pageIndicator);
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


    public void toggleLanguage() {
        currentLanguage = getNextLanguage(currentLanguage);
        
        // 保存語言設置
        localeManager.setLanguage(this, currentLanguage);
        
        // 立即播放語言切換確認語音（使用當前TTS語言）
        announceLanguageChange(currentLanguage);
        
        // 立即更新TTS語言
        ttsManager.changeLanguage(currentLanguage);
        
        // 立即更新界面文字
        updateLanguageUI();
        
        // 重新創建Activity以應用新語言
        recreate();
    }
    
    /**
     * 更新語言UI
     */
    private void updateLanguageUI() {
        // 更新語言按鈕文字
        updateLanguageButton();
        
        // 更新功能列表
        setupFunctionList();
        
        // 更新標題
        updatePageTitle();
    }
    
    /**
     * 更新頁面標題
     */
    private void updatePageTitle() {
        // 更新Function Selection標題
        TextView functionTitle = findViewById(R.id.functionSelectionTitle);
        if (functionTitle != null) {
            String functionSelectionTitle;
            if ("english".equals(currentLanguage)) {
                functionSelectionTitle = "Function Selection";
            } else if ("mandarin".equals(currentLanguage)) {
                functionSelectionTitle = "功能选择";
            } else {
                functionSelectionTitle = "功能選擇";
            }
            functionTitle.setText(functionSelectionTitle);
        }
    }
    
    /**
     * 獲取下一個語言
     */
    private String getNextLanguage(String currentLang) {
        switch (currentLang) {
            case "cantonese":
                return "english";
            case "english":
                return "mandarin";
            case "mandarin":
            default:
                return "cantonese";
        }
    }
    
    /**
     * 平滑語言切換 - 避免畫面閃爍
     */
    private void smoothLanguageSwitch() {
        // 選項1: 淡入淡出動畫
        smoothFadeTransition();
        
        // 選項2: 滑動動畫 (可選)
        // smoothSlideTransition();
        
        // 選項3: 縮放動畫 (可選)
        // smoothScaleTransition();
    }
    
    /**
     * 淡入淡出動畫
     */
    private void smoothFadeTransition() {
        getWindow().getDecorView().animate()
                .alpha(0.3f)
                .setDuration(200)
                .withEndAction(() -> {
                    // 更新UI文字
                    updateUITexts();
                    
                    // 淡入動畫
                    getWindow().getDecorView().animate()
                            .alpha(1.0f)
                            .setDuration(200)
                            .start();
                })
                .start();
    }
    
    /**
     * 滑動動畫 (備選方案)
     */
    private void smoothSlideTransition() {
        getWindow().getDecorView().animate()
                .translationX(-getWindow().getDecorView().getWidth())
                .setDuration(300)
                .withEndAction(() -> {
                    // 更新UI文字
                    updateUITexts();
                    
                    // 重置位置並滑入
                    getWindow().getDecorView().setTranslationX(getWindow().getDecorView().getWidth());
                    getWindow().getDecorView().animate()
                            .translationX(0)
                            .setDuration(300)
                            .start();
                })
                .start();
    }
    
    /**
     * 縮放動畫 (備選方案)
     */
    private void smoothScaleTransition() {
        getWindow().getDecorView().animate()
                .scaleX(0.8f)
                .scaleY(0.8f)
                .alpha(0.5f)
                .setDuration(250)
                .withEndAction(() -> {
                    // 更新UI文字
                    updateUITexts();
                    
                    // 恢復縮放
                    getWindow().getDecorView().animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .alpha(1.0f)
                            .setDuration(250)
                            .start();
                })
                .start();
    }
    
    /**
     * 更新UI文字 - 不重新創建Activity
     */
    private void updateUITexts() {
        // 更新標題
        setTitle(getString(R.string.app_name));
        
        // 更新功能列表
        updateFunctionList();
        
        // 更新語言按鈕
        updateLanguageButton();
        
        // 更新操作指南
        updateOperationGuide();
        
        // 更新無障礙內容描述
        setupAccessibilityContent();
        
        Log.d("MainActivity", "UI文字已更新為: " + currentLanguage);
    }
    
    /**
     * 更新功能列表
     */
    private void updateFunctionList() {
        if (pagerAdapter != null) {
            // 重新創建功能列表
            setupFunctionList();
            pagerAdapter.notifyDataSetChanged();
            
            // 重新設置點擊監聽器
            viewPager.post(() -> {
                for (int i = 0; i < functionPages.size(); i++) {
                    long itemId = pagerAdapter.getItemId(i);
                    Fragment fragment = getSupportFragmentManager().findFragmentByTag("f" + itemId);
                    if (fragment instanceof FunctionListFragment) {
                        ((FunctionListFragment) fragment).setOnFunctionClickListener(functionClickListener);
                    }
                }
            });
        }
    }
    
    
    /**
     * 更新操作指南
     */
    private void updateOperationGuide() {
        // 操作指南在布局中沒有單獨的ID，跳過更新
        // 因為操作指南內容是通過strings.xml動態設置的
    }
    
    private void announceLanguageChange(String language) {
        String currentTTSLanguage = ttsManager.getCurrentLanguage();
        boolean isEnglishTTS = AppConstants.LANGUAGE_ENGLISH.equals(currentTTSLanguage);
        
        String[] messages = getLanguageChangeMessages(language);
        String cantoneseText = messages[0];
        String englishText = isEnglishTTS ? messages[1] : null;
        
        ttsManager.speak(cantoneseText, englishText, true);
    }
    
    /**
     * 獲取語言切換消息
     */
    private String[] getLanguageChangeMessages(String language) {
        switch (language) {
            case AppConstants.LANGUAGE_CANTONESE:
                return new String[]{"已切換到廣東話", "Switched to Cantonese"};
            case AppConstants.LANGUAGE_ENGLISH:
                return new String[]{"已切換到英文", "Switched to English"};
            case AppConstants.LANGUAGE_MANDARIN:
                return new String[]{"已切換到普通話", "Switched to Mandarin"};
            default:
                return new String[]{"語言切換完成", "Language switched"};
        }
    }
    
    private String getLanguageDescription(String language) {
        switch (language) {
            case AppConstants.LANGUAGE_CANTONESE: 
                return getString(R.string.language_cantonese_desc);
            case AppConstants.LANGUAGE_ENGLISH: 
                return getString(R.string.language_english_desc);
            case AppConstants.LANGUAGE_MANDARIN: 
                return getString(R.string.language_mandarin_desc);
            default: 
                return getString(R.string.language_english_desc);
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
            case AppConstants.LANGUAGE_CANTONESE: 
                return getString(R.string.language_button_cantonese);
            case AppConstants.LANGUAGE_ENGLISH: 
                return getString(R.string.language_button_english);
            case AppConstants.LANGUAGE_MANDARIN: 
                return getString(R.string.language_button_mandarin);
            default: 
                return getString(R.string.language_button_cantonese);
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
        functionPages.clear(); // 清空列表，避免重複添加
        
        // 根據當前語言獲取對應的字符串
        String envTitle, envDesc, docTitle, docDesc, voiceTitle, voiceDesc, 
               findTitle, findDesc, liveTitle, liveDesc, travelTitle, travelDesc, 
               gestureTitle, gestureDesc;
        
        if ("english".equals(currentLanguage)) {
            // 英文版本
            envTitle = "Environment Recognition";
            envDesc = "Describe surroundings and objects";
            docTitle = "Document Assistant";
            docDesc = "Scan documents and recognize currency";
            voiceTitle = "Voice Command";
            voiceDesc = "Control app with voice commands";
            findTitle = "Find Items";
            findDesc = "Find marked personal items";
            liveTitle = "Live Assistance";
            liveDesc = "Video call with volunteers";
            travelTitle = "Travel Assistant";
            travelDesc = "Provide navigation, route planning, traffic information, weather updates and emergency location sharing services";
            gestureTitle = "Gesture Management";
            gestureDesc = "Draw gestures to bind functions";
        } else if ("mandarin".equals(currentLanguage)) {
            // 普通話版本
            envTitle = "环境识别";
            envDesc = "描述周围环境和物体";
            docTitle = "文档助手";
            docDesc = "扫描文档和识别货币";
            voiceTitle = "语音命令";
            voiceDesc = "使用语音命令控制应用";
            findTitle = "查找物品";
            findDesc = "查找标记的个人物品";
            liveTitle = "即时协助";
            liveDesc = "与志愿者视频通话";
            travelTitle = "出行协助";
            travelDesc = "提供导航、路线规划、交通信息、天气更新和紧急位置分享服务";
            gestureTitle = "手势管理";
            gestureDesc = "绘制手势绑定应用功能";
        } else {
            // 廣東話版本（預設）
            envTitle = "環境識別";
            envDesc = "描述周圍環境和物體";
            docTitle = "文檔助手";
            docDesc = "掃描文檔和識別貨幣";
            voiceTitle = "語音命令";
            voiceDesc = "使用語音命令控制應用";
            findTitle = "尋找物品";
            findDesc = "尋找標記的個人物品";
            liveTitle = "即時協助";
            liveDesc = "與志願者視頻通話";
            travelTitle = "出行協助";
            travelDesc = "提供導航、路線規劃、交通信息、天氣更新和緊急位置分享服務";
            gestureTitle = "手勢管理";
            gestureDesc = "繪製手勢綁定應用功能";
        }
        
        // 第一頁：前4個功能
        ArrayList<HomeFunction> page1 = new ArrayList<>();
        page1.add(new HomeFunction("environment", envTitle, envDesc, R.drawable.ic_environment));
        page1.add(new HomeFunction("document", docTitle, docDesc, R.drawable.ic_scan));
        page1.add(new HomeFunction("voice_command", voiceTitle, voiceDesc, R.drawable.ic_voice_command));
        page1.add(new HomeFunction("find_items", findTitle, findDesc, R.drawable.ic_search));
        
        // 第二頁：後3個功能
        ArrayList<HomeFunction> page2 = new ArrayList<>();
        page2.add(new HomeFunction("live_assistance", liveTitle, liveDesc, R.drawable.ic_assistance));
        page2.add(new HomeFunction("travel_assistant", travelTitle, travelDesc, R.drawable.ic_travel));
        page2.add(new HomeFunction("gesture_management", gestureTitle, gestureDesc, R.drawable.ic_search));
        
        functionPages.add(page1);
        functionPages.add(page2);
        
        Log.d("MainActivity", "Total pages: " + functionPages.size());
        for (int i = 0; i < functionPages.size(); i++) {
            Log.d("MainActivity", "Page " + i + " has " + functionPages.get(i).size() + " functions");
        }
        
        // 通知適配器數據已更新
        if (pagerAdapter != null) {
            pagerAdapter.notifyDataSetChanged();
        }
    }

    private void setupViewPager() {
        pagerAdapter = new FunctionPagerAdapter(this, functionPages, currentLanguage);
        viewPager.setAdapter(pagerAdapter);
        
        // 設置 Fragment 點擊監聽器
        functionClickListener = function -> {
            vibrationManager.vibrateClick();
            String announcement = (currentLanguage.equals("english") ? "Starting " : "正在啟動") + function.getName();
            ttsManager.speak(announcement, announcement, true);
            handleFunctionClick(function.getId());
        };
        
        // 設置頁面變更監聽器，更新指示器
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updatePageIndicator(position);
                
                // 為當前頁面的Fragment設置點擊監聽器
                setupFragmentClickListener(position);
            }
        });
        
        // 初始化指示器
        if (functionPages.size() > 0) {
            updatePageIndicator(0);
        }
        
        // 延後設置點擊監聽器，確保 Fragment 已完全創建
        viewPager.post(() -> {
            for (int i = 0; i < functionPages.size(); i++) {
                setupFragmentClickListener(i);
            }
        });
    }
    
    private void setupFragmentClickListener(int position) {
        long itemId = pagerAdapter.getItemId(position);
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("f" + itemId);
        if (fragment instanceof FunctionListFragment) {
            ((FunctionListFragment) fragment).setOnFunctionClickListener(functionClickListener);
        }
    }
    
    private void updatePageIndicator(int currentPage) {
        if (pageIndicator != null && functionPages.size() > 1) {
            int totalPages = functionPages.size();
            String indicatorText;
            
            if ("english".equals(currentLanguage)) {
                if (currentPage == 0) {
                    indicatorText = "← Swipe left for more functions";
                } else {
                    indicatorText = "Swipe right for more functions →";
                }
            } else if ("mandarin".equals(currentLanguage)) {
                if (currentPage == 0) {
                    indicatorText = "← 左滑查看更多功能";
                } else {
                    indicatorText = "右滑返回上一頁 →";
                }
            } else {
                if (currentPage == 0) {
                    indicatorText = "← 左滑查看更多功能";
                } else {
                    indicatorText = "右滑返回上一頁 →";
                }
            }
            
            pageIndicator.setText(indicatorText);
            pageIndicator.setContentDescription(indicatorText);
        } else if (pageIndicator != null) {
            pageIndicator.setText("");
            pageIndicator.setContentDescription("");
        }
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
                startFindItemsActivity();
                break;
            case "live_assistance":
                announceNavigation("正在打開即時協助");
                startActivity(new Intent(this, InstantAssistanceActivity.class).putExtra("language", currentLanguage));
                break;
            case "travel_assistant":
                startTravelAssistantActivity();
                break;
            case "gesture_management":
                startGestureManagementActivity();
                break;
        }
    }
    
    protected void startGestureManagementActivity() {
        try {
            Intent intent = new Intent(MainActivity.this, GestureManagementActivity.class);
            intent.putExtra("language", currentLanguage);
            announceNavigation("正在進入手勢管理頁面");
            startActivity(intent);
        } catch (Exception e) {
            announceError("手勢管理功能暫不可用");
        }
    }

    protected void startEnvironmentActivity() {
        try {
            // 啟動真實AI檢測演示
            Intent intent = new Intent(MainActivity.this, RealAIDetectionActivity.class);
            intent.putExtra("language", currentLanguage);
            announceNavigation("正在進入真實AI檢測頁面");
            startActivity(intent);
        } catch (Exception e) {
            announceError("AI檢測功能暫不可用");
        }
    }
    
    protected void startVoiceCommandActivity() {
        try {
            Intent intent = new Intent(MainActivity.this, VoiceCommandActivity.class);
            intent.putExtra("language", currentLanguage);
            announceNavigation("正在進入語音命令頁面");
            startActivity(intent);
        } catch (Exception e) {
            announceError("語音命令功能暫不可用");
        }
    }

    protected void startDocumentCurrencyActivity() {
        try {
            Intent intent = new Intent(MainActivity.this, DocumentCurrencyActivity.class);
            intent.putExtra("language", currentLanguage);
            announceNavigation(getString(R.string.document_assistant_announcement));
            startActivity(intent);
        } catch (Exception e) {
            announceError(getString(R.string.document_assistant_unavailable));
        }
    }
    
    protected void startFindItemsActivity() {
        try {
            Intent intent = new Intent(MainActivity.this, FindItemsActivity.class);
            intent.putExtra("language", currentLanguage);
            announceNavigation("正在進入尋找物品頁面");
            startActivity(intent);
        } catch (Exception e) {
            announceError("尋找物品功能暫不可用");
            Log.e("MainActivity", "打開尋找物品失敗: " + e.getMessage());
        }
    }
    
    protected void startTravelAssistantActivity() {
        try {
            Intent intent = new Intent(MainActivity.this, TravelAssistantActivity.class);
            intent.putExtra("language", currentLanguage);
            announceNavigation("正在進入出行協助頁面");
            startActivity(intent);
        } catch (Exception e) {
            announceError("出行協助功能暫不可用");
            Log.e("MainActivity", "打開出行協助失敗: " + e.getMessage());
        }
    }

    private String getEnglishFunctionName(String chineseName) {
        switch (chineseName) {
            case "環境識別": return "Environment Recognition";
            case "閱讀助手": return "Document Assistant";
            case "語音命令": return "Voice Command";
            case "尋找物品": return "Find Items";
            case "即時協助": return "Live Assistance";
            case "出行協助": return "Travel Assistant";
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
            case "提供導航、路線規劃、交通信息、天氣更新和緊急位置分享服務": return "Provides navigation, route planning, traffic information, weather updates, and emergency location sharing services";
            default: return chineseDescription;
        }
    }
    
    public void openSettings() {
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
    
    public void openEmergencySettings() {
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