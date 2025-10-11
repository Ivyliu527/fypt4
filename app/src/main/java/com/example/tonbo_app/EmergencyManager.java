package com.example.tonbo_app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.SmsManager;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class EmergencyManager {
    private static final String TAG = "EmergencyManager";
    private static EmergencyManager instance;
    
    private Context context;
    private TTSManager ttsManager;
    private VibrationManager vibrationManager;
    
    // 緊急聯絡人列表（可以從設置中配置）
    private List<String> emergencyContacts = new ArrayList<>();
    private String emergencyMessage = "緊急求助！我在使用瞳伴應用時遇到緊急情況，需要立即協助。請盡快聯繫我。";
    private String emergencyMessageEn = "Emergency! I'm using Tonbo app and need immediate assistance. Please contact me as soon as possible.";
    
    private EmergencyManager(Context context) {
        this.context = context.getApplicationContext();
        ttsManager = TTSManager.getInstance(context);
        vibrationManager = VibrationManager.getInstance(context);
        
        // 初始化默認緊急聯絡人（可以從設置中讀取）
        initializeEmergencyContacts();
    }
    
    public static synchronized EmergencyManager getInstance(Context context) {
        if (instance == null) {
            instance = new EmergencyManager(context);
        }
        return instance;
    }
    
    private void initializeEmergencyContacts() {
        // 添加默認緊急聯絡人（實際應用中應該從用戶設置中讀取）
        emergencyContacts.add("999"); // 香港緊急服務
        emergencyContacts.add("+852-999"); // 帶國際區號
        // 可以添加更多聯絡人
    }
    
    public void triggerEmergencyAlert() {
        Log.d(TAG, "緊急求助觸發");
        
        // 播放緊急提示音
        String cantoneseText = "緊急求助已發送！請保持冷靜，協助正在趕來。已通知緊急聯絡人並撥打緊急服務。";
        String englishText = "Emergency alert sent! Please stay calm, assistance is on the way. Emergency contacts have been notified and emergency services have been called.";
        ttsManager.speak(cantoneseText, englishText, true);
        
        // 強烈震動提醒
        vibrationManager.vibrateEmergencyPattern();
        
        // 發送緊急短信
        sendEmergencySMS();
        
        // 撥打緊急電話
        callEmergencyService();
        
        // 記錄緊急事件
        logEmergencyEvent();
    }
    
    private void sendEmergencySMS() {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            String message = emergencyMessage;
            if (ttsManager.getCurrentLanguage().equals("english")) {
                message = emergencyMessageEn;
            }
            
            for (String contact : emergencyContacts) {
                if (contact.matches("\\d+")) { // 只發送給電話號碼
                    smsManager.sendTextMessage(contact, null, message, null, null);
                    Log.d(TAG, "緊急短信已發送給: " + contact);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "發送緊急短信失敗: " + e.getMessage());
            ttsManager.speakError("緊急短信發送失敗，請手動聯繫緊急服務");
        }
    }
    
    private void callEmergencyService() {
        try {
            // 撥打緊急服務電話
            String emergencyNumber = "tel:999";
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse(emergencyNumber));
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            // 注意：需要CALL_PHONE權限
            context.startActivity(callIntent);
            Log.d(TAG, "緊急電話已撥打");
            
        } catch (SecurityException e) {
            Log.e(TAG, "撥打緊急電話失敗，缺少權限: " + e.getMessage());
            // 嘗試使用ACTION_DIAL（不需要權限）
            try {
                Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                dialIntent.setData(Uri.parse("tel:999"));
                dialIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(dialIntent);
                ttsManager.speak("正在撥打緊急電話，請確認撥號", "Calling emergency service, please confirm dial");
            } catch (Exception ex) {
                Log.e(TAG, "撥號也失敗: " + ex.getMessage());
                ttsManager.speakError("無法撥打緊急電話，請手動撥打999");
            }
        } catch (Exception e) {
            Log.e(TAG, "撥打緊急電話失敗: " + e.getMessage());
            ttsManager.speakError("撥打緊急電話失敗，請手動撥打999");
        }
    }
    
    private void logEmergencyEvent() {
        // 記錄緊急事件到日誌或本地數據庫
        Log.i(TAG, "緊急事件已記錄: " + System.currentTimeMillis());
        
        // 可以在這裡添加更多功能：
        // - 記錄GPS位置
        // - 保存到本地數據庫
        // - 上傳到雲端服務
        // - 發送給家人朋友
    }
    
    // 添加緊急聯絡人
    public void addEmergencyContact(String contact) {
        if (!emergencyContacts.contains(contact)) {
            emergencyContacts.add(contact);
            Log.d(TAG, "已添加緊急聯絡人: " + contact);
        }
    }
    
    // 移除緊急聯絡人
    public void removeEmergencyContact(String contact) {
        emergencyContacts.remove(contact);
        Log.d(TAG, "已移除緊急聯絡人: " + contact);
    }
    
    // 獲取緊急聯絡人列表
    public List<String> getEmergencyContacts() {
        return new ArrayList<>(emergencyContacts);
    }
    
    // 設置緊急訊息
    public void setEmergencyMessage(String message, String messageEn) {
        this.emergencyMessage = message;
        this.emergencyMessageEn = messageEn;
    }
    
    // 測試緊急功能（用於測試，不會真正發送）
    public void testEmergencyAlert() {
        String cantoneseText = "這是緊急功能測試，沒有發送實際求助信息";
        String englishText = "This is an emergency function test, no actual help request was sent";
        ttsManager.speak(cantoneseText, englishText, true);
        vibrationManager.vibrateSuccessPattern();
    }
}
