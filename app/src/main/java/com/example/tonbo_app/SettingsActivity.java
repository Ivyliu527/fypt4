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
        
        // 初始化TTSManager
        ttsManager = TTSManager.getInstance(this);
        
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
        
        updateToggleButton(vibrationToggleButton, vibrationEnabled, getString(R.string.vibration_feedback));
        updateToggleButton(screenReaderToggleButton, screenReaderEnabled, getString(R.string.screen_reader_support));
        updateToggleButton(gestureToggleButton, gestureEnabled, getString(R.string.gesture_operations));
        
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
                    updateSpeechTexts();
                    // 不在這裡播報，避免連續播報
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                vibrationManager.vibrateClick();
                ttsManager.stop(); // 停止當前播報
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 停止調整時才播報和保存
                int progress = seekBar.getProgress();
                float rate = progress / 100.0f;
                preferences.edit().putFloat("speech_rate", rate).apply();
                announceSettingChange(String.format(getString(R.string.speech_rate_changed_to), progress));
            }
        });
        
        // 音調調整
        speechPitchSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float pitch = progress / 100.0f;
                    ttsManager.setSpeechPitch(pitch);
                    updateSpeechTexts();
                    // 不在這裡播報，避免連續播報
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                vibrationManager.vibrateClick();
                ttsManager.stop(); // 停止當前播報
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 停止調整時才播報和保存
                int progress = seekBar.getProgress();
                float pitch = progress / 100.0f;
                preferences.edit().putFloat("speech_pitch", pitch).apply();
                announceSettingChange(String.format(getString(R.string.speech_pitch_changed_to), progress));
            }
        });
        
        // 音量調整
        speechVolumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float volume = progress / 100.0f;
                    ttsManager.setSpeechVolume(volume);
                    updateSpeechTexts();
                    // 不在這裡播報，避免連續播報
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                vibrationManager.vibrateClick();
                ttsManager.stop(); // 停止當前播報
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 停止調整時才播報和保存
                int progress = seekBar.getProgress();
                float volume = progress / 100.0f;
                preferences.edit().putFloat("speech_volume", volume).apply();
                announceSettingChange(String.format(getString(R.string.speech_volume_changed_to), progress));
            }
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
        
        speechRateText.setText(String.format(getString(R.string.speech_rate_display), rate));
        speechPitchText.setText(String.format(getString(R.string.speech_pitch_display), pitch));
        speechVolumeText.setText(String.format(getString(R.string.speech_volume_display), volume));
    }
    
    private void updateToggleButton(Button button, boolean enabled, String settingName) {
        if (enabled) {
            button.setText(getString(R.string.status_on));
            button.setContentDescription(settingName + getString(R.string.status_on) + "，點擊關閉");
        } else {
            button.setText(getString(R.string.status_off));
            button.setContentDescription(settingName + getString(R.string.status_off) + "，點擊開啟");
        }
    }
    
    private void toggleVibration() {
        boolean currentState = preferences.getBoolean("vibration_enabled", true);
        boolean newState = !currentState;
        
        preferences.edit().putBoolean("vibration_enabled", newState).apply();
        updateToggleButton(vibrationToggleButton, newState, getString(R.string.vibration_feedback));
        
        // 更新VibrationManager狀態
        vibrationManager.setEnabled(newState);
        
        String message = newState ? getString(R.string.vibration_feedback_status_on) : getString(R.string.vibration_feedback_status_off);
        announceSettingChange(message);
        
        if (newState) {
            vibrationManager.vibrateClick();
        }
    }
    
    private void toggleScreenReader() {
        boolean currentState = preferences.getBoolean("screen_reader_enabled", true);
        boolean newState = !currentState;
        
        preferences.edit().putBoolean("screen_reader_enabled", newState).apply();
        updateToggleButton(screenReaderToggleButton, newState, getString(R.string.screen_reader_support));
        
        String message = newState ? getString(R.string.screen_reader_status_on) : getString(R.string.screen_reader_status_off);
        announceSettingChange(message);
        
        // 這裡可以添加讀屏相關的設定邏輯
        if (newState) {
            announceInfo(getString(R.string.screen_reader_status_on));
        }
    }
    
    private void toggleGesture() {
        boolean currentState = preferences.getBoolean("gesture_enabled", false);
        boolean newState = !currentState;
        
        preferences.edit().putBoolean("gesture_enabled", newState).apply();
        updateToggleButton(gestureToggleButton, newState, getString(R.string.gesture_operations));
        
        String message = newState ? getString(R.string.gesture_operations_status_on) : getString(R.string.gesture_operations_status_off);
        announceSettingChange(message);
        
        if (newState) {
            announceInfo(getString(R.string.gesture_operations_status_on));
        }
    }
    
    private void testVoice() {
        announceInfo(getString(R.string.testing_voice));
        
        // 根據當前語言生成測試文字
        String currentLang = ttsManager.getCurrentLanguage();
        String testText;
        
        if ("english".equals(currentLang)) {
            testText = "This is voice test. Speech rate: " + speechRateSeekBar.getProgress() + "%, pitch: " + 
                      speechPitchSeekBar.getProgress() + "%, volume: " + speechVolumeSeekBar.getProgress() + "%.";
        } else if ("mandarin".equals(currentLang)) {
            testText = "这是语音测试。语速：" + speechRateSeekBar.getProgress() + "%，音调：" + 
                      speechPitchSeekBar.getProgress() + "%，音量：" + speechVolumeSeekBar.getProgress() + "%。";
        } else { // cantonese (default)
            testText = "這是語音測試。語速：" + speechRateSeekBar.getProgress() + "%，音調：" + 
                      speechPitchSeekBar.getProgress() + "%，音量：" + speechVolumeSeekBar.getProgress() + "%。";
        }
        
        new android.os.Handler().postDelayed(() -> {
            ttsManager.speak(testText, null, true);
            
            // 再延遲播報詳細設定狀態
            new android.os.Handler().postDelayed(() -> {
                announceCurrentSettings();
            }, 3000);
        }, 1000);
    }
    
    private void announceCurrentSettings() {
        // 播報當前所有設定狀態
        String currentSettings = String.format(getString(R.string.current_speech_rate), speechRateSeekBar.getProgress()) + "。";
        currentSettings += String.format(getString(R.string.current_speech_pitch), speechPitchSeekBar.getProgress()) + "。";
        currentSettings += String.format(getString(R.string.current_speech_volume), speechVolumeSeekBar.getProgress()) + "。";
        
        boolean vibrationEnabled = preferences.getBoolean("vibration_enabled", true);
        boolean screenReaderEnabled = preferences.getBoolean("screen_reader_enabled", true);
        boolean gestureEnabled = preferences.getBoolean("gesture_enabled", false);
        
        currentSettings += String.format(getString(R.string.current_vibration_status), 
            vibrationEnabled ? getString(R.string.status_enabled) : getString(R.string.status_disabled)) + "。";
        currentSettings += String.format(getString(R.string.current_screen_reader_status), 
            screenReaderEnabled ? getString(R.string.status_enabled) : getString(R.string.status_disabled)) + "。";
        currentSettings += String.format(getString(R.string.current_gesture_status), 
            gestureEnabled ? getString(R.string.status_enabled) : getString(R.string.status_disabled)) + "。";
        
        ttsManager.speak(currentSettings, null, true);
    }
    
    private void showResetDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.reset_settings_title))
                .setMessage(getString(R.string.reset_settings_message))
                .setPositiveButton(getString(R.string.confirm), (dialog, which) -> {
                    resetAllSettings();
                    announceInfo(getString(R.string.settings_reset));
                })
                .setNegativeButton(getString(R.string.back), (dialog, which) -> {
                    announceInfo(getString(R.string.reset_cancelled));
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
        
        updateToggleButton(vibrationToggleButton, true, getString(R.string.vibration_feedback));
        updateToggleButton(screenReaderToggleButton, true, getString(R.string.screen_reader_support));
        updateToggleButton(gestureToggleButton, false, getString(R.string.gesture_operations));
        
        // 重置TTS設定
        ttsManager.setSpeechRate(1.0f);
        ttsManager.setSpeechPitch(1.0f);
        ttsManager.setSpeechVolume(1.0f);
        
        // 重置震動設定
        vibrationManager.setEnabled(true);
    }
    
    private void announceSettingChange(String message) {
        // 直接使用傳入的詳細訊息，不需要額外的英文訊息
        ttsManager.speak(message, null, false);
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
