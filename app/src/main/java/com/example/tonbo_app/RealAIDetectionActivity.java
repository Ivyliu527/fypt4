package com.example.tonbo_app;

import android.Manifest;
import android.graphics.Rect;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 真實AI檢測演示活動
 * 展示真實的AI物體檢測功能
 */
public class RealAIDetectionActivity extends BaseAccessibleActivity {
    private static final String TAG = "RealAIDetection";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;
    
    // 靜態標誌，用於追蹤是否在環境識別頁面（供語音命令使用）
    private static boolean isEnvironmentActivityActive = false;
    
    /**
     * 檢查是否在環境識別頁面（供其他 Activity 調用）
     */
    public static boolean isActive() {
        return isEnvironmentActivityActive;
    }
    
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
    
    // 用於去重，避免重複播報相同的識別結果
    private String lastAnnouncedObjects = "";
    private Set<String> lastAnnouncedLabels = new HashSet<>(); // 用於更精確的比較
    private long lastAnnounceTime = 0;
    private static final long MIN_ANNOUNCE_INTERVAL_MS = 200; // 最小播報間隔（0.2秒，避免過度重複播報完全相同的結果，縮短以減少延遲）
    
    // 用於延長檢測框顯示時間
    private Handler detectionBoxHandler = new Handler(Looper.getMainLooper());
    private Runnable clearDetectionBoxRunnable;
    private static final long DETECTION_BOX_DISPLAY_DURATION_MS = 2000; // 檢測框保留2秒
    
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
        // 設置檢測框的語言，確保標籤顯示正確的語言
        if (detectionOverlay != null) {
            detectionOverlay.setLanguage(currentLanguage);
        }
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
            
            // 設置預覽視圖的縮放模式為 FIT（保持寬高比，居中顯示）
            // 這與座標轉換邏輯匹配
            previewView.setScaleType(androidx.camera.view.PreviewView.ScaleType.FIT_CENTER);
            
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
            
            // 獲取圖像旋轉角度（考慮相機方向）
            int rotationDegrees = image.getImageInfo().getRotationDegrees();
            Log.d(TAG, "圖像旋轉角度: " + rotationDegrees + "度, 圖像尺寸: " + image.getWidth() + "x" + image.getHeight());
            
            // 在主線程更新UI
            runOnUiThread(() -> {
                if (results != null && !results.isEmpty()) {
                    updateStatusIndicator("scanning");
                    
                    // 取消之前的清除任務（如果有）
                    if (clearDetectionBoxRunnable != null) {
                        detectionBoxHandler.removeCallbacks(clearDetectionBoxRunnable);
                        clearDetectionBoxRunnable = null;
                    }
                    
                    // 調整結果座標以適應圖像旋轉
                    List<YoloDetector.DetectionResult> adjustedResults = adjustResultsForRotation(
                        results, image.getWidth(), image.getHeight(), rotationDegrees);
                    Log.d(TAG, "座標調整後結果數量: " + adjustedResults.size());
                    
                    // 過濾掉室內不合理的檢測結果（如滑浪板、雪櫃等）
                    List<YoloDetector.DetectionResult> filteredResults = filterUnreasonableDetections(adjustedResults);
                    Log.d(TAG, "過濾後結果數量: " + filteredResults.size());
                    
                    // 統一限制為前2個結果，確保顯示和語音播報完全同步
                    List<YoloDetector.DetectionResult> displayResults;
                    if (filteredResults.size() > 2) {
                        displayResults = new ArrayList<>(filteredResults.subList(0, 2));
                        Log.d(TAG, "限制顯示和播報為前2個檢測結果");
                    } else {
                        displayResults = filteredResults;
                    }
                    
                    // 更新檢測結果覆蓋層
                    if (detectionOverlay != null) {
                        Log.d(TAG, "=== 更新檢測結果到覆蓋層，數量: " + displayResults.size() + " ===");
                        
                        // 獲取預覽視圖的實際顯示尺寸
                        int previewWidth = previewView.getWidth();
                        int previewHeight = previewView.getHeight();
                        Log.d(TAG, "預覽視圖尺寸: " + previewWidth + "x" + previewHeight);
                        
                        // 告知覆蓋層來源影像尺寸，確保像素坐標正確映射
                        // 注意：如果圖像旋轉了90度或270度，寬高需要互換
                        // 因為旋轉後的座標系統已經改變，但圖像的寬高值沒有自動互換
                        int sourceWidth = image.getWidth();
                        int sourceHeight = image.getHeight();
                        if (rotationDegrees == 90 || rotationDegrees == 270) {
                            // 旋轉90度或270度時，寬高互換
                            int temp = sourceWidth;
                            sourceWidth = sourceHeight;
                            sourceHeight = temp;
                            Log.d(TAG, "圖像旋轉 " + rotationDegrees + " 度，寬高互換: " + sourceWidth + "x" + sourceHeight);
                        }
                        detectionOverlay.setSourceImageSize(sourceWidth, sourceHeight);
                        
                        // 設置當前語言，確保標籤顯示正確的語言（必須在更新結果前設置）
                        detectionOverlay.setLanguage(currentLanguage);
                        Log.d(TAG, "設置檢測覆蓋層語言為: " + currentLanguage);
                        
                        // 記錄將要顯示的標籤（用於驗證一致性）
                        for (int i = 0; i < displayResults.size(); i++) {
                            YoloDetector.DetectionResult result = displayResults.get(i);
                            String displayLabel;
                            if ("english".equals(currentLanguage)) {
                                displayLabel = result.getLabel();
                            } else {
                                displayLabel = result.getLabelZh();
                            }
                            Log.d(TAG, String.format("顯示結果[%d]: %s (英文: %s, 中文: %s, 置信度: %.2f)", 
                                i, displayLabel, result.getLabel(), result.getLabelZh(), result.getConfidence()));
                        }
                        
                        // 直接傳遞已限制的結果，不再在 overlay 中限制
                        detectionOverlay.updateDetectionResults(displayResults);
                        Log.d(TAG, "已更新檢測覆蓋層，結果數量: " + displayResults.size());
                    } else {
                        Log.e(TAG, "❌ detectionOverlay 為 null，無法更新檢測結果！");
                    }
                    
                    // 使用相同的結果列表進行語音播報，確保完全同步
                    Log.d(TAG, "=== 開始語音播報，使用相同的結果列表 ===");
                    
                    // 驗證：記錄將要播報的標籤，確保與顯示一致
                    StringBuilder verificationLog = new StringBuilder("驗證播報標籤順序: ");
                    for (int i = 0; i < displayResults.size(); i++) {
                        YoloDetector.DetectionResult result = displayResults.get(i);
                        String label;
                        if ("english".equals(currentLanguage)) {
                            label = result.getLabel();
                        } else {
                            label = result.getLabelZh();
                        }
                        verificationLog.append("[").append(i).append("]=").append(label).append(" ");
                    }
                    Log.d(TAG, verificationLog.toString());
                    
                    announceDetectionResults(displayResults);
                } else {
                    Log.d(TAG, "檢測結果為空，延遲清除覆蓋層");
                    updateStatusIndicator("scanning");
                    
                    // 延遲清除檢測結果覆蓋層，讓檢測框停留更長時間
                    if (clearDetectionBoxRunnable != null) {
                        detectionBoxHandler.removeCallbacks(clearDetectionBoxRunnable);
                    }
                    
                    clearDetectionBoxRunnable = () -> {
                        if (detectionOverlay != null) {
                            detectionOverlay.clearDetectionResults();
                            Log.d(TAG, "延遲清除檢測框完成");
                        }
                        clearDetectionBoxRunnable = null;
                    };
                    
                    detectionBoxHandler.postDelayed(clearDetectionBoxRunnable, DETECTION_BOX_DISPLAY_DURATION_MS);
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
        
        // 立即停止所有正在進行的語音播報
        if (ttsManager != null) {
            ttsManager.stopSpeaking();
            Log.d(TAG, "已停止當前語音播報");
        }
        
        isDetecting = true;
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        updateStatusIndicator("scanning");
        
        // 設置真正的圖像分析器，立即開始識別（不等待語音播報）
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
        
        // 播報"環境識別已開始"（但不會阻塞識別功能的啟動）
        String announcement = (currentLanguage.equals("english")) 
            ? "Environment recognition started" 
            : (currentLanguage.equals("mandarin") ? "環境識別已開始" : "環境識別已開始");
        ttsManager.speak(announcement, announcement, true);
        
        Log.d(TAG, "環境識別已立即開始，語音播報在後台進行");
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
        
        // 取消延遲清除任務
        if (clearDetectionBoxRunnable != null) {
            detectionBoxHandler.removeCallbacks(clearDetectionBoxRunnable);
            clearDetectionBoxRunnable = null;
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
        if (results == null || results.isEmpty()) {
            // 空結果不播報，避免信息過載
            lastAnnouncedObjects = ""; // 清除上次播報記錄
            lastAnnouncedLabels.clear(); // 清除標籤集合
            Log.d(TAG, "語音播報：結果為空，跳過播報並清除記錄");
            return;
        }
        
        // 直接使用傳入的結果（已在調用前限制為前2個，與顯示完全同步）
        // 確保播報的物體與屏幕上顯示的檢測框完全一致
        StringBuilder announcement = new StringBuilder();
        
        // 根據當前語言選擇標籤
        boolean useEnglish = "english".equals(currentLanguage);
        String separator = useEnglish ? ", " : "、";
        
        Log.d(TAG, "語音播報：當前語言 = " + currentLanguage + ", 結果數量 = " + results.size());
        
        // 播報所有傳入的結果（已限制為前2個，與顯示完全同步）
        // 確保標籤選擇邏輯與顯示完全一致（與 OptimizedDetectionOverlayView.drawDetectionResult() 完全一致）
        // 使用與顯示完全相同的順序和標籤選擇邏輯
        for (int i = 0; i < results.size(); i++) {
            YoloDetector.DetectionResult result = results.get(i);
            if (result == null) {
                Log.w(TAG, "語音播報結果[" + i + "]為 null，跳過");
                continue;
            }
            
            // 根據語言選擇標籤（與 OptimizedDetectionOverlayView 中的邏輯完全一致）
            String label;
            if ("english".equals(currentLanguage)) {
                label = result.getLabel(); // 英文模式使用英文標籤
            } else if ("mandarin".equals(currentLanguage)) {
                label = result.getLabelZh(); // 普通話模式使用中文標籤
            } else {
                label = result.getLabelZh(); // 廣東話模式使用中文標籤
            }
            
            // 詳細日誌，確保與顯示一致
            Log.d(TAG, String.format("語音播報結果[%d]: %s (英文: %s, 中文: %s, 置信度: %.2f, bbox: %s)", 
                i, label, result.getLabel(), result.getLabelZh(), result.getConfidence(),
                result.getBoundingBox() != null ? result.getBoundingBox().toString() : "null"));
            
            announcement.append(label);
            if (i < results.size() - 1) {
                announcement.append(separator);
            }
        }
        
        String currentObjects = announcement.toString();
        long currentTime = System.currentTimeMillis();
        Log.d(TAG, "語音播報：構建的播報文本 = \"" + currentObjects + "\"");
        
        // 驗證：確保播報文本與顯示標籤完全一致
        StringBuilder expectedText = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            YoloDetector.DetectionResult result = results.get(i);
            if (result == null) continue;
            String label;
            if ("english".equals(currentLanguage)) {
                label = result.getLabel();
            } else {
                label = result.getLabelZh();
            }
            expectedText.append(label);
            if (i < results.size() - 1) {
                expectedText.append(separator);
            }
        }
        String expected = expectedText.toString();
        if (!currentObjects.equals(expected)) {
            Log.e(TAG, "❌ 語音播報文本不一致！預期: \"" + expected + "\", 實際: \"" + currentObjects + "\"");
        } else {
            Log.d(TAG, "✅ 語音播報文本驗證通過: \"" + currentObjects + "\"");
        }
        
        // 構建當前檢測到的標籤集合（用於更精確的比較）
        Set<String> currentLabels = new HashSet<>();
        for (YoloDetector.DetectionResult result : results) {
            String label;
            if ("english".equals(currentLanguage)) {
                label = result.getLabel();
            } else {
                label = result.getLabelZh();
            }
            currentLabels.add(label);
        }
        
        // 檢查是否需要播報：
        // 1. 第一次檢測到物品（上次記錄為空）- 立即播報（無延遲）
        // 2. 檢測到的物體集合與上次不同（使用集合比較，不依賴順序）- 立即播報（無延遲）
        // 3. 檢測結果數量有變化 - 立即播報（無延遲）
        // 4. 播報文本與上次不同 - 立即播報（無延遲）
        // 5. 距離上次播報已超過最小間隔（即使標籤相同，也定期播報以確保響應性）
        boolean isFirstDetection = lastAnnouncedLabels.isEmpty();
        boolean labelsChanged = !currentLabels.equals(lastAnnouncedLabels);
        boolean countChanged = currentLabels.size() != lastAnnouncedLabels.size();
        boolean textChanged = !currentObjects.equals(lastAnnouncedObjects);
        
        // 優先級：檢測結果有變化時立即播報（無延遲）
        // 如果播報文本完全相同，則不播報（避免重複播報相同的內容，如"筆筆筆筆筆"）
        // 只有在檢測結果真的改變時才播報，確保不會重複播報相同的內容
        boolean hasChange = isFirstDetection || labelsChanged || countChanged || textChanged;
        
        // 如果檢測結果完全相同，即使時間過期也不重複播報（避免重複播報相同的內容）
        // 只有在檢測結果真的改變時才播報
        boolean shouldAnnounce = hasChange;
        
        if (!hasChange) {
            Log.d(TAG, "⏭️ 跳過重複播報：播報文本完全相同 (\"" + currentObjects + "\")，不重複播報");
        }
        
        Log.d(TAG, String.format("播報判斷 - 首次檢測: %s, 標籤變化: %s, 數量變化: %s, 文本變化: %s, 結果: %s", 
            isFirstDetection, labelsChanged, countChanged, textChanged, shouldAnnounce));
        
        if (shouldAnnounce) {
            // 根據語言播報（確保與顯示標籤完全一致）
            // currentObjects 已經根據 currentLanguage 選擇了正確的標籤（英文或中文）
            String cantoneseText;
            String englishText;
            
            if ("english".equals(currentLanguage)) {
                // 英文模式：currentObjects 包含英文標籤
                englishText = currentObjects; // 直接使用，與顯示標籤完全一致
                // 對於英文模式，cantoneseText 可以是空或英文（TTS 會使用 englishText）
                cantoneseText = currentObjects;
                Log.d(TAG, "英文模式播報 - 英文文本: " + englishText + " (與顯示標籤一致)");
            } else if ("mandarin".equals(currentLanguage)) {
                // 普通話模式：currentObjects 包含中文標籤
                cantoneseText = currentObjects; // 直接使用，與顯示標籤完全一致
                // 獲取對應的英文標籤用於備用（確保順序與顯示一致）
                StringBuilder englishBuilder = new StringBuilder();
                for (int i = 0; i < results.size(); i++) {
                    YoloDetector.DetectionResult result = results.get(i);
                    if (result == null) continue;
                    englishBuilder.append(result.getLabel());
                    if (i < results.size() - 1) {
                        englishBuilder.append(", ");
                    }
                }
                englishText = englishBuilder.toString();
                Log.d(TAG, "普通話模式播報 - 中文文本: " + cantoneseText + " (與顯示標籤一致), 英文文本: " + englishText);
            } else {
                // 廣東話模式：currentObjects 包含中文標籤
                cantoneseText = currentObjects; // 直接使用，與顯示標籤完全一致
                // 獲取對應的英文標籤用於備用（確保順序與顯示一致）
                StringBuilder englishBuilder = new StringBuilder();
                for (int i = 0; i < results.size(); i++) {
                    YoloDetector.DetectionResult result = results.get(i);
                    if (result == null) continue;
                    englishBuilder.append(result.getLabel());
                    if (i < results.size() - 1) {
                        englishBuilder.append(", ");
                    }
                }
                englishText = englishBuilder.toString();
                Log.d(TAG, "廣東話模式播報 - 中文文本: " + cantoneseText + " (與顯示標籤一致), 英文文本: " + englishText);
            }
            
            Log.d(TAG, "準備播報 - 當前語言: " + currentLanguage + ", 中文文本: " + cantoneseText + ", 英文文本: " + englishText);
            
            // 最終驗證：確保播報文本與顯示標籤完全一致
            Log.d(TAG, "=== 最終驗證：播報文本 ===");
            Log.d(TAG, "顯示標籤順序: " + currentObjects);
            Log.d(TAG, "TTS 將播報的文本（中文模式）: " + cantoneseText);
            Log.d(TAG, "TTS 將播報的文本（英文模式）: " + englishText);
            if (!cantoneseText.equals(currentObjects) && !"english".equals(currentLanguage)) {
                Log.e(TAG, "❌ 警告：TTS 中文文本與顯示標籤不一致！");
            }
            if (!englishText.equals(currentObjects) && "english".equals(currentLanguage)) {
                Log.e(TAG, "❌ 警告：TTS 英文文本與顯示標籤不一致！");
            }
            
            // 使用優先級播報，立即停止當前語音並播放新的，減少延遲
            ttsManager.speak(cantoneseText, englishText, true);
            
            // 更新記錄（同時更新字符串和標籤集合）
            lastAnnouncedObjects = currentObjects;
            lastAnnouncedLabels = new HashSet<>(currentLabels); // 創建副本
            lastAnnounceTime = currentTime;
            
            Log.d(TAG, "✅ 已播報檢測結果: " + currentObjects + " (標籤集合: " + currentLabels + ", 與顯示標籤完全一致)");
        } else {
            Log.d(TAG, String.format("⏭️ 跳過重複播報: %s (上次: %s, 當前標籤: %s, 上次標籤: %s, 距離上次: %dms)", 
                currentObjects, lastAnnouncedObjects, currentLabels, lastAnnouncedLabels, (currentTime - lastAnnounceTime)));
        }
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
    
    /**
     * 過濾掉室內不合理的檢測結果
     * 移除一些在室內環境中不應該出現的物品（如滑浪板、雪櫃等）
     */
    private List<YoloDetector.DetectionResult> filterUnreasonableDetections(
            List<YoloDetector.DetectionResult> results) {
        if (results == null || results.isEmpty()) {
            return results;
        }
        
        // 定義室內不應該出現的物品列表（英文標籤）
        Set<String> unreasonableItems = new HashSet<>();
        
        // 交通工具
        unreasonableItems.add("car");            // 汽車
        unreasonableItems.add("bus");            // 公車
        unreasonableItems.add("train");          // 火車
        unreasonableItems.add("truck");          // 卡車
        unreasonableItems.add("boat");           // 船
        unreasonableItems.add("airplane");       // 飛機
        unreasonableItems.add("aeroplane");       // 飛機（另一種拼寫）
        unreasonableItems.add("bicycle");        // 腳踏車
        unreasonableItems.add("motorcycle");     // 摩托車
        unreasonableItems.add("motorbike");      // 摩托車（另一種拼寫）
        
        // 運動用品
        unreasonableItems.add("frisbee");        // 飛盤
        unreasonableItems.add("skis");           // 滑雪板
        unreasonableItems.add("snowboard");      // 滑雪板
        unreasonableItems.add("sports ball");    // 運動球
        unreasonableItems.add("kite");           // 風箏
        unreasonableItems.add("baseball bat");   // 棒球棒
        unreasonableItems.add("baseball glove");  // 棒球手套
        unreasonableItems.add("skateboard");     // 滑板
        unreasonableItems.add("surfboard");      // 衝浪板
        unreasonableItems.add("tennis racket");  // 網球拍
        
        // 廚房用品
        unreasonableItems.add("bottle");         // 瓶子
        unreasonableItems.add("wine glass");     // 酒杯
        unreasonableItems.add("cup");            // 杯子
        unreasonableItems.add("fork");           // 叉子
        unreasonableItems.add("knife");         // 刀
        unreasonableItems.add("spoon");          // 湯匙
        unreasonableItems.add("bowl");           // 碗
        unreasonableItems.add("microwave");      // 微波爐
        unreasonableItems.add("oven");           // 烤箱
        unreasonableItems.add("toaster");        // 烤麵包機
        unreasonableItems.add("sink");           // 水槽
        unreasonableItems.add("refrigerator");   // 冰箱
        
        // 動物
        unreasonableItems.add("cat");            // 貓
        unreasonableItems.add("dog");            // 狗
        unreasonableItems.add("bird");           // 鳥
        unreasonableItems.add("horse");          // 馬
        unreasonableItems.add("sheep");          // 羊
        unreasonableItems.add("cow");            // 牛
        unreasonableItems.add("elephant");       // 大象
        unreasonableItems.add("bear");           // 熊
        unreasonableItems.add("zebra");          // 斑馬
        unreasonableItems.add("giraffe");        // 長頸鹿
        
        // 其他
        unreasonableItems.add("toilet");         // 馬桶
        unreasonableItems.add("traffic light");  // 交通燈
        
        List<YoloDetector.DetectionResult> filteredResults = new ArrayList<>();
        int filteredCount = 0;
        
        // 容易混淆的類別需要更高的置信度閾值
        // 電視和筆記本電腦容易混淆，需要更高的置信度
        Set<String> ambiguousClasses = new HashSet<>();
        ambiguousClasses.add("tv");              // 電視（容易與筆記本電腦混淆）
        ambiguousClasses.add("tvmonitor");       // 電視（另一種拼寫）
        float ambiguousConfidenceThreshold = 0.6f;  // 容易混淆的類別需要至少60%置信度
        
        for (YoloDetector.DetectionResult result : results) {
            String label = result.getLabel().toLowerCase();
            float confidence = result.getConfidence();
            
            Log.d(TAG, "檢查檢測結果: " + label + " (置信度: " + confidence + ")");
            
            // 檢查是否為不合理的物品，直接過濾掉
            if (unreasonableItems.contains(label)) {
                Log.d(TAG, "過濾掉室內不合理的檢測: " + label);
                filteredCount++;
                continue;
            }
            
            // 對於容易混淆的類別（如電視），需要更高的置信度
            if (ambiguousClasses.contains(label) && confidence < ambiguousConfidenceThreshold) {
                Log.d(TAG, "過濾掉低置信度的容易混淆檢測: " + label + " (置信度: " + confidence + ", 需要 >= " + ambiguousConfidenceThreshold + ")");
                filteredCount++;
                continue;
            }
            
            filteredResults.add(result);
        }
        
        if (filteredCount > 0) {
            Log.d(TAG, "過濾統計: 過濾了 " + filteredCount + " 個不合理檢測，保留 " + filteredResults.size() + " 個結果 (原始: " + results.size() + ")");
        }
        
        return filteredResults;
    }
    
    /**
     * 根據圖像旋轉角度調整檢測結果的座標
     */
    private List<YoloDetector.DetectionResult> adjustResultsForRotation(
            List<YoloDetector.DetectionResult> results, 
            int imageWidth, int imageHeight, 
            int rotationDegrees) {
        
        if (results == null || results.isEmpty() || rotationDegrees == 0) {
            return results;
        }
        
        List<YoloDetector.DetectionResult> adjustedResults = new ArrayList<>();
        
        for (YoloDetector.DetectionResult result : results) {
            Rect bbox = result.getBoundingBox();
            if (bbox == null) {
                adjustedResults.add(result);
                continue;
            }
            
            Rect adjustedBbox = rotateBoundingBox(bbox, imageWidth, imageHeight, rotationDegrees);
            
            // 創建新的檢測結果，使用調整後的邊界框
            YoloDetector.DetectionResult adjustedResult = new YoloDetector.DetectionResult(
                result.getLabel(), 
                result.getLabelZh(), 
                result.getConfidence(), 
                adjustedBbox
            );
            adjustedResults.add(adjustedResult);
        }
        
        return adjustedResults;
    }
    
    /**
     * 旋轉邊界框座標
     * 注意：旋轉後的座標系統寬高會互換
     */
    private Rect rotateBoundingBox(Rect bbox, int imageWidth, int imageHeight, int rotationDegrees) {
        int left = bbox.left;
        int top = bbox.top;
        int right = bbox.right;
        int bottom = bbox.bottom;
        
        int newLeft, newTop, newRight, newBottom;
        
        switch (rotationDegrees) {
            case 90:
                // 順時針旋轉90度：
                // 原圖像 (W x H) -> 旋轉後 (H x W)
                // 原座標 (x, y) -> 新座標 (y, W - x)
                newLeft = top;
                newTop = imageWidth - right;
                newRight = bottom;
                newBottom = imageWidth - left;
                // 確保 left < right 和 top < bottom
                if (newLeft > newRight) {
                    int temp = newLeft;
                    newLeft = newRight;
                    newRight = temp;
                }
                if (newTop > newBottom) {
                    int temp = newTop;
                    newTop = newBottom;
                    newBottom = temp;
                }
                return new Rect(newLeft, newTop, newRight, newBottom);
                
            case 180:
                // 旋轉180度：上下左右都翻轉
                newLeft = imageWidth - right;
                newTop = imageHeight - bottom;
                newRight = imageWidth - left;
                newBottom = imageHeight - top;
                return new Rect(newLeft, newTop, newRight, newBottom);
                
            case 270:
                // 順時針旋轉270度（或逆時針90度）：
                // 原圖像 (W x H) -> 旋轉後 (H x W)
                // 原座標 (x, y) -> 新座標 (H - y, x)
                newLeft = imageHeight - bottom;
                newTop = left;
                newRight = imageHeight - top;
                newBottom = right;
                // 確保 left < right 和 top < bottom
                if (newLeft > newRight) {
                    int temp = newLeft;
                    newLeft = newRight;
                    newRight = temp;
                }
                if (newTop > newBottom) {
                    int temp = newTop;
                    newTop = newBottom;
                    newBottom = temp;
                }
                return new Rect(newLeft, newTop, newRight, newBottom);
                
            default:
                // 0度或其他，不需要調整
                return bbox;
        }
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
        isEnvironmentActivityActive = true;
        if (cameraExecutor == null || cameraExecutor.isShutdown()) {
            cameraExecutor = Executors.newSingleThreadExecutor();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        isEnvironmentActivityActive = false;
        
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
