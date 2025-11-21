package com.example.tonbo_app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.video.VideoEncoderConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * 視障人士視頻通話頁面
 * 使用聲網 (Agora) SDK 實現視頻通話功能
 */
public class VideoCallActivity extends BaseAccessibleActivity {
    private static final String TAG = "VideoCallActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;
    
    // Agora 配置
    private static final String APP_ID = "27576174aeaa4b2d88ea23cd63aae2a8";
    private static final String CHANNEL_NAME = "blind_help_channel";
    private String token = null; // 如果使用 Token 驗證，請在此設置
    
    // Agora RTC Engine
    private RtcEngine mRtcEngine;
    private boolean isJoined = false;
    private int myUid = 0;
    
    // UI 元素
    private FrameLayout localVideoContainer;
    private FrameLayout remoteVideoContainer;
    private Button joinButton;
    private Button leaveButton;
    private Button switchCameraButton;
    private Button muteAudioButton;
    private Button muteVideoButton;
    private ImageButton backButton;
    private TextView statusText;
    private TextView connectionStatus;
    private TextView pageTitle;
    
    // 狀態標記
    private boolean isAudioMuted = false;
    private boolean isVideoMuted = false;
    private boolean isFrontCamera = true;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);
        
        // 獲取語言設置
        if (getIntent() != null && getIntent().hasExtra("language")) {
            currentLanguage = getIntent().getStringExtra("language");
        }
        
        initViews();
        checkPermissions();
        announcePageTitle();
    }
    
    @Override
    protected void announcePageTitle() {
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            String cantoneseText = "視頻通話頁面。點擊加入按鈕開始視頻通話，可以切換攝像頭、靜音或關閉視頻。";
            String englishText = "Video call page. Tap join button to start video call, you can switch camera, mute audio or disable video.";
            String mandarinText = "视频通话页面。点击加入按钮开始视频通话，可以切换摄像头、静音或关闭视频。";
            
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
        localVideoContainer = findViewById(R.id.localVideoContainer);
        remoteVideoContainer = findViewById(R.id.remoteVideoContainer);
        joinButton = findViewById(R.id.joinButton);
        leaveButton = findViewById(R.id.leaveButton);
        switchCameraButton = findViewById(R.id.switchCameraButton);
        muteAudioButton = findViewById(R.id.muteAudioButton);
        muteVideoButton = findViewById(R.id.muteVideoButton);
        backButton = findViewById(R.id.backButton);
        statusText = findViewById(R.id.statusText);
        connectionStatus = findViewById(R.id.connectionStatus);
        pageTitle = findViewById(R.id.pageTitle);
        
        // 設置按鈕點擊事件
        backButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            leaveChannel();
            finish();
        });
        
        joinButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            joinChannel();
        });
        
        leaveButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            leaveChannel();
        });
        
        switchCameraButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            switchCamera();
        });
        
        muteAudioButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            toggleAudioMute();
        });
        
        muteVideoButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            toggleVideoMute();
        });
        
        // 初始狀態：只顯示加入按鈕
        leaveButton.setEnabled(false);
        switchCameraButton.setEnabled(false);
        muteAudioButton.setEnabled(false);
        muteVideoButton.setEnabled(false);
        
        // 更新語言UI
        updateLanguageUI();
    }
    
    private void updateLanguageUI() {
        if (pageTitle != null) {
            pageTitle.setText(getLocalizedString("video_call_title"));
        }
        
        if (joinButton != null) {
            joinButton.setText(getLocalizedString("join_channel"));
        }
        
        if (leaveButton != null) {
            leaveButton.setText(getLocalizedString("leave_channel"));
        }
        
        if (switchCameraButton != null) {
            switchCameraButton.setText(getLocalizedString("switch_camera"));
        }
        
        if (muteAudioButton != null) {
            muteAudioButton.setText(getLocalizedString("mute_audio"));
        }
        
        if (muteVideoButton != null) {
            muteVideoButton.setText(getLocalizedString("mute_video"));
        }
        
        if (statusText != null) {
            statusText.setText(getLocalizedString("ready_to_join"));
        }
        
        if (connectionStatus != null) {
            connectionStatus.setText(getLocalizedString("not_connected"));
        }
    }
    
    private String getLocalizedString(String key) {
        switch (key) {
            case "video_call_title":
                if ("english".equals(currentLanguage)) {
                    return "Video Call";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "视频通话";
                } else {
                    return "視訊通話";
                }
            case "join_channel":
                if ("english".equals(currentLanguage)) {
                    return "📞 Join Channel";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "📞 加入频道";
                } else {
                    return "📞 加入頻道";
                }
            case "leave_channel":
                if ("english".equals(currentLanguage)) {
                    return "📴 Leave Channel";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "📴 离开频道";
                } else {
                    return "📴 離開頻道";
                }
            case "switch_camera":
                if ("english".equals(currentLanguage)) {
                    return "🔄 Switch Camera";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "🔄 切换摄像头";
                } else {
                    return "🔄 切換攝像頭";
                }
            case "mute_audio":
                if ("english".equals(currentLanguage)) {
                    return "🔇 Mute Audio";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "🔇 静音";
                } else {
                    return "🔇 靜音";
                }
            case "unmute_audio":
                if ("english".equals(currentLanguage)) {
                    return "🔊 Unmute Audio";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "🔊 取消静音";
                } else {
                    return "🔊 取消靜音";
                }
            case "mute_video":
                if ("english".equals(currentLanguage)) {
                    return "📹 Disable Video";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "📹 关闭视频";
                } else {
                    return "📹 關閉視頻";
                }
            case "unmute_video":
                if ("english".equals(currentLanguage)) {
                    return "📹 Enable Video";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "📹 开启视频";
                } else {
                    return "📹 開啟視頻";
                }
            case "ready_to_join":
                if ("english".equals(currentLanguage)) {
                    return "Ready to Join";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "准备加入";
                } else {
                    return "準備加入";
                }
            case "not_connected":
                if ("english".equals(currentLanguage)) {
                    return "Not Connected";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "未连接";
                } else {
                    return "未連接";
                }
            case "connecting":
                if ("english".equals(currentLanguage)) {
                    return "Connecting...";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "连接中...";
                } else {
                    return "連接中...";
                }
            case "connected":
                if ("english".equals(currentLanguage)) {
                    return "Connected";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "已连接";
                } else {
                    return "已連接";
                }
            case "disconnected":
                if ("english".equals(currentLanguage)) {
                    return "Disconnected";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "已断开";
                } else {
                    return "已斷開";
                }
            case "permission_granted":
                if ("english".equals(currentLanguage)) {
                    return "Permissions granted";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "权限已授予";
                } else {
                    return "權限已授予";
                }
            case "permission_denied":
                if ("english".equals(currentLanguage)) {
                    return "Permissions denied";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "权限被拒绝";
                } else {
                    return "權限被拒絕";
                }
            case "engine_initialized":
                if ("english".equals(currentLanguage)) {
                    return "Engine initialized";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "引擎已初始化";
                } else {
                    return "引擎已初始化";
                }
            case "engine_init_failed":
                if ("english".equals(currentLanguage)) {
                    return "Engine initialization failed";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "引擎初始化失败";
                } else {
                    return "引擎初始化失敗";
                }
            case "engine_not_ready":
                if ("english".equals(currentLanguage)) {
                    return "Engine not ready";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "引擎未就绪";
                } else {
                    return "引擎未就緒";
                }
            case "already_joined":
                if ("english".equals(currentLanguage)) {
                    return "Already joined";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "已加入频道";
                } else {
                    return "已加入頻道";
                }
            case "joining_channel":
                if ("english".equals(currentLanguage)) {
                    return "Joining channel";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "正在加入频道";
                } else {
                    return "正在加入頻道";
                }
            case "join_failed":
                if ("english".equals(currentLanguage)) {
                    return "Join failed";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "加入失败";
                } else {
                    return "加入失敗";
                }
            case "left_channel":
                if ("english".equals(currentLanguage)) {
                    return "Left channel";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "已离开频道";
                } else {
                    return "已離開頻道";
                }
            case "camera_switched":
                if ("english".equals(currentLanguage)) {
                    return "Camera switched";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "摄像头已切换";
                } else {
                    return "攝像頭已切換";
                }
            case "audio_muted":
                if ("english".equals(currentLanguage)) {
                    return "Audio muted";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "已静音";
                } else {
                    return "已靜音";
                }
            case "audio_unmuted":
                if ("english".equals(currentLanguage)) {
                    return "Audio unmuted";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "已取消静音";
                } else {
                    return "已取消靜音";
                }
            case "video_disabled":
                if ("english".equals(currentLanguage)) {
                    return "Video disabled";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "视频已关闭";
                } else {
                    return "視頻已關閉";
                }
            case "video_enabled":
                if ("english".equals(currentLanguage)) {
                    return "Video enabled";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "视频已开启";
                } else {
                    return "視頻已開啟";
                }
            case "channel_joined":
                if ("english".equals(currentLanguage)) {
                    return "Channel joined successfully";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "成功加入频道";
                } else {
                    return "成功加入頻道";
                }
            case "user_joined":
                if ("english".equals(currentLanguage)) {
                    return "User joined";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "用户已加入";
                } else {
                    return "用戶已加入";
                }
            case "user_left":
                if ("english".equals(currentLanguage)) {
                    return "User left";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "用户已离开";
                } else {
                    return "用戶已離開";
                }
            case "error_occurred":
                if ("english".equals(currentLanguage)) {
                    return "Error occurred";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "发生错误";
                } else {
                    return "發生錯誤";
                }
            default:
                return "";
        }
    }
    
    private void checkPermissions() {
        List<String> permissions = new ArrayList<>();
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA);
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO);
        }
        
        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                    permissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            initializeAgoraEngine();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                announceInfo(getLocalizedString("permission_granted"));
                initializeAgoraEngine();
            } else {
                announceError(getLocalizedString("permission_denied"));
            }
        }
    }
    
    private void initializeAgoraEngine() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getApplicationContext();
            config.mAppId = APP_ID;
            config.mEventHandler = mRtcEventHandler;
            
            mRtcEngine = RtcEngine.create(config);
            
            // 設置視頻編碼配置
            VideoEncoderConfiguration videoConfig = new VideoEncoderConfiguration(
                    VideoEncoderConfiguration.VD_640x360,
                    VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                    VideoEncoderConfiguration.STANDARD_BITRATE,
                    VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
            );
            mRtcEngine.setVideoEncoderConfiguration(videoConfig);
            
            // 啟用視頻
            mRtcEngine.enableVideo();
            
            // 啟用音頻
            mRtcEngine.enableAudio();
            
            Log.d(TAG, "Agora Engine 初始化成功");
            announceSuccess(getLocalizedString("engine_initialized"));
            
        } catch (Exception e) {
            Log.e(TAG, "Agora Engine 初始化失敗", e);
            announceError(getLocalizedString("engine_init_failed"));
        }
    }
    
    private void joinChannel() {
        if (mRtcEngine == null) {
            announceError(getLocalizedString("engine_not_ready"));
            return;
        }
        
        if (isJoined) {
            announceInfo(getLocalizedString("already_joined"));
            return;
        }
        
        // 設置本地視頻視圖
        setupLocalVideo();
        
        // 加入頻道
        int result = mRtcEngine.joinChannel(token, CHANNEL_NAME, 0, null);
        
        if (result == 0) {
            updateStatus(getLocalizedString("connecting"));
            announceInfo(getLocalizedString("joining_channel"));
            vibrationManager.vibrateNotification();
        } else {
            Log.e(TAG, "加入頻道失敗，錯誤碼: " + result);
            announceError(getLocalizedString("join_failed"));
        }
    }
    
    private void leaveChannel() {
        if (mRtcEngine == null || !isJoined) {
            return;
        }
        
        mRtcEngine.leaveChannel();
        isJoined = false;
        
        // 清理本地視頻視圖
        localVideoContainer.removeAllViews();
        remoteVideoContainer.removeAllViews();
        
        // 更新UI
        joinButton.setEnabled(true);
        leaveButton.setEnabled(false);
        switchCameraButton.setEnabled(false);
        muteAudioButton.setEnabled(false);
        muteVideoButton.setEnabled(false);
        
        updateStatus(getLocalizedString("disconnected"));
        connectionStatus.setText(getLocalizedString("not_connected"));
        announceInfo(getLocalizedString("left_channel"));
        vibrationManager.vibrateNotification();
    }
    
    private void setupLocalVideo() {
        SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
        surfaceView.setZOrderMediaOverlay(true);
        localVideoContainer.addView(surfaceView);
        
        mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, Constants.RENDER_MODE_HIDDEN, 0));
        mRtcEngine.startPreview();
    }
    
    private void setupRemoteVideo(int uid) {
        SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
        remoteVideoContainer.addView(surfaceView);
        
        mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceView, Constants.RENDER_MODE_HIDDEN, uid));
    }
    
    private void switchCamera() {
        if (mRtcEngine != null && isJoined) {
            mRtcEngine.switchCamera();
            isFrontCamera = !isFrontCamera;
            announceInfo(getLocalizedString("camera_switched"));
        }
    }
    
    private void toggleAudioMute() {
        if (mRtcEngine != null && isJoined) {
            isAudioMuted = !isAudioMuted;
            mRtcEngine.muteLocalAudioStream(isAudioMuted);
            
            if (isAudioMuted) {
                muteAudioButton.setText(getLocalizedString("unmute_audio"));
                announceInfo(getLocalizedString("audio_muted"));
            } else {
                muteAudioButton.setText(getLocalizedString("mute_audio"));
                announceInfo(getLocalizedString("audio_unmuted"));
            }
        }
    }
    
    private void toggleVideoMute() {
        if (mRtcEngine != null && isJoined) {
            isVideoMuted = !isVideoMuted;
            mRtcEngine.muteLocalVideoStream(isVideoMuted);
            
            if (isVideoMuted) {
                muteVideoButton.setText(getLocalizedString("unmute_video"));
                localVideoContainer.setVisibility(View.GONE);
                announceInfo(getLocalizedString("video_disabled"));
            } else {
                muteVideoButton.setText(getLocalizedString("mute_video"));
                localVideoContainer.setVisibility(View.VISIBLE);
                announceInfo(getLocalizedString("video_enabled"));
            }
        }
    }
    
    private void updateStatus(String status) {
        if (statusText != null) {
            statusText.setText(status);
        }
    }
    
    // Agora RTC 事件處理器
    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            runOnUiThread(() -> {
                Log.d(TAG, "加入頻道成功: " + channel + ", UID: " + uid);
                myUid = uid;
                isJoined = true;
                
                // 更新UI
                joinButton.setEnabled(false);
                leaveButton.setEnabled(true);
                switchCameraButton.setEnabled(true);
                muteAudioButton.setEnabled(true);
                muteVideoButton.setEnabled(true);
                
                updateStatus(getLocalizedString("connected"));
                connectionStatus.setText(getLocalizedString("connected"));
                announceSuccess(getLocalizedString("channel_joined"));
                vibrationManager.vibrateSuccess();
            });
        }
        
        @Override
        public void onUserJoined(int uid, int elapsed) {
            runOnUiThread(() -> {
                Log.d(TAG, "遠程用戶加入: " + uid);
                setupRemoteVideo(uid);
                announceInfo(getLocalizedString("user_joined"));
                vibrationManager.vibrateNotification();
            });
        }
        
        @Override
        public void onUserOffline(int uid, int reason) {
            runOnUiThread(() -> {
                Log.d(TAG, "遠程用戶離開: " + uid);
                remoteVideoContainer.removeAllViews();
                announceInfo(getLocalizedString("user_left"));
                vibrationManager.vibrateNotification();
            });
        }
        
        @Override
        public void onLeaveChannel(RtcStats stats) {
            runOnUiThread(() -> {
                Log.d(TAG, "離開頻道");
                isJoined = false;
                localVideoContainer.removeAllViews();
                remoteVideoContainer.removeAllViews();
            });
        }
        
        @Override
        public void onError(int err) {
            runOnUiThread(() -> {
                Log.e(TAG, "Agora 錯誤: " + err);
                announceError(getLocalizedString("error_occurred") + ": " + err);
            });
        }
    };
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        leaveChannel();
        
        if (mRtcEngine != null) {
            RtcEngine.destroy();
            mRtcEngine = null;
        }
    }
}

