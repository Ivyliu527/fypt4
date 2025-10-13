package com.example.tonbo_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/**
 * 緊急求助設置頁面
 * 用於配置緊急聯絡人、測試緊急功能
 */
public class EmergencySettingsActivity extends BaseAccessibleActivity {
    private static final String TAG = "EmergencySettingsActivity";
    private static final String PREFS_NAME = "emergency_settings";
    private static final String KEY_CONTACTS = "emergency_contacts";
    private static final String KEY_MESSAGE = "emergency_message";
    private static final String KEY_MESSAGE_EN = "emergency_message_en";
    private static final String KEY_SETUP_COMPLETED = "setup_completed";
    
    private EmergencyManager emergencyManager;
    private TTSManager ttsManager;
    private VibrationManager vibrationManager;
    
    private RecyclerView contactsRecyclerView;
    private EmergencyContactsAdapter contactsAdapter;
    private List<String> emergencyContacts;
    private EditText newContactEditText;
    private Button addContactButton;
    private Button testEmergencyButton;
    private Button backButton;
    private Button setupCompleteButton;
    private TextView messagePreviewText;
    private LinearLayout addContactSection;
    private LinearLayout contactsSection;
    private LinearLayout messageSection;
    private boolean setupCompleted = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_settings);
        
        // 初始化管理器
        emergencyManager = EmergencyManager.getInstance(this);
        ttsManager = TTSManager.getInstance(this);
        vibrationManager = VibrationManager.getInstance(this);
        
        // 初始化視圖
        initViews();
        
        // 載入設置
        loadSettings();
        
        // 設置語言（靜默設置，不播報）
        ttsManager.setLanguageSilently(currentLanguage);
    }
    
    private void initViews() {
        // 標題欄
        backButton = findViewById(R.id.backButton);
        TextView titleText = findViewById(R.id.titleText);
        titleText.setText(getString(R.string.emergency_settings_page_title));
        titleText.setContentDescription(getString(R.string.emergency_settings_page_desc));
        
        // 獲取各個區域的引用
        addContactSection = findViewById(R.id.addContactSection);
        contactsSection = findViewById(R.id.contactsSection);
        messageSection = findViewById(R.id.messageSection);
        
        // 聯絡人列表
        contactsRecyclerView = findViewById(R.id.contactsRecyclerView);
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // 添加聯絡人
        newContactEditText = findViewById(R.id.newContactEditText);
        newContactEditText.setHint(getString(R.string.enter_phone_number));
        newContactEditText.setContentDescription(getString(R.string.enter_phone_number_desc));
        
        addContactButton = findViewById(R.id.addContactButton);
        
        // 測試按鈕
        testEmergencyButton = findViewById(R.id.testEmergencyButton);
        
        // 設置完成按鈕
        setupCompleteButton = findViewById(R.id.setupCompleteButton);
        
        // 訊息預覽
        messagePreviewText = findViewById(R.id.messagePreviewText);
        
        // 設置點擊事件
        backButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            finish();
        });

        
        addContactButton.setOnClickListener(v -> addEmergencyContact());
        
        testEmergencyButton.setOnClickListener(v -> testEmergencyFunction());
        
        setupCompleteButton.setOnClickListener(v -> completeSetup());
        
        // 設置內容描述
        setContentDescriptions();
    }
    
    private void setContentDescriptions() {
        backButton.setContentDescription(getString(R.string.back_button_desc_emergency));
        addContactButton.setContentDescription(getString(R.string.add_contact_button_desc));
        testEmergencyButton.setContentDescription(getString(R.string.test_emergency_button_desc));
        setupCompleteButton.setContentDescription(getString(R.string.setup_complete_button_desc));
        contactsRecyclerView.setContentDescription(getString(R.string.contacts_list_desc));
    }
    
    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // 載入聯絡人列表
        String contactsJson = prefs.getString(KEY_CONTACTS, "");
        emergencyContacts = new ArrayList<>();
        
        if (!contactsJson.isEmpty()) {
            String[] contacts = contactsJson.split(",");
            for (String contact : contacts) {
                if (!contact.trim().isEmpty()) {
                    emergencyContacts.add(contact.trim());
                }
            }
        }
        
        // 如果沒有聯絡人，添加默認的
        if (emergencyContacts.isEmpty()) {
            emergencyContacts.add("999"); // 香港緊急服務
            emergencyContacts.add("+852-999");
        }
        
        // 設置適配器
        contactsAdapter = new EmergencyContactsAdapter(emergencyContacts, this::removeEmergencyContact);
        contactsRecyclerView.setAdapter(contactsAdapter);
        
        // 更新緊急管理器
        updateEmergencyManager();
        
        // 載入訊息設置
        String message = prefs.getString(KEY_MESSAGE, getString(R.string.emergency_message_content));
        String messageEn = prefs.getString(KEY_MESSAGE_EN, "Emergency! I'm using Tonbo app and need immediate assistance. Please contact me as soon as possible.");
        
        emergencyManager.setEmergencyMessage(message, messageEn);
        
        // 更新訊息預覽
        updateMessagePreview();
        
        // 檢查設置是否已完成
        setupCompleted = prefs.getBoolean(KEY_SETUP_COMPLETED, false);
        
        // 根據設置狀態調整界面
        updateUIForSetupStatus();
    }
    
    private void updateEmergencyManager() {
        // 清除現有聯絡人
        List<String> currentContacts = emergencyManager.getEmergencyContacts();
        for (String contact : currentContacts) {
            emergencyManager.removeEmergencyContact(contact);
        }
        
        // 添加新聯絡人
        for (String contact : emergencyContacts) {
            emergencyManager.addEmergencyContact(contact);
        }
    }
    
    private void updateMessagePreview() {
        String currentLang = ttsManager.getCurrentLanguage();
        String message = getString(R.string.emergency_message_content);
        
        messagePreviewText.setText(String.format(getString(R.string.emergency_message_preview_format), message));
        messagePreviewText.setContentDescription("緊急求助訊息預覽：" + message);
    }
    
    private void addEmergencyContact() {
        String contact = newContactEditText.getText().toString().trim();
        
        if (contact.isEmpty()) {
            announceError(getString(R.string.error_enter_phone));
            return;
        }
        
        // 簡單驗證電話號碼格式
        if (!isValidPhoneNumber(contact)) {
            announceError(getString(R.string.error_invalid_phone));
            return;
        }
        
        if (emergencyContacts.contains(contact)) {
            announceError(getString(R.string.error_contact_exists));
            return;
        }
        
        emergencyContacts.add(contact);
        contactsAdapter.notifyItemInserted(emergencyContacts.size() - 1);
        emergencyManager.addEmergencyContact(contact);
        
        newContactEditText.setText("");
        announceSuccess("已添加緊急聯絡人：" + contact);
        
        saveSettings();
    }
    
    private void removeEmergencyContact(String contact) {
        int index = emergencyContacts.indexOf(contact);
        if (index >= 0) {
            emergencyContacts.remove(index);
            contactsAdapter.notifyItemRemoved(index);
            emergencyManager.removeEmergencyContact(contact);
            announceInfo("已移除緊急聯絡人：" + contact);
            saveSettings();
        }
    }
    
    private boolean isValidPhoneNumber(String phoneNumber) {
        // 簡單的電話號碼驗證
        return phoneNumber.matches("^[+]?[0-9\\-\\s()]{8,}$");
    }
    
    private void testEmergencyFunction() {
        vibrationManager.vibrateClick();
        
        // 顯示確認對話框
        String cantoneseText = getString(R.string.test_emergency_confirmation);
        String englishText = "About to test emergency function, this will not send actual help request, just test voice and vibration. Continue?";
        
        announceInfo(cantoneseText);
        
        // 延遲執行測試
        new android.os.Handler().postDelayed(() -> {
            emergencyManager.testEmergencyAlert();
        }, 2000);
    }
    
    private void saveSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // 保存聯絡人列表
        StringBuilder contactsBuilder = new StringBuilder();
        for (int i = 0; i < emergencyContacts.size(); i++) {
            if (i > 0) contactsBuilder.append(",");
            contactsBuilder.append(emergencyContacts.get(i));
        }
        editor.putString(KEY_CONTACTS, contactsBuilder.toString());
        
        editor.apply();
        Log.d(TAG, "緊急設置已保存");
    }
    
    private void updateUIForSetupStatus() {
        if (setupCompleted) {
            // 設置已完成，隱藏添加聯絡人區域，顯示簡化界面
            addContactSection.setVisibility(View.GONE);
            setupCompleteButton.setVisibility(View.GONE);
            
            // 更新標題
            TextView titleText = findViewById(R.id.titleText);
            titleText.setText(getString(R.string.emergency_setup_complete_title));
            titleText.setContentDescription(getString(R.string.emergency_setup_complete_desc));
            
            // 更新測試按鈕文字
            testEmergencyButton.setText(getString(R.string.test_emergency_button_complete));
            
        } else {
            // 設置未完成，顯示完整設置界面
            addContactSection.setVisibility(View.VISIBLE);
            setupCompleteButton.setVisibility(View.VISIBLE);
            
            // 更新標題
            TextView titleText = findViewById(R.id.titleText);
            titleText.setText(getString(R.string.emergency_settings_page_title));
            titleText.setContentDescription(getString(R.string.emergency_settings_page_desc));
            
            // 更新測試按鈕文字
            testEmergencyButton.setText(getString(R.string.test_emergency_button_incomplete));
        }
    }
    
    private void completeSetup() {
        vibrationManager.vibrateClick();
        
        if (emergencyContacts.size() < 1) {
            announceError(getString(R.string.error_add_contact));
            return;
        }
        
        // 標記設置為已完成
        setupCompleted = true;
        
        // 保存設置狀態
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_SETUP_COMPLETED, true);
        editor.apply();
        
        // 更新界面
        updateUIForSetupStatus();
        
        // 播報完成信息
        String cantoneseText = "緊急求助設置已完成！現在可以使用緊急求助功能。長按主頁面的紅色緊急按鈕3秒即可觸發緊急求助。";
        String englishText = "Emergency setup completed! You can now use the emergency function. Long press the red emergency button on the main page for 3 seconds to trigger emergency alert.";
        announceSuccess(cantoneseText);
        
        // 延遲返回主頁面
        new android.os.Handler().postDelayed(() -> {
            finish();
        }, 3000);
    }
    
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveSettings();
    }
    
    @Override
    protected void announcePageTitle() {
        // 延遲播報，確保語言設置生效
        new android.os.Handler().postDelayed(() -> {
            if (setupCompleted) {
                String cantoneseText = "緊急求助已設置完成。當前有" + emergencyContacts.size() + "個緊急聯絡人。可以測試緊急功能。";
                String englishText = "Emergency setup completed. Currently have " + emergencyContacts.size() + " emergency contacts. You can test emergency function.";
                ttsManager.speak(cantoneseText, englishText, true);
            } else {
                String cantoneseText = "緊急求助設置頁面。當前有" + emergencyContacts.size() + "個緊急聯絡人。請添加聯絡人並完成設置。";
                String englishText = "Emergency settings page. Currently have " + emergencyContacts.size() + " emergency contacts. Please add contacts and complete setup.";
                ttsManager.speak(cantoneseText, englishText, true);
            }
        }, 500);
    }
}
