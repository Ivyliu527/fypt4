package com.example.tonbo_app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;

/**
 * 出行協助Activity
 * 提供出行導航、路線規劃、交通信息等功能
 */
public class TravelAssistantActivity extends BaseAccessibleActivity {
    private static final String TAG = "TravelAssistant";
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 100;
    
    private TextView pageTitle;
    private TextView voiceStatusTitle;
    private TextView voiceStatusText;
    private Button startTravelVoiceButton;
    private Button environmentRecognitionButton;
    private Button emergencyButton;
    
    // 語音識別相關
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private boolean isListening = false;
    
    // 導航控制器
    private NavigationController navigationController;
    
    /**
     * 語音識別結果回調接口
     */
    public interface TravelVoiceListener {
        void onDestinationRecognized(String destination);
        void onError(String error);
    }
    
    private TravelVoiceListener voiceListener;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_travel_assistant);
        
        initViews();
        setupButtons();
        initializeSpeechRecognizer();
        initializeNavigationController();
        announcePageTitle();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.cancel();
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        if (navigationController != null) {
            navigationController.stopNavigation();
            navigationController = null;
        }
    }
    
    private void initViews() {
        pageTitle = findViewById(R.id.page_title);
        voiceStatusTitle = findViewById(R.id.voice_status_title);
        voiceStatusText = findViewById(R.id.voice_status_text);
        startTravelVoiceButton = findViewById(R.id.start_travel_voice_button);
        environmentRecognitionButton = findViewById(R.id.environment_recognition_button);
        emergencyButton = findViewById(R.id.emergency_button);
        
        // 設置返回按鈕
        android.widget.ImageButton backButton = findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                handleBackPressed();
            });
        }
        
        // 根據當前語言更新界面文字
        updateLanguageUI();
    }
    
    private void setupButtons() {
        // 開始出行（語音）
        startTravelVoiceButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            startVoiceRecognition();
        });
        
        // 前方環境識別
        environmentRecognitionButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            updateVoiceStatus(getLocalizedString("voice_status_processing"));
            Intent intent = new Intent(this, RealAIDetectionActivity.class);
            intent.putExtra("language", currentLanguage);
            startActivity(intent);
        });
        
        // 緊急求助
        emergencyButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            updateVoiceStatus(getLocalizedString("voice_status_emergency"));
            EmergencyManager.getInstance(this).triggerEmergencyAlert();
        });
    }
    
    /**
     * 初始化語音識別器
     */
    private void initializeSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "設備不支持語音識別");
            updateVoiceStatus(getLocalizedString("voice_status_error_not_available"));
            return;
        }
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        
        // 設置語言
        String currentLang = LocaleManager.getInstance(this).getCurrentLanguage();
        if ("english".equals(currentLang)) {
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH.toString());
        } else if ("mandarin".equals(currentLang)) {
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.SIMPLIFIED_CHINESE.toString());
        } else {
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-HK");
        }
        
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d(TAG, "準備接收語音");
                isListening = true;
                updateVoiceStatus(getLocalizedString("voice_status_listening"));
                vibrationManager.vibrateClick();
            }
            
            @Override
            public void onBeginningOfSpeech() {
                Log.d(TAG, "開始說話");
                updateVoiceStatus(getLocalizedString("voice_status_speaking"));
            }
            
            @Override
            public void onRmsChanged(float rmsdB) {
                // 音量變化，可用於視覺反饋
            }
            
            @Override
            public void onBufferReceived(byte[] buffer) {
                // 接收緩衝區數據
            }
            
            @Override
            public void onEndOfSpeech() {
                Log.d(TAG, "說話結束");
                updateVoiceStatus(getLocalizedString("voice_status_processing"));
            }
            
            @Override
            public void onError(int error) {
                isListening = false;
                String errorText = getErrorText(error);
                Log.e(TAG, "語音識別錯誤: " + errorText);
                updateVoiceStatus(getLocalizedString("voice_status_ready"));
                
                // 為視障用戶播報錯誤信息
                String errorMessage = getLocalizedString("voice_recognition_error") + ": " + errorText;
                announceInfo(errorMessage);
                
                if (voiceListener != null) {
                    voiceListener.onError(errorText);
                }
            }
            
            @Override
            public void onResults(Bundle results) {
                isListening = false;
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                
                if (matches != null && !matches.isEmpty()) {
                    String recognizedText = matches.get(0);
                    Log.d(TAG, "識別結果: " + recognizedText);
                    
                    updateVoiceStatus(getLocalizedString("voice_status_recognized") + ": " + recognizedText);
                    
                    // 播報識別結果給視障用戶
                    String announceText = getLocalizedString("voice_recognition_success") + ": " + recognizedText;
                    announceInfo(announceText);
                    
                    // 通過回調傳出識別結果
                    if (voiceListener != null) {
                        voiceListener.onDestinationRecognized(recognizedText);
                    }
                    
                    // 啟動導航
                    if (navigationController != null) {
                        navigationController.startNavigation(recognizedText);
                    }
                } else {
                    Log.w(TAG, "沒有識別結果");
                    updateVoiceStatus(getLocalizedString("voice_status_ready"));
                    String noResultMsg = getLocalizedString("voice_recognition_no_result");
                    announceInfo(noResultMsg);
                    
                    if (voiceListener != null) {
                        voiceListener.onError(noResultMsg);
                    }
                }
                
                // 延遲後恢復就緒狀態
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    updateVoiceStatus(getLocalizedString("voice_status_ready"));
                }, 3000);
            }
            
            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String partialText = matches.get(0);
                    Log.d(TAG, "部分識別結果: " + partialText);
                    // 可以選擇是否顯示部分結果
                }
            }
            
            @Override
            public void onEvent(int eventType, Bundle params) {
                // 其他事件
            }
        });
        
        Log.d(TAG, "語音識別器初始化成功");
    }
    
    /**
     * 初始化導航控制器
     */
    private void initializeNavigationController() {
        navigationController = new NavigationController(this);
        navigationController.setNavigationListener(new NavigationController.NavigationListener() {
            @Override
            public void onLocationObtained(android.location.Location location) {
                Log.d(TAG, "位置獲取成功: " + location.getLatitude() + ", " + location.getLongitude());
                // 可以更新UI顯示位置信息
            }
            
            @Override
            public void onRoutePlanned(String routeInfo) {
                Log.d(TAG, "路線規劃完成: " + routeInfo);
                updateVoiceStatus(getLocalizedString("voice_status_route_planned"));
                announceInfo(routeInfo);
            }
            
            @Override
            public void onNavigationStarted() {
                Log.d(TAG, "導航已開始");
                updateVoiceStatus(getLocalizedString("voice_status_navigating"));
                // 導航開始的播報由 NavigationController 內部處理
            }
            
            @Override
            public void onNavigationStateChanged(NavigationController.NavigationState state) {
                Log.d(TAG, "導航狀態變化: " + state);
                // 根據狀態更新UI
                switch (state) {
                    case GETTING_LOCATION:
                        updateVoiceStatus(getLocalizedString("voice_status_getting_location"));
                        break;
                    case PLANNING_ROUTE:
                        updateVoiceStatus(getLocalizedString("voice_status_planning_route"));
                        break;
                    case NAVIGATING:
                        updateVoiceStatus(getLocalizedString("voice_status_navigating"));
                        break;
                    case ARRIVED:
                        updateVoiceStatus(getLocalizedString("voice_status_arrived"));
                        break;
                    case ERROR:
                        updateVoiceStatus(getLocalizedString("voice_status_ready"));
                        break;
                    case IDLE:
                        updateVoiceStatus(getLocalizedString("voice_status_ready"));
                        break;
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "導航錯誤: " + error);
                updateVoiceStatus(getLocalizedString("voice_status_ready"));
                announceInfo(error);
            }
        });
        
        Log.d(TAG, "導航控制器初始化成功");
    }
    
    /**
     * 開始語音識別
     */
    private void startVoiceRecognition() {
        if (isListening) {
            Log.w(TAG, "語音識別正在進行中");
            announceInfo(getLocalizedString("voice_recognition_already_listening"));
            return;
        }
        
        // 檢查權限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "沒有錄音權限，請求權限");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_RECORD_AUDIO);
            return;
        }
        
        // 檢查語音識別器是否可用
        if (speechRecognizer == null) {
            Log.e(TAG, "語音識別器未初始化");
            updateVoiceStatus(getLocalizedString("voice_status_error_not_available"));
            announceInfo(getLocalizedString("voice_recognition_error_not_available"));
            return;
        }
        
        try {
            speechRecognizer.startListening(recognizerIntent);
            Log.d(TAG, "開始語音識別");
        } catch (Exception e) {
            Log.e(TAG, "啟動語音識別失敗", e);
            updateVoiceStatus(getLocalizedString("voice_status_ready"));
            announceInfo(getLocalizedString("voice_recognition_error_start_failed"));
            
            if (voiceListener != null) {
                voiceListener.onError("啟動語音識別失敗: " + e.getMessage());
            }
        }
    }
    
    /**
     * 停止語音識別
     */
    private void stopVoiceRecognition() {
        if (speechRecognizer != null && isListening) {
            speechRecognizer.stopListening();
            isListening = false;
            updateVoiceStatus(getLocalizedString("voice_status_ready"));
            Log.d(TAG, "停止語音識別");
        }
    }
    
    /**
     * 獲取錯誤文本
     */
    private String getErrorText(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                return getLocalizedString("error_audio");
            case SpeechRecognizer.ERROR_CLIENT:
                return getLocalizedString("error_client");
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return getLocalizedString("error_permissions");
            case SpeechRecognizer.ERROR_NETWORK:
                return getLocalizedString("error_network");
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return getLocalizedString("error_network_timeout");
            case SpeechRecognizer.ERROR_NO_MATCH:
                return getLocalizedString("error_no_match");
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return getLocalizedString("error_recognizer_busy");
            case SpeechRecognizer.ERROR_SERVER:
                return getLocalizedString("error_server");
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return getLocalizedString("error_speech_timeout");
            default:
                return getLocalizedString("error_unknown");
        }
    }
    
    /**
     * 設置語音識別結果回調
     */
    public void setTravelVoiceListener(TravelVoiceListener listener) {
        this.voiceListener = listener;
    }
    
    /**
     * 處理權限請求結果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "錄音權限已授予");
                startVoiceRecognition();
            } else {
                Log.w(TAG, "錄音權限被拒絕");
                updateVoiceStatus(getLocalizedString("voice_status_error_permission"));
                announceInfo(getLocalizedString("voice_recognition_permission_denied"));
                
                if (voiceListener != null) {
                    voiceListener.onError(getLocalizedString("error_permissions"));
                }
            }
        }
    }
    
    /**
     * 更新語音狀態提示
     */
    private void updateVoiceStatus(String status) {
        if (voiceStatusText != null) {
            voiceStatusText.setText(status);
            // 為視障用戶播報狀態（避免重複播報，只在關鍵狀態變化時播報）
        }
    }
    
    /**
     * 更新語言UI
     */
    private void updateLanguageUI() {
        if (pageTitle != null) {
            pageTitle.setText(getLocalizedString("travel_assistant_title"));
        }
        
        if (voiceStatusTitle != null) {
            voiceStatusTitle.setText(getLocalizedString("voice_status_title"));
        }
        
        if (voiceStatusText != null) {
            voiceStatusText.setText(getLocalizedString("voice_status_ready"));
        }
        
        if (startTravelVoiceButton != null) {
            startTravelVoiceButton.setText(getLocalizedString("start_travel_voice"));
        }
        
        if (environmentRecognitionButton != null) {
            environmentRecognitionButton.setText(getLocalizedString("environment_recognition"));
        }
        
        if (emergencyButton != null) {
            emergencyButton.setText(getLocalizedString("emergency_assistance"));
        }
    }
    
    /**
     * 根據當前語言獲取本地化字符串
     */
    private String getLocalizedString(String key) {
        String currentLang = LocaleManager.getInstance(this).getCurrentLanguage();
        
        switch (key) {
            case "travel_assistant_title":
                if ("english".equals(currentLang)) {
                    return "Travel Assistant";
                } else if ("mandarin".equals(currentLang)) {
                    return "出行协助";
                } else {
                    return "出行協助";
                }
            case "travel_assistant_status":
                if ("english".equals(currentLang)) {
                    return "Travel assistance functions are ready";
                } else if ("mandarin".equals(currentLang)) {
                    return "出行协助功能准备就绪";
                } else {
                    return "出行協助功能準備就緒";
                }
            case "navigation":
                if ("english".equals(currentLang)) {
                    return "Navigation";
                } else if ("mandarin".equals(currentLang)) {
                    return "导航";
                } else {
                    return "導航";
                }
            case "route_planning":
                if ("english".equals(currentLang)) {
                    return "Route Planning";
                } else if ("mandarin".equals(currentLang)) {
                    return "路线规划";
                } else {
                    return "路線規劃";
                }
            case "traffic_info":
                if ("english".equals(currentLang)) {
                    return "Traffic Info";
                } else if ("mandarin".equals(currentLang)) {
                    return "交通信息";
                } else {
                    return "交通信息";
                }
            case "weather_info":
                if ("english".equals(currentLang)) {
                    return "Weather Info";
                } else if ("mandarin".equals(currentLang)) {
                    return "天气信息";
                } else {
                    return "天氣信息";
                }
            case "emergency_location":
                if ("english".equals(currentLang)) {
                    return "Emergency Location";
                } else if ("mandarin".equals(currentLang)) {
                    return "紧急位置分享";
                } else {
                    return "緊急位置分享";
                }
            case "going_back_to_home":
                if ("english".equals(currentLang)) {
                    return "Going back to home";
                } else if ("mandarin".equals(currentLang)) {
                    return "返回主页";
                } else {
                    return "返回主頁";
                }
            case "voice_status_title":
                if ("english".equals(currentLang)) {
                    return "Voice Status";
                } else if ("mandarin".equals(currentLang)) {
                    return "语音状态";
                } else {
                    return "語音狀態";
                }
            case "voice_status_ready":
                if ("english".equals(currentLang)) {
                    return "Ready";
                } else if ("mandarin".equals(currentLang)) {
                    return "就绪";
                } else {
                    return "就緒";
                }
            case "voice_status_listening":
                if ("english".equals(currentLang)) {
                    return "Listening...";
                } else if ("mandarin".equals(currentLang)) {
                    return "正在聆听...";
                } else {
                    return "正在聆聽...";
                }
            case "voice_status_processing":
                if ("english".equals(currentLang)) {
                    return "Processing...";
                } else if ("mandarin".equals(currentLang)) {
                    return "处理中...";
                } else {
                    return "處理中...";
                }
            case "voice_status_emergency":
                if ("english".equals(currentLang)) {
                    return "Emergency Mode";
                } else if ("mandarin".equals(currentLang)) {
                    return "紧急模式";
                } else {
                    return "緊急模式";
                }
            case "start_travel_voice":
                if ("english".equals(currentLang)) {
                    return "Start Travel (Voice)";
                } else if ("mandarin".equals(currentLang)) {
                    return "开始出行（语音）";
                } else {
                    return "開始出行（語音）";
                }
            case "environment_recognition":
                if ("english".equals(currentLang)) {
                    return "Environment Recognition";
                } else if ("mandarin".equals(currentLang)) {
                    return "前方环境识别";
                } else {
                    return "前方環境識別";
                }
            case "emergency_assistance":
                if ("english".equals(currentLang)) {
                    return "Emergency Assistance";
                } else if ("mandarin".equals(currentLang)) {
                    return "紧急求助";
                } else {
                    return "緊急求助";
                }
            case "voice_status_speaking":
                if ("english".equals(currentLang)) {
                    return "Speaking...";
                } else if ("mandarin".equals(currentLang)) {
                    return "正在说话...";
                } else {
                    return "正在說話...";
                }
            case "voice_status_recognized":
                if ("english".equals(currentLang)) {
                    return "Recognized";
                } else if ("mandarin".equals(currentLang)) {
                    return "已识别";
                } else {
                    return "已識別";
                }
            case "voice_status_getting_location":
                if ("english".equals(currentLang)) {
                    return "Getting location...";
                } else if ("mandarin".equals(currentLang)) {
                    return "正在获取位置...";
                } else {
                    return "正在獲取位置...";
                }
            case "voice_status_planning_route":
                if ("english".equals(currentLang)) {
                    return "Planning route...";
                } else if ("mandarin".equals(currentLang)) {
                    return "正在规划路线...";
                } else {
                    return "正在規劃路線...";
                }
            case "voice_status_route_planned":
                if ("english".equals(currentLang)) {
                    return "Route planned";
                } else if ("mandarin".equals(currentLang)) {
                    return "路线已规划";
                } else {
                    return "路線已規劃";
                }
            case "voice_status_navigating":
                if ("english".equals(currentLang)) {
                    return "Navigating...";
                } else if ("mandarin".equals(currentLang)) {
                    return "导航中...";
                } else {
                    return "導航中...";
                }
            case "voice_status_arrived":
                if ("english".equals(currentLang)) {
                    return "Arrived";
                } else if ("mandarin".equals(currentLang)) {
                    return "已到达";
                } else {
                    return "已到達";
                }
            case "voice_status_error_not_available":
                if ("english".equals(currentLang)) {
                    return "Not Available";
                } else if ("mandarin".equals(currentLang)) {
                    return "不可用";
                } else {
                    return "不可用";
                }
            case "voice_status_error_permission":
                if ("english".equals(currentLang)) {
                    return "Permission Denied";
                } else if ("mandarin".equals(currentLang)) {
                    return "权限被拒绝";
                } else {
                    return "權限被拒絕";
                }
            case "voice_recognition_success":
                if ("english".equals(currentLang)) {
                    return "Recognized";
                } else if ("mandarin".equals(currentLang)) {
                    return "识别成功";
                } else {
                    return "識別成功";
                }
            case "voice_recognition_error":
                if ("english".equals(currentLang)) {
                    return "Recognition Error";
                } else if ("mandarin".equals(currentLang)) {
                    return "识别错误";
                } else {
                    return "識別錯誤";
                }
            case "voice_recognition_no_result":
                if ("english".equals(currentLang)) {
                    return "No result, please try again";
                } else if ("mandarin".equals(currentLang)) {
                    return "未识别到结果，请重试";
                } else {
                    return "未識別到結果，請重試";
                }
            case "voice_recognition_already_listening":
                if ("english".equals(currentLang)) {
                    return "Already listening";
                } else if ("mandarin".equals(currentLang)) {
                    return "正在聆听中";
                } else {
                    return "正在聆聽中";
                }
            case "voice_recognition_error_not_available":
                if ("english".equals(currentLang)) {
                    return "Speech recognition not available";
                } else if ("mandarin".equals(currentLang)) {
                    return "语音识别不可用";
                } else {
                    return "語音識別不可用";
                }
            case "voice_recognition_error_start_failed":
                if ("english".equals(currentLang)) {
                    return "Failed to start recognition";
                } else if ("mandarin".equals(currentLang)) {
                    return "启动识别失败";
                } else {
                    return "啟動識別失敗";
                }
            case "voice_recognition_permission_denied":
                if ("english".equals(currentLang)) {
                    return "Microphone permission denied";
                } else if ("mandarin".equals(currentLang)) {
                    return "麦克风权限被拒绝";
                } else {
                    return "麥克風權限被拒絕";
                }
            case "error_audio":
                if ("english".equals(currentLang)) {
                    return "Audio error";
                } else if ("mandarin".equals(currentLang)) {
                    return "音频错误";
                } else {
                    return "音頻錯誤";
                }
            case "error_client":
                if ("english".equals(currentLang)) {
                    return "Client error";
                } else if ("mandarin".equals(currentLang)) {
                    return "客户端错误";
                } else {
                    return "客戶端錯誤";
                }
            case "error_permissions":
                if ("english".equals(currentLang)) {
                    return "Permission denied";
                } else if ("mandarin".equals(currentLang)) {
                    return "权限不足";
                } else {
                    return "權限不足";
                }
            case "error_network":
                if ("english".equals(currentLang)) {
                    return "Network error";
                } else if ("mandarin".equals(currentLang)) {
                    return "网络错误";
                } else {
                    return "網絡錯誤";
                }
            case "error_network_timeout":
                if ("english".equals(currentLang)) {
                    return "Network timeout";
                } else if ("mandarin".equals(currentLang)) {
                    return "网络超时";
                } else {
                    return "網絡超時";
                }
            case "error_no_match":
                if ("english".equals(currentLang)) {
                    return "No match found";
                } else if ("mandarin".equals(currentLang)) {
                    return "未找到匹配";
                } else {
                    return "未找到匹配";
                }
            case "error_recognizer_busy":
                if ("english".equals(currentLang)) {
                    return "Recognizer busy";
                } else if ("mandarin".equals(currentLang)) {
                    return "识别器忙碌";
                } else {
                    return "識別器忙碌";
                }
            case "error_server":
                if ("english".equals(currentLang)) {
                    return "Server error";
                } else if ("mandarin".equals(currentLang)) {
                    return "服务器错误";
                } else {
                    return "服務器錯誤";
                }
            case "error_speech_timeout":
                if ("english".equals(currentLang)) {
                    return "Speech timeout";
                } else if ("mandarin".equals(currentLang)) {
                    return "语音超时";
                } else {
                    return "語音超時";
                }
            case "error_unknown":
                if ("english".equals(currentLang)) {
                    return "Unknown error";
                } else if ("mandarin".equals(currentLang)) {
                    return "未知错误";
                } else {
                    return "未知錯誤";
                }
            default:
                return getString(R.string.app_name);
        }
    }

    protected void announcePageTitle() {
        String currentLang = LocaleManager.getInstance(this).getCurrentLanguage();
        
        switch (currentLang) {
            case "english":
                ttsManager.speak(null, "Travel Assistant. Start travel with voice, environment recognition, and emergency assistance.", true);
                break;
            case "mandarin":
                ttsManager.speak("出行协助。开始出行语音、前方环境识别和紧急求助。", null, true);
                break;
            case "cantonese":
            default:
                ttsManager.speak("出行協助。開始出行語音、前方環境識別和緊急求助。", "Travel Assistant. Start travel with voice, environment recognition, and emergency assistance.", true);
                break;
        }
    }
    
    private String getEnglishDescription() {
        return "This feature provides travel assistance including navigation, route planning, traffic information, weather updates, and emergency location sharing.";
    }
    
    private String getSimplifiedChineseDescription() {
        return "出行助手功能，提供导航、路线规划、交通信息、天气更新和紧急位置分享等服务。";
    }
    
    @Override
    protected void startEnvironmentActivity() {
        // 重寫父類方法，避免語音命令衝突
    }
    
    @Override
    protected void startDocumentCurrencyActivity() {
        // 重寫父類方法，避免語音命令衝突
    }
    
    @Override
    protected void startFindItemsActivity() {
        // 重寫父類方法，避免語音命令衝突
    }
    
    @Override
    protected void startSettingsActivity() {
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra("language", currentLanguage);
        startActivity(intent);
    }
    
    @Override
    protected void handleEmergencyCommand() {
        announceInfo(getString(R.string.emergency_location_feature_coming_soon));
    }
    
    @Override
    protected void goToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("language", currentLanguage);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
    
    @Override
    protected void handleLanguageSwitch() {
        announceInfo(getString(R.string.language_switch_feature_coming_soon));
    }
    
    @Override
    protected void stopCurrentOperation() {
        announceInfo(getString(R.string.operation_stopped));
    }
}
