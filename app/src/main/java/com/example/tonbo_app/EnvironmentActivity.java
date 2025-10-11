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
    
    // 記憶體監控
    private long lastMemoryCheck = 0;
    private static final long MEMORY_CHECK_INTERVAL = 10000; // 10秒檢查一次記憶體
    private static final long MEMORY_WARNING_THRESHOLD = 100 * 1024 * 1024; // 100MB警告閾值

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_environment);

        Log.d(TAG, "EnvironmentActivity onCreate開始");
        
        initViews();
        cameraExecutor = Executors.newSingleThreadExecutor();
        
        // 初始化物體檢測器
        objectDetectorHelper = new ObjectDetectorHelper(this);

        // 檢查相機權限
        Log.d(TAG, "檢查相機權限...");
        if (allPermissionsGranted()) {
            Log.d(TAG, "相機權限已授予，開始啟動相機");
            startCamera();
        } else {
            Log.d(TAG, "相機權限未授予，請求權限");
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
                startCamera();
            } else {
                Log.e(TAG, "權限被拒絕");
                announceError("需要相機權限才能使用環境識別功能");
                Toast.makeText(this, "需要相機權限才能使用此功能", Toast.LENGTH_LONG).show();
                
                // 更新UI顯示權限錯誤
                runOnUiThread(() -> {
                    updateDetectionStatus("需要相機權限");
                    updateDetectionResults("請在設置中授予相機權限，然後重新打開此功能");
                });
                
                // 延遲3秒後返回主頁
                new android.os.Handler().postDelayed(() -> {
                    finish();
                }, 3000);
            }
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
                announceSuccess("相機已啟動，開始偵測環境");
            } catch (Exception e) {
                Log.e(TAG, "相機啟動失敗: " + e.getMessage());
                e.printStackTrace();
                announceError("相機啟動失敗: " + e.getMessage());
                
                // 在UI線程顯示錯誤信息
                runOnUiThread(() -> {
                    updateDetectionStatus("相機啟動失敗");
                    updateDetectionResults("錯誤: " + e.getMessage());
                });
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
            updateDetectionStatus("正在偵測環境...");
            Log.d(TAG, "相機初始化完成，開始檢測");

        } catch (Exception e) {
            Log.e(TAG, "綁定相機失敗: " + e.getMessage());
            e.printStackTrace();
            announceError("相機設置失敗: " + e.getMessage());
            
            runOnUiThread(() -> {
                updateDetectionStatus("相機綁定失敗");
                updateDetectionResults("錯誤: " + e.getMessage());
            });
        }
    }

    private void analyzeImage(ImageProxy image) {
        try {
            detectionCount++;
            
            // 定期檢查記憶體使用情況
            checkMemoryUsage();
            
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
}
