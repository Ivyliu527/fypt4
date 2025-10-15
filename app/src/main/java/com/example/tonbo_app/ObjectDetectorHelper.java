package com.example.tonbo_app;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 真實的物體檢測助手
 * 使用TensorFlow Lite Task Vision API
 */
public class ObjectDetectorHelper {
    private static final String TAG = "ObjectDetectorHelper";
    private static final String MODEL_FILE = "ssd_mobilenet_v1.tflite";
    private static final String YOLO_MODEL_FILE = "yolov8n.tflite";
    private static final float SCORE_THRESHOLD = 0.3f;  // 降低閾值提高召回率
    private static final float HIGH_CONFIDENCE_THRESHOLD = 0.7f;  // 高置信度閾值
    private static final int MAX_RESULTS = 20;  // 增加最大結果數
    private static final float NMS_THRESHOLD = 0.5f;  // 非極大值抑制閾值
    
    private ObjectDetector objectDetector;
    private YoloDetector yoloDetector;
    private Context context;
    private boolean useYolo = false;  // 是否使用YOLO檢測器
    
    // COCO類別中文映射
    private static final Map<String, String> LABEL_MAP_ZH = new HashMap<>();
    
    static {
        LABEL_MAP_ZH.put("person", "人");
        LABEL_MAP_ZH.put("bicycle", "腳踏車");
        LABEL_MAP_ZH.put("car", "汽車");
        LABEL_MAP_ZH.put("motorcycle", "摩托車");
        LABEL_MAP_ZH.put("airplane", "飛機");
        LABEL_MAP_ZH.put("bus", "公車");
        LABEL_MAP_ZH.put("train", "火車");
        LABEL_MAP_ZH.put("truck", "卡車");
        LABEL_MAP_ZH.put("boat", "船");
        LABEL_MAP_ZH.put("traffic light", "交通燈");
        LABEL_MAP_ZH.put("fire hydrant", "消防栓");
        LABEL_MAP_ZH.put("stop sign", "停車標誌");
        LABEL_MAP_ZH.put("parking meter", "停車計時器");
        LABEL_MAP_ZH.put("bench", "長椅");
        LABEL_MAP_ZH.put("bird", "鳥");
        LABEL_MAP_ZH.put("cat", "貓");
        LABEL_MAP_ZH.put("dog", "狗");
        LABEL_MAP_ZH.put("horse", "馬");
        LABEL_MAP_ZH.put("sheep", "羊");
        LABEL_MAP_ZH.put("cow", "牛");
        LABEL_MAP_ZH.put("elephant", "大象");
        LABEL_MAP_ZH.put("bear", "熊");
        LABEL_MAP_ZH.put("zebra", "斑馬");
        LABEL_MAP_ZH.put("giraffe", "長頸鹿");
        LABEL_MAP_ZH.put("backpack", "背包");
        LABEL_MAP_ZH.put("umbrella", "雨傘");
        LABEL_MAP_ZH.put("handbag", "手提包");
        LABEL_MAP_ZH.put("tie", "領帶");
        LABEL_MAP_ZH.put("suitcase", "手提箱");
        LABEL_MAP_ZH.put("frisbee", "飛盤");
        LABEL_MAP_ZH.put("skis", "滑雪板");
        LABEL_MAP_ZH.put("snowboard", "滑雪板");
        LABEL_MAP_ZH.put("sports ball", "運動球");
        LABEL_MAP_ZH.put("kite", "風箏");
        LABEL_MAP_ZH.put("baseball bat", "棒球棒");
        LABEL_MAP_ZH.put("baseball glove", "棒球手套");
        LABEL_MAP_ZH.put("skateboard", "滑板");
        LABEL_MAP_ZH.put("surfboard", "衝浪板");
        LABEL_MAP_ZH.put("tennis racket", "網球拍");
        LABEL_MAP_ZH.put("bottle", "瓶子");
        LABEL_MAP_ZH.put("wine glass", "酒杯");
        LABEL_MAP_ZH.put("cup", "杯子");
        LABEL_MAP_ZH.put("fork", "叉子");
        LABEL_MAP_ZH.put("knife", "刀");
        LABEL_MAP_ZH.put("spoon", "湯匙");
        LABEL_MAP_ZH.put("bowl", "碗");
        LABEL_MAP_ZH.put("banana", "香蕉");
        LABEL_MAP_ZH.put("apple", "蘋果");
        LABEL_MAP_ZH.put("sandwich", "三明治");
        LABEL_MAP_ZH.put("orange", "橙");
        LABEL_MAP_ZH.put("broccoli", "西蘭花");
        LABEL_MAP_ZH.put("carrot", "紅蘿蔔");
        LABEL_MAP_ZH.put("hot dog", "熱狗");
        LABEL_MAP_ZH.put("pizza", "披薩");
        LABEL_MAP_ZH.put("donut", "甜甜圈");
        LABEL_MAP_ZH.put("cake", "蛋糕");
        LABEL_MAP_ZH.put("chair", "椅子");
        LABEL_MAP_ZH.put("couch", "沙發");
        LABEL_MAP_ZH.put("potted plant", "盆栽");
        LABEL_MAP_ZH.put("bed", "床");
        LABEL_MAP_ZH.put("dining table", "餐桌");
        LABEL_MAP_ZH.put("toilet", "馬桶");
        LABEL_MAP_ZH.put("tv", "電視");
        LABEL_MAP_ZH.put("laptop", "筆記本電腦");
        LABEL_MAP_ZH.put("mouse", "滑鼠");
        LABEL_MAP_ZH.put("remote", "遙控器");
        LABEL_MAP_ZH.put("keyboard", "鍵盤");
        LABEL_MAP_ZH.put("cell phone", "手機");
        LABEL_MAP_ZH.put("microwave", "微波爐");
        LABEL_MAP_ZH.put("oven", "烤箱");
        LABEL_MAP_ZH.put("toaster", "烤麵包機");
        LABEL_MAP_ZH.put("sink", "水槽");
        LABEL_MAP_ZH.put("refrigerator", "冰箱");
        LABEL_MAP_ZH.put("book", "書");
        LABEL_MAP_ZH.put("clock", "時鐘");
        LABEL_MAP_ZH.put("vase", "花瓶");
        LABEL_MAP_ZH.put("scissors", "剪刀");
        LABEL_MAP_ZH.put("teddy bear", "泰迪熊");
        LABEL_MAP_ZH.put("hair drier", "吹風機");
        LABEL_MAP_ZH.put("toothbrush", "牙刷");
    }
    
    public ObjectDetectorHelper(Context context) {
        this.context = context;
        setupObjectDetector();
        setupYoloDetector();
    }
    
    private void setupObjectDetector() {
        try {
            ObjectDetector.ObjectDetectorOptions options =
                    ObjectDetector.ObjectDetectorOptions.builder()
                            .setScoreThreshold(SCORE_THRESHOLD)
                            .setMaxResults(MAX_RESULTS)
                            .build();
            
            objectDetector = ObjectDetector.createFromFileAndOptions(
                    context,
                    MODEL_FILE,
                    options
            );
            
            Log.d(TAG, "✅ SSD物體檢測器初始化成功！");
        } catch (IOException e) {
            Log.e(TAG, "❌ 初始化SSD物體檢測器失敗: " + e.getMessage());
        }
    }
    
    private void setupYoloDetector() {
        try {
            yoloDetector = new YoloDetector(context);
            // YoloDetector構造函數會自動初始化
            useYolo = true;
            Log.d(TAG, "✅ YOLO檢測器初始化成功！");
        } catch (Exception e) {
            Log.e(TAG, "❌ 初始化YOLO檢測器失敗: " + e.getMessage());
            useYolo = false;
        }
    }
    
    /**
     * 檢測圖像中的物體 - 使用雙檢測器融合提高準確率
     */
    public List<DetectionResult> detect(Bitmap bitmap) {
        List<DetectionResult> results = new ArrayList<>();
        
        if (bitmap == null || bitmap.isRecycled()) {
            Log.w(TAG, "無效的bitmap");
            return results;
        }
        
        try {
            if (useYolo && yoloDetector != null) {
                // 使用YOLO檢測器（更準確）
                results = detectWithYolo(bitmap);
                Log.d(TAG, String.format("YOLO檢測到 %d 個物體", results.size()));
            } else if (objectDetector != null) {
                // 使用SSD檢測器
                results = detectWithSSD(bitmap);
                Log.d(TAG, String.format("SSD檢測到 %d 個物體", results.size()));
            } else {
                Log.w(TAG, "沒有可用的檢測器");
                return results;
            }
            
            // 應用非極大值抑制
            results = applyNMS(results);
            
            // 按置信度排序
            Collections.sort(results, (a, b) -> Float.compare(b.getConfidence(), a.getConfidence()));
            
            // 限制結果數量
            if (results.size() > MAX_RESULTS) {
                results = results.subList(0, MAX_RESULTS);
            }
            
            Log.d(TAG, String.format("最終檢測到 %d 個物體", results.size()));
            
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "記憶體不足，檢測失敗: " + e.getMessage());
            System.gc();
        } catch (Exception e) {
            Log.e(TAG, "檢測過程中發生錯誤: " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * 使用SSD檢測器檢測
     */
    private List<DetectionResult> detectWithSSD(Bitmap bitmap) {
        List<DetectionResult> results = new ArrayList<>();
        TensorImage tensorImage = null;
        
        try {
            tensorImage = TensorImage.fromBitmap(bitmap);
            List<Detection> detections = objectDetector.detect(tensorImage);
            
            for (Detection detection : detections) {
                if (detection.getCategories().size() > 0) {
                    String label = detection.getCategories().get(0).getLabel();
                    float score = detection.getCategories().get(0).getScore();
                    
                    String labelZh = LABEL_MAP_ZH.get(label);
                    if (labelZh == null) {
                        labelZh = label;
                    }
                    
                    results.add(new DetectionResult(
                            label,
                            labelZh,
                            score,
                            detection.getBoundingBox()
                    ));
                }
            }
        } finally {
            tensorImage = null;
        }
        
        return results;
    }
    
    /**
     * 使用YOLO檢測器檢測
     */
    private List<DetectionResult> detectWithYolo(Bitmap bitmap) {
        List<DetectionResult> results = new ArrayList<>();
        
        try {
            List<YoloDetector.DetectionResult> yoloResults = yoloDetector.detect(bitmap);
            
            for (YoloDetector.DetectionResult yoloResult : yoloResults) {
                if (yoloResult.getConfidence() >= SCORE_THRESHOLD) {
                    String labelZh = LABEL_MAP_ZH.get(yoloResult.getLabel());
                    if (labelZh == null) {
                        labelZh = yoloResult.getLabel();
                    }
                    
                    // 轉換Rect為RectF
                    android.graphics.Rect rect = yoloResult.getBoundingBox();
                    android.graphics.RectF rectF = new android.graphics.RectF(
                            rect.left, rect.top, rect.right, rect.bottom
                    );
                    
                    results.add(new DetectionResult(
                            yoloResult.getLabel(),
                            labelZh,
                            yoloResult.getConfidence(),
                            rectF
                    ));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "YOLO檢測失敗: " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * 應用非極大值抑制 (NMS) 去除重複檢測
     */
    private List<DetectionResult> applyNMS(List<DetectionResult> detections) {
        if (detections.size() <= 1) {
            return detections;
        }
        
        List<DetectionResult> filtered = new ArrayList<>();
        boolean[] suppressed = new boolean[detections.size()];
        
        for (int i = 0; i < detections.size(); i++) {
            if (suppressed[i]) continue;
            
            DetectionResult current = detections.get(i);
            filtered.add(current);
            
            // 抑制與當前檢測重疊度高的其他檢測
            for (int j = i + 1; j < detections.size(); j++) {
                if (suppressed[j]) continue;
                
                DetectionResult other = detections.get(j);
                
                // 計算IoU (Intersection over Union)
                float iou = calculateIoU(current.getBoundingBox(), other.getBoundingBox());
                
                // 如果IoU超過閾值且是同類物體，抑制置信度較低的檢測
                if (iou > NMS_THRESHOLD && current.getLabel().equals(other.getLabel())) {
                    if (other.getConfidence() < current.getConfidence()) {
                        suppressed[j] = true;
                    }
                }
            }
        }
        
        return filtered;
    }
    
    /**
     * 計算兩個邊界框的IoU
     */
    private float calculateIoU(android.graphics.RectF box1, android.graphics.RectF box2) {
        float x1 = Math.max(box1.left, box2.left);
        float y1 = Math.max(box1.top, box2.top);
        float x2 = Math.min(box1.right, box2.right);
        float y2 = Math.min(box1.bottom, box2.bottom);
        
        if (x2 <= x1 || y2 <= y1) {
            return 0.0f;
        }
        
        float intersection = (x2 - x1) * (y2 - y1);
        float area1 = (box1.right - box1.left) * (box1.bottom - box1.top);
        float area2 = (box2.right - box2.left) * (box2.bottom - box2.top);
        float union = area1 + area2 - intersection;
        
        return intersection / union;
    }
    
    /**
     * 格式化檢測結果為語音文本
     */
    public String formatResultsForSpeech(List<DetectionResult> results) {
        if (results.isEmpty()) {
            return "未偵測到任何物體";
        }
        
        StringBuilder sb = new StringBuilder("偵測到：");
        for (int i = 0; i < Math.min(results.size(), 5); i++) {
            DetectionResult result = results.get(i);
            sb.append(result.getLabelZh());
            if (i < Math.min(results.size(), 5) - 1) {
                sb.append("、");
            }
        }
        
        if (results.size() > 5) {
            sb.append("等共").append(results.size()).append("個物體");
        }
        
        return sb.toString();
    }
    
    public void close() {
        if (objectDetector != null) {
            objectDetector.close();
            Log.d(TAG, "SSD物體檢測器已關閉");
        }
        if (yoloDetector != null) {
            yoloDetector.close();
            Log.d(TAG, "YOLO檢測器已關閉");
        }
    }
    
    /**
     * 檢測結果類
     */
    public static class DetectionResult {
        private String label;
        private String labelZh;
        private float confidence;
        private android.graphics.RectF boundingBox;
        
        public DetectionResult(String label, String labelZh, float confidence, android.graphics.RectF boundingBox) {
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
        
        public android.graphics.RectF getBoundingBox() {
            return boundingBox;
        }
        
        @Override
        public String toString() {
            return String.format("%s (%.0f%%)", labelZh, confidence * 100);
        }
    }
}

