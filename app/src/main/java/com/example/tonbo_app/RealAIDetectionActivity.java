package com.example.tonbo_app;

import android.Manifest;
import android.graphics.Rect;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 真實AI檢測演示活動
 * 展示真實的AI物體檢測功能
 */
public class RealAIDetectionActivity extends BaseAccessibleActivity {
    private static final String TAG = "RealAIDetection";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;
    
    private PreviewView previewView;
    private View statusIndicator;
    private Button backButton;
    private Button startButton;
    private Button stopButton;
    
    private TTSManager ttsManager;
    private String currentLanguage = "cantonese";
    private TextView pageTitle;
    
    private ProcessCameraProvider cameraProvider;
    private ImageAnalysis imageAnalysis;
    private YoloDetector yoloDetector;
    private ExecutorService cameraExecutor;
    private boolean isDetecting = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_ai_detection);
        
        // 獲取語言設置
        if (getIntent() != null && getIntent().hasExtra("language")) {
            currentLanguage = getIntent().getStringExtra("language");
        }
        
        // 初始化TTS
        ttsManager = TTSManager.getInstance(this);
        ttsManager.changeLanguage(currentLanguage);
        
        initViews();
        initDetector();
        checkCameraPermission();
        
        // 播報頁面標題
        announcePageTitle();
    }
    
    private void initViews() {
        previewView = findViewById(R.id.previewView);
        statusIndicator = findViewById(R.id.statusIndicator);
        backButton = findViewById(R.id.backButton);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        pageTitle = findViewById(R.id.pageTitle);
        
        // 設置按鈕點擊事件
        backButton.setOnClickListener(v -> {
            // 停止檢測
            if (isDetecting) {
                stopDetection();
            }
            // 返回主頁
            finish();
        });
        
        startButton.setOnClickListener(v -> startDetection());
        stopButton.setOnClickListener(v -> stopDetection());
        
        // 根據當前語言更新界面文字
        updateLanguageUI();
        
        // 初始化狀態指示燈
        updateStatusIndicator("ready");
        stopButton.setEnabled(false);
    }
    
    /**
     * 更新語言UI
     */
    private void updateLanguageUI() {
        if (pageTitle != null) {
            pageTitle.setText(getLocalizedString("environment_recognition_title"));
        }
        
        if (startButton != null) {
            startButton.setText(getLocalizedString("start_recognition"));
        }
        
        if (stopButton != null) {
            stopButton.setText(getLocalizedString("stop_recognition"));
        }
    }
    
    /**
     * 根據當前語言獲取本地化字符串
     */
    private String getLocalizedString(String key) {
        switch (key) {
            case "environment_recognition_title":
                if ("english".equals(currentLanguage)) {
                    return "Environment Recognition";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "环境识别助手";
                } else {
                    return "環境識別助手";
                }
            case "start_recognition":
                if ("english".equals(currentLanguage)) {
                    return "Start Recognition";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "开始识别";
                } else {
                    return "開始識別";
                }
            case "stop_recognition":
                if ("english".equals(currentLanguage)) {
                    return "Stop Recognition";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "停止识别";
                } else {
                    return "停止識別";
                }
            default:
                return "";
        }
    }
    
    private void initDetector() {
        try {
            yoloDetector = new YoloDetector(this);
            Log.d(TAG, "環境識別器初始化完成");
            updateStatusIndicator("ready");
        } catch (Exception e) {
            Log.e(TAG, "環境識別器初始化失敗: " + e.getMessage());
            updateStatusIndicator("error");
            Toast.makeText(this, "環境識別器初始化失敗", Toast.LENGTH_LONG).show();
        }
    }
    
    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.CAMERA}, 
                CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            initializeCamera();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeCamera();
            } else {
                updateStatusIndicator("error");
                Toast.makeText(this, "需要相機權限才能使用環境識別", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void initializeCamera() {
        try {
            ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
                ProcessCameraProvider.getInstance(this);
            
            cameraProviderFuture.addListener(() -> {
                try {
                    cameraProvider = cameraProviderFuture.get();
                    setupCamera();
                } catch (Exception e) {
                    Log.e(TAG, "相機初始化失敗: " + e.getMessage());
                    updateStatusIndicator("error");
                }
            }, ContextCompat.getMainExecutor(this));
            
        } catch (Exception e) {
            Log.e(TAG, "相機提供者獲取失敗: " + e.getMessage());
            updateStatusIndicator("error");
        }
    }
    
    private void setupCamera() {
        try {
            // 設置相機預覽
            androidx.camera.core.Preview preview = new androidx.camera.core.Preview.Builder()
                .build();
            
            preview.setSurfaceProvider(previewView.getSurfaceProvider());
            
            // 設置圖像分析
            imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
            
            // 設置圖像分析器
            imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
                @Override
                public void analyze(@NonNull ImageProxy image) {
                    if (isDetecting && yoloDetector != null) {
                        analyzeImage(image);
                    }
                    image.close();
                }
            });
            
            // 綁定相機
            CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            
            updateStatusIndicator("ready");
            startButton.setEnabled(true);
            
        } catch (Exception e) {
            Log.e(TAG, "相機設置失敗: " + e.getMessage());
            updateStatusIndicator("error");
        }
    }
    
    private void analyzeImage(ImageProxy image) {
        try {
            // 執行AI檢測
            List<YoloDetector.DetectionResult> results = yoloDetector.detect(image);
            
            // 在主線程更新UI
            runOnUiThread(() -> {
                if (results != null && !results.isEmpty()) {
                    updateStatusIndicator("scanning");
                    announceDetectionResults(results);
                } else {
                    updateStatusIndicator("scanning");
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "圖像分析失敗: " + e.getMessage());
            runOnUiThread(() -> updateStatusIndicator("error"));
        }
    }
    
    private void startDetection() {
        if (yoloDetector == null) {
            Toast.makeText(this, "環境識別器未初始化", Toast.LENGTH_SHORT).show();
            return;
        }
        
        isDetecting = true;
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        updateStatusIndicator("scanning");
        
        
        Toast.makeText(this, "環境識別已開始", Toast.LENGTH_SHORT).show();
    }
    
    private void stopDetection() {
        isDetecting = false;
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        updateStatusIndicator("ready");
        
        Toast.makeText(this, "環境識別已停止", Toast.LENGTH_SHORT).show();
    }
    
    
    private void updateStatusIndicator(String status) {
        int drawableRes;
        switch (status) {
            case "ready":
                drawableRes = R.drawable.status_indicator_ready;
                break;
            case "scanning":
                drawableRes = R.drawable.status_indicator_scanning;
                break;
            case "error":
                drawableRes = R.drawable.status_indicator_error;
                break;
            default:
                drawableRes = R.drawable.status_indicator_ready;
                break;
        }
        statusIndicator.setBackgroundResource(drawableRes);
        Log.d(TAG, "狀態指示燈更新: " + status);
    }
    
    
    private void announceDetectionResults(List<YoloDetector.DetectionResult> results) {
        if (results.isEmpty()) return;
        
        StringBuilder announcement = new StringBuilder();
        announcement.append("檢測到 ").append(results.size()).append(" 個物體：");
        
        for (int i = 0; i < Math.min(results.size(), 5); i++) { // 最多播報5個物體
            YoloDetector.DetectionResult result = results.get(i);
            String position = getPositionDescription(result.getBoundingBox());
            announcement.append(" ").append(result.getLabelZh())
                      .append("在").append(position);
            
            if (i < Math.min(results.size(), 5) - 1) {
                announcement.append("，");
            }
        }
        
        if (results.size() > 5) {
            announcement.append("等").append(results.size()).append("個物體");
        }
        
        ttsManager.speak(announcement.toString(), announcement.toString(), false);
    }
    
    private String getPositionDescription(Rect boundingBox) {
        if (boundingBox == null) {
            return "未知位置";
        }
        
        // 假設畫面大小為標準比例，計算相對位置
        float centerX = (boundingBox.left + boundingBox.right) / 2.0f;
        float centerY = (boundingBox.top + boundingBox.bottom) / 2.0f;
        
        // 假設畫面寬度為1000，高度為1500（可以根據實際情況調整）
        float relativeX = centerX / 1000.0f;
        float relativeY = centerY / 1500.0f;
        
        String horizontal = relativeX < 0.33f ? "左側" : (relativeX > 0.66f ? "右側" : "中間");
        String vertical = relativeY < 0.33f ? "上方" : (relativeY > 0.66f ? "下方" : "中間");
        
        if (horizontal.equals("中間") && vertical.equals("中間")) {
            return "正中央";
        } else if (horizontal.equals("中間")) {
            return vertical;
        } else if (vertical.equals("中間")) {
            return horizontal;
        } else {
            return horizontal + vertical;
        }
    }
    
    @Override
    protected void announcePageTitle() {
        String cantoneseText = "環境識別助手。這個功能可以幫助你識別前方的物體。點擊開始識別按鈕開始掃描環境。";
        String englishText = "Environment Recognition Assistant. This feature can help you identify objects in front of you. Tap the start recognition button to begin scanning the environment.";
        ttsManager.speak(cantoneseText, englishText, true);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        cameraExecutor = Executors.newSingleThreadExecutor();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (yoloDetector != null) {
            yoloDetector.close();
        }
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }
}
