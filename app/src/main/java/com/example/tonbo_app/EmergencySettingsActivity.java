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
    private TextView messagePreviewText;
    
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
        
        // 設置語言
        ttsManager.changeLanguage(currentLanguage);
        
        // 播報頁面信息
        announcePageInfo();
    }
    
    private void initViews() {
        // 標題欄
        backButton = findViewById(R.id.backButton);
        TextView titleText = findViewById(R.id.titleText);
        titleText.setText("緊急求助設置");
        titleText.setContentDescription("緊急求助設置頁面標題");
        
        // 聯絡人列表
        contactsRecyclerView = findViewById(R.id.contactsRecyclerView);
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // 添加聯絡人
        newContactEditText = findViewById(R.id.newContactEditText);
        newContactEditText.setHint("輸入電話號碼");
        newContactEditText.setContentDescription("輸入新的緊急聯絡人電話號碼");
        
        addContactButton = findViewById(R.id.addContactButton);
        
        // 測試按鈕
        testEmergencyButton = findViewById(R.id.testEmergencyButton);
        
        // 訊息預覽
        messagePreviewText = findViewById(R.id.messagePreviewText);
        
        // 設置點擊事件
        backButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            finish();
        });
        
        addContactButton.setOnClickListener(v -> addEmergencyContact());
        
        testEmergencyButton.setOnClickListener(v -> testEmergencyFunction());
        
        // 設置內容描述
        setContentDescriptions();
    }
    
    private void setContentDescriptions() {
        backButton.setContentDescription("返回主頁按鈕");
        addContactButton.setContentDescription("添加緊急聯絡人按鈕");
        testEmergencyButton.setContentDescription("測試緊急求助功能按鈕");
        contactsRecyclerView.setContentDescription("緊急聯絡人列表");
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
        String message = prefs.getString(KEY_MESSAGE, "緊急求助！我在使用瞳伴應用時遇到緊急情況，需要立即協助。請盡快聯繫我。");
        String messageEn = prefs.getString(KEY_MESSAGE_EN, "Emergency! I'm using Tonbo app and need immediate assistance. Please contact me as soon as possible.");
        
        emergencyManager.setEmergencyMessage(message, messageEn);
        
        // 更新訊息預覽
        updateMessagePreview();
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
        String message = currentLang.equals("english") ? 
            "Emergency! I'm using Tonbo app and need immediate assistance. Please contact me as soon as possible." :
            "緊急求助！我在使用瞳伴應用時遇到緊急情況，需要立即協助。請盡快聯繫我。";
        
        messagePreviewText.setText("預設訊息：\n" + message);
        messagePreviewText.setContentDescription("緊急求助訊息預覽：" + message);
    }
    
    private void addEmergencyContact() {
        String contact = newContactEditText.getText().toString().trim();
        
        if (contact.isEmpty()) {
            announceError("請輸入電話號碼");
            return;
        }
        
        // 簡單驗證電話號碼格式
        if (!isValidPhoneNumber(contact)) {
            announceError("電話號碼格式不正確");
            return;
        }
        
        if (emergencyContacts.contains(contact)) {
            announceError("該聯絡人已存在");
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
        String cantoneseText = "即將測試緊急求助功能，這不會發送實際的求助信息，只是測試語音和震動。是否繼續？";
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
    
    private void announcePageInfo() {
        new android.os.Handler().postDelayed(() -> {
            String cantoneseText = "緊急求助設置頁面。當前有" + emergencyContacts.size() + "個緊急聯絡人。可以添加新聯絡人或測試緊急功能。";
            String englishText = "Emergency settings page. Currently have " + emergencyContacts.size() + " emergency contacts. You can add new contacts or test emergency function.";
            announceInfo(cantoneseText);
        }, 1000);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveSettings();
    }
    
    @Override
    protected void announcePageTitle() {
        announceInfo("緊急求助設置頁面");
    }
}
