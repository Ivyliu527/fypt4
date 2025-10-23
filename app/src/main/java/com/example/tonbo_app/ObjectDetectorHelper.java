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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 真實的物體檢測助手
 * 使用TensorFlow Lite Task Vision API
 */
public class ObjectDetectorHelper {
    private static final String TAG = "ObjectDetectorHelper";
    private static final String MODEL_FILE = "ssd_mobilenet_v1.tflite";
    private static final String YOLO_MODEL_FILE = "yolov8n.tflite";
    private static final float SCORE_THRESHOLD = 0.25f;  // 進一步降低閾值，檢測更多潛在物體
    private static final float HIGH_CONFIDENCE_THRESHOLD = 0.55f;  // 降低高置信度閾值
    private static final int MAX_RESULTS = 25;  // 增加結果數，檢測更多物體
    private static final float NMS_THRESHOLD = 0.35f;  // 進一步降低NMS閾值，保留更多檢測結果
    
    // 環境識別相關的物體類別（優先檢測）
    private static final Set<String> ENVIRONMENT_RELEVANT_OBJECTS = new HashSet<>();
    
    static {
        // 環境識別相關的重要物體
        ENVIRONMENT_RELEVANT_OBJECTS.add("person");           // 人
        ENVIRONMENT_RELEVANT_OBJECTS.add("car");              // 汽車
        ENVIRONMENT_RELEVANT_OBJECTS.add("truck");            // 卡車
        ENVIRONMENT_RELEVANT_OBJECTS.add("bus");              // 公車
        ENVIRONMENT_RELEVANT_OBJECTS.add("motorcycle");       // 摩托車
        ENVIRONMENT_RELEVANT_OBJECTS.add("bicycle");          // 腳踏車
        ENVIRONMENT_RELEVANT_OBJECTS.add("traffic light");    // 交通燈
        ENVIRONMENT_RELEVANT_OBJECTS.add("stop sign");        // 停車標誌
        ENVIRONMENT_RELEVANT_OBJECTS.add("bench");            // 長椅
        ENVIRONMENT_RELEVANT_OBJECTS.add("chair");            // 椅子
        ENVIRONMENT_RELEVANT_OBJECTS.add("table");            // 桌子
        ENVIRONMENT_RELEVANT_OBJECTS.add("bed");              // 床
        ENVIRONMENT_RELEVANT_OBJECTS.add("couch");            // 沙發
        ENVIRONMENT_RELEVANT_OBJECTS.add("tv");               // 電視
        ENVIRONMENT_RELEVANT_OBJECTS.add("laptop");           // 筆記本電腦
        ENVIRONMENT_RELEVANT_OBJECTS.add("book");             // 書
        ENVIRONMENT_RELEVANT_OBJECTS.add("bottle");           // 瓶子
        ENVIRONMENT_RELEVANT_OBJECTS.add("cup");              // 杯子
        ENVIRONMENT_RELEVANT_OBJECTS.add("bowl");             // 碗
        ENVIRONMENT_RELEVANT_OBJECTS.add("clock");            // 時鐘
        ENVIRONMENT_RELEVANT_OBJECTS.add("vase");             // 花瓶
        ENVIRONMENT_RELEVANT_OBJECTS.add("scissors");         // 剪刀
        ENVIRONMENT_RELEVANT_OBJECTS.add("teddy bear");       // 泰迪熊
        ENVIRONMENT_RELEVANT_OBJECTS.add("toothbrush");       // 牙刷
        ENVIRONMENT_RELEVANT_OBJECTS.add("hair drier");       // 吹風機
        ENVIRONMENT_RELEVANT_OBJECTS.add("umbrella");         // 雨傘
        ENVIRONMENT_RELEVANT_OBJECTS.add("handbag");          // 手提包
        ENVIRONMENT_RELEVANT_OBJECTS.add("backpack");         // 背包
        ENVIRONMENT_RELEVANT_OBJECTS.add("suitcase");         // 手提箱
        ENVIRONMENT_RELEVANT_OBJECTS.add("frisbee");          // 飛盤
        ENVIRONMENT_RELEVANT_OBJECTS.add("sports ball");      // 運動球
        ENVIRONMENT_RELEVANT_OBJECTS.add("kite");             // 風箏
        ENVIRONMENT_RELEVANT_OBJECTS.add("baseball bat");     // 棒球棒
        ENVIRONMENT_RELEVANT_OBJECTS.add("baseball glove");   // 棒球手套
        ENVIRONMENT_RELEVANT_OBJECTS.add("skateboard");       // 滑板
        ENVIRONMENT_RELEVANT_OBJECTS.add("surfboard");        // 衝浪板
        ENVIRONMENT_RELEVANT_OBJECTS.add("tennis racket");    // 網球拍
        ENVIRONMENT_RELEVANT_OBJECTS.add("bottle");           // 瓶子
        ENVIRONMENT_RELEVANT_OBJECTS.add("wine glass");       // 酒杯
        ENVIRONMENT_RELEVANT_OBJECTS.add("cup");              // 杯子
        ENVIRONMENT_RELEVANT_OBJECTS.add("fork");             // 叉子
        ENVIRONMENT_RELEVANT_OBJECTS.add("knife");            // 刀子
        ENVIRONMENT_RELEVANT_OBJECTS.add("spoon");            // 勺子
        ENVIRONMENT_RELEVANT_OBJECTS.add("bowl");             // 碗
        ENVIRONMENT_RELEVANT_OBJECTS.add("banana");           // 香蕉
        ENVIRONMENT_RELEVANT_OBJECTS.add("apple");            // 蘋果
        ENVIRONMENT_RELEVANT_OBJECTS.add("sandwich");         // 三明治
        ENVIRONMENT_RELEVANT_OBJECTS.add("orange");           // 橙子
        ENVIRONMENT_RELEVANT_OBJECTS.add("broccoli");         // 西蘭花
        ENVIRONMENT_RELEVANT_OBJECTS.add("carrot");           // 胡蘿蔔
        ENVIRONMENT_RELEVANT_OBJECTS.add("hot dog");          // 熱狗
        ENVIRONMENT_RELEVANT_OBJECTS.add("pizza");            // 披薩
        ENVIRONMENT_RELEVANT_OBJECTS.add("donut");            // 甜甜圈
        ENVIRONMENT_RELEVANT_OBJECTS.add("cake");             // 蛋糕
        ENVIRONMENT_RELEVANT_OBJECTS.add("chair");            // 椅子
        ENVIRONMENT_RELEVANT_OBJECTS.add("couch");            // 沙發
        ENVIRONMENT_RELEVANT_OBJECTS.add("bed");              // 床
        ENVIRONMENT_RELEVANT_OBJECTS.add("dining table");     // 餐桌
        ENVIRONMENT_RELEVANT_OBJECTS.add("toilet");           // 廁所
        ENVIRONMENT_RELEVANT_OBJECTS.add("tv");               // 電視
        ENVIRONMENT_RELEVANT_OBJECTS.add("laptop");           // 筆記本電腦
        ENVIRONMENT_RELEVANT_OBJECTS.add("mouse");            // 滑鼠
        ENVIRONMENT_RELEVANT_OBJECTS.add("remote");           // 遙控器
        ENVIRONMENT_RELEVANT_OBJECTS.add("keyboard");         // 鍵盤
        ENVIRONMENT_RELEVANT_OBJECTS.add("cell phone");       // 手機
        ENVIRONMENT_RELEVANT_OBJECTS.add("microwave");        // 微波爐
        ENVIRONMENT_RELEVANT_OBJECTS.add("oven");             // 烤箱
        ENVIRONMENT_RELEVANT_OBJECTS.add("toaster");          // 烤麵包機
        ENVIRONMENT_RELEVANT_OBJECTS.add("sink");             // 水槽
        ENVIRONMENT_RELEVANT_OBJECTS.add("refrigerator");     // 冰箱
        ENVIRONMENT_RELEVANT_OBJECTS.add("book");             // 書
        ENVIRONMENT_RELEVANT_OBJECTS.add("clock");            // 時鐘
        ENVIRONMENT_RELEVANT_OBJECTS.add("vase");             // 花瓶
        ENVIRONMENT_RELEVANT_OBJECTS.add("scissors");         // 剪刀
        ENVIRONMENT_RELEVANT_OBJECTS.add("teddy bear");       // 泰迪熊
        ENVIRONMENT_RELEVANT_OBJECTS.add("hair drier");       // 吹風機
        ENVIRONMENT_RELEVANT_OBJECTS.add("toothbrush");       // 牙刷
    }
    
    // 穩定性增強參數
    private static final int MAX_RETRY_ATTEMPTS = 4;  // 增加重試次數
    private static final long RETRY_DELAY_MS = 50;   // 減少重試延遲
    private static final int MAX_CONSECUTIVE_FAILURES = 5;  // 最大連續失敗次數
    private static final long DETECTION_TIMEOUT_MS = 5000;  // 檢測超時時間
    
    private ObjectDetector objectDetector;
    private YoloDetector yoloDetector;
    private Context context;
    private boolean useYolo = false;  // 是否使用YOLO檢測器
    
    // 穩定性監控變量
    private int consecutiveFailures = 0;
    private long lastSuccessfulDetection = 0;
    private int totalDetections = 0;
    private int successfulDetections = 0;
    private List<DetectionResult> lastSuccessfulResults = new ArrayList<>();
    private long lastDetectionTime = 0;
    
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
            // 環境識別主要使用SSD，YOLO作為備用
            useYolo = false; // 默認禁用YOLO，專注於環境識別
            Log.d(TAG, "✅ YOLO檢測器初始化成功（作為備用）！");
        } catch (Exception e) {
            Log.e(TAG, "❌ 初始化YOLO檢測器失敗: " + e.getMessage());
            useYolo = false;
        }
    }
    
    /**
     * 檢測圖像中的物體 - 使用雙檢測器融合提高準確率和穩定性
     */
    public List<DetectionResult> detect(Bitmap bitmap) {
        List<DetectionResult> results = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        
        if (bitmap == null || bitmap.isRecycled()) {
            Log.w(TAG, "無效的bitmap");
            return getLastSuccessfulResults();
        }
        
        // 檢查檢測頻率，避免過於頻繁
        if (System.currentTimeMillis() - lastDetectionTime < 100) {
            Log.d(TAG, "檢測頻率過高，返回上次結果");
            return getLastSuccessfulResults();
        }
        lastDetectionTime = System.currentTimeMillis();
        
        totalDetections++;
        
        try {
            // 檢查連續失敗次數
            if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                Log.w(TAG, "連續失敗次數過多，重置檢測器狀態");
                resetDetectorState();
            }
            
            // 使用重試機制進行檢測
            results = detectWithRetry(bitmap);
            
            if (!results.isEmpty()) {
                // 檢測成功
                consecutiveFailures = 0;
                successfulDetections++;
                lastSuccessfulDetection = System.currentTimeMillis();
                lastSuccessfulResults = new ArrayList<>(results);
                
                // 應用後處理
                results = applyPostProcessing(results);
                
                Log.d(TAG, String.format("檢測成功: %d 個物體 (成功率: %.1f%%)", 
                    results.size(), (float)successfulDetections / totalDetections * 100));
            } else {
                // 檢測失敗，返回上次成功結果
                Log.w(TAG, "檢測失敗，返回上次成功結果");
                results = getLastSuccessfulResults();
                consecutiveFailures++;
            }
            
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "記憶體不足，檢測失敗: " + e.getMessage());
            System.gc();
            consecutiveFailures++;
            results = getLastSuccessfulResults();
        } catch (Exception e) {
            Log.e(TAG, "檢測過程中發生錯誤: " + e.getMessage());
            consecutiveFailures++;
            results = getLastSuccessfulResults();
        }
        
        long detectionTime = System.currentTimeMillis() - startTime;
        if (detectionTime > 1000) {
            Log.w(TAG, "檢測時間過長: " + detectionTime + "ms");
        }
        
        // 只返回置信度最高的2個物體
        if (results.size() > 2) {
            // 按置信度排序
            Collections.sort(results, new Comparator<DetectionResult>() {
                @Override
                public int compare(DetectionResult a, DetectionResult b) {
                    return Float.compare(b.getConfidence(), a.getConfidence());
                }
            });
            results = results.subList(0, 2);
            Log.d(TAG, "限制檢測結果為2個物體");
        }
        
        return results;
    }
    
    /**
     * 使用重試機制進行檢測
     */
    private List<DetectionResult> detectWithRetry(Bitmap bitmap) {
        List<DetectionResult> results = new ArrayList<>();
        
        for (int attempt = 0; attempt < MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                // 環境識別優先使用SSD檢測器（更適合環境描述）
                if (objectDetector != null) {
                    results = detectWithSSD(bitmap);
                    if (!results.isEmpty()) {
                        Log.d(TAG, String.format("SSD檢測成功 (嘗試 %d/%d): %d 個物體", 
                            attempt + 1, MAX_RETRY_ATTEMPTS, results.size()));
                        break;
                    }
                }
                
                // SSD失敗時才嘗試YOLO（作為備用）
                if (useYolo && yoloDetector != null && results.isEmpty()) {
                    results = detectWithYolo(bitmap);
                    if (!results.isEmpty()) {
                        Log.d(TAG, String.format("YOLO檢測成功 (嘗試 %d/%d): %d 個物體", 
                            attempt + 1, MAX_RETRY_ATTEMPTS, results.size()));
                        break;
                    }
                }
                
                // 如果檢測失敗，等待後重試
                if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Log.w(TAG, "重試延遲被中斷");
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, String.format("檢測嘗試 %d/%d 失敗: %s", 
                    attempt + 1, MAX_RETRY_ATTEMPTS, e.getMessage()));
                if (attempt == MAX_RETRY_ATTEMPTS - 1) {
                    throw e;
                }
            }
        }
        
        return results;
    }
    
    /**
     * 應用後處理
     */
    private List<DetectionResult> applyPostProcessing(List<DetectionResult> results) {
        // 過濾環境相關物體
        results = filterEnvironmentRelevantObjects(results);
        
        // 應用非極大值抑制
        results = applyNMS(results);
        
        // 按置信度排序
        Collections.sort(results, (a, b) -> Float.compare(b.getConfidence(), a.getConfidence()));
        
        // 限制結果數量
        if (results.size() > MAX_RESULTS) {
            results = results.subList(0, MAX_RESULTS);
        }
        
        return results;
    }
    
    /**
     * 過濾環境相關物體 - 增強版本，提高精準度
     */
    private List<DetectionResult> filterEnvironmentRelevantObjects(List<DetectionResult> results) {
        List<DetectionResult> filtered = new ArrayList<>();
        
        for (DetectionResult result : results) {
            // 檢查是否為環境相關物體
            if (ENVIRONMENT_RELEVANT_OBJECTS.contains(result.getLabel())) {
                // 額外檢查置信度，確保檢測質量
                if (result.getConfidence() >= SCORE_THRESHOLD) {
                    filtered.add(result);
                    Log.d(TAG, "保留環境相關物體: " + result.getLabelZh() + " (置信度: " + result.getConfidence() + ")");
                } else {
                    Log.d(TAG, "過濾低置信度環境物體: " + result.getLabelZh() + " (置信度: " + result.getConfidence() + ")");
                }
            } else {
                Log.d(TAG, "過濾非環境物體: " + result.getLabelZh());
            }
        }
        
        // 按置信度排序，優先顯示高置信度結果
        filtered.sort((a, b) -> Float.compare(b.getConfidence(), a.getConfidence()));
        
        Log.d(TAG, String.format("環境物體過濾: %d -> %d", results.size(), filtered.size()));
        return filtered;
    }
    
    /**
     * 獲取上次成功的檢測結果
     */
    private List<DetectionResult> getLastSuccessfulResults() {
        if (lastSuccessfulResults.isEmpty()) {
            Log.d(TAG, "沒有可用的歷史檢測結果");
            return new ArrayList<>();
        }
        
        // 檢查歷史結果是否過期
        if (System.currentTimeMillis() - lastSuccessfulDetection > 10000) { // 10秒過期
            Log.d(TAG, "歷史檢測結果已過期");
            return new ArrayList<>();
        }
        
        Log.d(TAG, "返回歷史檢測結果: " + lastSuccessfulResults.size() + " 個物體");
        return new ArrayList<>(lastSuccessfulResults);
    }
    
    /**
     * 重置檢測器狀態
     */
    private void resetDetectorState() {
        consecutiveFailures = 0;
        useYolo = true; // 重新啟用YOLO
        Log.d(TAG, "檢測器狀態已重置");
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
                    
                    // 檢查邊界框是否為null
                    android.graphics.Rect rect = yoloResult.getBoundingBox();
                    if (rect != null) {
                        // 轉換Rect為RectF
                        android.graphics.RectF rectF = new android.graphics.RectF(
                                rect.left, rect.top, rect.right, rect.bottom
                        );
                        
                        results.add(new DetectionResult(
                                yoloResult.getLabel(),
                                labelZh,
                                yoloResult.getConfidence(),
                                rectF
                        ));
                    } else {
                        // 如果邊界框為null，創建一個默認邊界框
                        Log.w(TAG, "YOLO檢測結果邊界框為null，使用默認邊界框");
                        android.graphics.RectF defaultRect = new android.graphics.RectF(0.1f, 0.1f, 0.9f, 0.9f);
                        results.add(new DetectionResult(
                                yoloResult.getLabel(),
                                labelZh,
                                yoloResult.getConfidence(),
                                defaultRect
                        ));
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "YOLO檢測失敗: " + e.getMessage());
            // YOLO失敗時，嘗試使用SSD檢測器
            Log.d(TAG, "YOLO檢測失敗，嘗試使用SSD檢測器");
            if (objectDetector != null) {
                results = detectWithSSD(bitmap);
            }
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
     * 格式化檢測結果為語音文本 - 專為視障人士優化（簡潔版本）
     */
    public String formatResultsForSpeech(List<DetectionResult> results) {
        if (results.isEmpty()) {
            return getNoObjectsDetectedText();
        }
        
        StringBuilder sb = new StringBuilder();
        
        // 簡潔的物體描述，最多2個物體
        int maxObjects = Math.min(results.size(), 2);
        
        for (int i = 0; i < maxObjects; i++) {
            DetectionResult result = results.get(i);
            
            // 物體名稱 - 根據當前語言選擇對應的標籤
            String objectLabel = getObjectLabelForCurrentLanguage(result);
            sb.append(objectLabel);
            
            // 簡潔的置信度描述
            if (result.getConfidence() > 0.7f) {
                sb.append("（高置信度）");
            }
            
            // 分隔符
            if (i < maxObjects - 1) {
                sb.append("、");
            }
        }
        
        // 如果物體超過2個，添加總數
        if (results.size() > 2) {
            sb.append("等").append(results.size()).append("個物體");
        }
        
        return sb.toString();
    }
    
    /**
     * 獲取未檢測到物體的文本
     */
    private String getNoObjectsDetectedText() {
        String currentLang = LocaleManager.getInstance(context).getCurrentLanguage();
        switch (currentLang) {
            case "english":
                return "No objects detected in the environment";
            case "mandarin":
                return "環境中未檢測到任何物體";
            case "cantonese":
            default:
                return "環境中未檢測到任何物體";
        }
    }
    
    /**
     * 獲取檢測到物體數量的文本
     */
    private String getDetectedObjectsCountText(int count) {
        String currentLang = LocaleManager.getInstance(context).getCurrentLanguage();
        switch (currentLang) {
            case "english":
                return "Detected " + count + " objects: ";
            case "mandarin":
                return "檢測到" + count + "個物體：";
            case "cantonese":
            default:
                return "檢測到" + count + "個物體：";
        }
    }
    
    /**
     * 獲取物體序號文本
     */
    private String getObjectNumberText(int number) {
        String currentLang = LocaleManager.getInstance(context).getCurrentLanguage();
        switch (currentLang) {
            case "english":
                return "Number " + number + ", ";
            case "mandarin":
                return "第" + number + "個，";
            case "cantonese":
            default:
                return "第" + number + "個，";
        }
    }
    
    /**
     * 獲取置信度描述
     */
    private String getConfidenceDescription(float confidence) {
        String currentLang = LocaleManager.getInstance(context).getCurrentLanguage();
        int percentage = Math.round(confidence * 100);
        
        String confidenceText;
        if (percentage >= 80) {
            confidenceText = currentLang.equals("english") ? "high confidence" : "高置信度";
        } else if (percentage >= 60) {
            confidenceText = currentLang.equals("english") ? "medium confidence" : "中等置信度";
        } else {
            confidenceText = currentLang.equals("english") ? "low confidence" : "低置信度";
        }
        
        return "，" + confidenceText + "（" + percentage + "%）";
    }
    
    /**
     * 根據當前語言獲取物體標籤
     */
    private String getObjectLabelForCurrentLanguage(DetectionResult result) {
        String currentLang = LocaleManager.getInstance(context).getCurrentLanguage();
        
        switch (currentLang) {
            case "english":
                return result.getLabel() != null ? result.getLabel() : result.getLabelZh();
            case "mandarin":
                return result.getLabelZh() != null ? result.getLabelZh() : result.getLabel();
            case "cantonese":
            default:
                return result.getLabelZh() != null ? result.getLabelZh() : result.getLabel();
        }
    }
    
    /**
     * 獲取位置描述
     */
    private String getPositionDescription(android.graphics.RectF boundingBox) {
        String currentLang = LocaleManager.getInstance(context).getCurrentLanguage();
        
        // 計算物體在畫面中的大致位置
        float centerX = (boundingBox.left + boundingBox.right) / 2;
        float centerY = (boundingBox.top + boundingBox.bottom) / 2;
        
        String horizontalPos, verticalPos;
        
        // 水平位置
        if (centerX < 0.33f) {
            horizontalPos = currentLang.equals("english") ? "left side" : "左側";
        } else if (centerX < 0.67f) {
            horizontalPos = currentLang.equals("english") ? "center" : "中央";
        } else {
            horizontalPos = currentLang.equals("english") ? "right side" : "右側";
        }
        
        // 垂直位置
        if (centerY < 0.33f) {
            verticalPos = currentLang.equals("english") ? "top" : "上方";
        } else if (centerY < 0.67f) {
            verticalPos = currentLang.equals("english") ? "middle" : "中間";
        } else {
            verticalPos = currentLang.equals("english") ? "bottom" : "下方";
        }
        
        String positionText = currentLang.equals("english") 
            ? " located at " + verticalPos + " " + horizontalPos
            : "位於" + verticalPos + horizontalPos;
            
        return positionText;
    }
    
    /**
     * 獲取檢測器穩定性統計
     */
    public String getStabilityStats() {
        float successRate = totalDetections > 0 ? (float)successfulDetections / totalDetections * 100 : 0;
        long timeSinceLastSuccess = System.currentTimeMillis() - lastSuccessfulDetection;
        
        return String.format("檢測統計 - 總檢測: %d, 成功: %d, 成功率: %.1f%%, 連續失敗: %d, 上次成功: %d秒前", 
            totalDetections, successfulDetections, successRate, consecutiveFailures, timeSinceLastSuccess / 1000);
    }
    
    /**
     * 檢查檢測器健康狀態
     */
    public boolean isHealthy() {
        return consecutiveFailures < MAX_CONSECUTIVE_FAILURES && 
               (System.currentTimeMillis() - lastSuccessfulDetection) < 30000; // 30秒內有成功檢測
    }
    
    /**
     * 強制重置檢測器
     */
    public void forceReset() {
        Log.d(TAG, "強制重置檢測器");
        consecutiveFailures = 0;
        useYolo = true;
        lastSuccessfulDetection = 0;
        lastSuccessfulResults.clear();
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
        
        // 輸出最終統計
        Log.d(TAG, getStabilityStats());
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

