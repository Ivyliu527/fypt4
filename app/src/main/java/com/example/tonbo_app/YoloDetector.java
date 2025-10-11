package com.example.tonbo_app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;

import androidx.camera.core.ImageProxy;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * YOLO 物體檢測器
 * 基於 Ultralytics YOLO 模型的檢測器封裝
 */
public class YoloDetector {
    private static final String TAG = "YoloDetector";
    
    private Context context;
    private boolean isInitialized = false;
    
    // COCO 數據集類別名稱（繁體中文）
    private static final Map<String, String> CLASS_NAMES_ZH = new HashMap<>();
    
    static {
        CLASS_NAMES_ZH.put("person", "人");
        CLASS_NAMES_ZH.put("bicycle", "腳踏車");
        CLASS_NAMES_ZH.put("car", "汽車");
        CLASS_NAMES_ZH.put("motorcycle", "摩托車");
        CLASS_NAMES_ZH.put("airplane", "飛機");
        CLASS_NAMES_ZH.put("bus", "公車");
        CLASS_NAMES_ZH.put("train", "火車");
        CLASS_NAMES_ZH.put("truck", "卡車");
        CLASS_NAMES_ZH.put("boat", "船");
        CLASS_NAMES_ZH.put("traffic light", "交通燈");
        CLASS_NAMES_ZH.put("fire hydrant", "消防栓");
        CLASS_NAMES_ZH.put("stop sign", "停車標誌");
        CLASS_NAMES_ZH.put("parking meter", "停車計時器");
        CLASS_NAMES_ZH.put("bench", "長椅");
        CLASS_NAMES_ZH.put("bird", "鳥");
        CLASS_NAMES_ZH.put("cat", "貓");
        CLASS_NAMES_ZH.put("dog", "狗");
        CLASS_NAMES_ZH.put("horse", "馬");
        CLASS_NAMES_ZH.put("sheep", "羊");
        CLASS_NAMES_ZH.put("cow", "牛");
        CLASS_NAMES_ZH.put("elephant", "大象");
        CLASS_NAMES_ZH.put("bear", "熊");
        CLASS_NAMES_ZH.put("zebra", "斑馬");
        CLASS_NAMES_ZH.put("giraffe", "長頸鹿");
        CLASS_NAMES_ZH.put("backpack", "背包");
        CLASS_NAMES_ZH.put("umbrella", "雨傘");
        CLASS_NAMES_ZH.put("handbag", "手提包");
        CLASS_NAMES_ZH.put("tie", "領帶");
        CLASS_NAMES_ZH.put("suitcase", "手提箱");
        CLASS_NAMES_ZH.put("frisbee", "飛盤");
        CLASS_NAMES_ZH.put("skis", "滑雪板");
        CLASS_NAMES_ZH.put("snowboard", "滑雪板");
        CLASS_NAMES_ZH.put("sports ball", "運動球");
        CLASS_NAMES_ZH.put("kite", "風箏");
        CLASS_NAMES_ZH.put("baseball bat", "棒球棒");
        CLASS_NAMES_ZH.put("baseball glove", "棒球手套");
        CLASS_NAMES_ZH.put("skateboard", "滑板");
        CLASS_NAMES_ZH.put("surfboard", "衝浪板");
        CLASS_NAMES_ZH.put("tennis racket", "網球拍");
        CLASS_NAMES_ZH.put("bottle", "瓶子");
        CLASS_NAMES_ZH.put("wine glass", "酒杯");
        CLASS_NAMES_ZH.put("cup", "杯子");
        CLASS_NAMES_ZH.put("fork", "叉子");
        CLASS_NAMES_ZH.put("knife", "刀");
        CLASS_NAMES_ZH.put("spoon", "湯匙");
        CLASS_NAMES_ZH.put("bowl", "碗");
        CLASS_NAMES_ZH.put("banana", "香蕉");
        CLASS_NAMES_ZH.put("apple", "蘋果");
        CLASS_NAMES_ZH.put("sandwich", "三明治");
        CLASS_NAMES_ZH.put("orange", "橙");
        CLASS_NAMES_ZH.put("broccoli", "西蘭花");
        CLASS_NAMES_ZH.put("carrot", "紅蘿蔔");
        CLASS_NAMES_ZH.put("hot dog", "熱狗");
        CLASS_NAMES_ZH.put("pizza", "披薩");
        CLASS_NAMES_ZH.put("donut", "甜甜圈");
        CLASS_NAMES_ZH.put("cake", "蛋糕");
        CLASS_NAMES_ZH.put("chair", "椅子");
        CLASS_NAMES_ZH.put("couch", "沙發");
        CLASS_NAMES_ZH.put("potted plant", "盆栽");
        CLASS_NAMES_ZH.put("bed", "床");
        CLASS_NAMES_ZH.put("dining table", "餐桌");
        CLASS_NAMES_ZH.put("toilet", "馬桶");
        CLASS_NAMES_ZH.put("tv", "電視");
        CLASS_NAMES_ZH.put("laptop", "筆記本電腦");
        CLASS_NAMES_ZH.put("mouse", "滑鼠");
        CLASS_NAMES_ZH.put("remote", "遙控器");
        CLASS_NAMES_ZH.put("keyboard", "鍵盤");
        CLASS_NAMES_ZH.put("cell phone", "手機");
        CLASS_NAMES_ZH.put("microwave", "微波爐");
        CLASS_NAMES_ZH.put("oven", "烤箱");
        CLASS_NAMES_ZH.put("toaster", "烤麵包機");
        CLASS_NAMES_ZH.put("sink", "水槽");
        CLASS_NAMES_ZH.put("refrigerator", "冰箱");
        CLASS_NAMES_ZH.put("book", "書");
        CLASS_NAMES_ZH.put("clock", "時鐘");
        CLASS_NAMES_ZH.put("vase", "花瓶");
        CLASS_NAMES_ZH.put("scissors", "剪刀");
        CLASS_NAMES_ZH.put("teddy bear", "泰迪熊");
        CLASS_NAMES_ZH.put("hair drier", "吹風機");
        CLASS_NAMES_ZH.put("toothbrush", "牙刷");
    }
    
    public YoloDetector(Context context) {
        this.context = context;
        initialize();
    }
    
    private void initialize() {
        try {
            // TODO: 初始化 YOLO 模型
            // 1. 從 assets 載入模型文件
            // 2. 初始化 TensorFlow Lite 解釋器
            // 3. 設置輸入輸出張量
            
            isInitialized = true;
            Log.d(TAG, "YOLO 檢測器初始化成功");
        } catch (Exception e) {
            Log.e(TAG, "YOLO 檢測器初始化失敗: " + e.getMessage());
            isInitialized = false;
        }
    }
    
    /**
     * 檢測圖像中的物體
     */
    public List<DetectionResult> detect(ImageProxy image) {
        if (!isInitialized) {
            Log.w(TAG, "檢測器尚未初始化");
            return new ArrayList<>();
        }
        
        try {
            // 將 ImageProxy 轉換為 Bitmap
            Bitmap bitmap = imageProxyToBitmap(image);
            if (bitmap == null) {
                return new ArrayList<>();
            }
            
            // TODO: 使用 YOLO 模型進行推理
            // 目前返回模擬數據
            return getMockDetections();
            
        } catch (Exception e) {
            Log.e(TAG, "檢測失敗: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 檢測 Bitmap 圖像
     */
    public List<DetectionResult> detect(Bitmap bitmap) {
        if (!isInitialized) {
            return new ArrayList<>();
        }
        
        try {
            // TODO: YOLO 推理邏輯
            return getMockDetections();
        } catch (Exception e) {
            Log.e(TAG, "檢測失敗: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 將 ImageProxy 轉換為 Bitmap
     */
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

            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, out);
            byte[] imageBytes = out.toByteArray();
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        } catch (Exception e) {
            Log.e(TAG, "圖像轉換失敗: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 獲取模擬檢測結果（用於測試）
     * 隨機返回不同的物體組合
     */
    private List<DetectionResult> getMockDetections() {
        List<DetectionResult> results = new ArrayList<>();
        
        // 隨機選擇3-5個物體
        java.util.Random random = new java.util.Random();
        String[][] mockObjects = {
            {"person", "人"},
            {"chair", "椅子"},
            {"cup", "杯子"},
            {"cell phone", "手機"},
            {"laptop", "筆記本電腦"},
            {"book", "書"},
            {"bottle", "瓶子"},
            {"clock", "時鐘"},
            {"keyboard", "鍵盤"},
            {"mouse", "滑鼠"},
            {"tv", "電視"},
            {"remote", "遙控器"},
            {"dining table", "餐桌"},
            {"couch", "沙發"},
            {"potted plant", "盆栽"},
            {"vase", "花瓶"},
            {"scissors", "剪刀"},
            {"backpack", "背包"},
            {"umbrella", "雨傘"},
            {"handbag", "手提包"}
        };
        
        // 隨機選擇3-5個不同的物體
        int numObjects = 3 + random.nextInt(3); // 3到5個物體
        java.util.Set<Integer> selectedIndices = new java.util.HashSet<>();
        
        while (selectedIndices.size() < numObjects && selectedIndices.size() < mockObjects.length) {
            int index = random.nextInt(mockObjects.length);
            if (selectedIndices.add(index)) {
                String[] obj = mockObjects[index];
                float confidence = 0.75f + random.nextFloat() * 0.24f; // 0.75-0.99
                results.add(new DetectionResult(obj[0], obj[1], confidence));
            }
        }
        
        // 按置信度排序
        results.sort((a, b) -> Float.compare(b.getConfidence(), a.getConfidence()));
        
        return results;
    }
    
    /**
     * 格式化檢測結果為語音文本
     */
    public String formatResultsForSpeech(List<DetectionResult> results) {
        if (results.isEmpty()) {
            return "未偵測到任何物體";
        }
        
        StringBuilder sb = new StringBuilder("偵測到：");
        for (int i = 0; i < results.size(); i++) {
            DetectionResult result = results.get(i);
            sb.append(result.getLabelZh());
            if (i < results.size() - 1) {
                sb.append("、");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * 獲取中文類別名稱
     */
    public static String getChineseLabel(String englishLabel) {
        return CLASS_NAMES_ZH.getOrDefault(englishLabel, englishLabel);
    }
    
    public void close() {
        // TODO: 釋放模型資源
        isInitialized = false;
    }
    
    /**
     * 檢測結果類
     */
    public static class DetectionResult {
        private String label;
        private String labelZh;
        private float confidence;
        private Rect boundingBox;
        
        public DetectionResult(String label, String labelZh, float confidence) {
            this.label = label;
            this.labelZh = labelZh;
            this.confidence = confidence;
        }
        
        public DetectionResult(String label, String labelZh, float confidence, Rect boundingBox) {
            this.label = label;
            this.labelZh = labelZh;
            this.confidence = confidence;
            this.boundingBox = boundingBox;
        }
        
        public String getLabel() {
            return label;
        }
        
        public String getLabelZh() {
            return labelZh;
        }
        
        public float getConfidence() {
            return confidence;
        }
        
        public Rect getBoundingBox() {
            return boundingBox;
        }
        
        @Override
        public String toString() {
            return String.format("%s (%.2f%%)", labelZh, confidence * 100);
        }
    }
}

