package com.example.tonbo_app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class DocumentCurrencyActivity extends BaseAccessibleActivity {
    private static final String TAG = "DocumentCurrencyActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};

    private PreviewView cameraPreview;
    private TextView statusText;
    private TextView resultText;
    private Button backButton;
    private Button flashButton;
    private Button captureButton;
    private Button readButton;
    private Button clearButton;

    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private boolean isFlashOn = false;
    private boolean isAnalyzing = false;

    // OCR和貨幣檢測相關變量
    private OCRHelper ocrHelper;
    private CurrencyDetector currencyDetector;
    private Bitmap currentBitmap;
    private String lastRecognitionResult = "";
    private List<OCRHelper.OCRResult> lastOCRResults;
    private List<CurrencyDetector.CurrencyResult> lastCurrencyResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_currency);

        initViews();
        cameraExecutor = Executors.newSingleThreadExecutor();

        // 初始化OCR和貨幣檢測器
        ocrHelper = new OCRHelper(this);
        currencyDetector = new CurrencyDetector(this);

        // 檢查相機權限
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    @Override
    protected void announcePageTitle() {
        announcePageTitle("閱讀助手頁面");
        announceNavigation("相機已啟動，將文件或貨幣放在掃描框內，然後點擊拍照掃描。系統會識別文字內容和貨幣面額");
    }

    private void initViews() {
        cameraPreview = findViewById(R.id.cameraPreview);
        statusText = findViewById(R.id.statusText);
        resultText = findViewById(R.id.resultText);
        backButton = findViewById(R.id.backButton);
        flashButton = findViewById(R.id.flashButton);
        captureButton = findViewById(R.id.captureButton);
        readButton = findViewById(R.id.readButton);
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

        // 拍照掃描按鈕
        captureButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            captureAndAnalyze();
        });

        // 語音朗讀按鈕
        readButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            speakRecognitionResults();
        });

        // 清除按鈕
        clearButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            clearResults();
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
                announceError("需要相機權限才能使用此功能");
                finish();
            }
        }
    }

    private void startCamera() {
        com.google.common.util.concurrent.ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
            ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (Exception e) {
                Log.e(TAG, "獲取相機提供者失敗: " + e.getMessage());
                announceError("相機啟動失敗");
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
        } catch (Exception e) {
            Log.e(TAG, "綁定相機失敗: " + e.getMessage());
            announceError("相機設置失敗");
        }
    }

    private void analyzeImage(ImageProxy image) {
        try {
            // 保存當前幀供拍照使用
            currentBitmap = imageProxyToBitmap(image);
            
        } catch (Exception e) {
            Log.e(TAG, "圖像分析失敗: " + e.getMessage());
        } finally {
            image.close();
        }
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

    private void captureAndAnalyze() {
        if (isAnalyzing) {
            announceInfo("正在分析中，請稍候");
            return;
        }

        announceInfo("正在拍照並分析");
        updateStatus("正在分析圖像...");

        if (currentBitmap != null) {
            isAnalyzing = true;
            
            new Thread(() -> {
                try {
                    // 同時進行OCR和貨幣檢測
                    List<OCRHelper.OCRResult> ocrResults = ocrHelper.recognizeText(currentBitmap);
                    List<CurrencyDetector.CurrencyResult> currencyResults = currencyDetector.detectCurrency(currentBitmap);

                    // 保存結果
                    lastOCRResults = ocrResults;
                    lastCurrencyResults = currencyResults;

                    // 格式化結果
                    String combinedResult = formatCombinedResults(ocrResults, currencyResults);
                    lastRecognitionResult = combinedResult;

                    runOnUiThread(() -> {
                        updateResults(combinedResult);
                        updateStatus("分析完成");
                        announceInfo("分析完成，共識別到" + 
                            (ocrResults.size() + currencyResults.size()) + "個項目");
                        isAnalyzing = false;
                    });

                } catch (Exception e) {
                    Log.e(TAG, "分析失敗: " + e.getMessage());
                    runOnUiThread(() -> {
                        updateResults("分析失敗：" + e.getMessage());
                        updateStatus("分析失敗");
                        announceError("分析失敗，請重試");
                        isAnalyzing = false;
                    });
                }
            }).start();
        } else {
            announceInfo("請等待相機準備就緒");
            updateStatus("相機未就緒");
        }
    }

    private String formatCombinedResults(List<OCRHelper.OCRResult> ocrResults, 
                                       List<CurrencyDetector.CurrencyResult> currencyResults) {
        StringBuilder sb = new StringBuilder();
        
        if (!ocrResults.isEmpty()) {
            sb.append("📄 文字識別結果：\n\n");
            sb.append(ocrHelper.formatDetailedResults(ocrResults));
            sb.append("\n\n");
        }
        
        if (!currencyResults.isEmpty()) {
            sb.append("💰 貨幣識別結果：\n\n");
            sb.append(currencyDetector.formatDetailedResults(currencyResults));
        }
        
        if (ocrResults.isEmpty() && currencyResults.isEmpty()) {
            sb.append("未識別到任何文字或貨幣");
        }
        
        return sb.toString();
    }

    private void speakRecognitionResults() {
        if (lastRecognitionResult.isEmpty()) {
            announceInfo("尚未進行掃描分析");
        } else {
            // 使用語音播報主要結果
            String speechText = "";
            
            if (lastOCRResults != null && !lastOCRResults.isEmpty()) {
                speechText += ocrHelper.formatResultsForSpeech(lastOCRResults);
            }
            
            if (lastCurrencyResults != null && !lastCurrencyResults.isEmpty()) {
                if (!speechText.isEmpty()) {
                    speechText += "。";
                }
                speechText += currencyDetector.formatResultsForSpeech(lastCurrencyResults);
            }
            
            if (speechText.isEmpty()) {
                speechText = "未識別到任何內容";
            }
            
            ttsManager.speak(speechText, translateToEnglish(speechText), true);
        }
    }

    private void clearResults() {
        resultText.setText("掃描結果將顯示在這裡...");
        lastRecognitionResult = "";
        lastOCRResults = null;
        lastCurrencyResults = null;
        updateStatus("準備掃描");
        announceInfo("結果已清除");
    }

    private void updateStatus(String status) {
        statusText.setText(status);
    }

    private void updateResults(String results) {
        resultText.setText(results);
    }

    private void toggleFlash() {
        if (cameraProvider != null) {
            Camera camera = cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA);
            if (camera.getCameraInfo().hasFlashUnit()) {
                isFlashOn = !isFlashOn;
                camera.getCameraControl().enableTorch(isFlashOn);
                if (isFlashOn) {
                    flashButton.setText("💡");
                    announceInfo("閃光燈已開啟");
                } else {
                    flashButton.setText("💡");
                    announceInfo("閃光燈已關閉");
                }
            } else {
                announceInfo("此設備不支持閃光燈");
            }
        }
    }

    // 簡單的翻譯方法
    private String translateToEnglish(String chinese) {
        return chinese
                .replace("識別到", "Recognized:")
                .replace("文字", "text")
                .replace("貨幣", "currency")
                .replace("港幣", "Hong Kong Dollar")
                .replace("元", "dollars")
                .replace("紙幣", "banknote")
                .replace("硬幣", "coin");
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
        if (ocrHelper != null) {
            ocrHelper.close();
        }
        if (currencyDetector != null) {
            currencyDetector.close();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (allPermissionsGranted()) {
            startCamera();
        }
    }
}
