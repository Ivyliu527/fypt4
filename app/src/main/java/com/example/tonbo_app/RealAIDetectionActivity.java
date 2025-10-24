package com.example.tonbo_app;

import android.Manifest;
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
public class RealAIDetectionActivity extends AppCompatActivity {
    private static final String TAG = "RealAIDetection";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;
    
    private PreviewView previewView;
    private OptimizedDetectionOverlayView detectionOverlay;
    private TextView statusText;
    private TextView performanceText;
    private Button backButton;
    private Button startButton;
    private Button stopButton;
    private Button performanceButton;
    
    private ProcessCameraProvider cameraProvider;
    private ImageAnalysis imageAnalysis;
    private YoloDetector yoloDetector;
    private ExecutorService cameraExecutor;
    private boolean isDetecting = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_ai_detection);
        
        initViews();
        initDetector();
        checkCameraPermission();
    }
    
    private void initViews() {
        previewView = findViewById(R.id.previewView);
        detectionOverlay = findViewById(R.id.detectionOverlay);
        statusText = findViewById(R.id.statusText);
        performanceText = findViewById(R.id.performanceText);
        backButton = findViewById(R.id.backButton);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        performanceButton = findViewById(R.id.performanceButton);
        
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
        performanceButton.setOnClickListener(v -> showPerformanceReport());
        
        // 初始化狀態
        updateStatus("準備就緒");
        stopButton.setEnabled(false);
    }
    
    private void initDetector() {
        try {
            yoloDetector = new YoloDetector(this);
            Log.d(TAG, "AI檢測器初始化完成");
            updateStatus("AI檢測器已準備就緒");
        } catch (Exception e) {
            Log.e(TAG, "AI檢測器初始化失敗: " + e.getMessage());
            updateStatus("AI檢測器初始化失敗");
            Toast.makeText(this, "AI檢測器初始化失敗", Toast.LENGTH_LONG).show();
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
                updateStatus("需要相機權限才能使用AI檢測");
                Toast.makeText(this, "需要相機權限才能使用AI檢測", Toast.LENGTH_LONG).show();
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
                    updateStatus("相機初始化失敗");
                }
            }, ContextCompat.getMainExecutor(this));
            
        } catch (Exception e) {
            Log.e(TAG, "相機提供者獲取失敗: " + e.getMessage());
            updateStatus("相機提供者獲取失敗");
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
            
            updateStatus("相機已準備就緒");
            startButton.setEnabled(true);
            
        } catch (Exception e) {
            Log.e(TAG, "相機設置失敗: " + e.getMessage());
            updateStatus("相機設置失敗");
        }
    }
    
    private void analyzeImage(ImageProxy image) {
        try {
            // 執行AI檢測
            List<YoloDetector.DetectionResult> results = yoloDetector.detect(image);
            
            // 在主線程更新UI
            runOnUiThread(() -> {
                if (results != null && !results.isEmpty()) {
                    detectionOverlay.setDetectionResultsWithRelativeCoords(results);
                    updateStatus("檢測到 " + results.size() + " 個物體");
                } else {
                    detectionOverlay.clearDetectionResults();
                    updateStatus("未檢測到物體");
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "圖像分析失敗: " + e.getMessage());
            runOnUiThread(() -> updateStatus("檢測失敗: " + e.getMessage()));
        }
    }
    
    private void startDetection() {
        if (yoloDetector == null) {
            Toast.makeText(this, "AI檢測器未初始化", Toast.LENGTH_SHORT).show();
            return;
        }
        
        isDetecting = true;
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        updateStatus("AI檢測已開始");
        
        // 重置性能統計
        yoloDetector.resetPerformanceStats();
        
        Toast.makeText(this, "AI檢測已開始", Toast.LENGTH_SHORT).show();
    }
    
    private void stopDetection() {
        isDetecting = false;
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        updateStatus("AI檢測已停止");
        
        // 清除檢測結果
        detectionOverlay.clearDetectionResults();
        
        Toast.makeText(this, "AI檢測已停止", Toast.LENGTH_SHORT).show();
    }
    
    private void showPerformanceReport() {
        if (yoloDetector == null) {
            Toast.makeText(this, "AI檢測器未初始化", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String report = yoloDetector.getPerformanceReport();
        performanceText.setText(report);
        
        // 檢查性能是否良好
        boolean isGood = yoloDetector.isPerformanceGood();
        String performanceStatus = isGood ? "性能良好" : "性能需要優化";
        updateStatus("性能報告: " + performanceStatus);
        
        Toast.makeText(this, "性能報告已更新", Toast.LENGTH_SHORT).show();
    }
    
    private void updateStatus(String status) {
        statusText.setText(status);
        Log.d(TAG, "狀態更新: " + status);
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
