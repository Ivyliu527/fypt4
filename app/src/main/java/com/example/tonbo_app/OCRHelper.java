package com.example.tonbo_app;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * OCR文字識別助手類
 * 使用Google ML Kit進行中文和英文文字識別
 */
public class OCRHelper {
    private static final String TAG = "OCRHelper";
    
    private com.google.mlkit.vision.text.TextRecognizer textRecognizer;
    private Context context;
    
    public OCRHelper(Context context) {
        this.context = context;
        initializeTextRecognizer();
    }
    
    /**
     * 初始化文字識別器
     */
    private void initializeTextRecognizer() {
        // 使用中文文字識別選項，支持中英文混合識別
        textRecognizer = TextRecognition.getClient(
            new ChineseTextRecognizerOptions.Builder().build()
        );
        Log.d(TAG, "OCR文字識別器初始化完成");
    }
    
    /**
     * 識別圖片中的文字
     * @param bitmap 要識別的圖片
     * @return 識別結果列表
     */
    public List<OCRResult> recognizeText(Bitmap bitmap) {
        List<OCRResult> results = new ArrayList<>();
        
        try {
            // 創建輸入圖像
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            
            // 使用CountDownLatch等待異步結果
            CountDownLatch latch = new CountDownLatch(1);
            
            textRecognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    // 處理識別結果
                    processTextRecognitionResult(visionText, results);
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "OCR識別失敗: " + e.getMessage());
                    latch.countDown();
                });
            
            // 等待識別完成（最多等待10秒）
            latch.await();
            
        } catch (Exception e) {
            Log.e(TAG, "OCR處理異常: " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * 處理文字識別結果
     */
    private void processTextRecognitionResult(Text visionText, List<OCRResult> results) {
        String fullText = visionText.getText();
        Log.d(TAG, "識別到的完整文字: " + fullText);
        
        if (fullText != null && !fullText.trim().isEmpty()) {
            // 創建主要識別結果
            OCRResult mainResult = new OCRResult(
                fullText,
                "完整文字",
                calculateConfidence(fullText)
            );
            results.add(mainResult);
            
            // 處理文字塊
            for (Text.TextBlock block : visionText.getTextBlocks()) {
                String blockText = block.getText();
                if (blockText != null && !blockText.trim().isEmpty()) {
                    OCRResult blockResult = new OCRResult(
                        blockText,
                        "文字塊",
                        calculateConfidence(blockText)
                    );
                    results.add(blockResult);
                }
                
                // 處理文字行
                for (Text.Line line : block.getLines()) {
                    String lineText = line.getText();
                    if (lineText != null && !lineText.trim().isEmpty()) {
                        OCRResult lineResult = new OCRResult(
                            lineText,
                            "文字行",
                            calculateConfidence(lineText)
                        );
                        results.add(lineResult);
                    }
                }
            }
        }
    }
    
    /**
     * 計算識別置信度（簡單實現）
     */
    private float calculateConfidence(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0.0f;
        }
        
        // 簡單的置信度計算：基於文字長度和字符類型
        float confidence = 0.5f; // 基礎置信度
        
        // 文字長度影響
        if (text.length() > 10) {
            confidence += 0.2f;
        }
        
        // 中文字符加分
        long chineseCount = text.chars().filter(ch -> 
            (ch >= 0x4E00 && ch <= 0x9FFF) || // 基本中文字符
            (ch >= 0x3400 && ch <= 0x4DBF) || // 擴展A區
            (ch >= 0x20000 && ch <= 0x2A6DF)   // 擴展B區
        ).count();
        
        if (chineseCount > 0) {
            confidence += 0.3f;
        }
        
        return Math.min(confidence, 1.0f);
    }
    
    /**
     * 格式化識別結果為語音文本
     */
    public String formatResultsForSpeech(List<OCRResult> results) {
        if (results.isEmpty()) {
            return "未識別到任何文字";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("識別到以下內容：\n\n");
        
        // 使用第一個結果（通常是最完整的）
        OCRResult mainResult = results.get(0);
        sb.append(mainResult.getText());
        
        return sb.toString();
    }
    
    /**
     * 格式化詳細結果
     */
    public String formatDetailedResults(List<OCRResult> results) {
        if (results.isEmpty()) {
            return "未識別到任何文字";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("識別到 %d 個文字區域：\n\n", results.size()));
        
        for (int i = 0; i < Math.min(results.size(), 5); i++) {
            OCRResult result = results.get(i);
            sb.append(String.format("%d. %s (%.0f%%)\n",
                i + 1,
                result.getText(),
                result.getConfidence() * 100
            ));
        }
        
        if (results.size() > 5) {
            sb.append(String.format("\n...還有 %d 個文字區域", results.size() - 5));
        }
        
        return sb.toString();
    }
    
    /**
     * 關閉文字識別器
     */
    public void close() {
        if (textRecognizer != null) {
            textRecognizer.close();
            textRecognizer = null;
            Log.d(TAG, "OCR文字識別器已關閉");
        }
    }
    
    /**
     * OCR識別結果類
     */
    public static class OCRResult {
        private String text;
        private String type;
        private float confidence;
        
        public OCRResult(String text, String type, float confidence) {
            this.text = text;
            this.type = type;
            this.confidence = confidence;
        }
        
        public String getText() { return text; }
        public String getType() { return type; }
        public float getConfidence() { return confidence; }
        
        @Override
        public String toString() {
            return String.format("[%s] %s (%.0f%%)", type, text, confidence * 100);
        }
    }
}
