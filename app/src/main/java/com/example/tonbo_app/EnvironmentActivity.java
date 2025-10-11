package com.example.tonbo_app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
    private TextView detectionStatus;
    private TextView detectionResults;
    private Button backButton;
    private Button flashButton;
    private Button captureButton;
    private Button speakButton;

    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private boolean isFlashOn = false;
    private boolean isDetecting = false;

    // YOLO 相關變量
    private String lastDetectionResult = "";
    private int detectionCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_environment);

        initViews();
        cameraExecutor = Executors.newSingleThreadExecutor();

        // 檢查相機權限
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    @Override
    protected void announcePageTitle() {
        announcePageTitle("環境識別頁面");
        announceNavigation("請將相機對準周圍物體，系統將自動識別並語音播報");
    }

    private void initViews() {
        cameraPreview = findViewById(R.id.cameraPreview);
        detectionStatus = findViewById(R.id.detectionStatus);
        detectionResults = findViewById(R.id.detectionResults);
        backButton = findViewById(R.id.backButton);
        flashButton = findViewById(R.id.flashButton);
        captureButton = findViewById(R.id.captureButton);
        speakButton = findViewById(R.id.speakButton);

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

        // 拍照識別按鈕
        captureButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            captureAndDetect();
        });

        // 語音播報按鈕
        speakButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            speakDetectionResults();
        });
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
        // 這裡將整合 YOLO 模型進行實時物體識別
        // 目前先實現基礎框架
        
        try {
            // TODO: 整合 YOLO 模型
            // 1. 將 ImageProxy 轉換為 Bitmap
            // 2. 使用 YOLO 模型進行推理
            // 3. 解析檢測結果
            // 4. 更新 UI 和語音播報
            
            detectionCount++;
            
            // 模擬檢測結果（實際應該來自 YOLO 模型）
            if (detectionCount % 30 == 0) { // 每30幀更新一次
                runOnUiThread(() -> {
                    String mockResult = "偵測到物體";
                    updateDetectionResults(mockResult);
                });
            }
            
        } catch (Exception e) {
            Log.e(TAG, "圖像分析失敗: " + e.getMessage());
        } finally {
            image.close();
        }
    }

    private void captureAndDetect() {
        announceInfo("正在拍照識別");
        updateDetectionStatus("正在分析圖像...");
        
        // TODO: 實現拍照和識別邏輯
        // 目前顯示模擬結果
        new android.os.Handler().postDelayed(() -> {
            String result = "偵測到：桌子、椅子、杯子";
            updateDetectionResults(result);
            speakDetectionResults();
        }, 1000);
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
