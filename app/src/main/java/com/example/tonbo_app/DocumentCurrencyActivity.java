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
    private static final String TAG = "DocumentCurrencyAct";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};

    private PreviewView cameraPreview;
    private Button backButton;
    private Button flashButton;
    private Button textModeButton;
    private Button currencyModeButton;
    private Button captureButton;
    private Button readButton;
    private Button clearButton;
    private TextView pageTitle;

    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private boolean isFlashOn = false;
    private boolean isAnalyzing = false;
    
    // 分析模式：true=文字分析，false=錢幣分析
    private boolean isTextMode = true;

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
        String pageTitle = getString(R.string.document_title);
        announcePageTitle(pageTitle);
        announceNavigation(getString(R.string.document_started_message));
    }

    private void initViews() {
        cameraPreview = findViewById(R.id.cameraPreview);
        backButton = findViewById(R.id.backButton);
        flashButton = findViewById(R.id.flashButton);
        textModeButton = findViewById(R.id.textModeButton);
        currencyModeButton = findViewById(R.id.currencyModeButton);
        captureButton = findViewById(R.id.captureButton);
        readButton = findViewById(R.id.readButton);
        clearButton = findViewById(R.id.clearButton);
        pageTitle = findViewById(R.id.pageTitle);

        // 返回按鈕
        backButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            announceNavigation(getString(R.string.go_back_home));
            finish();
        });

        // 閃光燈按鈕
        flashButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            toggleFlash();
        });

        // 文字分析模式按鈕
        textModeButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            switchToTextMode();
        });

        // 錢幣分析模式按鈕
        currencyModeButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            switchToCurrencyMode();
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
        
        // 根據當前語言更新界面文字
        updateLanguageUI();
    }
    
    /**
     * 更新語言UI
     */
    private void updateLanguageUI() {
        if (pageTitle != null) {
            pageTitle.setText(getLocalizedString("document_assistant_title"));
        }
        
        if (textModeButton != null) {
            textModeButton.setText(getLocalizedString("text_analysis"));
        }
        
        if (currencyModeButton != null) {
            currencyModeButton.setText(getLocalizedString("currency_analysis"));
        }
        
        if (captureButton != null) {
            captureButton.setText(getLocalizedString("capture_photo"));
        }
        
        if (readButton != null) {
            readButton.setText(getLocalizedString("voice_read"));
        }
        
        if (clearButton != null) {
            clearButton.setText(getLocalizedString("clear"));
        }
    }
    
    /**
     * 根據當前語言獲取本地化字符串
     */
    private String getLocalizedString(String key) {
        switch (key) {
            case "document_assistant_title":
                if ("english".equals(currentLanguage)) {
                    return "Document Assistant";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "文字助手";
                } else {
                    return "閱讀助手";
                }
            case "text_analysis":
                if ("english".equals(currentLanguage)) {
                    return "Text Analysis";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "文字分析";
                } else {
                    return "文字分析";
                }
            case "currency_analysis":
                if ("english".equals(currentLanguage)) {
                    return "Currency Analysis";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "货币分析";
                } else {
                    return "錢幣分析";
                }
            case "capture_photo":
                if ("english".equals(currentLanguage)) {
                    return "Capture Photo";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "拍照扫描";
                } else {
                    return "拍照掃描";
                }
            case "voice_read":
                if ("english".equals(currentLanguage)) {
                    return "Voice Read";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "语音朗读";
                } else {
                    return "語音朗讀";
                }
            case "clear":
                if ("english".equals(currentLanguage)) {
                    return "Clear";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "清除";
                } else {
                    return "清除";
                }
            default:
                return "";
        }
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
                announceError(getString(R.string.camera_permission_message));
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

        announceInfo(getString(R.string.capturing_analyzing));
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
                            String.format(getString(R.string.items_detected), (ocrResults.size() + currencyResults.size())));
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
            announceInfo(getString(R.string.not_scanned_yet));
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
                speechText = getString(R.string.no_content_detected);
            }
            
            // 根據當前語言選擇對應的語音內容
            String cantoneseText = currentLanguage.equals("english") ? translateToChinese(speechText) : speechText;
            String englishText = currentLanguage.equals("english") ? speechText : translateToEnglish(speechText);
            ttsManager.speak(cantoneseText, englishText, true);
        }
    }

    private void clearResults() {
        lastRecognitionResult = "";
        lastOCRResults = null;
        lastCurrencyResults = null;
        announceInfo(getString(R.string.results_cleared));
    }

    private void updateStatus(String status) {
        // 狀態更新現在通過語音播報
        announceInfo(status);
    }

    private void updateResults(String results) {
        // 結果更新現在通過語音播報
        announceInfo("掃描完成：" + results);
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

    // 切換到文字分析模式
    private void switchToTextMode() {
        isTextMode = true;
        updateModeUI();
        announceInfo("已切換到文字分析模式");
    }

    // 切換到錢幣分析模式
    private void switchToCurrencyMode() {
        isTextMode = false;
        updateModeUI();
        announceInfo("已切換到錢幣分析模式");
    }

    // 更新模式UI
    private void updateModeUI() {
        if (isTextMode) {
            // 文字模式
            textModeButton.setBackgroundResource(R.drawable.button_modern_background);
            currencyModeButton.setBackgroundResource(R.drawable.button_emergency_background);
        } else {
            // 錢幣模式
            textModeButton.setBackgroundResource(R.drawable.button_emergency_background);
            currencyModeButton.setBackgroundResource(R.drawable.button_modern_background);
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


    /**
     * 將英文描述翻譯為中文
     */
    private String translateToChinese(String english) {
        // 簡單的翻譯映射
        return english
                .replace("No content detected", "未識別到任何內容")
                .replace("Text recognized", "識別到文字")
                .replace("Currency detected", "檢測到貨幣")
                .replace("Scanning", "掃描中")
                .replace("Ready to scan", "準備掃描")
                .replace("Results cleared", "結果已清除")
                .replace("Camera permission needed", "需要相機權限")
                .replace("Text content", "文字內容")
                .replace("Currency amount", "貨幣金額")
                .replace("Document", "文件")
                .replace("Menu", "菜單")
                .replace("Banknote", "紙幣")
                .replace("Coin", "硬幣")
                .replace("Dollar", "美元")
                .replace("Euro", "歐元")
                .replace("Yuan", "人民幣")
                .replace("Yen", "日圓")
                .replace("Pound", "英鎊")
                .replace("Franc", "法郎")
                .replace("Mark", "馬克")
                .replace("Lira", "里拉")
                .replace("Peseta", "比塞塔")
                .replace("Guilder", "荷蘭盾")
                .replace("Crown", "克朗")
                .replace("Krone", "克朗")
                .replace("Krona", "克朗")
                .replace("Rupee", "盧比")
                .replace("Peso", "比索")
                .replace("Real", "雷亞爾")
                .replace("Rand", "蘭特")
                .replace("Dinar", "第納爾")
                .replace("Dirham", "迪拉姆")
                .replace("Riyal", "里亞爾")
                .replace("Ruble", "盧布")
                .replace("Hryvnia", "格里夫納")
                .replace("Zloty", "茲羅提")
                .replace("Forint", "福林")
                .replace("Koruna", "克朗")
                .replace("Leu", "列伊")
                .replace("Lev", "列弗")
                .replace("Kuna", "庫納")
                .replace("Dram", "德拉姆")
                .replace("Taka", "塔卡")
                .replace("Rupiah", "印尼盾")
                .replace("Ringgit", "林吉特")
                .replace("Baht", "泰銖")
                .replace("Won", "韓圓")
                .replace("Dong", "越南盾")
                .replace("Kip", "基普")
                .replace("Riel", "瑞爾")
                .replace("Pataca", "澳門元")
                .replace("Singapore dollar", "新加坡元")
                .replace("Hong Kong dollar", "港幣")
                .replace("New Taiwan dollar", "新台幣")
                .replace("Korean won", "韓圓")
                .replace("Japanese yen", "日圓")
                .replace("Chinese yuan", "人民幣")
                .replace("British pound", "英鎊")
                .replace("US dollar", "美元")
                .replace("Canadian dollar", "加拿大元")
                .replace("Australian dollar", "澳元")
                .replace("New Zealand dollar", "紐西蘭元")
                .replace("Swiss franc", "瑞士法郎")
                .replace("Swedish krona", "瑞典克朗")
                .replace("Norwegian krone", "挪威克朗")
                .replace("Danish krone", "丹麥克朗")
                .replace("Polish zloty", "波蘭茲羅提")
                .replace("Czech koruna", "捷克克朗")
                .replace("Hungarian forint", "匈牙利福林")
                .replace("Romanian leu", "羅馬尼亞列伊")
                .replace("Bulgarian lev", "保加利亞列弗")
                .replace("Croatian kuna", "克羅地亞庫納")
                .replace("Serbian dinar", "塞爾維亞第納爾")
                .replace("Turkish lira", "土耳其里拉")
                .replace("Israeli shekel", "以色列謝克爾")
                .replace("Saudi riyal", "沙烏地里亞爾")
                .replace("UAE dirham", "阿聯酋迪拉姆")
                .replace("Qatari riyal", "卡塔爾里亞爾")
                .replace("Kuwaiti dinar", "科威特第納爾")
                .replace("Bahraini dinar", "巴林第納爾")
                .replace("Omani rial", "阿曼里亞爾")
                .replace("Jordanian dinar", "約旦第納爾")
                .replace("Lebanese pound", "黎巴嫩鎊")
                .replace("Egyptian pound", "埃及鎊")
                .replace("South African rand", "南非蘭特")
                .replace("Nigerian naira", "奈及利亞奈拉")
                .replace("Kenyan shilling", "肯尼亞先令")
                .replace("Ugandan shilling", "烏干達先令")
                .replace("Tanzanian shilling", "坦桑尼亞先令")
                .replace("Ethiopian birr", "衣索比亞比爾")
                .replace("Moroccan dirham", "摩洛哥迪拉姆")
                .replace("Algerian dinar", "阿爾及利亞第納爾")
                .replace("Tunisian dinar", "突尼斯第納爾")
                .replace("Libyan dinar", "利比亞第納爾")
                .replace("Sudanese pound", "蘇丹鎊")
                .replace("Ghanaian cedi", "加納塞地")
                .replace("Botswana pula", "博茨瓦納普拉")
                .replace("Namibian dollar", "納米比亞元")
                .replace("Zambian kwacha", "贊比亞克瓦查")
                .replace("Zimbabwean dollar", "津巴布韋元")
                .replace("Mauritian rupee", "毛里求斯盧比")
                .replace("Seychellois rupee", "塞舌爾盧比")
                .replace("Malagasy ariary", "馬達加斯加阿里亞里")
                .replace("Comorian franc", "科摩羅法郎")
                .replace("Djiboutian franc", "吉布提法郎")
                .replace("Eritrean nakfa", "厄立特里亞納克法")
                .replace("Somalian shilling", "索馬里先令")
                .replace("Burundian franc", "布隆迪法郎")
                .replace("Rwandan franc", "盧旺達法郎")
                .replace("Congolese franc", "剛果法郎")
                .replace("Central African franc", "中非法郎")
                .replace("West African franc", "西非法郎")
                .replace("Cape Verdean escudo", "佛得角埃斯庫多")
                .replace("Sao Tomean dobra", "聖多美多布拉")
                .replace("Guinean franc", "幾內亞法郎")
                .replace("Sierra Leonean leone", "塞拉利昂利昂")
                .replace("Liberian dollar", "利比里亞元")
                .replace("Gambian dalasi", "岡比亞達拉西")
                .replace("Senegalese franc", "塞內加爾法郎")
                .replace("Mauritanian ouguiya", "毛里塔尼亞烏吉亞")
                .replace("Malian franc", "馬里法郎")
                .replace("Burkina Faso franc", "布基納法索法郎")
                .replace("Nigerian franc", "尼日爾法郎")
                .replace("Chadian franc", "乍得法郎")
                .replace("Cameroonian franc", "喀麥隆法郎")
                .replace("Gabonese franc", "加蓬法郎")
                .replace("Equatorial Guinean franc", "赤道幾內亞法郎")
                .replace("Central African franc", "中非法郎")
                .replace("Republic of Congo franc", "剛果共和國法郎")
                .replace("Democratic Republic of Congo franc", "剛果民主共和國法郎")
                .replace("Angolan kwanza", "安哥拉寬扎")
                .replace("Mozambican metical", "莫桑比克梅蒂卡爾")
                .replace("Malawian kwacha", "馬拉維克瓦查")
                .replace("Lesotho loti", "萊索托洛蒂")
                .replace("Swazi lilangeni", "斯威士蘭里蘭吉尼")
                .replace("South African rand", "南非蘭特")
                .replace("Namibian dollar", "納米比亞元")
                .replace("Botswana pula", "博茨瓦納普拉")
                .replace("Zambian kwacha", "贊比亞克瓦查")
                .replace("Zimbabwean dollar", "津巴布韋元")
                .replace("Malagasy ariary", "馬達加斯加阿里亞里")
                .replace("Mauritian rupee", "毛里求斯盧比")
                .replace("Seychellois rupee", "塞舌爾盧比")
                .replace("Comorian franc", "科摩羅法郎");
    }
}
