package com.example.tonbo_app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EnvironmentActivity extends BaseAccessibleActivity {
    private static final String TAG = "EnvironmentActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};

    private PreviewView cameraPreview;
    private DetectionOverlayView detectionOverlay;
    private TextView detectionStatus;
    private TextView detectionResults;
    private Button backButton;
    private Button flashButton;
    private Button startDetectionButton;

    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private boolean isFlashOn = false;
    private boolean isDetecting = false;
    private boolean isDetectionActive = false;

    // 物體檢測相關變量
    private ObjectDetectorHelper objectDetectorHelper;
    private String lastDetectionResult = "";
    private int detectionCount = 0;
    private Bitmap currentBitmap;
    private List<ObjectDetectorHelper.DetectionResult> lastDetections;
    private long lastDetectionTime = 0;
    private boolean isAnalyzing = false;
    private int frameSkipCount = 3; // 每3幀檢測一次，平衡性能和精準度
    private long lastStabilityCheck = 0; // 上次穩定性檢查時間
    
    // 語音播報控制
    private long lastSpeechTime = 0;
    private static final long SPEECH_INTERVAL_MS = 2000; // 語音播報間隔2秒
    
    // 顏色和光線分析
    private ColorLightingAnalyzer colorLightingAnalyzer;
    private ColorLightingAnalyzer.ColorAnalysisResult lastColorAnalysis;
    private ColorLightingAnalyzer.LightingAnalysisResult lastLightingAnalysis;
    private long lastColorAnalysisTime = 0;
    private int colorAnalysisSkipCount = 45; // 每45幀分析一次顏色和光線，減少頻率
    
    // 備用相機實現
    private LegacyCameraHelper legacyCameraHelper;
    private boolean useLegacyCamera = false;
    
    // 記憶體監控
    private long lastMemoryCheck = 0;
    private static final long MEMORY_CHECK_INTERVAL = 10000; // 10秒檢查一次記憶體
    private static final long MEMORY_WARNING_THRESHOLD = 100 * 1024 * 1024; // 100MB警告閾值

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_environment);

        Log.d(TAG, "EnvironmentActivity onCreate開始");
        
        // 強制初始化TTS，確保語音功能可用
        Log.d(TAG, "🔊 強制初始化TTS...");
        if (ttsManager != null) {
            // 觸發TTS初始化
            ttsManager.speak("TTS初始化測試", "TTS initialization test", true);
            Log.d(TAG, "🔊 TTS初始化觸發完成");
        } else {
            Log.e(TAG, "❌ TTS管理器為空！");
        }
        
        // 檢查API版本兼容性
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Log.e(TAG, "Android版本過舊，不支持CameraX");
            announceError("您的Android版本過舊，請升級到Android 5.0或更高版本");
            Toast.makeText(this, "Android版本過舊，請升級系統", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        initViews();
        cameraExecutor = Executors.newSingleThreadExecutor();
        
        // 初始化物體檢測器
        objectDetectorHelper = new ObjectDetectorHelper(this);
        colorLightingAnalyzer = new ColorLightingAnalyzer();

        // 檢查相機權限
        Log.d(TAG, "檢查相機權限...");
        if (allPermissionsGranted()) {
            Log.d(TAG, "相機權限已授予，開始啟動相機");
            startCameraWithFallback();
        } else {
            Log.d(TAG, "相機權限未授予，請求權限");
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    @Override
    protected void announcePageTitle() {
        Log.d(TAG, "🔊 EnvironmentActivity announcePageTitle 被調用");
        Log.d(TAG, "🔊 當前語言: " + currentLanguage);
        Log.d(TAG, "🔊 TTS管理器狀態: " + (ttsManager != null ? "已初始化" : "未初始化"));
        
        // 使用更簡單的方式，直接播放頁面標題
        if (ttsManager != null) {
            Log.d(TAG, "🔊 直接播放頁面標題");
            
            // 根據當前語言播放對應的語音
            if ("english".equals(currentLanguage)) {
                ttsManager.speak("", "Current page: Environment Recognition", true);
            } else if ("mandarin".equals(currentLanguage)) {
                ttsManager.speak("", "當前頁面：環境識別", true);
            } else {
                // 默認廣東話
                ttsManager.speak("當前頁面：環境識別", "", true);
            }
            
            // 延遲播放相機啟動提示，確保頁面標題播放完成
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                Log.d(TAG, "🔊 播放相機啟動提示");
                if ("english".equals(currentLanguage)) {
                    ttsManager.speak("", "Camera started, ready for detection", false);
                } else if ("mandarin".equals(currentLanguage)) {
                    ttsManager.speak("", "相機已啟動，準備檢測", false);
                } else {
                    // 默認廣東話
                    ttsManager.speak("相機已啟動，準備檢測", "", false);
                }
                
                // 延遲開始檢測，確保語音播放完成
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    Log.d(TAG, "🔊 語音播放完成，等待用戶點擊開始檢測");
                    // 不再自動開始檢測，等待用戶點擊開始按鈕
                }, 3000); // 再延遲3秒
                
            }, 4000); // 延遲4秒，確保頁面標題播放完成
            
        } else {
            Log.e(TAG, "❌ TTS管理器為空，無法播放頁面標題");
        }
    }

    private void initViews() {
        cameraPreview = findViewById(R.id.cameraPreview);
        detectionOverlay = findViewById(R.id.detectionOverlay);
        detectionStatus = findViewById(R.id.detectionStatus);
        detectionResults = findViewById(R.id.detectionResults);
        backButton = findViewById(R.id.backButton);
        flashButton = findViewById(R.id.flashButton);
        startDetectionButton = findViewById(R.id.startDetectionButton);

        // 設置按鈕點擊事件
        backButton.setOnClickListener(v -> {
            announceNavigation(getString(R.string.go_home_announcement));
            finish();
        });

        flashButton.setOnClickListener(v -> toggleFlash());

        startDetectionButton.setOnClickListener(v -> toggleDetection());

        // 設置無障礙支持
        backButton.setContentDescription(getString(R.string.back_to_home));
        flashButton.setContentDescription(getString(R.string.flash_button_desc));
        startDetectionButton.setContentDescription(getString(R.string.start_detection_desc));
        
        // 初始化按鈕文字
        updateButtonText();


        // 移除了語音播報和清除顯示按鈕，因為已有實時報讀功能
    }
    
    /**
     * 更新按鈕文字和描述
     */
    private void updateButtonText() {
        if (startDetectionButton != null) {
            if (isDetectionActive) {
                startDetectionButton.setText(getString(R.string.stop_detection));
                startDetectionButton.setContentDescription(getString(R.string.stop_detection_desc));
            } else {
                startDetectionButton.setText(getString(R.string.start_detection));
                startDetectionButton.setContentDescription(getString(R.string.start_detection_desc));
            }
        }
    }
    
    /**
     * 切換檢測狀態
     */
    private void toggleDetection() {
        vibrationManager.vibrateClick();
        
        if (isDetectionActive) {
            // 停止檢測
            stopDetection();
        } else {
            // 開始檢測
            startDetection();
        }
    }
    
    /**
     * 開始檢測
     */
    private void startDetection() {
        Log.d(TAG, "🔊 開始檢測");
        isDetectionActive = true;
        
        // 更新按鈕文字
        updateButtonText();
        
        // 更新狀態文字
        detectionStatus.setText(getString(R.string.detection_started));
        
        // 播放開始檢測語音
        if ("english".equals(currentLanguage)) {
            announceInfo("Detection started");
        } else if ("mandarin".equals(currentLanguage)) {
            announceInfo("檢測已開始");
        } else {
            // 默認廣東話
            announceInfo("檢測已開始");
        }
        
        // 清除之前的檢測結果
        detectionResults.setText(getString(R.string.point_to_objects_instruction));
        lastDetectionResult = "";
    }
    
    /**
     * 停止檢測
     */
    private void stopDetection() {
        Log.d(TAG, "🔊 停止檢測");
        isDetectionActive = false;
        
        // 更新按鈕文字
        updateButtonText();
        
        // 更新狀態文字
        detectionStatus.setText(getString(R.string.detection_stopped));
        
        // 播放停止檢測語音
        if ("english".equals(currentLanguage)) {
            announceInfo("Detection stopped");
        } else if ("mandarin".equals(currentLanguage)) {
            announceInfo("檢測已停止");
        } else {
            // 默認廣東話
            announceInfo("檢測已停止");
        }
        
        // 清除檢測結果
        detectionResults.setText(getString(R.string.point_to_objects_instruction));
        lastDetectionResult = "";
        
        // 清除檢測覆蓋層
        if (detectionOverlay != null) {
            detectionOverlay.clearDetections();
        }
    }
    

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            int permissionStatus = ContextCompat.checkSelfPermission(this, permission);
            Log.d(TAG, "權限檢查: " + permission + " = " + permissionStatus);
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "權限未授予: " + permission);
                return false;
            }
        }
        Log.d(TAG, "所有權限已授予");
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "權限請求結果: requestCode=" + requestCode);
        
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            for (int i = 0; i < permissions.length; i++) {
                Log.d(TAG, "權限結果: " + permissions[i] + " = " + grantResults[i]);
            }
            
            if (allPermissionsGranted()) {
                Log.d(TAG, "權限授予成功，開始啟動相機");
                startCameraWithFallback();
            } else {
                Log.e(TAG, "權限被拒絕");
                announceError("需要相機權限才能使用環境識別功能");
                Toast.makeText(this, "需要相機權限才能使用此功能", Toast.LENGTH_LONG).show();
                
                // 更新UI顯示權限錯誤
                runOnUiThread(() -> {
                    updateDetectionStatus(getString(R.string.camera_permission_needed));
                    updateDetectionResults(getString(R.string.camera_permission_message));
                });
                
                // 延遲3秒後返回主頁
                new android.os.Handler().postDelayed(() -> {
                    finish();
                }, 3000);
            }
        }
    }

    /**
     * 帶備用方案的相機啟動
     */
    private void startCameraWithFallback() {
        Log.d(TAG, "開始啟動相機（帶備用方案）...");
        
        try {
            startCamera();
        } catch (NoSuchMethodError e) {
            Log.w(TAG, "CameraX不兼容，嘗試使用傳統相機API");
            startLegacyCamera();
        } catch (Exception e) {
            Log.w(TAG, "CameraX啟動失敗，嘗試使用傳統相機API: " + e.getMessage());
            startLegacyCamera();
        }
    }
    
    /**
     * 啟動傳統相機API
     */
    private void startLegacyCamera() {
        Log.d(TAG, "啟動傳統相機API...");
        
        try {
            legacyCameraHelper = new LegacyCameraHelper(this);
            if (legacyCameraHelper.initializeCamera()) {
                useLegacyCamera = true;
                // 相機啟動成功，但不播放語音，避免與頁面標題衝突
                Log.d(TAG, "相機啟動成功（兼容模式）");
                updateDetectionStatus(getString(R.string.camera_started_compatibility));
                updateDetectionResults(getString(R.string.camera_compatibility_message));
                Log.d(TAG, "傳統相機啟動成功");
            } else {
                announceError("所有相機模式都無法啟動");
                updateDetectionStatus(getString(R.string.camera_start_failed));
                updateDetectionResults(getString(R.string.camera_start_error));
            }
        } catch (Exception e) {
            Log.e(TAG, "傳統相機啟動失敗: " + e.getMessage());
            announceError("所有相機模式都無法啟動");
            updateDetectionStatus(getString(R.string.camera_start_failed));
            updateDetectionResults(getString(R.string.camera_bind_error, e.getMessage()));
        }
    }

    private void startCamera() {
        Log.d(TAG, "開始啟動相機...");
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                Log.d(TAG, "ProcessCameraProvider獲取成功");
                bindCameraUseCases();
                // 相機啟動成功，但不播放語音，避免與頁面標題衝突
                Log.d(TAG, "相機啟動成功（CameraX模式）");
            } catch (NoSuchMethodError e) {
                Log.e(TAG, "相機API兼容性錯誤: " + e.getMessage());
                e.printStackTrace();
                
                // 嘗試使用傳統相機API
                runOnUiThread(() -> startLegacyCamera());
                
            } catch (Exception e) {
                Log.e(TAG, "相機啟動失敗: " + e.getMessage());
                e.printStackTrace();
                
                // 嘗試使用傳統相機API
                runOnUiThread(() -> startLegacyCamera());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        Log.d(TAG, "開始綁定相機用例...");
        
        if (cameraProvider == null) {
            Log.e(TAG, "cameraProvider為null，無法綁定相機");
            return;
        }
        
        if (cameraPreview == null) {
            Log.e(TAG, "cameraPreview為null，無法設置預覽");
            return;
        }
        
        try {
            // Preview
            Preview preview = new Preview.Builder().build();
            preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());
            Log.d(TAG, "Preview設置完成");

            // Image Analysis for YOLO detection
            ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();

            imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);
            Log.d(TAG, "ImageAnalysis設置完成");

            // Camera selector
            CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
            Log.d(TAG, "CameraSelector設置完成");

            // Unbind all use cases before rebinding
            cameraProvider.unbindAll();
            Log.d(TAG, "已解除所有相機綁定");

            // Bind use cases to camera
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            Log.d(TAG, "相機綁定成功");

            isDetecting = true;
            updateDetectionStatus(getString(R.string.detecting_environment));
            Log.d(TAG, "相機初始化完成，開始檢測");

        } catch (NoSuchMethodError e) {
            Log.e(TAG, "API兼容性錯誤: " + e.getMessage());
            e.printStackTrace();
            announceError("相機API不兼容，請更新Android系統");
            
            runOnUiThread(() -> {
                updateDetectionStatus(getString(R.string.camera_api_incompatible));
                updateDetectionResults(getString(R.string.camera_api_error));
            });
        } catch (Exception e) {
            Log.e(TAG, "綁定相機失敗: " + e.getMessage());
            e.printStackTrace();
            announceError("相機設置失敗: " + e.getMessage());
            
            runOnUiThread(() -> {
                updateDetectionStatus(getString(R.string.camera_bind_failed));
                updateDetectionResults(getString(R.string.camera_bind_error, e.getMessage()));
            });
        }
    }

    private void analyzeImage(ImageProxy image) {
        try {
            // 只有在檢測激活時才進行分析
            if (!isDetectionActive) {
                image.close();
                return;
            }
            
            detectionCount++;
            
            // 定期檢查記憶體使用情況
            checkMemoryUsage();
            
            // 定期檢查檢測器穩定性
            if (System.currentTimeMillis() - lastStabilityCheck > 5000) { // 每5秒檢查一次
                checkDetectorStability();
                lastStabilityCheck = System.currentTimeMillis();
            }
            
            // 跳過幀以提高性能，並且避免同時進行多個檢測
            if (detectionCount % frameSkipCount == 0 && 
                objectDetectorHelper != null && 
                !isAnalyzing) {
                
                isAnalyzing = true;
                
                // 將ImageProxy轉換為Bitmap（在後台線程）
                Bitmap bitmap = imageProxyToBitmap(image);
                
                if (bitmap != null) {
                    // 保存當前幀並回收舊的bitmap
                    if (currentBitmap != null && !currentBitmap.isRecycled()) {
                        currentBitmap.recycle();
                    }
                    currentBitmap = bitmap;
                    
                    // 在後台線程執行檢測，避免阻塞相機預覽
                    cameraExecutor.execute(() -> {
                        try {
                            long startTime = System.currentTimeMillis();
                            
                            // 執行物體檢測
                            List<ObjectDetectorHelper.DetectionResult> results = 
                                    objectDetectorHelper.detect(bitmap);
                            
                            long detectionTime = System.currentTimeMillis() - startTime;
                            lastDetections = results;
                            lastDetectionTime = detectionTime;
                            
                            // 更新UI
                            if (!results.isEmpty()) {
                                String resultText = formatDetailedResults(results);
                                String speechText = objectDetectorHelper.formatResultsForSpeech(results);
                                
                                runOnUiThread(() -> {
                                    Log.d(TAG, "更新UI，檢測結果數量: " + results.size());
                                    
                                    // 更新覆蓋層顯示檢測框
                                    detectionOverlay.updateDetections(results);
                                    
                                    // 添加測試邊界框（用於調試）
                                    if (results.isEmpty()) {
                                        Log.d(TAG, "沒有檢測到物體，添加測試邊界框");
                                        addTestBoundingBox();
                                    }
                                    
                                    updateDetectionResults(resultText);
                                    updateDetectionStatus(String.format(
                                        getString(R.string.detection_status_format), 
                                        results.size(), 
                                        (int)detectionTime
                                    ));
                                    
                                    // 實時語音播報檢測結果（優化版本）
                                    if (!speechText.equals(lastDetectionResult)) {
                                        lastDetectionResult = speechText;
                                        Log.d(TAG, "🔊 檢測到新物體，準備播報語音: " + speechText);
                                        
                                        // 檢查語音播報間隔，避免過於頻繁
                                        long currentTime = System.currentTimeMillis();
                                        if (currentTime - lastSpeechTime >= SPEECH_INTERVAL_MS) {
                                            lastSpeechTime = currentTime;
                                            // 立即播報檢測結果
                                            speakDetectionResultsImmediate(speechText);
                                        } else {
                                            Log.d(TAG, "🔊 語音播報間隔太短，跳過此次播報");
                                        }
                                    } else {
                                        Log.d(TAG, "🔊 檢測結果與上次相同，跳過語音播報");
                                    }
                                    
                                    // 定期進行顏色和光線分析
                                    if (detectionCount % colorAnalysisSkipCount == 0) {
                                        performColorLightingAnalysis(bitmap);
                                    }
                                });
                            } else {
                                runOnUiThread(() -> {
                                    // 清除覆蓋層
                                    detectionOverlay.clearDetections();
                                    updateDetectionStatus(getString(R.string.detection_no_objects));
                                });
                            }
                            
                        } catch (Exception e) {
                            Log.e(TAG, "檢測失敗: " + e.getMessage());
                        } finally {
                            // 回收處理完的bitmap
                            if (bitmap != null && !bitmap.isRecycled()) {
                                bitmap.recycle();
                            }
                            isAnalyzing = false;
                        }
                    });
                } else {
                    isAnalyzing = false;
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "圖像分析失敗: " + e.getMessage());
            isAnalyzing = false;
        } finally {
            image.close();
        }
    }
    
    /**
     * 執行顏色和光線分析
     */
    private void performColorLightingAnalysis(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled() || colorLightingAnalyzer == null) {
            return;
        }
        
        // 在新線程中執行分析
        cameraExecutor.execute(() -> {
            try {
                // 顏色分析
                ColorLightingAnalyzer.ColorAnalysisResult colorResult = colorLightingAnalyzer.analyzeColors(bitmap);
                
                // 光線分析
                ColorLightingAnalyzer.LightingAnalysisResult lightingResult = colorLightingAnalyzer.analyzeLighting(bitmap);
                
                // 更新UI
                runOnUiThread(() -> {
                    lastColorAnalysis = colorResult;
                    lastLightingAnalysis = lightingResult;
                    lastColorAnalysisTime = System.currentTimeMillis();
                    
                    // 更新檢測結果顯示
                    updateEnvironmentDescription();
                    
                    Log.d(TAG, String.format("顏色分析: %s + %s (%s), 光線: %s", 
                        colorResult.getPrimaryColor(),
                        colorResult.getSecondaryColor(),
                        colorResult.getDominantTone(),
                        lightingResult.getLightingCondition()));
                });
                
            } catch (Exception e) {
                Log.e(TAG, "顏色光線分析失敗: " + e.getMessage());
            }
        });
    }
    
    /**
     * 更新環境描述
     */
    private void updateEnvironmentDescription() {
        StringBuilder description = new StringBuilder();
        
        // 物體檢測結果
        if (lastDetections != null && !lastDetections.isEmpty()) {
            description.append("檢測到物體: ");
            for (int i = 0; i < Math.min(lastDetections.size(), 3); i++) {
                if (i > 0) description.append(", ");
                description.append(lastDetections.get(i).getLabelZh());
            }
            description.append("\n");
        }
        
        // 顏色分析結果
        if (lastColorAnalysis != null) {
            description.append("主要顏色: ");
            if (lastColorAnalysis.getPrimaryColor() != null) {
                description.append(lastColorAnalysis.getPrimaryColor());
            }
            if (lastColorAnalysis.getSecondaryColor() != null) {
                description.append(" + ").append(lastColorAnalysis.getSecondaryColor());
            }
            if (lastColorAnalysis.getDominantTone() != null) {
                description.append(" (").append(lastColorAnalysis.getDominantTone()).append(")");
            }
            description.append("\n");
        }
        
        // 光線分析結果
        if (lastLightingAnalysis != null) {
            description.append("光線條件: ").append(lastLightingAnalysis.getLightingCondition());
            if (lastLightingAnalysis.getLightDirection() != null) {
                description.append(", ").append(lastLightingAnalysis.getLightDirection());
            }
        }
        
        String finalDescription = description.toString().trim();
        if (!finalDescription.isEmpty()) {
            updateDetectionResults(finalDescription);
        }
    }
    
    /**
     * 格式化詳細檢測結果
     */
    private String formatDetailedResults(List<ObjectDetectorHelper.DetectionResult> results) {
        if (results.isEmpty()) {
            return getString(R.string.no_objects_detected);
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(getString(R.string.detection_results_format), results.size()));
        
        for (int i = 0; i < Math.min(results.size(), 10); i++) {
            ObjectDetectorHelper.DetectionResult result = results.get(i);
            String label = getObjectLabel(result);
            sb.append(String.format("%d. %s (%.0f%%)\n", 
                i + 1, 
                label, 
                result.getConfidence() * 100
            ));
        }
        
        if (results.size() > 10) {
            sb.append(String.format("\n" + getString(R.string.more_objects_format), results.size() - 10));
        }
        
        return sb.toString();
    }
    
    private String getObjectLabel(ObjectDetectorHelper.DetectionResult result) {
        // 根據當前語言選擇對應的標籤
        switch (currentLanguage) {
            case "english":
                return result.getLabel() != null ? result.getLabel() : result.getLabelZh();
            case "mandarin":
                return result.getLabelZh() != null ? result.getLabelZh() : result.getLabel();
            case "cantonese":
            default:
                return result.getLabelZh() != null ? result.getLabelZh() : result.getLabel();
        }
    }
    
    private Bitmap imageProxyToBitmap(ImageProxy image) {
        java.io.ByteArrayOutputStream out = null;
        try {
            ImageProxy.PlaneProxy[] planes = image.getPlanes();
            ByteBuffer yBuffer = planes[0].getBuffer();
            ByteBuffer uBuffer = planes[1].getBuffer();
            ByteBuffer vBuffer = planes[2].getBuffer();

            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            byte[] nv21 = new byte[ySize + uSize + vSize];
            yBuffer.get(nv21, 0, ySize);
            vBuffer.get(nv21, ySize, vSize);
            uBuffer.get(nv21, ySize + vSize, uSize);

            android.graphics.YuvImage yuvImage = new android.graphics.YuvImage(
                    nv21, android.graphics.ImageFormat.NV21, 
                    image.getWidth(), image.getHeight(), null);
            
            out = new java.io.ByteArrayOutputStream();
            yuvImage.compressToJpeg(
                    new android.graphics.Rect(0, 0, image.getWidth(), image.getHeight()), 
                    85, out); // 降低JPEG質量以節省記憶體
            byte[] imageBytes = out.toByteArray();
            return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        } catch (Exception e) {
            Log.e(TAG, "圖像轉換失敗: " + e.getMessage());
            return null;
        } finally {
            // 確保ByteArrayOutputStream被正確關閉
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    Log.w(TAG, "關閉ByteArrayOutputStream失敗: " + e.getMessage());
                }
            }
        }
    }


    private void speakDetectionResults() {
        StringBuilder fullDescription = new StringBuilder();
        
        // 物體檢測結果
        if (lastDetectionResult != null && !lastDetectionResult.isEmpty()) {
            fullDescription.append("偵測到物體：").append(lastDetectionResult).append("。");
        }
        
        // 顏色分析結果
        if (lastColorAnalysis != null) {
            fullDescription.append("主要顏色是");
            if (lastColorAnalysis.getPrimaryColor() != null) {
                fullDescription.append(lastColorAnalysis.getPrimaryColor());
            }
            if (lastColorAnalysis.getSecondaryColor() != null) {
                fullDescription.append("和").append(lastColorAnalysis.getSecondaryColor());
            }
            if (lastColorAnalysis.getDominantTone() != null) {
                fullDescription.append("，整體是").append(lastColorAnalysis.getDominantTone());
            }
            fullDescription.append("。");
        }
        
        // 光線分析結果
        if (lastLightingAnalysis != null) {
            fullDescription.append("光線條件是").append(lastLightingAnalysis.getLightingCondition());
            if (lastLightingAnalysis.getLightDirection() != null) {
                fullDescription.append("，光線來自").append(lastLightingAnalysis.getLightDirection());
            }
            fullDescription.append("。");
        }
        
        String descriptionText = fullDescription.toString();
        if (descriptionText.isEmpty()) {
            descriptionText = getString(R.string.no_objects_detected);
        }
        
        // 根據當前語言選擇對應的描述文字和語音
        if ("english".equals(currentLanguage)) {
            // 英文版本：使用英文文字和英語語音
            ttsManager.speak(descriptionText, null, true);
        } else if ("mandarin".equals(currentLanguage)) {
            // 普通話版本：使用簡體中文文字和普通話語音
            String simplifiedText = translateToSimplifiedChinese(descriptionText);
            ttsManager.speak(simplifiedText, null, true);
        } else {
            // 廣東話版本：使用繁體中文文字和廣東話語音
            ttsManager.speak(descriptionText, null, true);
        }
        vibrationManager.vibrateSuccess();
    }

    /**
     * 將英文描述翻譯為中文
     */
    private String translateToChinese(String english) {
        // 簡單的翻譯映射
        return english
                .replace("table", "桌子")
                .replace("chair", "椅子")
                .replace("person", "人")
                .replace("keyboard", "鍵盤")
                .replace("mouse", "滑鼠")
                .replace("monitor", "螢幕")
                .replace("laptop", "筆記本電腦")
                .replace("phone", "手機")
                .replace("cup", "杯子")
                .replace("bottle", "瓶子")
                .replace("book", "書")
                .replace("desk", "桌子")
                .replace("window", "窗戶")
                .replace("door", "門")
                .replace("wall", "牆")
                .replace("floor", "地板")
                .replace("light", "燈")
                .replace("lamp", "燈")
                .replace("detected", "偵測到")
                .replace("objects", "物體")
                .replace("object", "物體")
                .replace("No objects detected", "未偵測到物體")
                .replace("confidence", "信心度")
                .replace("location", "位置")
                .replace("center", "中央")
                .replace("left", "左邊")
                .replace("right", "右邊")
                .replace("top", "上方")
                .replace("bottom", "下方")
                .replace("front", "前方")
                .replace("back", "後方")
                .replace("large", "大型")
                .replace("small", "小型")
                .replace("bright", "明亮")
                .replace("dark", "黑暗")
                .replace("wooden", "木製")
                .replace("plastic", "塑膠")
                .replace("metal", "金屬")
                .replace("glass", "玻璃")
                .replace("red", "紅色")
                .replace("blue", "藍色")
                .replace("green", "綠色")
                .replace("yellow", "黃色")
                .replace("black", "黑色")
                .replace("white", "白色");
    }

    /**
     * 將繁體中文翻譯為簡體中文
     */
    private String translateToSimplifiedChinese(String traditionalText) {
        // 簡化的繁體轉簡體映射
        return traditionalText
                .replace("偵測", "检测")
                .replace("物體", "物体")
                .replace("主要", "主要")
                .replace("顏色", "颜色")
                .replace("整體", "整体")
                .replace("光線", "光线")
                .replace("條件", "条件")
                .replace("來自", "来自")
                .replace("紅色", "红色")
                .replace("藍色", "蓝色")
                .replace("綠色", "绿色")
                .replace("黃色", "黄色")
                .replace("橙色", "橙色")
                .replace("紫色", "紫色")
                .replace("黑色", "黑色")
                .replace("白色", "白色")
                .replace("灰色", "灰色")
                .replace("桌子", "桌子")
                .replace("椅子", "椅子")
                .replace("人", "人")
                .replace("門", "门")
                .replace("窗", "窗")
                .replace("牆", "墙")
                .replace("地板", "地板")
                .replace("天花板", "天花板");
    }

    /**
     * 將環境描述翻譯為英文
     */
    private String translateEnvironmentDescriptionToEnglish(String cantoneseText) {
        // 簡化的翻譯映射
        String englishText = cantoneseText
            .replace("偵測到物體", "Detected objects")
            .replace("主要顏色是", "Main colors are")
            .replace("整體是", "overall tone is")
            .replace("光線條件是", "lighting condition is")
            .replace("光線來自", "light comes from")
            .replace("紅色", "red")
            .replace("藍色", "blue")
            .replace("綠色", "green")
            .replace("黃色", "yellow")
            .replace("橙色", "orange")
            .replace("紫色", "purple")
            .replace("黑色", "black")
            .replace("白色", "white")
            .replace("灰色", "gray")
            .replace("暖色調", "warm tone")
            .replace("冷色調", "cool tone")
            .replace("中性色調", "neutral tone")
            .replace("明亮環境", "bright environment")
            .replace("昏暗環境", "dark environment")
            .replace("正常光線", "normal lighting")
            .replace("高對比", "high contrast")
            .replace("左側光線", "left side lighting")
            .replace("右側光線", "right side lighting")
            .replace("頂部光線", "top lighting")
            .replace("底部光線", "bottom lighting")
            .replace("均勻光線", "even lighting")
            .replace("尚未偵測到任何物體", "No objects detected yet");
        
        return englishText;
    }

    private void toggleFlash() {
        isFlashOn = !isFlashOn;
        // TODO: 實現閃光燈控制
        String status = isFlashOn ? getString(R.string.flash_on) : getString(R.string.flash_off);
        announceInfo(status);
        flashButton.setText(isFlashOn ? "🔦" : "💡");
    }




    private void updateDetectionStatus(String status) {
        runOnUiThread(() -> {
            detectionStatus.setText(status);
            detectionStatus.setContentDescription(getString(R.string.detection_status_prefix) + status);
        });
    }

    private void updateDetectionResults(String results) {
        lastDetectionResult = results;
        runOnUiThread(() -> {
            detectionResults.setText(results);
            detectionResults.setContentDescription(getString(R.string.detection_results_prefix) + results);
        });
    }

    private String translateToEnglish(String chinese) {
        // 簡單的翻譯映射
        return chinese
                .replace("桌子", "table")
                .replace("椅子", "chair")
                .replace("杯子", "cup")
                .replace("手機", "phone")
                .replace("電腦", "computer")
                .replace("人", "person");
    }
    
    /**
     * 檢查記憶體使用情況
     */
    private void checkMemoryUsage() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMemoryCheck > MEMORY_CHECK_INTERVAL) {
            lastMemoryCheck = currentTime;
            
            // 獲取記憶體信息
            android.app.ActivityManager activityManager = 
                (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
            android.app.ActivityManager.MemoryInfo memoryInfo = new android.app.ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);
            
            long usedMemory = memoryInfo.totalMem - memoryInfo.availMem;
            
            Log.d(TAG, String.format("記憶體使用情況: %.1fMB / %.1fMB (%.1f%%)", 
                usedMemory / (1024.0 * 1024.0),
                memoryInfo.totalMem / (1024.0 * 1024.0),
                (double)usedMemory / memoryInfo.totalMem * 100));
            
            // 如果記憶體使用過高，觸發垃圾回收
            if (usedMemory > MEMORY_WARNING_THRESHOLD) {
                Log.w(TAG, "記憶體使用過高，觸發垃圾回收");
                System.gc();
                
                // 可以選擇性地清理一些資源
                if (currentBitmap != null && !currentBitmap.isRecycled()) {
                    currentBitmap.recycle();
                    currentBitmap = null;
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        Log.d(TAG, "開始清理資源...");
        
        // 停止檢測
        isDetecting = false;
        isAnalyzing = false;
        
        // 關閉相機執行器
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
            try {
                if (!cameraExecutor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                    cameraExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cameraExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            cameraExecutor = null;
        }
        
        // 解除相機綁定
        if (cameraProvider != null) {
            try {
                cameraProvider.unbindAll();
                cameraProvider = null;
            } catch (Exception e) {
                Log.e(TAG, "解除相機綁定失敗: " + e.getMessage());
            }
        }
        
        // 關閉物體檢測器
        if (objectDetectorHelper != null) {
            objectDetectorHelper.close();
            objectDetectorHelper = null;
        }
        
        // 回收bitmap
        if (currentBitmap != null && !currentBitmap.isRecycled()) {
            currentBitmap.recycle();
            currentBitmap = null;
        }
        
        // 清理其他引用
        lastDetections = null;
        lastDetectionResult = "";
        
        // 清理顏色光線分析器
        if (colorLightingAnalyzer != null) {
            colorLightingAnalyzer = null;
        }
        
        // 清理分析結果
        lastColorAnalysis = null;
        lastLightingAnalysis = null;
        
        Log.d(TAG, "資源清理完成");
    }

    @Override
    protected void onPause() {
        super.onPause();
        isDetecting = false;
        
        // 暫停相機以節省資源
        if (cameraProvider != null) {
            try {
                cameraProvider.unbindAll();
                Log.d(TAG, "相機已暫停");
            } catch (Exception e) {
                Log.e(TAG, "暫停相機失敗: " + e.getMessage());
            }
        }
        
        // 回收當前bitmap
        if (currentBitmap != null && !currentBitmap.isRecycled()) {
            currentBitmap.recycle();
            currentBitmap = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (allPermissionsGranted()) {
            if (cameraProvider != null) {
                try {
                    bindCameraUseCases();
                    isDetecting = true;
                    Log.d(TAG, "相機已恢復");
                } catch (Exception e) {
                    Log.e(TAG, "恢復相機失敗: " + e.getMessage());
                }
            } else {
                // 如果cameraProvider為null，重新啟動相機
                startCamera();
            }
        }
    }
    
    /**
     * 檢查檢測器穩定性
     */
    private void checkDetectorStability() {
        if (objectDetectorHelper != null) {
            boolean isHealthy = objectDetectorHelper.isHealthy();
            String stats = objectDetectorHelper.getStabilityStats();
            
            Log.d(TAG, "檢測器穩定性檢查: " + (isHealthy ? "健康" : "異常"));
            Log.d(TAG, stats);
            
            if (!isHealthy) {
                Log.w(TAG, "檢測器狀態異常，嘗試重置");
                objectDetectorHelper.forceReset();
                announceInfo("檢測器狀態已重置，正在恢復正常檢測");
            }
        }
    }
    
    /**
     * 立即語音播報檢測結果（優化版本）
     */
    private void speakDetectionResultsImmediate(String speechText) {
        Log.d(TAG, "🔊 speakDetectionResultsImmediate 被調用，speechText: " + speechText);
        
        if (ttsManager != null && speechText != null && !speechText.isEmpty()) {
            // 直接播報檢測結果，不添加前綴，讓語音更簡潔
            Log.d(TAG, "🔊 立即播報檢測結果: " + speechText);
            
            // 根據當前語言選擇對應的語音內容
            String cantoneseText = currentLanguage.equals("english") ? translateToChinese(speechText) : speechText;
            String englishText = currentLanguage.equals("english") ? speechText : translateToEnglish(speechText);
            
            // 使用優先播放，確保檢測結果語音不被其他語音打斷
            ttsManager.speak(cantoneseText, englishText, true);
            
            // 震動反饋
            if (vibrationManager != null) {
                vibrationManager.vibrateClick();
            }
        } else {
            Log.w(TAG, "❌ 立即語音播報條件不滿足");
        }
    }
    
    /**
     * 語音播報檢測結果（原版本，保留用於其他場景）
     */
    private void speakDetectionResults(String speechText) {
        Log.d(TAG, "🔊 speakDetectionResults 被調用，speechText: " + speechText);
        Log.d(TAG, "🔊 ttsManager 狀態: " + (ttsManager != null ? "已初始化" : "未初始化"));
        Log.d(TAG, "🔊 當前語言: " + currentLanguage);
        
        if (ttsManager != null && speechText != null && !speechText.isEmpty()) {
            Log.d(TAG, "語音播報檢測結果: " + speechText);
            
            // 添加環境描述前綴
            String fullSpeechText = getEnvironmentDescriptionPrefix() + speechText;
            Log.d(TAG, "🔊 完整語音文本: " + fullSpeechText);
            
            // 播報檢測結果 - 使用優先播放確保檢測結果語音不被其他語音打斷
            // 根據當前語言選擇對應的語音內容
            String cantoneseText = currentLanguage.equals("english") ? translateToChinese(fullSpeechText) : fullSpeechText;
            String englishText = currentLanguage.equals("english") ? fullSpeechText : translateToEnglish(fullSpeechText);
            ttsManager.speak(cantoneseText, englishText, true);
            
            // 震動反饋
            if (vibrationManager != null) {
                vibrationManager.vibrateClick();
            }
        } else {
            Log.w(TAG, "❌ 語音播報條件不滿足 - ttsManager: " + (ttsManager != null) + 
                  ", speechText: " + speechText + ", isEmpty: " + (speechText != null && speechText.isEmpty()));
        }
    }
    
    /**
     * 獲取環境描述前綴
     */
    private String getEnvironmentDescriptionPrefix() {
        String currentLang = LocaleManager.getInstance(this).getCurrentLanguage();
        
        switch (currentLang) {
            case "english":
                return "Environment detected: ";
            case "mandarin":
                return "環境檢測到：";
            case "cantonese":
            default:
                return "環境檢測到：";
        }
    }
    
    /**
     * 添加測試邊界框（用於調試邊界框顯示問題）
     */
    private void addTestBoundingBox() {
        Log.d(TAG, "添加測試邊界框");
        
        // 創建一個測試檢測結果
        List<ObjectDetectorHelper.DetectionResult> testResults = new ArrayList<>();
        
        // 在屏幕中央添加一個大的測試邊界框（像素座標）
        android.graphics.RectF testBoundingBox = new android.graphics.RectF(200.0f, 200.0f, 600.0f, 500.0f);
        ObjectDetectorHelper.DetectionResult testDetection = new ObjectDetectorHelper.DetectionResult(
            "test", "測試邊界框", 0.9f, testBoundingBox
        );
        testResults.add(testDetection);
        
        // 添加第二個測試邊界框
        android.graphics.RectF testBoundingBox2 = new android.graphics.RectF(800.0f, 300.0f, 1200.0f, 600.0f);
        ObjectDetectorHelper.DetectionResult testDetection2 = new ObjectDetectorHelper.DetectionResult(
            "test2", "測試邊界框2", 0.85f, testBoundingBox2
        );
        testResults.add(testDetection2);
        
        // 更新覆蓋層
        detectionOverlay.updateDetections(testResults);
        
        Log.d(TAG, "測試邊界框已添加 - 兩個大邊界框");
    }
}
