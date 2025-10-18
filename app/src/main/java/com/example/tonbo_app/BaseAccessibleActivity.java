package com.example.tonbo_app;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public abstract class BaseAccessibleActivity extends AppCompatActivity {
    protected TTSManager ttsManager;
    protected VibrationManager vibrationManager;
    protected LocaleManager localeManager;
    protected GlobalVoiceCommandManager globalVoiceManager;
    protected String currentLanguage;
    
    // 權限請求常數
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1001;
    
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
        
        // 強制初始化TTS，確保語音播報可用
        ttsManager.forceInitialize();
        
        // 初始化全局語音命令管理器
        initializeGlobalVoiceManager();
        
        // 檢查並請求麥克風權限
        checkAndRequestMicrophonePermission();
        
        // 設置無障礙支持
        setupAccessibility();
        
        // 頁面載入完成後播放頁面標題
        getWindow().getDecorView().post(() -> {
            // 延遲播報，確保TTS初始化完成
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                Log.d("BaseAccessibleActivity", "🔊 開始播報頁面標題");
                announcePageTitle();
            }, 1000); // 延遲1秒確保TTS初始化完成
        });
    }
    
    private void initializeGlobalVoiceManager() {
        globalVoiceManager = GlobalVoiceCommandManager.getInstance(this, ttsManager);
        
        // 設置語音命令回調
        globalVoiceManager.setCallback(new GlobalVoiceCommandManager.VoiceCommandCallback() {
            @Override
            public void onCommandRecognized(String command) {
                handleGlobalVoiceCommand(command);
            }
            
            @Override
            public void onVoiceError(String error) {
                announceError("語音命令錯誤: " + error);
            }
        });
    }
    
    /**
     * 處理全局語音命令
     * 子類可以重寫此方法來處理特定的語音命令
     */
    protected void handleGlobalVoiceCommand(String command) {
        switch (command) {
            case "environment":
                startEnvironmentActivity();
                break;
            case "document":
                startDocumentCurrencyActivity();
                break;
            case "find_items":
                startFindItemsActivity();
                break;
            case "live_assistance":
                announceInfo("即時協助功能開發中");
                break;
            case "emergency":
                handleEmergencyCommand();
                break;
            case "home":
                goToHome();
                break;
            case "settings":
                startSettingsActivity();
                break;
            case "language":
                handleLanguageSwitch();
                break;
            case "time":
                announceCurrentTime();
                break;
            case "stop":
                stopCurrentOperation();
                break;
            default:
                announceInfo("未識別的語音命令: " + command);
                break;
        }
    }
    
    /**
     * 啟動全局語音命令聆聽
     */
    public void startGlobalVoiceCommand() {
        if (globalVoiceManager != null) {
            globalVoiceManager.startListening(null);
        }
    }
    
    /**
     * 停止全局語音命令聆聽
     */
    public void stopGlobalVoiceCommand() {
        if (globalVoiceManager != null) {
            globalVoiceManager.stopListening();
        }
    }
    
    // 默認的Activity啟動方法，子類可以重寫
    protected void startEnvironmentActivity() {
        Intent intent = new Intent(this, EnvironmentActivity.class);
        intent.putExtra("language", currentLanguage);
        startActivity(intent);
    }
    
    protected void startDocumentCurrencyActivity() {
        Intent intent = new Intent(this, DocumentCurrencyActivity.class);
        intent.putExtra("language", currentLanguage);
        startActivity(intent);
    }
    
    protected void startFindItemsActivity() {
        Intent intent = new Intent(this, FindItemsActivity.class);
        intent.putExtra("language", currentLanguage);
        startActivity(intent);
    }
    
    protected void startSettingsActivity() {
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra("language", currentLanguage);
        startActivity(intent);
    }
    
    protected void handleEmergencyCommand() {
        announceInfo("緊急求助功能");
        // 子類可以重寫此方法實現具體的緊急求助邏輯
    }
    
    protected void goToHome() {
        if (!(this instanceof MainActivity)) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("language", currentLanguage);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else {
            announceInfo("已在主頁");
        }
    }
    
    protected void handleLanguageSwitch() {
        announceInfo("語言切換功能");
        // 子類可以重寫此方法實現語言切換
    }
    
    protected void announceCurrentTime() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        String currentTime = sdf.format(new java.util.Date());
        announceInfo("現在時間: " + currentTime);
    }
    
    protected void stopCurrentOperation() {
        announceInfo("停止當前操作");
        // 子類可以重寫此方法實現停止邏輯
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
        // 停止語音識別但不銷毀全局語音命令管理器（保持單例）
        if (globalVoiceManager != null) {
            globalVoiceManager.stopListening();
            // 不銷毀實例，保持全局可用
        }
        // 頁面銷毀時清理資源（但保持管理器實例）
        // ttsManager和vibrationManager是單例，由MainActivity統一管理
    }
    
    // 獲取當前語言
    protected String getCurrentLanguage() {
        return currentLanguage;
    }
    
    /**
     * 檢查並請求麥克風權限
     */
    private void checkAndRequestMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            
            // 權限未授予，請求權限
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.RECORD_AUDIO}, 
                REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            // 權限已授予
            android.util.Log.d("BaseAccessibleActivity", "麥克風權限已授予");
        }
    }
    
    /**
     * 處理權限請求結果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 麥克風權限已授予
                android.util.Log.d("BaseAccessibleActivity", "麥克風權限請求成功");
                announceInfo("麥克風權限已授予，語音命令功能已啟用");
            } else {
                // 麥克風權限被拒絕
                android.util.Log.w("BaseAccessibleActivity", "麥克風權限請求被拒絕");
                announceInfo("麥克風權限被拒絕，語音命令功能將不可用");
            }
        }
    }
    
    // 切換語言
    protected void switchLanguage(String language) {
        currentLanguage = language;
        ttsManager.changeLanguage(language);
    }
}
