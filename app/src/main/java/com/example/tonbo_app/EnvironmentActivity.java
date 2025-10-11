package com.example.tonbo_app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
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
    private Button speakButton;
    private Button clearButton;

    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private boolean isFlashOn = false;
    private boolean isDetecting = false;

    // 物體檢測相關變量
    private ObjectDetectorHelper objectDetectorHelper;
    private String lastDetectionResult = "";
    private int detectionCount = 0;
    private Bitmap currentBitmap;
    private List<ObjectDetectorHelper.DetectionResult> lastDetections;
    private long lastDetectionTime = 0;
    private boolean isAnalyzing = false;
    private int frameSkipCount = 3; // 每3幀檢測一次，實時響應

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_environment);

        initViews();
        cameraExecutor = Executors.newSingleThreadExecutor();
        
        // 初始化物體檢測器
        objectDetectorHelper = new ObjectDetectorHelper(this);

        // 檢查相機權限
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    @Override
    protected void announcePageTitle() {
        announcePageTitle("實時環境識別頁面");
        announceNavigation("相機已啟動，系統正在實時分析畫面並顯示檢測框。將相機對準物體即可看到識別結果");
    }

    private void initViews() {
        cameraPreview = findViewById(R.id.cameraPreview);
        detectionOverlay = findViewById(R.id.detectionOverlay);
        detectionStatus = findViewById(R.id.detectionStatus);
        detectionResults = findViewById(R.id.detectionResults);
        backButton = findViewById(R.id.backButton);
        flashButton = findViewById(R.id.flashButton);
        speakButton = findViewById(R.id.speakButton);
        clearButton = findViewById(R.id.clearButton);

        // 返回按鈕
        backButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            announceNavigation("返回主頁");
            finish();
        });

        // 閃光燈按鈕
        flashButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            toggleFlash();
        });

        // 語音播報按鈕
        speakButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            speakDetectionResults();
        });
        
        // 清除顯示按鈕
        clearButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            clearDetectionDisplay();
        });
    }
    
    /**
     * 清除檢測顯示
     */
    private void clearDetectionDisplay() {
        detectionOverlay.clearDetections();
        updateDetectionResults("已清除檢測顯示");
        updateDetectionStatus("實時檢測中...");
        announceInfo("檢測框已清除，系統繼續實時檢測");
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                announceError("需要相機權限才能使用環境識別功能");
                Toast.makeText(this, "需要相機權限", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
                announceSuccess("相機已啟動，開始偵測環境");
            } catch (Exception e) {
                Log.e(TAG, "相機啟動失敗: " + e.getMessage());
                announceError("相機啟動失敗");
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        // Preview
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

        // Image Analysis for YOLO detection
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

        // Camera selector
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        try {
            // Unbind all use cases before rebinding
            cameraProvider.unbindAll();

            // Bind use cases to camera
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            isDetecting = true;
            updateDetectionStatus("正在偵測環境...");

        } catch (Exception e) {
            Log.e(TAG, "綁定相機失敗: " + e.getMessage());
            announceError("相機設置失敗");
        }
    }

    private void analyzeImage(ImageProxy image) {
        try {
            detectionCount++;
            
            // 跳過幀以提高性能，並且避免同時進行多個檢測
            if (detectionCount % frameSkipCount == 0 && 
                objectDetectorHelper != null && 
                !isAnalyzing) {
                
                isAnalyzing = true;
                
                // 將ImageProxy轉換為Bitmap（在後台線程）
                Bitmap bitmap = imageProxyToBitmap(image);
                
                if (bitmap != null) {
                    // 保存當前幀
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
                                    // 更新覆蓋層顯示檢測框
                                    detectionOverlay.updateDetections(results);
                                    
                                    updateDetectionResults(resultText);
                                    updateDetectionStatus(String.format(
                                        "實時檢測中 - %d個物體 (%.0fms)", 
                                        results.size(), 
                                        (float)detectionTime
                                    ));
                                    
                                    // 只在有新物體時播報（避免重複播報）
                                    if (!speechText.equals(lastDetectionResult)) {
                                        lastDetectionResult = speechText;
                                        // 可選：自動播報
                                        // speakDetectionResults();
                                    }
                                });
                            } else {
                                runOnUiThread(() -> {
                                    // 清除覆蓋層
                                    detectionOverlay.clearDetections();
                                    updateDetectionStatus("實時檢測中 - 未發現物體");
                                });
                            }
                            
                        } catch (Exception e) {
                            Log.e(TAG, "檢測失敗: " + e.getMessage());
                        } finally {
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
     * 格式化詳細檢測結果
     */
    private String formatDetailedResults(List<ObjectDetectorHelper.DetectionResult> results) {
        if (results.isEmpty()) {
            return "未偵測到物體";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("偵測到 %d 個物體：\n\n", results.size()));
        
        for (int i = 0; i < Math.min(results.size(), 10); i++) {
            ObjectDetectorHelper.DetectionResult result = results.get(i);
            sb.append(String.format("%d. %s (%.0f%%)\n", 
                i + 1, 
                result.getLabelZh(), 
                result.getConfidence() * 100
            ));
        }
        
        if (results.size() > 10) {
            sb.append(String.format("\n...還有 %d 個物體", results.size() - 10));
        }
        
        return sb.toString();
    }
    
    private Bitmap imageProxyToBitmap(ImageProxy image) {
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
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            yuvImage.compressToJpeg(
                    new android.graphics.Rect(0, 0, image.getWidth(), image.getHeight()), 
                    100, out);
            byte[] imageBytes = out.toByteArray();
            return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        } catch (Exception e) {
            Log.e(TAG, "圖像轉換失敗: " + e.getMessage());
            return null;
        }
    }


    private void speakDetectionResults() {
        if (lastDetectionResult.isEmpty()) {
            announceInfo("尚未偵測到任何物體");
        } else {
            String cantoneseText = "當前偵測結果：" + lastDetectionResult;
            String englishText = "Current detection: " + translateToEnglish(lastDetectionResult);
            ttsManager.speak(cantoneseText, englishText, true);
            vibrationManager.vibrateSuccess();
        }
    }

    private void toggleFlash() {
        isFlashOn = !isFlashOn;
        // TODO: 實現閃光燈控制
        String status = isFlashOn ? "閃光燈已開啟" : "閃光燈已關閉";
        announceInfo(status);
        flashButton.setText(isFlashOn ? "🔦" : "💡");
    }

    private void updateDetectionStatus(String status) {
        runOnUiThread(() -> {
            detectionStatus.setText(status);
            detectionStatus.setContentDescription("偵測狀態：" + status);
        });
    }

    private void updateDetectionResults(String results) {
        lastDetectionResult = results;
        runOnUiThread(() -> {
            detectionResults.setText(results);
            detectionResults.setContentDescription("偵測結果：" + results);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        if (objectDetectorHelper != null) {
            objectDetectorHelper.close();
        }
        isDetecting = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isDetecting = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (allPermissionsGranted() && cameraProvider != null) {
            isDetecting = true;
        }
    }
}
