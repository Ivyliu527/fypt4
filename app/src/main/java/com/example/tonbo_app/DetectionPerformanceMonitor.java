package com.example.tonbo_app;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;

/**
 * 檢測性能監控器
 * 監控AI檢測的性能指標，包括檢測時間、準確率等
 */
public class DetectionPerformanceMonitor {
    private static final String TAG = "DetectionPerformance";
    
    private List<Long> detectionTimes = new ArrayList<>();
    private List<Float> confidenceScores = new ArrayList<>();
    private int totalDetections = 0;
    private int successfulDetections = 0;
    
    /**
     * 記錄檢測時間
     */
    public void recordDetectionTime(long detectionTimeMs) {
        detectionTimes.add(detectionTimeMs);
        totalDetections++;
        
        // 保持最近100次檢測的記錄
        if (detectionTimes.size() > 100) {
            detectionTimes.remove(0);
        }
        
        Log.d(TAG, "檢測時間: " + detectionTimeMs + "ms");
    }
    
    /**
     * 記錄檢測結果
     */
    public void recordDetectionResult(List<YoloDetector.DetectionResult> results) {
        if (results != null && !results.isEmpty()) {
            successfulDetections++;
            
            // 記錄置信度分數
            for (YoloDetector.DetectionResult result : results) {
                confidenceScores.add(result.getConfidence());
                
                // 保持最近200個置信度記錄
                if (confidenceScores.size() > 200) {
                    confidenceScores.remove(0);
                }
            }
            
            Log.d(TAG, "檢測到 " + results.size() + " 個物體");
        }
    }
    
    /**
     * 獲取平均檢測時間
     */
    public float getAverageDetectionTime() {
        if (detectionTimes.isEmpty()) {
            return 0f;
        }
        
        long total = 0;
        for (Long time : detectionTimes) {
            total += time;
        }
        
        return (float) total / detectionTimes.size();
    }
    
    /**
     * 獲取檢測成功率
     */
    public float getDetectionSuccessRate() {
        if (totalDetections == 0) {
            return 0f;
        }
        
        return (float) successfulDetections / totalDetections;
    }
    
    /**
     * 獲取平均置信度
     */
    public float getAverageConfidence() {
        if (confidenceScores.isEmpty()) {
            return 0f;
        }
        
        float total = 0;
        for (Float confidence : confidenceScores) {
            total += confidence;
        }
        
        return total / confidenceScores.size();
    }
    
    /**
     * 獲取性能報告
     */
    public String getPerformanceReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== AI檢測性能報告 ===\n");
        report.append("總檢測次數: ").append(totalDetections).append("\n");
        report.append("成功檢測次數: ").append(successfulDetections).append("\n");
        report.append("檢測成功率: ").append(String.format("%.1f%%", getDetectionSuccessRate() * 100)).append("\n");
        report.append("平均檢測時間: ").append(String.format("%.1f", getAverageDetectionTime())).append("ms\n");
        report.append("平均置信度: ").append(String.format("%.2f", getAverageConfidence())).append("\n");
        
        if (!detectionTimes.isEmpty()) {
            report.append("最快檢測時間: ").append(getMinDetectionTime()).append("ms\n");
            report.append("最慢檢測時間: ").append(getMaxDetectionTime()).append("ms\n");
        }
        
        return report.toString();
    }
    
    /**
     * 獲取最快檢測時間
     */
    private long getMinDetectionTime() {
        long min = Long.MAX_VALUE;
        for (Long time : detectionTimes) {
            if (time < min) {
                min = time;
            }
        }
        return min == Long.MAX_VALUE ? 0 : min;
    }
    
    /**
     * 獲取最慢檢測時間
     */
    private long getMaxDetectionTime() {
        long max = 0;
        for (Long time : detectionTimes) {
            if (time > max) {
                max = time;
            }
        }
        return max;
    }
    
    /**
     * 重置統計數據
     */
    public void reset() {
        detectionTimes.clear();
        confidenceScores.clear();
        totalDetections = 0;
        successfulDetections = 0;
        Log.d(TAG, "性能監控數據已重置");
    }
    
    /**
     * 檢查性能是否良好
     */
    public boolean isPerformanceGood() {
        float avgTime = getAverageDetectionTime();
        float successRate = getDetectionSuccessRate();
        float avgConfidence = getAverageConfidence();
        
        // 性能良好的標準：
        // 1. 平均檢測時間 < 500ms
        // 2. 檢測成功率 > 80%
        // 3. 平均置信度 > 0.6
        return avgTime < 500 && successRate > 0.8f && avgConfidence > 0.6f;
    }
}
