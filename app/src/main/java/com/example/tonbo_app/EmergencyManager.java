package com.example.tonbo_app;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.telephony.SmsManager;
import android.util.Log;
import androidx.core.content.ContextCompat;
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
        // 可以添加更多聯絡人
    }
    
    public void triggerEmergencyAlert() {
        triggerEmergencyAlert(null); // 發送給所有聯絡人
    }
    
    /**
     * 觸發緊急求助（可選擇特定聯絡人）
     * @param selectedContacts 要發送給的聯絡人列表，如果為null則發送給所有聯絡人
     */
    public void triggerEmergencyAlert(List<String> selectedContacts) {
        Log.d(TAG, "緊急求助觸發，選擇的聯絡人: " + (selectedContacts != null ? selectedContacts.toString() : "全部"));
        
        // 確定要發送的聯絡人列表
        List<String> contactsToNotify = (selectedContacts != null && !selectedContacts.isEmpty()) 
            ? selectedContacts 
            : emergencyContacts;
        
        // 播放緊急提示音
        String cantoneseText = "緊急求助已發送！請保持冷靜，協助正在趕來。已通知" + contactsToNotify.size() + "個緊急聯絡人。";
        String englishText = "Emergency alert sent! Please stay calm, assistance is on the way. " + contactsToNotify.size() + " emergency contacts have been notified.";
        ttsManager.speak(cantoneseText, englishText, true);
        
        // 強烈震動提醒
        vibrationManager.vibrateEmergencyPattern();
        
        // 發送緊急短信（只發送給選擇的聯絡人）
        sendEmergencySMS(contactsToNotify);
        
        // 撥打緊急電話（撥打給選中的聯絡人，如果選擇了999則撥打999，否則撥打第一個選中的聯絡人）
        callEmergencyService(contactsToNotify);
        
        // 記錄緊急事件
        logEmergencyEvent();
    }
    
    private void sendEmergencySMS() {
        sendEmergencySMS(emergencyContacts); // 發送給所有聯絡人
    }
    
    /**
     * 發送緊急短信給指定聯絡人
     * @param contactsToNotify 要發送給的聯絡人列表
     */
    private void sendEmergencySMS(List<String> contactsToNotify) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            String message = emergencyMessage;
            if (ttsManager.getCurrentLanguage().equals("english")) {
                message = emergencyMessageEn;
            }
            
            int sentCount = 0;
            for (String contact : contactsToNotify) {
                // 清理電話號碼（移除空格、連字符等）
                String cleanContact = contact.replaceAll("[\\s\\-()]", "");
                if (cleanContact.matches("^[+]?\\d+$")) { // 只發送給有效的電話號碼
                    try {
                        smsManager.sendTextMessage(cleanContact, null, message, null, null);
                        Log.d(TAG, "緊急短信已發送給: " + cleanContact);
                        sentCount++;
                    } catch (Exception e) {
                        Log.e(TAG, "發送給 " + cleanContact + " 失敗: " + e.getMessage());
                    }
                } else {
                    Log.w(TAG, "跳過無效的聯絡人號碼: " + contact);
                }
            }
            
            if (sentCount == 0) {
                Log.w(TAG, "沒有成功發送任何緊急短信");
                ttsManager.speakError("緊急短信發送失敗，請手動聯繫緊急服務");
            } else {
                Log.d(TAG, "成功發送 " + sentCount + " 條緊急短信");
            }
        } catch (Exception e) {
            Log.e(TAG, "發送緊急短信失敗: " + e.getMessage());
            ttsManager.speakError("緊急短信發送失敗，請手動聯繫緊急服務");
        }
    }
    
    private void callEmergencyService() {
        callEmergencyService(null); // 默認撥打999
    }
    
    /**
     * 撥打緊急電話給指定聯絡人
     * @param contactsToCall 要撥打的聯絡人列表，如果為null或空則撥打999
     */
    private void callEmergencyService(List<String> contactsToCall) {
        try {
            String phoneNumber = null;
            
            // 確定要撥打的電話號碼
            if (contactsToCall != null && !contactsToCall.isEmpty()) {
                // 優先查找999（緊急服務）
                for (String contact : contactsToCall) {
                    String cleanContact = contact.replaceAll("[\\s\\-()]", "");
                    if (cleanContact.equals("999") || cleanContact.equals("+852999") || cleanContact.equals("852999")) {
                        phoneNumber = "999";
                        break;
                    }
                }
                
                // 如果沒有999，使用第一個有效的電話號碼
                if (phoneNumber == null) {
                    for (String contact : contactsToCall) {
                        String cleanContact = contact.replaceAll("[\\s\\-()]", "");
                        if (cleanContact.matches("^[+]?\\d+$")) {
                            phoneNumber = cleanContact;
                            break;
                        }
                    }
                }
            }
            
            // 如果沒有找到有效的電話號碼，使用默認的999
            if (phoneNumber == null) {
                phoneNumber = "999";
            }
            
            String phoneUri = "tel:" + phoneNumber;
            Log.d(TAG, "準備直接撥打緊急電話: " + phoneNumber);
            
            // 檢查是否有撥打電話的權限
            boolean hasCallPermission = ContextCompat.checkSelfPermission(context, 
                android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED;
            
            // 優先直接撥打（適合視障用戶，無需確認）
            if (hasCallPermission) {
            try {
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse(phoneUri));
                callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(callIntent);
                    Log.d(TAG, "緊急電話已直接撥打: " + phoneNumber);
                
                // 播報撥打信息
                    String cantoneseText = "正在撥打緊急電話：" + phoneNumber;
                    String englishText = "Calling emergency number: " + phoneNumber;
                    ttsManager.speak(cantoneseText, englishText, false);
                    return; // 成功撥打，直接返回
                    
                } catch (SecurityException e) {
                    Log.e(TAG, "直接撥打失敗（權限問題）: " + e.getMessage());
                } catch (Exception e) {
                    Log.e(TAG, "直接撥打失敗: " + e.getMessage());
                }
            }
            
            // 如果沒有權限或直接撥打失敗，嘗試使用 ACTION_DIAL（後備方案）
            // 注意：對於緊急號碼999，某些系統可能允許直接撥打
            try {
                // 即使沒有權限，也嘗試直接撥打（緊急號碼可能被允許）
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse(phoneUri));
                callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(callIntent);
                Log.d(TAG, "緊急電話已撥打（無權限但系統允許）: " + phoneNumber);
                
                String cantoneseText = "正在撥打緊急電話：" + phoneNumber;
                String englishText = "Calling emergency number: " + phoneNumber;
                ttsManager.speak(cantoneseText, englishText, false);
                
            } catch (SecurityException e) {
                Log.e(TAG, "無法直接撥打，使用撥號界面: " + e.getMessage());
                // 最後的後備方案：打開撥號界面
                try {
                    Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                    dialIntent.setData(Uri.parse(phoneUri));
                    dialIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(dialIntent);
                    
                    String cantoneseText = "已打開撥號界面，號碼已填入：" + phoneNumber + "，請按撥打按鈕";
                    String englishText = "Dialer opened, number " + phoneNumber + " is ready, please press call button";
                    ttsManager.speak(cantoneseText, englishText, false);
                    Log.d(TAG, "已打開撥號界面: " + phoneNumber);
                } catch (Exception ex) {
                    Log.e(TAG, "打開撥號界面失敗: " + ex.getMessage());
                    ttsManager.speakError("無法撥打緊急電話，請手動撥打" + phoneNumber);
                }
            } catch (Exception e) {
                Log.e(TAG, "撥打緊急電話失敗: " + e.getMessage());
                ttsManager.speakError("撥打緊急電話失敗，請手動撥打" + phoneNumber);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "撥打緊急電話失敗: " + e.getMessage());
            ttsManager.speakError("撥打緊急電話失敗，請手動撥打");
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
