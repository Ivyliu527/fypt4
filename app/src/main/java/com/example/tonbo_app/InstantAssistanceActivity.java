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
    private Button backButton;
    private Button helpButton;
    private Button quickCallButton;
    private Button quickMessageButton;
    private Button videoCallButton;
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
        statusText = findViewById(R.id.statusText);
        connectionStatus = findViewById(R.id.connectionStatus);
        pageTitle = findViewById(R.id.pageTitle);

        // 返回按鈕
        backButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            announceNavigation(getString(R.string.go_back_home));
            finish();
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
        
        if (quickCallButton != null) {
            quickCallButton.setText(getLocalizedString("quick_call"));
        }
        
        if (quickMessageButton != null) {
            quickMessageButton.setText(getLocalizedString("quick_message"));
        }
        
        if (videoCallButton != null) {
            videoCallButton.setText(getLocalizedString("video_call"));
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
                    return "📞 One-touch Call Volunteer";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "📞 一键呼叫志愿者";
                } else {
                    return "📞 一鍵呼叫志工";
                }
            case "quick_message":
                if ("english".equals(currentLanguage)) {
                    return "💬 Quick Message";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "💬 快速消息";
                } else {
                    return "💬 快速訊息";
                }
            case "video_call":
                if ("english".equals(currentLanguage)) {
                    return "📹 Video Call";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "📹 视频通话";
                } else {
                    return "📹 視訊連線";
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

        // 檢查相機權限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
            return;
        }

        isConnecting = true;
        updateStatus(getString(R.string.connecting_video));
        announceInfo(getString(R.string.connecting_video));

        // 模擬視訊連線
        handler.postDelayed(() -> {
            try {
                // 這裡可以整合視訊通話SDK，如Zoom、Teams等
                Intent videoIntent = new Intent(Intent.ACTION_VIEW);
                videoIntent.setData(Uri.parse("https://meet.google.com/volunteer-assistance"));
                startActivity(videoIntent);
                announceSuccess(getString(R.string.connecting_video));
            } catch (Exception e) {
                Log.e(TAG, "視訊連線失敗", e);
                announceError(getString(R.string.video_failed));
                updateStatus(getString(R.string.video_failed));
            }
            isConnecting = false;
        }, 2500);
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
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
