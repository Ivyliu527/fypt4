package com.example.tonbo_app;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 顏色和光線分析器
 * 用於分析圖像中的主要顏色、色調、亮度和光線條件
 */
public class ColorLightingAnalyzer {
    private static final String TAG = "ColorLightingAnalyzer";
    
    // 顏色分析參數
    private static final int SAMPLE_SIZE = 100; // 採樣大小
    private static final int COLOR_TOLERANCE = 50; // 顏色容差
    private static final float MIN_COLOR_PERCENTAGE = 5.0f; // 最小顏色百分比
    
    // 光線分析參數
    private static final int BRIGHTNESS_THRESHOLD_LOW = 85; // 低亮度閾值
    private static final int BRIGHTNESS_THRESHOLD_HIGH = 170; // 高亮度閾值
    private static final int CONTRAST_THRESHOLD_LOW = 30; // 低對比度閾值
    private static final int CONTRAST_THRESHOLD_HIGH = 80; // 高對比度閾值
    
    /**
     * 顏色分析結果
     */
    public static class ColorAnalysisResult {
        private String primaryColor;
        private String secondaryColor;
        private String dominantTone;
        private List<ColorInfo> colorPalette;
        
        public ColorAnalysisResult() {
            colorPalette = new ArrayList<>();
        }
        
        // Getters and setters
        public String getPrimaryColor() { return primaryColor; }
        public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }
        
        public String getSecondaryColor() { return secondaryColor; }
        public void setSecondaryColor(String secondaryColor) { this.secondaryColor = secondaryColor; }
        
        public String getDominantTone() { return dominantTone; }
        public void setDominantTone(String dominantTone) { this.dominantTone = dominantTone; }
        
        public List<ColorInfo> getColorPalette() { return colorPalette; }
        
        public void addColorInfo(ColorInfo colorInfo) {
            colorPalette.add(colorInfo);
        }
    }
    
    /**
     * 光線分析結果
     */
    public static class LightingAnalysisResult {
        private String brightnessLevel;
        private String contrastLevel;
        private String lightingCondition;
        private float averageBrightness;
        private float contrastRatio;
        private String lightDirection;
        
        // Getters and setters
        public String getBrightnessLevel() { return brightnessLevel; }
        public void setBrightnessLevel(String brightnessLevel) { this.brightnessLevel = brightnessLevel; }
        
        public String getContrastLevel() { return contrastLevel; }
        public void setContrastLevel(String contrastLevel) { this.contrastLevel = contrastLevel; }
        
        public String getLightingCondition() { return lightingCondition; }
        public void setLightingCondition(String lightingCondition) { this.lightingCondition = lightingCondition; }
        
        public float getAverageBrightness() { return averageBrightness; }
        public void setAverageBrightness(float averageBrightness) { this.averageBrightness = averageBrightness; }
        
        public float getContrastRatio() { return contrastRatio; }
        public void setContrastRatio(float contrastRatio) { this.contrastRatio = contrastRatio; }
        
        public String getLightDirection() { return lightDirection; }
        public void setLightDirection(String lightDirection) { this.lightDirection = lightDirection; }
    }
    
    /**
     * 顏色信息
     */
    public static class ColorInfo {
        private String colorName;
        private int colorValue;
        private float percentage;
        
        public ColorInfo(String colorName, int colorValue, float percentage) {
            this.colorName = colorName;
            this.colorValue = colorValue;
            this.percentage = percentage;
        }
        
        // Getters
        public String getColorName() { return colorName; }
        public int getColorValue() { return colorValue; }
        public float getPercentage() { return percentage; }
    }
    
    /**
     * 分析圖像的顏色
     */
    public ColorAnalysisResult analyzeColors(Bitmap bitmap) {
        Log.d(TAG, "開始顏色分析");
        ColorAnalysisResult result = new ColorAnalysisResult();
        
        if (bitmap == null || bitmap.isRecycled()) {
            Log.w(TAG, "無效的bitmap");
            return result;
        }
        
        try {
            // 採樣像素
            List<Integer> pixelSamples = samplePixels(bitmap);
            
            // 分析主要顏色
            Map<String, Integer> colorCount = countColors(pixelSamples);
            
            // 生成顏色調色板
            generateColorPalette(colorCount, result);
            
            // 確定主要和次要顏色
            determinePrimarySecondaryColors(result);
            
            // 確定主色調
            determineDominantTone(result);
            
            Log.d(TAG, "顏色分析完成: " + result.getPrimaryColor() + " + " + result.getSecondaryColor());
            
        } catch (Exception e) {
            Log.e(TAG, "顏色分析失敗: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 分析圖像的光線條件
     */
    public LightingAnalysisResult analyzeLighting(Bitmap bitmap) {
        Log.d(TAG, "開始光線分析");
        LightingAnalysisResult result = new LightingAnalysisResult();
        
        if (bitmap == null || bitmap.isRecycled()) {
            Log.w(TAG, "無效的bitmap");
            return result;
        }
        
        try {
            // 計算平均亮度
            float averageBrightness = calculateAverageBrightness(bitmap);
            result.setAverageBrightness(averageBrightness);
            
            // 分析亮度等級
            result.setBrightnessLevel(analyzeBrightnessLevel(averageBrightness));
            
            // 計算對比度
            float contrastRatio = calculateContrastRatio(bitmap);
            result.setContrastRatio(contrastRatio);
            
            // 分析對比度等級
            result.setContrastLevel(analyzeContrastLevel(contrastRatio));
            
            // 分析光線方向
            result.setLightDirection(analyzeLightDirection(bitmap));
            
            // 綜合光線條件
            result.setLightingCondition(determineLightingCondition(averageBrightness, contrastRatio));
            
            Log.d(TAG, "光線分析完成: " + result.getLightingCondition());
            
        } catch (Exception e) {
            Log.e(TAG, "光線分析失敗: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 採樣像素
     */
    private List<Integer> samplePixels(Bitmap bitmap) {
        List<Integer> samples = new ArrayList<>();
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        // 隨機採樣
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            int x = (int) (Math.random() * width);
            int y = (int) (Math.random() * height);
            samples.add(bitmap.getPixel(x, y));
        }
        
        return samples;
    }
    
    /**
     * 統計顏色
     */
    private Map<String, Integer> countColors(List<Integer> pixels) {
        Map<String, Integer> colorCount = new HashMap<>();
        
        for (int pixel : pixels) {
            String colorCategory = categorizeColor(pixel);
            colorCount.put(colorCategory, colorCount.getOrDefault(colorCategory, 0) + 1);
        }
        
        return colorCount;
    }
    
    /**
     * 將像素分類為顏色類別
     */
    private String categorizeColor(int pixel) {
        int r = Color.red(pixel);
        int g = Color.green(pixel);
        int b = Color.blue(pixel);
        
        // 轉換為HSV進行更好的顏色分類
        float[] hsv = new float[3];
        Color.RGBToHSV(r, g, b, hsv);
        
        float hue = hsv[0];
        float saturation = hsv[1];
        float value = hsv[2];
        
        // 低飽和度 = 灰階
        if (saturation < 0.2f) {
            if (value < 0.3f) return "黑色";
            if (value > 0.7f) return "白色";
            return "灰色";
        }
        
        // 基於色相分類顏色
        if (hue < 15 || hue > 345) return "紅色";
        if (hue < 45) return "橙色";
        if (hue < 75) return "黃色";
        if (hue < 165) return "綠色";
        if (hue < 210) return "青色";
        if (hue < 270) return "藍色";
        if (hue < 315) return "紫色";
        return "紅色";
    }
    
    /**
     * 生成顏色調色板
     */
    private void generateColorPalette(Map<String, Integer> colorCount, ColorAnalysisResult result) {
        int totalSamples = SAMPLE_SIZE;
        
        for (Map.Entry<String, Integer> entry : colorCount.entrySet()) {
            float percentage = (float) entry.getValue() / totalSamples * 100;
            if (percentage >= MIN_COLOR_PERCENTAGE) {
                result.addColorInfo(new ColorInfo(entry.getKey(), 0, percentage));
            }
        }
        
        // 按百分比排序
        Collections.sort(result.getColorPalette(), new Comparator<ColorInfo>() {
            @Override
            public int compare(ColorInfo a, ColorInfo b) {
                return Float.compare(b.getPercentage(), a.getPercentage());
            }
        });
    }
    
    /**
     * 確定主要和次要顏色
     */
    private void determinePrimarySecondaryColors(ColorAnalysisResult result) {
        if (result.getColorPalette().size() > 0) {
            result.setPrimaryColor(result.getColorPalette().get(0).getColorName());
        }
        if (result.getColorPalette().size() > 1) {
            result.setSecondaryColor(result.getColorPalette().get(1).getColorName());
        }
    }
    
    /**
     * 確定主色調
     */
    private void determineDominantTone(ColorAnalysisResult result) {
        if (result.getColorPalette().isEmpty()) {
            result.setDominantTone("中性");
            return;
        }
        
        // 計算暖色和冷色的比例
        float warmColors = 0;
        float coolColors = 0;
        
        for (ColorInfo colorInfo : result.getColorPalette()) {
            String colorName = colorInfo.getColorName();
            if (isWarmColor(colorName)) {
                warmColors += colorInfo.getPercentage();
            } else if (isCoolColor(colorName)) {
                coolColors += colorInfo.getPercentage();
            }
        }
        
        if (warmColors > coolColors + 10) {
            result.setDominantTone("暖色調");
        } else if (coolColors > warmColors + 10) {
            result.setDominantTone("冷色調");
        } else {
            result.setDominantTone("中性色調");
        }
    }
    
    /**
     * 判斷是否為暖色
     */
    private boolean isWarmColor(String colorName) {
        return colorName.equals("紅色") || colorName.equals("橙色") || colorName.equals("黃色");
    }
    
    /**
     * 判斷是否為冷色
     */
    private boolean isCoolColor(String colorName) {
        return colorName.equals("藍色") || colorName.equals("青色") || colorName.equals("紫色");
    }
    
    /**
     * 計算平均亮度
     */
    private float calculateAverageBrightness(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        long totalBrightness = 0;
        int sampleCount = 0;
        
        // 採樣計算平均亮度
        for (int i = 0; i < width; i += width / 20) {
            for (int j = 0; j < height; j += height / 20) {
                int pixel = bitmap.getPixel(i, j);
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);
                
                // 使用標準亮度公式
                float brightness = 0.299f * r + 0.587f * g + 0.114f * b;
                totalBrightness += brightness;
                sampleCount++;
            }
        }
        
        return totalBrightness / (float) sampleCount;
    }
    
    /**
     * 分析亮度等級
     */
    private String analyzeBrightnessLevel(float averageBrightness) {
        if (averageBrightness < BRIGHTNESS_THRESHOLD_LOW) {
            return "較暗";
        } else if (averageBrightness > BRIGHTNESS_THRESHOLD_HIGH) {
            return "較亮";
        } else {
            return "適中";
        }
    }
    
    /**
     * 計算對比度比率
     */
    private float calculateContrastRatio(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        int minBrightness = 255;
        int maxBrightness = 0;
        
        // 採樣計算最大最小亮度
        for (int i = 0; i < width; i += width / 15) {
            for (int j = 0; j < height; j += height / 15) {
                int pixel = bitmap.getPixel(i, j);
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);
                
                float brightness = 0.299f * r + 0.587f * g + 0.114f * b;
                
                if (brightness < minBrightness) minBrightness = (int) brightness;
                if (brightness > maxBrightness) maxBrightness = (int) brightness;
            }
        }
        
        return maxBrightness - minBrightness;
    }
    
    /**
     * 分析對比度等級
     */
    private String analyzeContrastLevel(float contrastRatio) {
        if (contrastRatio < CONTRAST_THRESHOLD_LOW) {
            return "低對比";
        } else if (contrastRatio > CONTRAST_THRESHOLD_HIGH) {
            return "高對比";
        } else {
            return "中等對比";
        }
    }
    
    /**
     * 分析光線方向（簡化版）
     */
    private String analyzeLightDirection(Bitmap bitmap) {
        // 簡化實現：分析圖像邊緣的亮度差異
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        float leftBrightness = 0;
        float rightBrightness = 0;
        float topBrightness = 0;
        float bottomBrightness = 0;
        
        int sampleSize = 10;
        
        // 左側亮度
        for (int j = 0; j < height; j += height / sampleSize) {
            int pixel = bitmap.getPixel(width / 8, j);
            leftBrightness += getPixelBrightness(pixel);
        }
        
        // 右側亮度
        for (int j = 0; j < height; j += height / sampleSize) {
            int pixel = bitmap.getPixel(width * 7 / 8, j);
            rightBrightness += getPixelBrightness(pixel);
        }
        
        // 頂部亮度
        for (int i = 0; i < width; i += width / sampleSize) {
            int pixel = bitmap.getPixel(i, height / 8);
            topBrightness += getPixelBrightness(pixel);
        }
        
        // 底部亮度
        for (int i = 0; i < width; i += width / sampleSize) {
            int pixel = bitmap.getPixel(i, height * 7 / 8);
            bottomBrightness += getPixelBrightness(pixel);
        }
        
        // 分析光線方向
        if (leftBrightness > rightBrightness + 20) {
            return "左側光線";
        } else if (rightBrightness > leftBrightness + 20) {
            return "右側光線";
        } else if (topBrightness > bottomBrightness + 20) {
            return "頂部光線";
        } else if (bottomBrightness > topBrightness + 20) {
            return "底部光線";
        } else {
            return "均勻光線";
        }
    }
    
    /**
     * 獲取像素亮度
     */
    private float getPixelBrightness(int pixel) {
        int r = Color.red(pixel);
        int g = Color.green(pixel);
        int b = Color.blue(pixel);
        return 0.299f * r + 0.587f * g + 0.114f * b;
    }
    
    /**
     * 綜合判斷光線條件
     */
    private String determineLightingCondition(float brightness, float contrast) {
        if (brightness < BRIGHTNESS_THRESHOLD_LOW && contrast < CONTRAST_THRESHOLD_LOW) {
            return "昏暗環境";
        } else if (brightness > BRIGHTNESS_THRESHOLD_HIGH && contrast > CONTRAST_THRESHOLD_HIGH) {
            return "明亮高對比";
        } else if (brightness > BRIGHTNESS_THRESHOLD_HIGH) {
            return "明亮環境";
        } else if (contrast > CONTRAST_THRESHOLD_HIGH) {
            return "高對比環境";
        } else {
            return "正常光線";
        }
    }
}
