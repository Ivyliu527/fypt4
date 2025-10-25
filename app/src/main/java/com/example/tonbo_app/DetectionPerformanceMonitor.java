package com.example.tonbo_app;

import android.util.Log;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * 檢測性能監控器 - 優化版本
 * 使用更高效的數據結構和算法
 */
public class DetectionPerformanceMonitor {
    private static final String TAG = "DetectionPerformance";
    
    // 使用Deque替代ArrayList，提供更好的性能
    private final Deque<Long> detectionTimes = new ArrayDeque<>();
    private final Deque<Float> confidenceScores = new ArrayDeque<>();
    
    private int totalDetections = 0;
    private int successfulDetections = 0;
    private long totalDetectionTime = 0;
    private float totalConfidence = 0f;
    
    /**
     * 記錄檢測時間 - 優化版本
     */
    public void recordDetectionTime(long detectionTimeMs) {
        detectionTimes.offerLast(detectionTimeMs);
        totalDetectionTime += detectionTimeMs;
        totalDetections++;
        
        // 保持最近N次檢測的記錄，移除最舊的
        if (detectionTimes.size() > AppConstants.MAX_DETECTION_TIME_RECORDS) {
            Long removed = detectionTimes.pollFirst();
            if (removed != null) {
                totalDetectionTime -= removed;
            }
        }
        
        Log.d(TAG, "檢測時間: " + detectionTimeMs + "ms");
    }
    
    /**
     * 記錄檢測結果 - 優化版本
     */
    public void recordDetectionResult(List<YoloDetector.DetectionResult> results) {
        if (results != null && !results.isEmpty()) {
            successfulDetections++;
            
            // 記錄置信度分數
            for (YoloDetector.DetectionResult result : results) {
                float confidence = result.getConfidence();
                confidenceScores.offerLast(confidence);
                totalConfidence += confidence;
                
                // 保持最近N個置信度記錄
                if (confidenceScores.size() > AppConstants.MAX_CONFIDENCE_RECORDS) {
                    Float removed = confidenceScores.pollFirst();
                    if (removed != null) {
                        totalConfidence -= removed;
                    }
                }
            }
            
            Log.d(TAG, "檢測到 " + results.size() + " 個物體");
        }
    }
    
    /**
     * 獲取平均檢測時間 - 優化版本
     */
    public float getAverageDetectionTime() {
        return detectionTimes.isEmpty() ? 0f : (float) totalDetectionTime / detectionTimes.size();
    }
    
    /**
     * 獲取檢測成功率
     */
    public float getSuccessRate() {
        return totalDetections == 0 ? 0f : (float) successfulDetections / totalDetections * 100f;
    }
    
    /**
     * 獲取平均置信度 - 優化版本
     */
    public float getAverageConfidence() {
        return confidenceScores.isEmpty() ? 0f : totalConfidence / confidenceScores.size();
    }
    
    /**
     * 獲取性能報告
     */
    public String getPerformanceReport() {
        return String.format(
            "檢測性能報告:\n" +
            "- 總檢測次數: %d\n" +
            "- 成功檢測次數: %d\n" +
            "- 成功率: %.1f%%\n" +
            "- 平均檢測時間: %.1fms\n" +
            "- 平均置信度: %.3f",
            totalDetections,
            successfulDetections,
            getSuccessRate(),
            getAverageDetectionTime(),
            getAverageConfidence()
        );
    }
    
    /**
     * 檢查性能是否良好
     */
    public boolean isPerformanceGood() {
        return getSuccessRate() > 70f && 
               getAverageDetectionTime() < 500f && 
               getAverageConfidence() > 0.5f;
    }
    
    /**
     * 重置統計數據
     */
    public void reset() {
        detectionTimes.clear();
        confidenceScores.clear();
        totalDetections = 0;
        successfulDetections = 0;
        totalDetectionTime = 0;
        totalConfidence = 0f;
        Log.d(TAG, "性能統計已重置");
    }
}