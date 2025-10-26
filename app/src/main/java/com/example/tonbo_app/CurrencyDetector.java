package com.example.tonbo_app;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 貨幣識別助手類
 * 專門用於識別港幣紙幣和硬幣
 */
public class CurrencyDetector {
    private static final String TAG = "CurrencyDetector";
    
    // 港幣面額特徵
    private static final Map<String, CurrencyInfo> CURRENCY_FEATURES = new HashMap<>();
    
    static {
        // 港幣紙幣特徵
        CURRENCY_FEATURES.put("10", new CurrencyInfo("十元港幣", "綠色", "獅子山", "紙幣"));
        CURRENCY_FEATURES.put("20", new CurrencyInfo("二十元港幣", "藍色", "獅子山", "紙幣"));
        CURRENCY_FEATURES.put("50", new CurrencyInfo("五十元港幣", "紫色", "獅子山", "紙幣"));
        CURRENCY_FEATURES.put("100", new CurrencyInfo("一百元港幣", "紅色", "獅子山", "紙幣"));
        CURRENCY_FEATURES.put("500", new CurrencyInfo("五百元港幣", "棕色", "獅子山", "紙幣"));
        CURRENCY_FEATURES.put("1000", new CurrencyInfo("一千元港幣", "金色", "獅子山", "紙幣"));
        
        // 港幣硬幣特徵
        CURRENCY_FEATURES.put("1", new CurrencyInfo("一元港幣", "銀色", "紫荊花", "硬幣"));
        CURRENCY_FEATURES.put("2", new CurrencyInfo("二元港幣", "銀色", "紫荊花", "硬幣"));
        CURRENCY_FEATURES.put("5", new CurrencyInfo("五元港幣", "銀色", "紫荊花", "硬幣"));
    }
    
    private Context context;
    private OCRHelper ocrHelper;
    
    public CurrencyDetector(Context context) {
        this.context = context;
        this.ocrHelper = new OCRHelper(context);
    }
    
    /**
     * 檢測圖片中的貨幣
     * @param bitmap 要檢測的圖片
     * @return 貨幣檢測結果列表
     */
    public List<CurrencyResult> detectCurrency(Bitmap bitmap) {
        List<CurrencyResult> results = new ArrayList<>();
        
        try {
            // 使用OCR識別文字
            List<OCRHelper.OCRResult> ocrResults = ocrHelper.recognizeText(bitmap);
            
            // 分析OCR結果尋找貨幣信息
            for (OCRHelper.OCRResult ocrResult : ocrResults) {
                CurrencyResult currencyResult = analyzeTextForCurrency(ocrResult.getText());
                if (currencyResult != null) {
                    results.add(currencyResult);
                }
            }
            
            // 如果沒有通過文字識別到貨幣，嘗試圖像分析
            if (results.isEmpty()) {
                results.addAll(analyzeImageForCurrency(bitmap));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "貨幣檢測失敗: " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * 分析文字內容尋找貨幣信息
     */
    private CurrencyResult analyzeTextForCurrency(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        
        String cleanText = text.replaceAll("\\s+", "").toLowerCase();
        
        // 檢查港幣相關關鍵詞
        if (cleanText.contains("港幣") || cleanText.contains("hk$") || 
            cleanText.contains("hongkong") || cleanText.contains("hkd")) {
            
            // 尋找數字
            Pattern numberPattern = Pattern.compile("\\d+");
            Matcher matcher = numberPattern.matcher(text);
            
            while (matcher.find()) {
                String number = matcher.group();
                CurrencyInfo info = CURRENCY_FEATURES.get(number);
                
                if (info != null) {
                    return new CurrencyResult(
                        info.getName(),
                        number,
                        info.getColor(),
                        info.getDesign(),
                        info.getType(),
                        0.8f, // 基於文字識別的置信度
                        "文字識別"
                    );
                }
            }
        }
        
        // 檢查數字模式（如：$100, HK$50等）
        Pattern currencyPattern = Pattern.compile("(?:hk\\$|\\$|港幣)?\\s*(\\d+)\\s*(?:港幣|hkd)?", Pattern.CASE_INSENSITIVE);
        Matcher currencyMatcher = currencyPattern.matcher(text);
        
        while (currencyMatcher.find()) {
            String amount = currencyMatcher.group(1);
            CurrencyInfo info = CURRENCY_FEATURES.get(amount);
            
            if (info != null) {
                return new CurrencyResult(
                    info.getName(),
                    amount,
                    info.getColor(),
                    info.getDesign(),
                    info.getType(),
                    0.9f, // 基於貨幣符號的置信度
                    "貨幣符號識別"
                );
            }
        }
        
        return null;
    }
    
    /**
     * 分析圖像特徵尋找貨幣（簡化實現）
     */
    private List<CurrencyResult> analyzeImageForCurrency(Bitmap bitmap) {
        List<CurrencyResult> results = new ArrayList<>();
        
        // 這裡可以實現更複雜的圖像分析
        // 例如：顏色分析、形狀檢測、邊緣檢測等
        
        // 簡化實現：基於圖片大小和顏色特徵
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        // 分析圖片比例，港幣紙幣通常是長方形
        float aspectRatio = (float) width / height;
        
        if (aspectRatio > 1.5f && aspectRatio < 3.0f) {
            // 可能是紙幣
            results.add(new CurrencyResult(
                "港幣紙幣",
                "未知面額",
                "多色",
                "獅子山",
                "紙幣",
                0.6f,
                "圖像分析"
            ));
        } else if (aspectRatio > 0.8f && aspectRatio < 1.2f) {
            // 可能是硬幣
            results.add(new CurrencyResult(
                "港幣硬幣",
                "未知面額",
                "銀色",
                "紫荊花",
                "硬幣",
                0.6f,
                "圖像分析"
            ));
        }
        
        return results;
    }
    
    /**
     * 格式化貨幣檢測結果為語音文本
     */
    public String formatResultsForSpeech(List<CurrencyResult> results) {
        if (results.isEmpty()) {
            return "未識別到任何貨幣";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("識別到貨幣：");
        
        for (int i = 0; i < results.size(); i++) {
            CurrencyResult result = results.get(i);
            sb.append(result.getName());
            
            if (!result.getAmount().equals("未知面額")) {
                sb.append("，面額").append(result.getAmount()).append("元");
            }
            
            sb.append("，").append(result.getType());
            
            if (i < results.size() - 1) {
                sb.append("；");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * 格式化詳細結果
     */
    public String formatDetailedResults(List<CurrencyResult> results) {
        if (results.isEmpty()) {
            return "未識別到任何貨幣";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("識別到 %d 種貨幣：\n\n", results.size()));
        
        for (int i = 0; i < results.size(); i++) {
            CurrencyResult result = results.get(i);
            sb.append(String.format("%d. %s\n", i + 1, result.getName()));
            sb.append(String.format("   面額：%s元\n", result.getAmount()));
            sb.append(String.format("   類型：%s\n", result.getType()));
            sb.append(String.format("   顏色：%s\n", result.getColor()));
            sb.append(String.format("   圖案：%s\n", result.getDesign()));
            sb.append(String.format("   置信度：%.0f%%\n", result.getConfidence() * 100));
            sb.append(String.format("   識別方式：%s\n\n", result.getDetectionMethod()));
        }
        
        return sb.toString();
    }
    
    /**
     * 關閉檢測器
     */
    public void close() {
        if (ocrHelper != null) {
            ocrHelper.close();
            ocrHelper = null;
        }
    }
    
    /**
     * 貨幣信息類
     */
    private static class CurrencyInfo {
        private String name;
        private String color;
        private String design;
        private String type;
        
        public CurrencyInfo(String name, String color, String design, String type) {
            this.name = name;
            this.color = color;
            this.design = design;
            this.type = type;
        }
        
        public String getName() { return name; }
        public String getColor() { return color; }
        public String getDesign() { return design; }
        public String getType() { return type; }
    }
    
    /**
     * 貨幣檢測結果類
     */
    public static class CurrencyResult {
        private String name;
        private String amount;
        private String color;
        private String design;
        private String type;
        private float confidence;
        private String detectionMethod;
        
        public CurrencyResult(String name, String amount, String color, 
                            String design, String type, float confidence, 
                            String detectionMethod) {
            this.name = name;
            this.amount = amount;
            this.color = color;
            this.design = design;
            this.type = type;
            this.confidence = confidence;
            this.detectionMethod = detectionMethod;
        }
        
        public String getName() { return name; }
        public String getAmount() { return amount; }
        public String getColor() { return color; }
        public String getDesign() { return design; }
        public String getType() { return type; }
        public float getConfidence() { return confidence; }
        public String getDetectionMethod() { return detectionMethod; }
        
        @Override
        public String toString() {
            return String.format("%s (%s元) - %s (%.0f%%)", 
                name, amount, type, confidence * 100);
        }
    }
}
