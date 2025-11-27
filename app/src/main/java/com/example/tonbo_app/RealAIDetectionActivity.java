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

import java.util.ArrayList;
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
    private android.widget.ImageButton backButton;
    private Button startButton;
    private Button stopButton;
    
    private TTSManager ttsManager;
    private String currentLanguage = "cantonese";
    private TextView pageTitle;
    
    private ProcessCameraProvider cameraProvider;
    private ImageAnalysis imageAnalysis;
    private YoloDetector yoloDetector;
    private OptimizedDetectionOverlayView detectionOverlay;
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
        detectionOverlay = findViewById(R.id.detectionOverlay);
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
            // 返回主頁（會檢查是否從手勢登入進入）
            handleBackPressed();
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
            
            // 設置空的分析器（不進行檢測）
            imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
                @Override
                public void analyze(@NonNull ImageProxy image) {
                    // 初始狀態不進行檢測，只關閉圖像
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
            // 檢查 Activity 是否正在銷毀或已銷毀
            if (isFinishing() || isDestroyed()) {
                Log.d(TAG, "Activity 正在銷毀，跳過圖像分析");
                image.close();
                return;
            }
            
            if (!isDetecting) {
                image.close();
                return;
            }
            
            Log.d(TAG, "開始分析圖像，寬度: " + image.getWidth() + ", 高度: " + image.getHeight());
            
            // 執行AI檢測
            List<YoloDetector.DetectionResult> results = yoloDetector.detect(image);
            
            Log.d(TAG, "檢測完成，結果數量: " + (results != null ? results.size() : 0));
            if (results != null && !results.isEmpty()) {
                for (int i = 0; i < results.size(); i++) {
                    YoloDetector.DetectionResult result = results.get(i);
                    android.graphics.Rect bbox = result.getBoundingBox();
                    Log.d(TAG, String.format("檢測結果[%d]: %s, 置信度: %.2f, bbox: [%d,%d,%d,%d]", 
                        i, result.getLabelZh(), result.getConfidence(),
                        bbox != null ? bbox.left : -1,
                        bbox != null ? bbox.top : -1,
                        bbox != null ? bbox.right : -1,
                        bbox != null ? bbox.bottom : -1));
                }
            }
            
            // 在主線程更新UI
            runOnUiThread(() -> {
                if (results != null && !results.isEmpty()) {
                    updateStatusIndicator("scanning");
                    
                    // 更新檢測結果覆蓋層
                    if (detectionOverlay != null) {
                        Log.d(TAG, "更新檢測結果到覆蓋層，數量: " + results.size());
                        // 告知覆蓋層來源影像尺寸，確保像素坐標正確映射
                        detectionOverlay.setSourceImageSize(image.getWidth(), image.getHeight());
                        detectionOverlay.updateDetectionResults(results);
                    } else {
                        Log.e(TAG, "❌ detectionOverlay 為 null，無法更新檢測結果！");
                    }
                    
                    announceDetectionResults(results);
                } else {
                    Log.d(TAG, "檢測結果為空，清除覆蓋層");
                    updateStatusIndicator("scanning");
                    
                    // 清除檢測結果覆蓋層
                    if (detectionOverlay != null) {
                        detectionOverlay.clearDetectionResults();
                    }
                }
            });
            
            image.close();
            
        } catch (Exception e) {
            Log.e(TAG, "圖像分析失敗: " + e.getMessage(), e);
            runOnUiThread(() -> updateStatusIndicator("error"));
            image.close();
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
        
        // 設置真正的圖像分析器
        if (imageAnalysis != null) {
            imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
                @Override
                public void analyze(@NonNull ImageProxy image) {
                    if (isDetecting && yoloDetector != null) {
                        analyzeImage(image);
                    } else {
                        image.close();
                    }
                }
            });
        }
        
        String announcement = (currentLanguage.equals("english")) 
            ? "Environment recognition started" 
            : (currentLanguage.equals("mandarin") ? "環境識別已開始" : "環境識別已開始");
        
        ttsManager.speak(announcement, announcement, true);
        
        Toast.makeText(this, "環境識別已開始", Toast.LENGTH_SHORT).show();
    }
    
    private void stopDetection() {
        isDetecting = false;
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        
        // 停止圖像分析，避免後台繼續處理
        if (imageAnalysis != null) {
            imageAnalysis.setAnalyzer(null, null);
        }
        
        // 清除檢測結果顯示
        if (detectionOverlay != null) {
            detectionOverlay.clearDetectionResults();
        }
        
        updateStatusIndicator("ready");
        
        Toast.makeText(this, "環境識別已停止", Toast.LENGTH_SHORT).show();

        // 停止一切語音播報
        if (ttsManager != null) {
            ttsManager.stopSpeaking();
        }
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
        if (results.isEmpty()) {
            // 空結果不播報，避免信息過載
            return;
        }
        
        // 1. 重要性排序
        List<YoloDetector.DetectionResult> critical = new ArrayList<>();
        List<YoloDetector.DetectionResult> important = new ArrayList<>();
        List<YoloDetector.DetectionResult> optional = new ArrayList<>();
        
        for (YoloDetector.DetectionResult result : results) {
            String category = categorizeImportance(result.getLabel());
            if ("critical".equals(category)) {
                critical.add(result);
            } else if ("important".equals(category)) {
                important.add(result);
            } else {
                optional.add(result);
            }
        }
        
        // 2. 只播報重要的結果
        List<YoloDetector.DetectionResult> toAnnounce = new ArrayList<>();
        if (!critical.isEmpty()) {
            toAnnounce = critical;  // 只播報關鍵物體
        } else if (!important.isEmpty()) {
            toAnnounce = important;  // 或播報重要物體
        } else if (optional.size() <= 3) {
            toAnnounce = optional;  // 只播報少量裝飾品
        }
        
        if (toAnnounce.isEmpty()) {
            return;
        }
        
        // 3. 優化語音播報 - 只播報物體名稱，不包含距離和位置信息
        StringBuilder announcement = new StringBuilder();
        
        // 直接列舉物體名稱，最多2個
        int maxObjects = Math.min(toAnnounce.size(), 2);
        for (int i = 0; i < maxObjects; i++) {
            announcement.append(toAnnounce.get(i).getLabelZh());
            if (i < maxObjects - 1) {
                announcement.append("、");
            }
        }
        
        // 如果物體超過2個，添加總數
        if (toAnnounce.size() > 2) {
            announcement.append("等").append(toAnnounce.size()).append("個");
        }
        
        // 4. 根據語言播報
        String cantoneseText = currentLanguage.equals("english") ? translateToEnglish(announcement.toString()) : announcement.toString();
        String englishText = currentLanguage.equals("english") ? announcement.toString() : translateToEnglish(announcement.toString());
        
        ttsManager.speak(cantoneseText, englishText, false);
    }
    
    private String categorizeImportance(String label) {
        // 關鍵物體：人、車輛、障礙物
        if (label.contains("person") || label.contains("car") || 
            label.contains("truck") || label.contains("bus") || 
            label.contains("motorcycle") || label.contains("obstacle")) {
            return "critical";
        }
        
        // 重要物體：家具、門、開關
        if (label.contains("chair") || label.contains("table") || 
            label.contains("door") || label.contains("sofa") ||
            label.contains("bed") || label.contains("keyboard")) {
            return "important";
        }
        
        // 可選物體：裝飾品
        return "optional";
    }
    
    private String estimateDistance(Rect boundingBox) {
        if (boundingBox == null) return "";
        
        // 估算相對距離
        float area = boundingBox.width() * boundingBox.height();
        float normalizedArea = area / (1080 * 1920); // 假設標準屏幕
        
        if (normalizedArea > 0.1) {
            return "約1米處";
        } else if (normalizedArea > 0.05) {
            return "約2米處";
        } else if (normalizedArea > 0.02) {
            return "約3米處";
        } else {
            return "遠處約";
        }
    }
    
    private String translateToEnglish(String text) {
        // 簡單翻譯映射
        if (text.contains("檢測到")) text = text.replace("檢測到", "detected");
        if (text.contains("個物體")) text = text.replace("個物體", " objects");
        if (text.contains("有")) text = text.replace("有", "has");
        if (text.contains("左側")) text = text.replace("左側", "left side");
        if (text.contains("右側")) text = text.replace("右側", "right side");
        if (text.contains("上方")) text = text.replace("上方", "above");
        if (text.contains("下方")) text = text.replace("下方", "below");
        if (text.contains("中央")) text = text.replace("中央", "center");
        return text;
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
        if (cameraExecutor == null || cameraExecutor.isShutdown()) {
            cameraExecutor = Executors.newSingleThreadExecutor();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        Log.d(TAG, "onPause: 停止檢測並清理相機資源");
        
        // 停止檢測
        if (isDetecting) {
            stopDetection();
        }
        
        // 解除相機綁定（防止在 Activity 暫停時繼續寫入 SurfaceView）
        if (cameraProvider != null) {
            try {
                cameraProvider.unbindAll();
                Log.d(TAG, "onPause: 已解除相機綁定");
            } catch (Exception e) {
                Log.e(TAG, "onPause: 解除相機綁定失敗: " + e.getMessage());
            }
        }
        
        // 清理圖像分析器
        if (imageAnalysis != null) {
            try {
                imageAnalysis.clearAnalyzer();
                Log.d(TAG, "onPause: 已清理圖像分析器");
            } catch (Exception e) {
                Log.e(TAG, "onPause: 清理圖像分析器失敗: " + e.getMessage());
            }
        }
        
        // 關閉相機執行器
        if (cameraExecutor != null) {
            try {
                cameraExecutor.shutdown();
                // 等待任務完成，但不強制終止
                if (!cameraExecutor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
                    Log.w(TAG, "onPause: 相機執行器未在1秒內關閉，強制關閉");
                    cameraExecutor.shutdownNow();
                }
                Log.d(TAG, "onPause: 已關閉相機執行器");
            } catch (InterruptedException e) {
                Log.e(TAG, "onPause: 關閉相機執行器被中斷");
                cameraExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        Log.d(TAG, "onDestroy: 開始清理所有資源");
        
        // 停止檢測（如果還在運行）
        if (isDetecting) {
            stopDetection();
        }
        
        // 確保停止檢測標誌已設置
        isDetecting = false;
        
        // 清理圖像分析器
        if (imageAnalysis != null) {
            try {
                imageAnalysis.clearAnalyzer();
                imageAnalysis = null;
                Log.d(TAG, "onDestroy: 已清理圖像分析器");
            } catch (Exception e) {
                Log.e(TAG, "onDestroy: 清理圖像分析器失敗: " + e.getMessage());
            }
        }
        
        // 解除相機綁定
        if (cameraProvider != null) {
            try {
                cameraProvider.unbindAll();
                cameraProvider = null;
                Log.d(TAG, "onDestroy: 已解除相機綁定");
            } catch (Exception e) {
                Log.e(TAG, "onDestroy: 解除相機綁定失敗: " + e.getMessage());
            }
        }
        
        // 關閉 YOLO 檢測器
        if (yoloDetector != null) {
            try {
                yoloDetector.close();
                yoloDetector = null;
                Log.d(TAG, "onDestroy: 已關閉 YOLO 檢測器");
            } catch (Exception e) {
                Log.e(TAG, "onDestroy: 關閉 YOLO 檢測器失敗: " + e.getMessage());
            }
        }
        
        // 關閉相機執行器
        if (cameraExecutor != null) {
            try {
                cameraExecutor.shutdown();
                if (!cameraExecutor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                    Log.w(TAG, "onDestroy: 相機執行器未在2秒內關閉，強制關閉");
                    cameraExecutor.shutdownNow();
                }
                cameraExecutor = null;
                Log.d(TAG, "onDestroy: 已關閉相機執行器");
            } catch (InterruptedException e) {
                Log.e(TAG, "onDestroy: 關閉相機執行器被中斷");
                cameraExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        Log.d(TAG, "onDestroy: 資源清理完成");
    }
}
