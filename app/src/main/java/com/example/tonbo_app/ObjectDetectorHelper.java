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
    
    // 環境識別相關的物體類別（只檢測這些常見物體）
    private static final Set<String> ENVIRONMENT_RELEVANT_OBJECTS = new HashSet<>();
    
    // 黑名單：明確排除不常見或不相關的物體
    private static final Set<String> EXCLUDED_OBJECTS = new HashSet<>();
    
    static {
        // 黑名單：排除不常見的物體
        EXCLUDED_OBJECTS.add("giraffe");          // 長頸鹿
        EXCLUDED_OBJECTS.add("zebra");            // 斑馬
        EXCLUDED_OBJECTS.add("elephant");         // 大象
        EXCLUDED_OBJECTS.add("bear");             // 熊
        EXCLUDED_OBJECTS.add("cow");              // 牛
        EXCLUDED_OBJECTS.add("sheep");            // 羊
        EXCLUDED_OBJECTS.add("horse");            // 馬
        EXCLUDED_OBJECTS.add("airplane");         // 飛機（通常不在室內環境）
        EXCLUDED_OBJECTS.add("train");            // 火車
        EXCLUDED_OBJECTS.add("boat");             // 船
        EXCLUDED_OBJECTS.add("surfboard");        // 衝浪板
        EXCLUDED_OBJECTS.add("kite");             // 風箏
        EXCLUDED_OBJECTS.add("frisbee");          // 飛盤
        EXCLUDED_OBJECTS.add("baseball bat");     // 棒球棒
        EXCLUDED_OBJECTS.add("baseball glove");   // 棒球手套
        EXCLUDED_OBJECTS.add("tennis racket");    // 網球拍
        EXCLUDED_OBJECTS.add("skateboard");       // 滑板
        
        // 環境識別相關的重要物體（白名單）- 只包含對視障人士有用的常見物體
        // 交通相關
        ENVIRONMENT_RELEVANT_OBJECTS.add("person");           // 人
        ENVIRONMENT_RELEVANT_OBJECTS.add("car");              // 汽車
        ENVIRONMENT_RELEVANT_OBJECTS.add("truck");            // 卡車
        ENVIRONMENT_RELEVANT_OBJECTS.add("bus");              // 公車
        ENVIRONMENT_RELEVANT_OBJECTS.add("motorcycle");       // 摩托車
        ENVIRONMENT_RELEVANT_OBJECTS.add("bicycle");          // 腳踏車
        ENVIRONMENT_RELEVANT_OBJECTS.add("traffic light");    // 交通燈
        ENVIRONMENT_RELEVANT_OBJECTS.add("stop sign");        // 停車標誌
        
        // 家具
        ENVIRONMENT_RELEVANT_OBJECTS.add("bench");            // 長椅
        ENVIRONMENT_RELEVANT_OBJECTS.add("chair");            // 椅子
        ENVIRONMENT_RELEVANT_OBJECTS.add("table");            // 桌子
        ENVIRONMENT_RELEVANT_OBJECTS.add("dining table");     // 餐桌
        ENVIRONMENT_RELEVANT_OBJECTS.add("bed");              // 床
        ENVIRONMENT_RELEVANT_OBJECTS.add("couch");            // 沙發
        ENVIRONMENT_RELEVANT_OBJECTS.add("toilet");           // 廁所
        
        // 電子設備
        ENVIRONMENT_RELEVANT_OBJECTS.add("tv");               // 電視
        ENVIRONMENT_RELEVANT_OBJECTS.add("laptop");           // 筆記本電腦
        ENVIRONMENT_RELEVANT_OBJECTS.add("mouse");            // 滑鼠
        ENVIRONMENT_RELEVANT_OBJECTS.add("remote");           // 遙控器
        ENVIRONMENT_RELEVANT_OBJECTS.add("keyboard");         // 鍵盤
        ENVIRONMENT_RELEVANT_OBJECTS.add("cell phone");       // 手機
        ENVIRONMENT_RELEVANT_OBJECTS.add("clock");            // 時鐘
        
        // 廚房用品
        ENVIRONMENT_RELEVANT_OBJECTS.add("microwave");        // 微波爐
        ENVIRONMENT_RELEVANT_OBJECTS.add("oven");             // 烤箱
        ENVIRONMENT_RELEVANT_OBJECTS.add("toaster");          // 烤麵包機
        ENVIRONMENT_RELEVANT_OBJECTS.add("sink");             // 水槽
        ENVIRONMENT_RELEVANT_OBJECTS.add("refrigerator");     // 冰箱
        ENVIRONMENT_RELEVANT_OBJECTS.add("bottle");           // 瓶子
        ENVIRONMENT_RELEVANT_OBJECTS.add("cup");              // 杯子
        ENVIRONMENT_RELEVANT_OBJECTS.add("wine glass");       // 酒杯
        ENVIRONMENT_RELEVANT_OBJECTS.add("bowl");             // 碗
        ENVIRONMENT_RELEVANT_OBJECTS.add("fork");             // 叉子
        ENVIRONMENT_RELEVANT_OBJECTS.add("knife");            // 刀子
        ENVIRONMENT_RELEVANT_OBJECTS.add("spoon");            // 勺子
        
        // 日常用品
        ENVIRONMENT_RELEVANT_OBJECTS.add("book");             // 書
        ENVIRONMENT_RELEVANT_OBJECTS.add("umbrella");         // 雨傘
        ENVIRONMENT_RELEVANT_OBJECTS.add("handbag");          // 手提包
        ENVIRONMENT_RELEVANT_OBJECTS.add("backpack");         // 背包
        ENVIRONMENT_RELEVANT_OBJECTS.add("suitcase");         // 手提箱
        ENVIRONMENT_RELEVANT_OBJECTS.add("vase");             // 花瓶
        ENVIRONMENT_RELEVANT_OBJECTS.add("scissors");         // 剪刀
        ENVIRONMENT_RELEVANT_OBJECTS.add("hair drier");       // 吹風機
        ENVIRONMENT_RELEVANT_OBJECTS.add("toothbrush");       // 牙刷
        
        // 食物（常見的）
        ENVIRONMENT_RELEVANT_OBJECTS.add("banana");           // 香蕉
        ENVIRONMENT_RELEVANT_OBJECTS.add("apple");            // 蘋果
        ENVIRONMENT_RELEVANT_OBJECTS.add("orange");           // 橙子
        ENVIRONMENT_RELEVANT_OBJECTS.add("sandwich");         // 三明治
        ENVIRONMENT_RELEVANT_OBJECTS.add("pizza");            // 披薩
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
    
    // 多幀融合穩定性過濾（提高準確率）
    private static final int STABILITY_FRAME_COUNT = 2;  // 需要連續2幀檢測到才認為穩定（降低以提高檢測率）
    private final Map<String, Integer> detectionStability = new HashMap<>();  // 物體標籤 -> 連續檢測次數
    private final Map<String, Float> detectionConfidenceSum = new HashMap<>();  // 物體標籤 -> 置信度累加
    private final Map<String, android.graphics.RectF> detectionBoundingBox = new HashMap<>();  // 物體標籤 -> 邊界框
    
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
                            .setScoreThreshold(AppConstants.SCORE_THRESHOLD)
                            .setMaxResults(AppConstants.MAX_RESULTS)
                            .build();
            
            objectDetector = ObjectDetector.createFromFileAndOptions(
                    context,
                    AppConstants.MODEL_FILE,
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
        
        // 應用多幀融合穩定性過濾（提高準確率）
        results = applyStabilityFilter(results);
        
        // 按置信度排序
        Collections.sort(results, (a, b) -> Float.compare(b.getConfidence(), a.getConfidence()));
        
        // 限制結果數量
        if (results.size() > AppConstants.MAX_RESULTS) {
            results = results.subList(0, AppConstants.MAX_RESULTS);
        }
        
        return results;
    }
    
    /**
     * 應用多幀融合穩定性過濾 - 只保留在連續多幀中穩定出現的檢測結果
     * 這可以顯著提高準確率，減少誤報和閃爍
     */
    private List<DetectionResult> applyStabilityFilter(List<DetectionResult> results) {
        if (results.isEmpty()) {
            // 如果當前幀沒有檢測結果，減少所有物體的穩定性計數
            List<String> toRemove = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : detectionStability.entrySet()) {
                int count = entry.getValue() - 1;
                if (count <= 0) {
                    toRemove.add(entry.getKey());
                } else {
                    detectionStability.put(entry.getKey(), count);
                }
            }
            for (String key : toRemove) {
                detectionStability.remove(key);
                detectionConfidenceSum.remove(key);
                detectionBoundingBox.remove(key);
            }
            return new ArrayList<>();
        }
        
        // 更新當前幀檢測到的物體
        Set<String> currentDetections = new HashSet<>();
        for (DetectionResult result : results) {
            String key = result.getLabel() + "_" + result.getLabelZh();
            currentDetections.add(key);
            
            // 更新穩定性計數
            int stabilityCount = detectionStability.getOrDefault(key, 0) + 1;
            detectionStability.put(key, Math.min(stabilityCount, STABILITY_FRAME_COUNT));
            
            // 累加置信度（用於計算平均置信度）
            float currentSum = detectionConfidenceSum.getOrDefault(key, 0f);
            detectionConfidenceSum.put(key, currentSum + result.getConfidence());
            
            // 更新邊界框（使用最新的）
            detectionBoundingBox.put(key, result.getBoundingBox());
        }
        
        // 減少未檢測到的物體的穩定性計數
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : detectionStability.entrySet()) {
            String key = entry.getKey();
            if (!currentDetections.contains(key)) {
                int count = entry.getValue() - 1;
                if (count <= 0) {
                    toRemove.add(key);
                } else {
                    detectionStability.put(key, count);
                }
            }
        }
        for (String key : toRemove) {
            detectionStability.remove(key);
            detectionConfidenceSum.remove(key);
            detectionBoundingBox.remove(key);
        }
        
        // 只返回穩定性達到閾值的檢測結果
        List<DetectionResult> stableResults = new ArrayList<>();
        for (DetectionResult result : results) {
            String key = result.getLabel() + "_" + result.getLabelZh();
            int stability = detectionStability.getOrDefault(key, 0);
            
            // 只有連續檢測到足夠次數的物體才被認為是穩定的
            if (stability >= STABILITY_FRAME_COUNT) {
                // 使用平均置信度（更穩定）
                float avgConfidence = detectionConfidenceSum.getOrDefault(key, 0f) / stability;
                android.graphics.RectF bbox = detectionBoundingBox.getOrDefault(key, result.getBoundingBox());
                
                stableResults.add(new DetectionResult(
                    result.getLabel(),
                    result.getLabelZh(),
                    avgConfidence,
                    bbox
                ));
                
                Log.d(TAG, String.format("穩定檢測: %s (穩定性: %d/%d, 平均置信度: %.2f)", 
                    result.getLabelZh(), stability, STABILITY_FRAME_COUNT, avgConfidence));
            } else {
                Log.d(TAG, String.format("不穩定檢測（跳過）: %s (穩定性: %d/%d)", 
                    result.getLabelZh(), stability, STABILITY_FRAME_COUNT));
            }
        }
        
        Log.d(TAG, String.format("穩定性過濾: %d -> %d", results.size(), stableResults.size()));
        return stableResults;
    }
    
    /**
     * 過濾檢測結果（白名單 + 黑名單 + 基本驗證）
     * 只保留對視障人士有用的常見物體，排除不常見或不相關的物體（如長頸鹿、斑馬等）
     */
    private List<DetectionResult> filterEnvironmentRelevantObjects(List<DetectionResult> results) {
        List<DetectionResult> filtered = new ArrayList<>();

        for (DetectionResult result : results) {
            String label = result.getLabel().toLowerCase();
            
            // 1. 檢查黑名單（優先排除不常見物體）
            if (EXCLUDED_OBJECTS.contains(label)) {
                Log.d(TAG, "黑名單過濾: " + result.getLabelZh() + " (不常見物體)");
                continue;
            }
            
            // 2. 檢查邊界框合理性（過濾異常檢測）
            if (!isValidBoundingBox(result.getBoundingBox())) {
                Log.d(TAG, "過濾無效邊界框: " + result.getLabelZh());
                continue;
            }

            // 3. 檢查置信度閾值
            if (result.getConfidence() < AppConstants.SCORE_THRESHOLD) {
                Log.d(TAG, "過濾低置信度物體: " + result.getLabelZh() + " (置信度: " + result.getConfidence() + ")");
                continue;
            }
            
            // 4. 白名單檢查：只保留環境識別相關的常見物體
            if (!ENVIRONMENT_RELEVANT_OBJECTS.contains(label)) {
                Log.d(TAG, "白名單過濾: " + result.getLabelZh() + " (不在常見物體列表中)");
                continue;
            }

            // 通過所有檢查，保留此物體
            filtered.add(result);
            Log.d(TAG, "保留物體: " + result.getLabelZh() + " (置信度: " + String.format("%.2f", result.getConfidence()) + ")");
        }

        // 按置信度排序，優先顯示高置信度結果
        filtered.sort((a, b) -> Float.compare(b.getConfidence(), a.getConfidence()));

        Log.d(TAG, String.format("物體過濾: %d -> %d (白名單+黑名單過濾)", results.size(), filtered.size()));
        return filtered;
    }
    
    /**
     * 檢查邊界框是否合理 - 過濾異常檢測，提高準確率
     */
    private boolean isValidBoundingBox(android.graphics.RectF bbox) {
        if (bbox == null) {
            return false;
        }
        
        float width = bbox.right - bbox.left;
        float height = bbox.bottom - bbox.top;
        
        // 檢查邊界框尺寸是否合理（不能太小或太大）
        if (width <= 0 || height <= 0) {
            return false;
        }
        
        // 檢查邊界框是否在有效範圍內（0-1）
        if (bbox.left < 0 || bbox.top < 0 || bbox.right > 1.0f || bbox.bottom > 1.0f) {
            return false;
        }
        
        // 檢查邊界框面積是否合理（不能太小，避免噪聲檢測）
        // 降低最小面積要求，讓小型物體（如瓶子）更容易被檢測到
        float area = width * height;
        if (area < 0.0005f) {  // 面積小於0.05%的檢測視為噪聲（降低閾值以提高檢測率）
            return false;
        }
        
        // 檢查寬高比是否合理（避免極端比例）
        // 放寬寬高比限制，讓細長物體（如瓶子）更容易被檢測到
        float aspectRatio = width / height;
        if (aspectRatio < 0.05f || aspectRatio > 20.0f) {  // 放寬限制（從0.1-10改為0.05-20）
            return false;
        }
        
        return true;
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
            int imageWidth = bitmap.getWidth();
            int imageHeight = bitmap.getHeight();
            
            for (YoloDetector.DetectionResult yoloResult : yoloResults) {
                if (yoloResult.getConfidence() >= AppConstants.SCORE_THRESHOLD) {
                    String labelZh = LABEL_MAP_ZH.get(yoloResult.getLabel());
                    if (labelZh == null) {
                        labelZh = yoloResult.getLabel();
                    }
                    
                    // 檢查邊界框是否為null
                    android.graphics.Rect rect = yoloResult.getBoundingBox();
                    if (rect != null && imageWidth > 0 && imageHeight > 0) {
                        // YOLO返回的是像素座標，需要標準化為0-1範圍
                        // 轉換為標準化的RectF（0.0-1.0）
                        android.graphics.RectF rectF = new android.graphics.RectF(
                                (float)rect.left / imageWidth,      // 標準化left
                                (float)rect.top / imageHeight,      // 標準化top
                                (float)rect.right / imageWidth,     // 標準化right
                                (float)rect.bottom / imageHeight    // 標準化bottom
                        );
                        
                        // 確保座標在有效範圍內 (0-1)
                        rectF.left = Math.max(0.0f, Math.min(1.0f, rectF.left));
                        rectF.top = Math.max(0.0f, Math.min(1.0f, rectF.top));
                        rectF.right = Math.max(rectF.left + 0.01f, Math.min(1.0f, rectF.right));
                        rectF.bottom = Math.max(rectF.top + 0.01f, Math.min(1.0f, rectF.bottom));
                        
                        results.add(new DetectionResult(
                                yoloResult.getLabel(),
                                labelZh,
                                yoloResult.getConfidence(),
                                rectF
                        ));
                    } else {
                        // 如果邊界框為null，創建一個默認邊界框
                        Log.w(TAG, "YOLO檢測結果邊界框為null或圖像尺寸無效，使用默認邊界框");
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
                if (iou > AppConstants.NMS_THRESHOLD && current.getLabel().equals(other.getLabel())) {
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
     * 格式化檢測結果為語音文本 - 專為視障人士優化（包含位置描述）
     * 播報物體名稱和位置信息（左/右/中央）
     */
    public String formatResultsForSpeech(List<DetectionResult> results) {
        if (results.isEmpty()) {
            return getNoObjectsDetectedText();
        }
        
        StringBuilder sb = new StringBuilder();
        String currentLang = LocaleManager.getInstance(context).getCurrentLanguage();
        
        // 簡潔的物體描述，最多2個物體
        int maxObjects = Math.min(results.size(), 2);
        
        Log.d(TAG, "開始格式化語音文本，物體數量: " + results.size() + ", 將播報: " + maxObjects);
        
        for (int i = 0; i < maxObjects; i++) {
            DetectionResult result = results.get(i);
            
            // 獲取物體名稱 - 根據當前語言選擇對應的標籤
            String objectLabel = getObjectLabelForCurrentLanguage(result);
            sb.append(objectLabel);
            Log.d(TAG, "物體 " + (i + 1) + ": " + objectLabel);
            
            // 添加位置描述（左/右/中央）
            android.graphics.RectF bbox = result.getBoundingBox();
            if (bbox != null) {
                Log.d(TAG, "邊界框: left=" + bbox.left + ", top=" + bbox.top + 
                      ", right=" + bbox.right + ", bottom=" + bbox.bottom);
                String positionDesc = getPositionDescription(bbox);
                if (positionDesc != null && !positionDesc.isEmpty()) {
                    sb.append(positionDesc);
                    Log.d(TAG, "添加位置描述: " + positionDesc);
                } else {
                    Log.w(TAG, "位置描述為空或null");
                }
            } else {
                Log.w(TAG, "邊界框為null，無法添加位置描述");
            }
            
            // 分隔符
            if (i < maxObjects - 1) {
                if (currentLang.equals("english")) {
                    sb.append(", ");
                } else {
                    sb.append("，");
                }
            }
        }
        
        // 如果物體超過2個，添加總數
        if (results.size() > 2) {
            if (currentLang.equals("english")) {
                sb.append(" and ").append(results.size()).append(" more objects");
            } else {
                sb.append("等").append(results.size()).append("個");
            }
        }
        
        String finalText = sb.toString();
        Log.d(TAG, "最終語音文本: " + finalText);
        return finalText;
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
                // 英文模式：優先使用英文標籤，如果為空則從中文標籤映射回英文
                String englishLabel = result.getLabel();
                if (englishLabel != null && !englishLabel.trim().isEmpty()) {
                    return englishLabel;
                }
                // 如果英文標籤為空，嘗試從中文標籤映射回英文
                String chineseLabel = result.getLabelZh();
                if (chineseLabel != null && !chineseLabel.trim().isEmpty()) {
                    // 從中文映射回英文（反向查找）
                    for (Map.Entry<String, String> entry : LABEL_MAP_ZH.entrySet()) {
                        if (entry.getValue().equals(chineseLabel)) {
                            return entry.getKey();
                        }
                    }
                }
                // 如果都找不到，返回英文標籤（即使為空）
                return englishLabel != null ? englishLabel : "object";
                
            case "mandarin":
                // 普通話模式：優先使用中文標籤
                return result.getLabelZh() != null && !result.getLabelZh().trim().isEmpty() 
                    ? result.getLabelZh() 
                    : (result.getLabel() != null ? result.getLabel() : "物體");
                    
            case "cantonese":
            default:
                // 粵語模式：優先使用中文標籤
                return result.getLabelZh() != null && !result.getLabelZh().trim().isEmpty() 
                    ? result.getLabelZh() 
                    : (result.getLabel() != null ? result.getLabel() : "物體");
        }
    }
    
    /**
     * 獲取位置描述（簡潔版 - 只描述水平位置：左/右/中央）
     * 專為視障人士優化，提供清晰的位置指引
     */
    private String getPositionDescription(android.graphics.RectF boundingBox) {
        if (boundingBox == null) {
            Log.w(TAG, "邊界框為null，無法獲取位置描述");
            return "";
        }
        
        String currentLang = LocaleManager.getInstance(context).getCurrentLanguage();
        
        // 計算物體在畫面中的水平位置（相對於畫面中心）
        // 邊界框座標是標準化的（0.0-1.0）
        float centerX = (boundingBox.left + boundingBox.right) / 2.0f;
        
        String horizontalPos;
        
        // 水平位置分為三區：左側、中央、右側
        // 使用更精確的閾值，避免邊界模糊
        if (centerX < 0.35f) {
            // 左側（0-35%）
            horizontalPos = currentLang.equals("english") ? " on the left" : "在左側";
            Log.d(TAG, String.format("位置描述: 左側 (centerX=%.2f)", centerX));
        } else if (centerX > 0.65f) {
            // 右側（65-100%）
            horizontalPos = currentLang.equals("english") ? " on the right" : "在右側";
            Log.d(TAG, String.format("位置描述: 右側 (centerX=%.2f)", centerX));
        } else {
            // 中央（35-65%）
            horizontalPos = currentLang.equals("english") ? " in the center" : "在中央";
            Log.d(TAG, String.format("位置描述: 中央 (centerX=%.2f)", centerX));
        }
        
        return horizontalPos;
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

