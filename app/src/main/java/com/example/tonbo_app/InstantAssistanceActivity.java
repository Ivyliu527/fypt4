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

    // 志工聯繫信息
    private static final String VOLUNTEER_PHONE = "+852-1234-5678"; // 示例電話號碼
    private static final String VOLUNTEER_MESSAGE = "您好，我是視障人士，需要即時協助。";
    
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
            ttsManager.speak(cantoneseText, englishText, true);
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
    }

    /**
     * 檢查連接狀態
     */
    private void checkConnectionStatus() {
        connectionStatus.setText("檢查連接中...");
        
        // 模擬檢查連接狀態
        handler.postDelayed(() -> {
            connectionStatus.setText("連接正常");
            announceInfo("系統連接正常，可以開始使用即時協助功能");
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
        updateStatus("正在呼叫志工...");
        announceInfo("正在呼叫志工，請稍候");

        // 模擬呼叫過程
        handler.postDelayed(() -> {
            try {
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:" + VOLUNTEER_PHONE));
                startActivity(callIntent);
                announceSuccess("正在呼叫志工");
            } catch (Exception e) {
                Log.e(TAG, "呼叫失敗", e);
                announceError("呼叫失敗，請檢查網絡連接");
                updateStatus("呼叫失敗");
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
        updateStatus("正在發送訊息...");
        announceInfo("正在發送快速訊息給志工");

        // 模擬發送訊息
        handler.postDelayed(() -> {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(VOLUNTEER_PHONE, null, VOLUNTEER_MESSAGE, null, null);
                announceSuccess("訊息已發送給志工");
                updateStatus("訊息已發送");
            } catch (Exception e) {
                Log.e(TAG, "發送訊息失敗", e);
                announceError("發送訊息失敗，請檢查網絡連接");
                updateStatus("發送失敗");
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
        updateStatus("正在連線視訊...");
        announceInfo("正在連線志工進行視訊通話");

        // 模擬視訊連線
        handler.postDelayed(() -> {
            try {
                // 這裡可以整合視訊通話SDK，如Zoom、Teams等
                Intent videoIntent = new Intent(Intent.ACTION_VIEW);
                videoIntent.setData(Uri.parse("https://meet.google.com/volunteer-assistance"));
                startActivity(videoIntent);
                announceSuccess("正在連線視訊通話");
            } catch (Exception e) {
                Log.e(TAG, "視訊連線失敗", e);
                announceError("視訊連線失敗，請檢查網絡連接");
                updateStatus("連線失敗");
            }
            isConnecting = false;
        }, 2500);
    }

    /**
     * 顯示幫助對話框
     */
    private void showHelpDialog() {
        String helpText = "即時協助功能說明：\n" +
                "1. 一鍵呼叫：直接撥打電話給志工\n" +
                "2. 快速訊息：發送預設訊息給志工\n" +
                "3. 視訊連線：進行視訊通話獲得協助\n" +
                "所有功能都需要網絡連接和相應權限。";
        
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
                    announceInfo("通話權限已授予");
                    initiateQuickCall();
                } else {
                    announceError("需要通話權限才能呼叫志工");
                }
                break;
                
            case PERMISSION_REQUEST_SMS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    announceInfo("簡訊權限已授予");
                    sendQuickMessage();
                } else {
                    announceError("需要簡訊權限才能發送訊息");
                }
                break;
                
            case PERMISSION_REQUEST_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    announceInfo("相機權限已授予");
                    initiateVideoCall();
                } else {
                    announceError("需要相機權限才能進行視訊通話");
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
