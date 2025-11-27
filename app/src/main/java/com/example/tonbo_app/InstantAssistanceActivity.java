package com.example.tonbo_app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * 即時協助頁面
 * 提供一鍵呼叫、快速訊息、視訊連線功能
 */
public class InstantAssistanceActivity extends BaseAccessibleActivity {
    private static final String TAG = "InstantAssistance";
    private static final int PERMISSION_REQUEST_CALL = 100;
    private static final int PERMISSION_REQUEST_SMS = 101;
    private static final int PERMISSION_REQUEST_CAMERA = 102;

    // UI元素
    private android.widget.ImageButton backButton;
    private Button helpButton;
    private LinearLayout quickCallButton;
    private LinearLayout quickMessageButton;
    private LinearLayout videoCallButton;
    private TextView quickCallButtonText;
    private TextView quickMessageButtonText;
    private TextView videoCallButtonText;
    private TextView statusText;
    private TextView connectionStatus;
    private TextView pageTitle;

    // 志工聯繫信息
    private static final String VOLUNTEER_PHONE = "+852-1234-5678"; // 示例電話號碼
    
    // 狀態
    private boolean isConnecting = false;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instant_assistance);

        // 獲取語言設置
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("language")) {
            currentLanguage = intent.getStringExtra("language");
        }

        initViews();
        checkConnectionStatus();
        announcePageTitle();
    }

    @Override
    protected void announcePageTitle() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            String cantoneseText = "即時協助頁面。你可以一鍵呼叫志工、發送快速訊息或進行視訊連線。";
            String englishText = "Instant Assistance page. You can make quick calls to volunteers, send messages, or video chat.";
            String mandarinText = "即时协助页面。你可以一键呼叫志愿者、发送快速消息或进行视频连线。";
            
            switch (currentLanguage) {
                case "english":
                    ttsManager.speak(englishText, englishText, true);
                    break;
                case "mandarin":
                    ttsManager.speak(mandarinText, mandarinText, true);
                    break;
                case "cantonese":
                default:
                    ttsManager.speak(cantoneseText, englishText, true);
                    break;
            }
        }, 500);
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        helpButton = findViewById(R.id.helpButton);
        quickCallButton = findViewById(R.id.quickCallButton);
        quickMessageButton = findViewById(R.id.quickMessageButton);
        videoCallButton = findViewById(R.id.videoCallButton);
        quickCallButtonText = findViewById(R.id.quickCallButtonText);
        quickMessageButtonText = findViewById(R.id.quickMessageButtonText);
        videoCallButtonText = findViewById(R.id.videoCallButtonText);
        statusText = findViewById(R.id.statusText);
        connectionStatus = findViewById(R.id.connectionStatus);
        pageTitle = findViewById(R.id.pageTitle);

        // 返回按鈕
        backButton.setOnClickListener(v -> {
            handleBackPressed();
        });

        // 幫助按鈕
        helpButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            showHelpDialog();
        });

        // 一鍵呼叫按鈕
        quickCallButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            initiateQuickCall();
        });

        // 快速訊息按鈕
        quickMessageButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            sendQuickMessage();
        });

        // 視訊連線按鈕
        videoCallButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            initiateVideoCall();
        });
        
        // 根據當前語言更新界面文字
        updateLanguageUI();
    }
    
    /**
     * 更新語言UI
     */
    private void updateLanguageUI() {
        if (pageTitle != null) {
            pageTitle.setText(getLocalizedString("instant_assistance_title"));
        }
        
        if (quickCallButtonText != null) {
            String text = getLocalizedString("quick_call");
            quickCallButtonText.setText(text);
            if (quickCallButton != null) {
                quickCallButton.setContentDescription(text);
            }
        }
        
        if (quickMessageButtonText != null) {
            String text = getLocalizedString("quick_message");
            quickMessageButtonText.setText(text);
            if (quickMessageButton != null) {
                quickMessageButton.setContentDescription(text);
            }
        }
        
        if (videoCallButtonText != null) {
            String text = getLocalizedString("video_call");
            videoCallButtonText.setText(text);
            if (videoCallButton != null) {
                videoCallButton.setContentDescription(text);
            }
        }
        
        if (statusText != null) {
            statusText.setText(getLocalizedString("ready_to_assist"));
        }
        
        if (connectionStatus != null) {
            connectionStatus.setText(getLocalizedString("checking_connection"));
        }
    }
    
    /**
     * 根據當前語言獲取本地化字符串
     */
    private String getLocalizedString(String key) {
        switch (key) {
            case "instant_assistance_title":
                if ("english".equals(currentLanguage)) {
                    return "Instant Assistance";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "即时协助";
                } else {
                    return "即時協助";
                }
            case "quick_call":
                if ("english".equals(currentLanguage)) {
                    return "One-touch Call";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "一键呼叫";
                } else {
                    return "一鍵呼叫";
                }
            case "quick_message":
                if ("english".equals(currentLanguage)) {
                    return "Quick Message";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "快速消息";
                } else {
                    return "快速訊息";
                }
            case "video_call":
                if ("english".equals(currentLanguage)) {
                    return "Video Call";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "视频通话";
                } else {
                    return "視訊連線";
                }
            case "ready_to_assist":
                if ("english".equals(currentLanguage)) {
                    return "Ready to Assist";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "准备协助";
                } else {
                    return "準備協助";
                }
            case "checking_connection":
                if ("english".equals(currentLanguage)) {
                    return "Checking connection...";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "检查连接中...";
                } else {
                    return "檢查連接中...";
                }
            default:
                return "";
        }
    }
    
    /**
     * 檢查連接狀態
     */
    private void checkConnectionStatus() {
        connectionStatus.setText(getString(R.string.checking_connection));
        
        // 模擬檢查連接狀態
        handler.postDelayed(() -> {
            connectionStatus.setText(getString(R.string.connection_normal));
            announceInfo(getString(R.string.connection_normal));
        }, 2000);
    }

    /**
     * 一鍵呼叫志工
     */
    private void initiateQuickCall() {
        if (isConnecting) {
            announceError("正在處理中，請稍候");
            return;
        }

        // 檢查通話權限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.CALL_PHONE}, PERMISSION_REQUEST_CALL);
            return;
        }

        isConnecting = true;
        updateStatus(getString(R.string.calling_volunteer));
        announceInfo(getString(R.string.calling_volunteer));

        // 模擬呼叫過程
        handler.postDelayed(() -> {
            try {
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:" + VOLUNTEER_PHONE));
                startActivity(callIntent);
                announceSuccess(getString(R.string.calling_volunteer));
            } catch (Exception e) {
                Log.e(TAG, "呼叫失敗", e);
                announceError(getString(R.string.call_failed));
                updateStatus(getString(R.string.call_failed));
            }
            isConnecting = false;
        }, 1500);
    }

    /**
     * 發送快速訊息
     */
    private void sendQuickMessage() {
        if (isConnecting) {
            announceError("正在處理中，請稍候");
            return;
        }

        // 檢查簡訊權限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.SEND_SMS}, PERMISSION_REQUEST_SMS);
            return;
        }

        isConnecting = true;
        updateStatus(getString(R.string.sending_message));
        announceInfo(getString(R.string.sending_message));

        // 模擬發送訊息
        handler.postDelayed(() -> {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                String message = getString(R.string.volunteer_message);
                smsManager.sendTextMessage(VOLUNTEER_PHONE, null, message, null, null);
                announceSuccess(getString(R.string.message_sent));
                updateStatus(getString(R.string.message_sent));
            } catch (Exception e) {
                Log.e(TAG, "發送訊息失敗", e);
                announceError(getString(R.string.message_failed));
                updateStatus(getString(R.string.message_failed));
            }
            isConnecting = false;
        }, 2000);
    }

    /**
     * 發起視訊連線
     */
    private void initiateVideoCall() {
        if (isConnecting) {
            announceError("正在處理中，請稍候");
            return;
        }

        // 檢查相機和錄音權限
        List<String> permissionsNeeded = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
        }
        
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                permissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CAMERA);
            return;
        }

        isConnecting = true;
        updateStatus(getString(R.string.connecting_video));
        announceInfo(getString(R.string.connecting_video));

        // 啟動視頻通話 Activity
        try {
            Intent videoIntent = new Intent(this, VideoCallActivity.class);
            videoIntent.putExtra("language", currentLanguage);
            startActivity(videoIntent);
            announceSuccess("正在打開視頻通話");
            vibrationManager.vibrateSuccess();
        } catch (Exception e) {
            Log.e(TAG, "視訊連線失敗", e);
            announceError(getString(R.string.video_failed));
            updateStatus(getString(R.string.video_failed));
        }
        isConnecting = false;
    }

    /**
     * 顯示幫助對話框
     */
    private void showHelpDialog() {
        String helpText = getString(R.string.instant_assistance_help);
        announceInfo(helpText);
        Toast.makeText(this, helpText, Toast.LENGTH_LONG).show();
    }

    /**
     * 更新狀態顯示
     */
    private void updateStatus(String status) {
        statusText.setText(status);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        switch (requestCode) {
            case PERMISSION_REQUEST_CALL:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    announceInfo(getString(R.string.call_permission_granted));
                    initiateQuickCall();
                } else {
                    announceError(getString(R.string.call_permission_denied));
                }
                break;
                
            case PERMISSION_REQUEST_SMS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    announceInfo(getString(R.string.sms_permission_granted));
                    sendQuickMessage();
                } else {
                    announceError(getString(R.string.sms_permission_denied));
                }
                break;
                
            case PERMISSION_REQUEST_CAMERA:
                boolean allGranted = true;
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    announceInfo(getString(R.string.camera_permission_granted));
                    initiateVideoCall();
                } else {
                    announceError(getString(R.string.camera_permission_denied));
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
