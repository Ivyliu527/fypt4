package com.example.tonbo_app;

import android.content.Context;
import android.graphics.Path;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 手勢識別管理器
 * 用於識別和匹配用戶繪製的手勢
 */
public class GestureRecognitionManager {
    private static final String TAG = "GestureRecognition";
    
    private static GestureRecognitionManager instance;
    
    // 手勢模板：Key = 手勢名稱，Value = 點列表
    private Map<String, List<GesturePoint>> templates;
    
    // 手勢綁定：Key = 手勢名稱，Value = 功能名稱
    private Map<String, String> gestureBindings;
    
    private Context context;
    
    private GestureRecognitionManager(Context context) {
        this.context = context;
        templates = new HashMap<>();
        gestureBindings = new HashMap<>();
        loadSavedGestures();
    }
    
    public static synchronized GestureRecognitionManager getInstance(Context context) {
        if (instance == null) {
            instance = new GestureRecognitionManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * 保存手勢模板
     */
    public void saveGesture(String name, List<GesturePoint> points, String functionName) {
        templates.put(name, points);
        gestureBindings.put(name, functionName);
        Log.d(TAG, "保存手勢: " + name + " -> " + functionName);
        persistGestures();
    }
    
    /**
     * 識別手勢
     * @param paths 當前繪畫的路徑列表
     * @return 匹配的手勢名稱，如果未找到返回null
     */
    public String recognizeGesture(List<Path> paths) {
        if (templates.isEmpty() || paths.isEmpty()) {
            return null;
        }
        
        // 將Path轉換為點列表
        List<GesturePoint> inputPoints = new ArrayList<>();
        for (Path path : paths) {
            // 簡化：從Path中提取關鍵點
            // 這裡使用簡化的點提取方法
            inputPoints.addAll(extractPoints(path));
        }
        
        // 與所有模板進行比對
        String bestMatch = null;
        double bestScore = Double.MAX_VALUE;
        
        for (Map.Entry<String, List<GesturePoint>> entry : templates.entrySet()) {
            double score = calculateDistance(inputPoints, entry.getValue());
            Log.d(TAG, "手勢匹配: " + entry.getKey() + " 分數: " + score);
            
            if (score < bestScore && score < 1000.0) { // 閾值可調整
                bestScore = score;
                bestMatch = entry.getKey();
            }
        }
        
        return bestMatch;
    }
    
    /**
     * 獲取手勢綁定的功能
     */
    public String getFunctionForGesture(String gestureName) {
        return gestureBindings.get(gestureName);
    }
    
    /**
     * 獲取所有已保存的手勢
     */
    public Map<String, String> getAllGestures() {
        return new HashMap<>(gestureBindings);
    }
    
    /**
     * 刪除手勢
     */
    public void deleteGesture(String name) {
        templates.remove(name);
        gestureBindings.remove(name);
        persistGestures();
    }
    
    /**
     * 從Path中提取點
     */
    private List<GesturePoint> extractPoints(Path path) {
        List<GesturePoint> points = new ArrayList<>();
        
        // 簡化實現：創建固定數量的樣本點
        // 實際實現需要從Path中提取真實點
        for (int i = 0; i < 20; i++) {
            points.add(new GesturePoint(
                (float) Math.random() * 100,
                (float) Math.random() * 100
            ));
        }
        
        return points;
    }
    
    /**
     * 計算兩個手勢之間的距離
     */
    private double calculateDistance(List<GesturePoint> points1, List<GesturePoint> points2) {
        // 簡化的歐氏距離計算
        // 實際應該使用DTW或類似的時間序列匹配算法
        
        int minSize = Math.min(points1.size(), points2.size());
        double totalDistance = 0;
        
        for (int i = 0; i < minSize; i++) {
            double dx = points1.get(i).x - points2.get(i).x;
            double dy = points1.get(i).y - points2.get(i).y;
            totalDistance += Math.sqrt(dx * dx + dy * dy);
        }
        
        return totalDistance / minSize;
    }
    
    /**
     * 保存手勢到SharedPreferences
     */
    private void persistGestures() {
        // TODO: 使用SharedPreferences保存手勢數據
        Log.d(TAG, "持久化手勢數據");
    }
    
    /**
     * 從SharedPreferences載入手勢
     */
    private void loadSavedGestures() {
        // TODO: 從SharedPreferences載入手勢數據
        Log.d(TAG, "載入已保存的手勢");
    }
    
    /**
     * 手勢點數據類
     */
    public static class GesturePoint {
        public float x;
        public float y;
        
        public GesturePoint(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}
