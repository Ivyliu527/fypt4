package com.example.tonbo_app;

import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseAccessibleActivity extends AppCompatActivity {
    protected TTSManager ttsManager;
    protected VibrationManager vibrationManager;
    protected LocaleManager localeManager;
    protected String currentLanguage;
    
    @Override
    protected void attachBaseContext(Context newBase) {
        // 在Activity創建前應用語言設置
        LocaleManager localeManager = LocaleManager.getInstance(newBase);
        Context context = localeManager.updateResources(newBase, localeManager.getCurrentLanguage());
        super.attachBaseContext(context);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 初始化管理器
        ttsManager = TTSManager.getInstance(this);
        vibrationManager = VibrationManager.getInstance(this);
        localeManager = LocaleManager.getInstance(this);
        
        // 獲取語言設置
        currentLanguage = getIntent().getStringExtra("language");
        if (currentLanguage == null) {
            currentLanguage = localeManager.getCurrentLanguage(); // 使用保存的語言
        }
        
        // 設定TTSManager的語言
        ttsManager.setLanguageSilently(currentLanguage);
        
        // 設置無障礙支持
        setupAccessibility();
        
        // 頁面載入完成後播放頁面標題
        getWindow().getDecorView().post(() -> {
            announcePageTitle();
        });
    }
    
    private void setupAccessibility() {
        // 啟用無障礙服務
        AccessibilityManager accessibilityManager = 
                (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        
        if (accessibilityManager != null && accessibilityManager.isEnabled()) {
            // 設置無障礙事件監聽
            getWindow().getDecorView().setAccessibilityDelegate(new View.AccessibilityDelegate() {
                @Override
                public void sendAccessibilityEvent(View host, int eventType) {
                    super.sendAccessibilityEvent(host, eventType);
                    
                    if (eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
                        // 當焦點移動時提供語音反饋
                        String contentDescription = host.getContentDescription() != null ? 
                                host.getContentDescription().toString() : "";
                        if (!contentDescription.isEmpty()) {
                            ttsManager.speak(contentDescription, contentDescription);
                            vibrationManager.vibrateFocus();
                        }
                    }
                }
            });
        }
    }
    
    protected abstract void announcePageTitle();
    
    protected void announcePageTitle(String pageName) {
        ttsManager.speakPageTitle(pageName);
    }
    
    protected void announceNavigation(String message) {
        ttsManager.speakNavigationHint(message);
    }
    
    protected void announceSuccess(String message) {
        ttsManager.speakSuccess(message);
        vibrationManager.vibrateSuccess();
    }
    
    protected void announceError(String message) {
        ttsManager.speakError(message);
        vibrationManager.vibrateError();
    }
    
    protected void announceInfo(String message) {
        String englishMessage = translateToEnglish(message);
        ttsManager.speak(message, englishMessage);
        vibrationManager.vibrateNotification();
    }
    
    // 提供觸控反饋
    protected void provideTouchFeedback(View view) {
        view.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
        });
        
        view.setOnLongClickListener(v -> {
            vibrationManager.vibrateLongPress();
            return false;
        });
    }
    
    // 簡單的中英文翻譯方法
    private String translateToEnglish(String chinese) {
        switch (chinese) {
            case "環境識別": return "Environment Recognition";
            case "閱讀助手": return "Document Assistant";
            case "尋找物品": return "Find Items";
            case "即時協助": return "Live Assistance";
            case "緊急求助": return "Emergency Help";
            case "語言切換": return "Language Switch";
            case "返回": return "Back";
            case "確定": return "Confirm";
            case "取消": return "Cancel";
            case "開始": return "Start";
            case "停止": return "Stop";
            case "設置": return "Settings";
            default: return chinese;
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // 返回按鈕
            announceInfo("返回主頁");
            vibrationManager.vibrateClick();
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 頁面恢復時重新播放頁面標題
        announcePageTitle();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // 頁面暫停時停止語音播放
        ttsManager.pauseSpeaking();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 頁面銷毀時清理資源（但保持管理器實例）
        // ttsManager和vibrationManager是單例，由MainActivity統一管理
    }
    
    // 獲取當前語言
    protected String getCurrentLanguage() {
        return currentLanguage;
    }
    
    // 切換語言
    protected void switchLanguage(String language) {
        currentLanguage = language;
        ttsManager.changeLanguage(language);
    }
}
