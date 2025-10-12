package com.example.tonbo_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

public class SettingsActivity extends BaseAccessibleActivity {
    private static final String TAG = "SettingsActivity";
    private static final String PREFS_NAME = "TonboSettings";
    
    // 語音設定相關
    private SeekBar speechRateSeekBar;
    private SeekBar speechPitchSeekBar;
    private SeekBar speechVolumeSeekBar;
    private TextView speechRateText;
    private TextView speechPitchText;
    private TextView speechVolumeText;
    
    // 無障礙設定相關
    private Button vibrationToggleButton;
    private Button screenReaderToggleButton;
    private Button gestureToggleButton;
    
    // 其他設定
    private Button resetSettingsButton;
    private Button testVoiceButton;
    
    private SharedPreferences preferences;
    private TTSManager ttsManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        // 初始化SharedPreferences
        preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        initViews();
        loadSettings();
        setupListeners();
        
        // 頁面標題播報
        announcePageTitle();
    }
    
    private void initViews() {
        // 語音設定
        speechRateSeekBar = findViewById(R.id.speechRateSeekBar);
        speechPitchSeekBar = findViewById(R.id.speechPitchSeekBar);
        speechVolumeSeekBar = findViewById(R.id.speechVolumeSeekBar);
        speechRateText = findViewById(R.id.speechRateText);
        speechPitchText = findViewById(R.id.speechPitchText);
        speechVolumeText = findViewById(R.id.speechVolumeText);
        
        // 無障礙設定
        vibrationToggleButton = findViewById(R.id.vibrationToggleButton);
        screenReaderToggleButton = findViewById(R.id.screenReaderToggleButton);
        gestureToggleButton = findViewById(R.id.gestureToggleButton);
        
        // 其他設定
        resetSettingsButton = findViewById(R.id.resetSettingsButton);
        testVoiceButton = findViewById(R.id.testVoiceButton);
        
        // 返回按鈕
        Button backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                vibrationManager.vibrateClick();
                finish();
            });
        }
    }
    
    private void loadSettings() {
        // 載入語音設定
        float speechRate = preferences.getFloat("speech_rate", 1.0f);
        float speechPitch = preferences.getFloat("speech_pitch", 1.0f);
        float speechVolume = preferences.getFloat("speech_volume", 1.0f);
        
        // 設定SeekBar值（0-200範圍，默認100）
        speechRateSeekBar.setProgress((int) (speechRate * 100));
        speechPitchSeekBar.setProgress((int) (speechPitch * 100));
        speechVolumeSeekBar.setProgress((int) (speechVolume * 100));
        
        updateSpeechTexts();
        
        // 載入無障礙設定
        boolean vibrationEnabled = preferences.getBoolean("vibration_enabled", true);
        boolean screenReaderEnabled = preferences.getBoolean("screen_reader_enabled", true);
        boolean gestureEnabled = preferences.getBoolean("gesture_enabled", false);
        
        updateToggleButton(vibrationToggleButton, vibrationEnabled, "震動反饋");
        updateToggleButton(screenReaderToggleButton, screenReaderEnabled, "讀屏支援");
        updateToggleButton(gestureToggleButton, gestureEnabled, "手勢操作");
        
        Log.d(TAG, "設定已載入 - 語速:" + speechRate + " 音調:" + speechPitch + " 音量:" + speechVolume);
    }
    
    private void setupListeners() {
        // 語速調整
        speechRateSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float rate = progress / 100.0f;
                    ttsManager.setSpeechRate(rate);
                    preferences.edit().putFloat("speech_rate", rate).apply();
                    updateSpeechTexts();
                    announceSettingChange("語速已調整為 " + progress + "%");
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                vibrationManager.vibrateClick();
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // 音調調整
        speechPitchSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float pitch = progress / 100.0f;
                    ttsManager.setSpeechPitch(pitch);
                    preferences.edit().putFloat("speech_pitch", pitch).apply();
                    updateSpeechTexts();
                    announceSettingChange("音調已調整為 " + progress + "%");
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                vibrationManager.vibrateClick();
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // 音量調整
        speechVolumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float volume = progress / 100.0f;
                    ttsManager.setSpeechVolume(volume);
                    preferences.edit().putFloat("speech_volume", volume).apply();
                    updateSpeechTexts();
                    announceSettingChange("音量已調整為 " + progress + "%");
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                vibrationManager.vibrateClick();
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // 震動反饋切換
        vibrationToggleButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            toggleVibration();
        });
        
        // 讀屏支援切換
        screenReaderToggleButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            toggleScreenReader();
        });
        
        // 手勢操作切換
        gestureToggleButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            toggleGesture();
        });
        
        // 測試語音
        testVoiceButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            testVoice();
        });
        
        // 重置設定
        resetSettingsButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            showResetDialog();
        });
    }
    
    private void updateSpeechTexts() {
        int rate = speechRateSeekBar.getProgress();
        int pitch = speechPitchSeekBar.getProgress();
        int volume = speechVolumeSeekBar.getProgress();
        
        speechRateText.setText("語速: " + rate + "%");
        speechPitchText.setText("音調: " + pitch + "%");
        speechVolumeText.setText("音量: " + volume + "%");
    }
    
    private void updateToggleButton(Button button, boolean enabled, String settingName) {
        if (enabled) {
            button.setText("開啟");
            button.setContentDescription(settingName + "已開啟，點擊關閉");
        } else {
            button.setText("關閉");
            button.setContentDescription(settingName + "已關閉，點擊開啟");
        }
    }
    
    private void toggleVibration() {
        boolean currentState = preferences.getBoolean("vibration_enabled", true);
        boolean newState = !currentState;
        
        preferences.edit().putBoolean("vibration_enabled", newState).apply();
        updateToggleButton(vibrationToggleButton, newState, "震動反饋");
        
        // 更新VibrationManager狀態
        vibrationManager.setEnabled(newState);
        
        String message = newState ? "震動反饋已開啟" : "震動反饋已關閉";
        announceSettingChange(message);
        
        if (newState) {
            vibrationManager.vibrateClick();
        }
    }
    
    private void toggleScreenReader() {
        boolean currentState = preferences.getBoolean("screen_reader_enabled", true);
        boolean newState = !currentState;
        
        preferences.edit().putBoolean("screen_reader_enabled", newState).apply();
        updateToggleButton(screenReaderToggleButton, newState, "讀屏支援");
        
        String message = newState ? "讀屏支援已開啟" : "讀屏支援已關閉";
        announceSettingChange(message);
        
        // 這裡可以添加讀屏相關的設定邏輯
        if (newState) {
            announceInfo("讀屏支援已開啟，將優化語音播報和內容描述");
        }
    }
    
    private void toggleGesture() {
        boolean currentState = preferences.getBoolean("gesture_enabled", false);
        boolean newState = !currentState;
        
        preferences.edit().putBoolean("gesture_enabled", newState).apply();
        updateToggleButton(gestureToggleButton, newState, "手勢操作");
        
        String message = newState ? "手勢操作已開啟" : "手勢操作已關閉";
        announceSettingChange(message);
        
        if (newState) {
            announceInfo("手勢操作已開啟，可以使用滑動手勢進行操作");
        }
    }
    
    private void testVoice() {
        announceInfo("正在測試語音設定");
        
        // 測試當前語音設定
        String testText = "這是語音測試。語速：" + speechRateSeekBar.getProgress() + "%，音調：" + 
                         speechPitchSeekBar.getProgress() + "%，音量：" + speechVolumeSeekBar.getProgress() + "%。";
        
        new android.os.Handler().postDelayed(() -> {
            ttsManager.speak(testText, null, true);
        }, 1000);
    }
    
    private void showResetDialog() {
        new AlertDialog.Builder(this)
                .setTitle("重置設定")
                .setMessage("確定要重置所有設定到預設值嗎？")
                .setPositiveButton("確定", (dialog, which) -> {
                    resetAllSettings();
                    announceInfo("所有設定已重置為預設值");
                })
                .setNegativeButton("取消", (dialog, which) -> {
                    announceInfo("已取消重置");
                })
                .show();
    }
    
    private void resetAllSettings() {
        // 重置語音設定
        preferences.edit()
                .putFloat("speech_rate", 1.0f)
                .putFloat("speech_pitch", 1.0f)
                .putFloat("speech_volume", 1.0f)
                .putBoolean("vibration_enabled", true)
                .putBoolean("screen_reader_enabled", true)
                .putBoolean("gesture_enabled", false)
                .apply();
        
        // 重置UI
        speechRateSeekBar.setProgress(100);
        speechPitchSeekBar.setProgress(100);
        speechVolumeSeekBar.setProgress(100);
        updateSpeechTexts();
        
        updateToggleButton(vibrationToggleButton, true, "震動反饋");
        updateToggleButton(screenReaderToggleButton, true, "讀屏支援");
        updateToggleButton(gestureToggleButton, false, "手勢操作");
        
        // 重置TTS設定
        ttsManager.setSpeechRate(1.0f);
        ttsManager.setSpeechPitch(1.0f);
        ttsManager.setSpeechVolume(1.0f);
        
        // 重置震動設定
        vibrationManager.setEnabled(true);
    }
    
    private void announceSettingChange(String message) {
        String englishMessage = "Setting changed";
        ttsManager.speak(message, englishMessage, false);
    }
    
    @Override
    protected void announcePageTitle() {
        new android.os.Handler().postDelayed(() -> {
            String cantoneseText = "系統設定頁面。可以調整語音設定、震動反饋、讀屏支援等選項。";
            String englishText = "System Settings. You can adjust voice settings, vibration feedback, screen reader support and more.";
            ttsManager.speak(cantoneseText, englishText, true);
        }, 500);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 保存當前設定
        saveSettings();
    }
    
    private void saveSettings() {
        // 確保所有設定都已保存
        Log.d(TAG, "設定已保存");
    }
}
