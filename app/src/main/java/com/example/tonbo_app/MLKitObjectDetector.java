package com.example.tonbo_app;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * ML Kit 物體檢測器（暫時禁用版本）
 * 
 * 注意：由於網絡問題，ML Kit 依賴已暫時禁用
 * 當網絡可以訪問 Google 服務時，可以恢復使用 ML Kit
 * 
 * 恢復步驟：
 * 1. 在 build.gradle.kts 中取消 ML Kit 依賴的註釋
 * 2. 恢復此文件中的 ML Kit 導入和實現
 * 3. 在 ObjectDetectorHelper 中啟用 setupMLKitDetector()
 * 
 * @author Auto (AI Assistant)
 */
public class MLKitObjectDetector {
    private static final String TAG = "MLKitObjectDetector";
    
    private Context context;
    private boolean isInitialized = false;
    
    public MLKitObjectDetector(Context context) {
        this.context = context;
        // ML Kit 暫時禁用，避免編譯錯誤
        isInitialized = false;
        Log.d(TAG, "ML Kit 檢測器已禁用（網絡問題）");
    }
    
    /**
     * 檢測圖像中的物體（暫時禁用）
     * @param bitmap 輸入圖像
     * @return 空的檢測結果列表
     */
    public List<ObjectDetectorHelper.DetectionResult> detect(Bitmap bitmap) {
        // ML Kit 暫時禁用，返回空結果
        Log.d(TAG, "ML Kit 檢測器已禁用，返回空結果");
        return new ArrayList<>();
    }
    
    /**
     * 關閉檢測器，釋放資源
     */
    public void close() {
        Log.d(TAG, "ML Kit 檢測器已關閉（已禁用）");
        isInitialized = false;
    }
    
    /**
     * 檢查檢測器是否已初始化
     */
    public boolean isInitialized() {
        return false;  // 始終返回 false，因為已禁用
    }
}
