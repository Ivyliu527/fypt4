package com.example.tonbo_app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.util.Log;

import androidx.camera.core.ImageProxy;

import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 真實AI物體檢測器
 * 基於 TensorFlow Lite 的 SSD MobileNet 模型實現
 * 提供真實的AI檢測能力，支持90個COCO類別
 */
public class YoloDetector {
    private static final String TAG = "YoloDetector";
    
    // 模型參數 - 使用AppConstants
    
    private Context context;
    private Interpreter tflite;
    private boolean isInitialized = false;
    private DetectionPerformanceMonitor performanceMonitor;
    
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
        
        // 添加更多SSD模型支持的類別
        CLASS_NAMES_ZH.put("background", "背景");
        CLASS_NAMES_ZH.put("aeroplane", "飛機");
        CLASS_NAMES_ZH.put("bicycle", "腳踏車");
        CLASS_NAMES_ZH.put("bird", "鳥");
        CLASS_NAMES_ZH.put("boat", "船");
        CLASS_NAMES_ZH.put("bottle", "瓶子");
        CLASS_NAMES_ZH.put("bus", "公車");
        CLASS_NAMES_ZH.put("car", "汽車");
        CLASS_NAMES_ZH.put("cat", "貓");
        CLASS_NAMES_ZH.put("chair", "椅子");
        CLASS_NAMES_ZH.put("cow", "牛");
        CLASS_NAMES_ZH.put("diningtable", "餐桌");
        CLASS_NAMES_ZH.put("dog", "狗");
        CLASS_NAMES_ZH.put("horse", "馬");
        CLASS_NAMES_ZH.put("motorbike", "摩托車");
        CLASS_NAMES_ZH.put("pottedplant", "盆栽");
        CLASS_NAMES_ZH.put("sheep", "羊");
        CLASS_NAMES_ZH.put("sofa", "沙發");
        CLASS_NAMES_ZH.put("train", "火車");
        CLASS_NAMES_ZH.put("tvmonitor", "電視");
    }
    
    // COCO 類別名稱數組（按索引順序）- SSD MobileNet 格式
    private static final String[] COCO_CLASSES = {
        "background", "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat",
        "traffic light", "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat",
        "dog", "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "backpack",
        "umbrella", "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball",
        "kite", "baseball bat", "baseball glove", "skateboard", "surfboard", "tennis racket",
        "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
        "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake",
        "chair", "couch", "potted plant", "bed", "dining table", "toilet", "tv", "laptop",
        "mouse", "remote", "keyboard", "cell phone", "microwave", "oven", "toaster", "sink",
        "refrigerator", "book", "clock", "vase", "scissors", "teddy bear", "hair drier", "toothbrush",
        "aeroplane", "bicycle", "bird", "boat", "bottle", "bus", "car", "cat", "chair", "cow",
        "diningtable", "dog", "horse", "motorbike", "pottedplant", "sheep", "sofa", "train", "tvmonitor"
    };
    
    public YoloDetector(Context context) {
        this.context = context;
        this.performanceMonitor = new DetectionPerformanceMonitor();
        initialize();
    }
    
    private void initialize() {
        try {
            Log.d(TAG, "開始初始化真實AI檢測器...");
            
            // 載入 TensorFlow Lite 模型
            tflite = new Interpreter(loadModelFile());
            
            if (tflite != null) {
                isInitialized = true;
                Log.d(TAG, "真實AI檢測器初始化成功 - 使用YOLOv8模型");
            } else {
                Log.e(TAG, "無法載入 TensorFlow Lite 模型");
                isInitialized = false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "真實AI檢測器初始化失敗: " + e.getMessage());
            isInitialized = false;
        }
    }
    
    /**
     * 載入模型文件
     */
    private MappedByteBuffer loadModelFile() throws IOException {
        try {
            // 嘗試從 assets 載入模型文件
            return loadModelFromAssets();
        } catch (IOException e) {
            Log.w(TAG, "無法從 assets 載入模型，使用備用檢測方法");
            return null;
        }
    }
    
    /**
     * 從 assets 載入模型文件
     */
    private MappedByteBuffer loadModelFromAssets() throws IOException {
        android.content.res.AssetFileDescriptor fileDescriptor = 
            context.getAssets().openFd(AppConstants.YOLO_MODEL_FILE);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
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
            
            return detect(bitmap);
            
        } catch (Exception e) {
            Log.e(TAG, "檢測失敗: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 檢測 Bitmap 圖像
     */
    public List<DetectionResult> detect(Bitmap bitmap) {
        if (bitmap == null) {
            return new ArrayList<>();
        }
        
        if (!isInitialized || tflite == null) {
            Log.w(TAG, "真實AI模型未載入，使用備用檢測方法");
            return getFallbackDetections(bitmap);
        }
        
        try {
            long startTime = System.currentTimeMillis();
            
            // 動態檢測模型的輸入張量要求
            org.tensorflow.lite.Tensor inputTensor = tflite.getInputTensor(0);
            int[] inputShape = inputTensor.shape();
            int inputHeight = inputShape[1];
            int inputWidth = inputShape[2];
            int inputChannels = inputShape.length > 3 ? inputShape[3] : 3;
            
            Log.d(TAG, String.format("模型輸入張量形狀: [%s], 期望尺寸: %dx%d, 通道數: %d", 
                java.util.Arrays.toString(inputShape), inputWidth, inputHeight, inputChannels));
            
            // 檢查數據類型（float32 vs uint8）
            org.tensorflow.lite.DataType inputDataType = inputTensor.dataType();
            boolean isFloatInput = (inputDataType == org.tensorflow.lite.DataType.FLOAT32);
            Log.d(TAG, "輸入數據類型: " + inputDataType + ", 是否為float: " + isFloatInput);
            
            // 根據模型要求調整圖像尺寸
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true);
            ByteBuffer inputBuffer = bitmapToByteBuffer(resizedBitmap, inputWidth, inputHeight, isFloatInput);
            
            // 動態獲取輸出張量的形狀
            int numOutputs = tflite.getOutputTensorCount();
            Log.d(TAG, "模型輸出張量數量: " + numOutputs);
            
            // 檢查第一個輸出張量的形狀，判斷模型格式
            org.tensorflow.lite.Tensor firstOutputTensor = tflite.getOutputTensor(0);
            int[] firstOutputShape = firstOutputTensor.shape();
            Log.d(TAG, "第一個輸出張量形狀: " + java.util.Arrays.toString(firstOutputShape));
            
            List<DetectionResult> results = new ArrayList<>();
            
            // 根據第一個輸出張量的形狀判斷模型格式
            // YOLOv8格式: [1, 84, 8400] = [batch, 4+80, num_anchors]
            // 後處理格式: [1, num_detections, 4] 或 [1, num_detections, 6]
            boolean isYoloV8Format = (numOutputs == 1 && firstOutputShape.length == 3 && 
                                      firstOutputShape[0] == 1 && 
                                      firstOutputShape[1] == 84 && 
                                      firstOutputShape[2] == 8400);
            boolean isPostProcessed = (numOutputs == 1 && firstOutputShape.length == 3 && 
                                      firstOutputShape[0] == 1 && 
                                      !isYoloV8Format &&
                                      (firstOutputShape[2] == 4 || firstOutputShape[2] == 6));
            
            if (isYoloV8Format) {
                // YOLOv8 原生格式：[1, 84, 8400]
                // 84 = 4 (bbox坐标) + 80 (COCO类别)
                // 8400 = 检测位置数量
                Log.d(TAG, "檢測到YOLOv8原生格式: [1, 84, 8400]");
                
                // 分配輸出緩衝區 [1, 84, 8400]
                float[][][] detectionOutput = new float[1][84][8400];
                
                // 執行推理
                Object[] inputs = {inputBuffer};
                Map<Integer, Object> outputs = new HashMap<>();
                outputs.put(0, detectionOutput);
                
                tflite.runForMultipleInputsOutputs(inputs, outputs);
                
                // 處理YOLOv8輸出格式
                results = postProcessYoloV8Output(
                    detectionOutput[0], inputWidth, inputHeight, bitmap.getWidth(), bitmap.getHeight());
                    
            } else if (isPostProcessed || numOutputs == 1) {
                // 後處理版本：單一輸出 [1, num_detections, 4] 或 [1, num_detections, 6]
                int maxDetections = firstOutputShape.length >= 2 ? firstOutputShape[1] : 10;
                int coordsPerDetection = firstOutputShape.length >= 3 ? firstOutputShape[2] : 4;
                
                Log.d(TAG, "檢測到後處理版本，最大檢測數: " + maxDetections + ", 每檢測座標數: " + coordsPerDetection);
                
                // 分配輸出緩衝區
                float[][][] detectionOutput = new float[1][maxDetections][coordsPerDetection];
                
                // 執行推理
                Object[] inputs = {inputBuffer};
                Map<Integer, Object> outputs = new HashMap<>();
                outputs.put(0, detectionOutput);
                
                tflite.runForMultipleInputsOutputs(inputs, outputs);
                
                // 處理後處理輸出格式 [1, num_detections, 6] 或 [1, num_detections, 4]
                results = postProcessDetections(
                    detectionOutput[0], bitmap.getWidth(), bitmap.getHeight(), coordsPerDetection);
                    
            } else if (numOutputs >= 2) {
                // 後處理版本：多個輸出張量
                // 格式可能是：boxes [1, N, 4], classes [1, N], scores [1, N], num_detections [1]
                // 或者：boxes [1, N, 4], classes [1, N, 91], scores [1, N], num_detections [1]
                Log.d(TAG, "檢測到多輸出格式，動態檢測輸出張量形狀");
                
                // 檢查所有輸出張量的形狀
                int maxDetections = 10; // 默認值
                boolean isClassIndexFormat = false; // classes 是索引還是概率矩陣
                
                // 檢查第一個輸出張量（boxes）的形狀
                if (firstOutputShape.length >= 2) {
                    maxDetections = firstOutputShape[1];
                    Log.d(TAG, "從第一個輸出張量檢測到最大檢測數: " + maxDetections);
                }
                
                // 檢查第二個輸出張量（classes）的形狀
                if (numOutputs > 1) {
                    org.tensorflow.lite.Tensor classesTensor = tflite.getOutputTensor(1);
                    int[] classesShape = classesTensor.shape();
                    Log.d(TAG, "第二個輸出張量（classes）形狀: " + java.util.Arrays.toString(classesShape));
                    
                    // 如果形狀是 [1, N]，說明是類別索引格式
                    // 如果形狀是 [1, N, num_classes]，說明是類別概率矩陣格式
                    if (classesShape.length == 2) {
                        isClassIndexFormat = true;
                        Log.d(TAG, "檢測到類別索引格式 [1, N]");
                    } else if (classesShape.length == 3) {
                        Log.d(TAG, "檢測到類別概率矩陣格式 [1, N, num_classes]");
                    }
                }
                
                // 根據實際形狀分配輸出緩衝區
                float[][][] detectionBoxes = new float[1][maxDetections][4];
                
                Object[] inputs = {inputBuffer};
                Map<Integer, Object> outputs = new HashMap<>();
                outputs.put(0, detectionBoxes);
                
                // 根據格式分配不同的輸出緩衝區
                if (isClassIndexFormat) {
                    // 後處理格式：classes 是索引數組 [1, N]
                    float[][] detectionClasses = new float[1][maxDetections];
                    float[][] detectionScores = numOutputs > 2 ? new float[1][maxDetections] : null;
                    float[] numDetections = numOutputs > 3 ? new float[1] : null;
                    
                    if (numOutputs > 1) outputs.put(1, detectionClasses);
                    if (numOutputs > 2 && detectionScores != null) outputs.put(2, detectionScores);
                    if (numOutputs > 3 && numDetections != null) outputs.put(3, numDetections);
                    
                    tflite.runForMultipleInputsOutputs(inputs, outputs);
                    
                    // 處理後處理格式的輸出
                    results = postProcessPostProcessedOutput(
                        detectionBoxes[0],
                        detectionClasses[0],
                        detectionScores != null ? detectionScores[0] : null,
                        numDetections != null ? (int)numDetections[0] : maxDetections,
                        bitmap.getWidth(), bitmap.getHeight());
                } else {
                    // 原始格式：classes 是概率矩陣 [1, N, num_classes]
                    int numClasses = 91; // 默認值
                    if (numOutputs > 1) {
                        org.tensorflow.lite.Tensor classesTensor = tflite.getOutputTensor(1);
                        int[] classesShape = classesTensor.shape();
                        if (classesShape.length >= 3) {
                            numClasses = classesShape[2];
                        }
                    }
                    
                    float[][][] detectionClasses = new float[1][maxDetections][numClasses];
                    float[][] detectionScores = numOutputs > 2 ? new float[1][maxDetections] : null;
                    float[] numDetections = numOutputs > 3 ? new float[1] : null;
                    
                    if (numOutputs > 1) outputs.put(1, detectionClasses);
                    if (numOutputs > 2 && detectionScores != null) outputs.put(2, detectionScores);
                    if (numOutputs > 3 && numDetections != null) outputs.put(3, numDetections);
                    
                    tflite.runForMultipleInputsOutputs(inputs, outputs);
                    
                    results = postProcessSSDOutput(
                        detectionBoxes[0], 
                        detectionClasses[0],
                        detectionScores != null ? detectionScores[0] : null, 
                        numDetections != null ? (int)numDetections[0] : maxDetections, 
                        bitmap.getWidth(), bitmap.getHeight());
                }
            } else {
                Log.e(TAG, "未知的輸出格式，輸出張量數量: " + numOutputs);
                return getFallbackDetections(bitmap);
            }
            
            // 記錄性能數據
            long detectionTime = System.currentTimeMillis() - startTime;
            performanceMonitor.recordDetectionTime(detectionTime);
            performanceMonitor.recordDetectionResult(results);
            
            // 回收臨時 bitmap
            if (resizedBitmap != bitmap) {
                resizedBitmap.recycle();
            }
            
            return results;
            
        } catch (Exception e) {
            Log.e(TAG, "真實AI檢測失敗，使用備用方法: " + e.getMessage());
            e.printStackTrace();
            return getFallbackDetections(bitmap);
        }
    }
    
    /**
     * 將 Bitmap 轉換為 ByteBuffer
     * 支持兩種格式：
     * - uint8: 1字節/通道，值範圍 0-255
     * - float32: 4字節/通道，值範圍 0.0-1.0 (歸一化)
     */
    private ByteBuffer bitmapToByteBuffer(Bitmap bitmap, int inputWidth, int inputHeight, boolean isFloatInput) {
        // 根據數據類型計算緩衝區大小
        int bufferSize;
        if (isFloatInput) {
            // float32: width × height × 3 × 4 字節
            bufferSize = inputWidth * inputHeight * 3 * 4;
        } else {
            // uint8: width × height × 3 字節
            bufferSize = inputWidth * inputHeight * 3;
        }
        
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bufferSize);
        byteBuffer.order(ByteOrder.nativeOrder());
        
        // 確保bitmap是正確尺寸
        if (bitmap.getWidth() != inputWidth || bitmap.getHeight() != inputHeight) {
            Log.w(TAG, String.format("Bitmap尺寸不正確: %dx%d, 期望: %dx%d", 
                bitmap.getWidth(), bitmap.getHeight(), inputWidth, inputHeight));
            return byteBuffer;
        }
        
        int[] pixels = new int[inputWidth * inputHeight];
        bitmap.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight);
        
        // 將像素值轉換為RGB並添加到緩衝區
        for (int pixel : pixels) {
            // 提取 RGB 值 (ARGB格式)
            int r = (pixel >> 16) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = pixel & 0xFF;
            
            if (isFloatInput) {
                // float32格式：歸一化到 0.0-1.0
                byteBuffer.putFloat(r / 255.0f);
                byteBuffer.putFloat(g / 255.0f);
                byteBuffer.putFloat(b / 255.0f);
            } else {
                // uint8格式：值範圍 0-255
                byteBuffer.put((byte) r);
                byteBuffer.put((byte) g);
                byteBuffer.put((byte) b);
            }
        }
        
        byteBuffer.rewind(); // 重置位置到開始
        return byteBuffer;
    }
    
    /**
     * 處理YOLOv8原生輸出格式 [84, 8400]
     * output[0-3][j] = bbox坐标 (x_center, y_center, width, height)，相对于模型输入尺寸
     * output[4-83][j] = 80个类别的置信度分数
     * j = 0-8399 是不同的检测位置
     */
    private List<DetectionResult> postProcessYoloV8Output(float[][] output, 
                                                          int modelWidth, int modelHeight,
                                                          int originalWidth, int originalHeight) {
        List<DetectionResult> rawResults = new ArrayList<>();
        int numAnchors = 8400;
        int numClasses = 80;
        
        // 第一步：提取所有有效检测
        for (int j = 0; j < numAnchors; j++) {
            // YOLOv8输出格式：bbox坐标可能是归一化(0-1)或像素坐标
            // 格式：output[0-3][j] = (x_center, y_center, width, height)
            float x_center = output[0][j];
            float y_center = output[1][j];
            float width = output[2][j];
            float height = output[3][j];
            
            // 找到最高置信度的类别
            float maxConfidence = 0.0f;
            int bestClassIndex = 0;
            for (int c = 0; c < numClasses; c++) {
                float confidence = output[4 + c][j];
                if (confidence > maxConfidence) {
                    maxConfidence = confidence;
                    bestClassIndex = c;
                }
            }
            
            // 过滤低置信度检测
            if (maxConfidence < AppConstants.CONFIDENCE_THRESHOLD) {
                continue;
            }
            
            // 判断坐标格式：如果坐标值 > 1，说明是像素坐标；否则是归一化坐标(0-1)
            boolean isNormalized = (x_center <= 1.0f && y_center <= 1.0f && 
                                   width <= 1.0f && height <= 1.0f && 
                                   x_center >= 0 && y_center >= 0);
            
            float left, top, right, bottom;
            if (isNormalized) {
                // 归一化坐标：直接转换为(left, top, right, bottom)
                left = x_center - width / 2;
                top = y_center - height / 2;
                right = x_center + width / 2;
                bottom = y_center + height / 2;
            } else {
                // 像素坐标：先归一化再转换
                left = (x_center - width / 2) / modelWidth;
                top = (y_center - height / 2) / modelHeight;
                right = (x_center + width / 2) / modelWidth;
                bottom = (y_center + height / 2) / modelHeight;
            }
            
            // 跳过无效的bbox
            if (width <= 0 || height <= 0 || left >= right || top >= bottom ||
                left < 0 || top < 0 || right > 1.0f || bottom > 1.0f) {
                continue;
            }
            
            // 确保坐标在有效范围内 (0-1)
            left = Math.max(0.0f, Math.min(1.0f, left));
            top = Math.max(0.0f, Math.min(1.0f, top));
            right = Math.max(left + 0.01f, Math.min(1.0f, right));
            bottom = Math.max(top + 0.01f, Math.min(1.0f, bottom));
            
            // 转换到像素坐标
            int pixelLeft = (int)(left * originalWidth);
            int pixelTop = (int)(top * originalHeight);
            int pixelRight = (int)(right * originalWidth);
            int pixelBottom = (int)(bottom * originalHeight);
            
            // 验证bbox尺寸（至少20像素）
            if (pixelRight - pixelLeft < 20 || pixelBottom - pixelTop < 20) {
                continue;
            }
            
            // 获取类别名称（YOLOv8使用COCO类别，索引0-79对应类别1-80，不包括background）
            // COCO_CLASSES[0]是"background"，所以YOLOv8的索引0对应COCO_CLASSES[1]
            String className = "object";
            int cocoClassIndex = bestClassIndex + 1; // YOLOv8索引+1 = COCO索引
            if (cocoClassIndex >= 1 && cocoClassIndex < COCO_CLASSES.length) {
                className = COCO_CLASSES[cocoClassIndex];
            } else if (bestClassIndex >= 0 && bestClassIndex < COCO_CLASSES.length) {
                // 备用：直接使用索引（如果数组对齐）
                className = COCO_CLASSES[bestClassIndex];
            }
            String chineseName = CLASS_NAMES_ZH.get(className);
            if (chineseName == null) {
                chineseName = className;
            }
            
            // 创建检测结果
            android.graphics.Rect boundingBox = new android.graphics.Rect(
                pixelLeft, pixelTop, pixelRight, pixelBottom);
            DetectionResult result = new DetectionResult(className, chineseName, maxConfidence, boundingBox);
            rawResults.add(result);
        }
        
        Log.d(TAG, "YOLOv8原始检测数量: " + rawResults.size());
        
        // 第二步：应用非极大值抑制（NMS）过滤重复检测
        List<DetectionResult> filteredResults = applyNMS(rawResults, AppConstants.NMS_THRESHOLD);
        
        // 第三步：按置信度排序并限制数量
        Collections.sort(filteredResults, new Comparator<DetectionResult>() {
            @Override
            public int compare(DetectionResult a, DetectionResult b) {
                return Float.compare(b.getConfidence(), a.getConfidence());
            }
        });
        
        int maxResults = Math.min(AppConstants.MAX_RESULTS, filteredResults.size());
        List<DetectionResult> finalResults = filteredResults.subList(0, maxResults);
        
        Log.d(TAG, "YOLOv8最终检测数量: " + finalResults.size());
        return finalResults;
    }
    
    /**
     * 非极大值抑制（NMS）过滤重叠的检测框
     */
    private List<DetectionResult> applyNMS(List<DetectionResult> detections, float iouThreshold) {
        if (detections.isEmpty()) {
            return detections;
        }
        
        List<DetectionResult> kept = new ArrayList<>();
        boolean[] suppressed = new boolean[detections.size()];
        
        for (int i = 0; i < detections.size(); i++) {
            if (suppressed[i]) {
                continue;
            }
            
            DetectionResult detection = detections.get(i);
            kept.add(detection);
            
            android.graphics.Rect box1 = detection.getBoundingBox();
            int area1 = box1.width() * box1.height();
            
            for (int j = i + 1; j < detections.size(); j++) {
                if (suppressed[j]) {
                    continue;
                }
                
                DetectionResult other = detections.get(j);
                android.graphics.Rect box2 = other.getBoundingBox();
                
                // 计算IoU
                int x1 = Math.max(box1.left, box2.left);
                int y1 = Math.max(box1.top, box2.top);
                int x2 = Math.min(box1.right, box2.right);
                int y2 = Math.min(box1.bottom, box2.bottom);
                
                int intersectionArea = Math.max(0, x2 - x1) * Math.max(0, y2 - y1);
                int area2 = box2.width() * box2.height();
                int unionArea = area1 + area2 - intersectionArea;
                
                float iou = unionArea > 0 ? (float)intersectionArea / unionArea : 0.0f;
                
                // 如果IoU超过阈值，抑制检测
                if (iou > iouThreshold) {
                    suppressed[j] = true;
                }
            }
        }
        
        return kept;
    }
    
    /**
     * 處理後處理版本的檢測輸出
     * 輸出格式可能是 [num_detections, 4] 或 [num_detections, 6]
     * 格式：[ymin, xmin, ymax, xmax] 或 [ymin, xmin, ymax, xmax, class, score]
     */
    private List<DetectionResult> postProcessDetections(float[][] detections, 
                                                       int originalWidth, int originalHeight, 
                                                       int coordsPerDetection) {
        List<DetectionResult> results = new ArrayList<>();
        
        for (float[] detection : detections) {
            if (detection == null || detection.length < 4) {
                continue;
            }
            
            float ymin, xmin, ymax, xmax;
            float confidence = 1.0f;
            int classIndex = 0;
            
            if (coordsPerDetection >= 6) {
                // 格式：[ymin, xmin, ymax, xmax, class, score]
                ymin = detection[0];
                xmin = detection[1];
                ymax = detection[2];
                xmax = detection[3];
                classIndex = (int) detection[4];
                confidence = detection[5];
            } else {
                // 格式：[ymin, xmin, ymax, xmax] 或 [xmin, ymin, xmax, ymax]
                // 嘗試檢測格式
                if (detection[0] > detection[2] || detection[1] > detection[3]) {
                    // 可能是 [xmin, ymin, xmax, ymax]
                    xmin = detection[0];
                    ymin = detection[1];
                    xmax = detection[2];
                    ymax = detection[3];
                } else {
                    // 可能是 [ymin, xmin, ymax, xmax]
                    ymin = detection[0];
                    xmin = detection[1];
                    ymax = detection[2];
                    xmax = detection[3];
                }
            }
            
            // 過濾低置信度檢測
            if (confidence < AppConstants.CONFIDENCE_THRESHOLD) {
                continue;
            }
            
            // 轉換為相對座標 (0-1)，因為 DetectionResult 期望 RectF 格式
            // 座標已經是相對座標 (0-1)，直接使用
            // 確保座標在有效範圍內 (0-1)
            xmin = Math.max(0.0f, Math.min(1.0f, xmin));
            ymin = Math.max(0.0f, Math.min(1.0f, ymin));
            xmax = Math.max(0.0f, Math.min(1.0f, xmax));
            ymax = Math.max(0.0f, Math.min(1.0f, ymax));
            
            // 確保 xmax >= xmin 且 ymax >= ymin
            if (xmax < xmin) {
                float temp = xmin;
                xmin = xmax;
                xmax = temp;
            }
            if (ymax < ymin) {
                float temp = ymin;
                ymin = ymax;
                ymax = temp;
            }
            
            // 驗證邊界框合理性（相對座標）
            float width = xmax - xmin;
            float height = ymax - ymin;
            // 最小尺寸約為 2% 的圖像尺寸
            if (width < 0.02f || height < 0.02f || width <= 0 || height <= 0) {
                continue;
            }
            
            // 獲取類別名稱
            String className = "object";
            if (classIndex > 0 && classIndex <= COCO_CLASSES.length) {
                className = COCO_CLASSES[classIndex - 1];
            }
            String chineseName = CLASS_NAMES_ZH.get(className);
            if (chineseName == null) {
                chineseName = className;
            }
            
            // 創建檢測結果（使用相對座標轉換為像素座標）
            // DetectionResult 期望 Rect，所以需要轉換為像素座標
            int left = (int)(xmin * originalWidth);
            int top = (int)(ymin * originalHeight);
            int right = (int)(xmax * originalWidth);
            int bottom = (int)(ymax * originalHeight);
            
            // 確保座標在有效範圍內
            left = Math.max(0, Math.min(originalWidth - 1, left));
            top = Math.max(0, Math.min(originalHeight - 1, top));
            right = Math.max(left + 1, Math.min(originalWidth, right));
            bottom = Math.max(top + 1, Math.min(originalHeight, bottom));
            
            android.graphics.Rect boundingBox = new android.graphics.Rect(left, top, right, bottom);
            results.add(new DetectionResult(className, chineseName, confidence, boundingBox));
        }
        
        return results;
    }
    
    /**
     * 處理後處理版本的輸出格式
     * boxes: [N, 4] - 邊界框座標（相對座標 0-1）
     * classes: [N] - 類別索引
     * scores: [N] - 置信度分數
     * numDetections: 實際檢測數量
     */
    private List<DetectionResult> postProcessPostProcessedOutput(float[][] boxes, 
                                                                 float[] classes,
                                                                 float[] scores,
                                                                 int numDetections,
                                                                 int originalWidth, 
                                                                 int originalHeight) {
        List<DetectionResult> results = new ArrayList<>();
        
        int actualDetections = Math.min(numDetections, boxes.length);
        Log.d(TAG, "處理後處理輸出，實際檢測數: " + actualDetections);
        
        for (int i = 0; i < actualDetections; i++) {
            // 獲取置信度
            float confidence = (scores != null && i < scores.length) ? scores[i] : 1.0f;
            
            // 過濾低置信度檢測
            if (confidence < AppConstants.CONFIDENCE_THRESHOLD) {
                continue;
            }
            
            // 獲取類別索引
            int classIndex = (classes != null && i < classes.length) ? (int)classes[i] : 0;
            
            // 跳過背景類別 (索引0)
            if (classIndex == 0) {
                continue;
            }
            
            // 獲取邊界框座標（相對座標 0-1）
            // 格式通常是 [ymin, xmin, ymax, xmax]
            float ymin = boxes[i][0];
            float xmin = boxes[i][1];
            float ymax = boxes[i][2];
            float xmax = boxes[i][3];
            
            // 確保座標順序正確
            if (xmax < xmin) {
                float temp = xmin;
                xmin = xmax;
                xmax = temp;
            }
            if (ymax < ymin) {
                float temp = ymin;
                ymin = ymax;
                ymax = temp;
            }
            
            // 驗證邊界框合理性（相對座標）
            float width = xmax - xmin;
            float height = ymax - ymin;
            if (width < 0.02f || height < 0.02f || width <= 0 || height <= 0) {
                continue;
            }
            
            // 轉換為像素座標
            int left = (int)(xmin * originalWidth);
            int top = (int)(ymin * originalHeight);
            int right = (int)(xmax * originalWidth);
            int bottom = (int)(ymax * originalHeight);
            
            // 確保座標在有效範圍內
            left = Math.max(0, Math.min(originalWidth - 1, left));
            top = Math.max(0, Math.min(originalHeight - 1, top));
            right = Math.max(left + 1, Math.min(originalWidth, right));
            bottom = Math.max(top + 1, Math.min(originalHeight, bottom));
            
            // 獲取類別名稱
            String className = "object";
            if (classIndex > 0 && classIndex <= COCO_CLASSES.length) {
                className = COCO_CLASSES[classIndex - 1];
            }
            String chineseName = CLASS_NAMES_ZH.get(className);
            if (chineseName == null) {
                chineseName = className;
            }
            
            // 創建檢測結果
            android.graphics.Rect boundingBox = new android.graphics.Rect(left, top, right, bottom);
            results.add(new DetectionResult(className, chineseName, confidence, boundingBox));
        }
        
        // 按置信度排序
        Collections.sort(results, new Comparator<DetectionResult>() {
            @Override
            public int compare(DetectionResult a, DetectionResult b) {
                return Float.compare(b.getConfidence(), a.getConfidence());
            }
        });
        
        // 只返回置信度最高的2個物體
        if (results.size() > 2) {
            results = results.subList(0, 2);
            Log.d(TAG, "後處理檢測限制為2個物體");
        }
        
        return results;
    }
    
    /**
     * 後處理 SSD MobileNet 輸出
     */
    private List<DetectionResult> postProcessSSDOutput(float[][] boxes, float[][] classes, 
                                                     float[] scores, int numDetections, 
                                                     int originalWidth, int originalHeight) {
        List<DetectionResult> results = new ArrayList<>();
        
        for (int i = 0; i < Math.min(numDetections, scores.length); i++) {
            float confidence = scores[i];
            
            // 過濾低置信度檢測
            if (confidence < AppConstants.CONFIDENCE_THRESHOLD) {
                continue;
            }
            
            // 找到最高概率的類別
            int maxClassIndex = 0;
            float maxClassScore = classes[i][0];
            
            for (int j = 1; j < classes[i].length; j++) {
                if (classes[i][j] > maxClassScore) {
                    maxClassScore = classes[i][j];
                    maxClassIndex = j;
                }
            }
            
            // 跳過背景類別 (索引0)
            if (maxClassIndex == 0) {
                continue;
            }
            
            // 獲取邊界框座標 (y1, x1, y2, x2)
            float y1 = boxes[i][0];
            float x1 = boxes[i][1];
            float y2 = boxes[i][2];
            float x2 = boxes[i][3];
            
            // 轉換為像素座標
            int left = (int)(x1 * originalWidth);
            int top = (int)(y1 * originalHeight);
            int right = (int)(x2 * originalWidth);
            int bottom = (int)(y2 * originalHeight);
            
            // 確保座標在有效範圍內
            left = Math.max(0, Math.min(originalWidth - 1, left));
            top = Math.max(0, Math.min(originalHeight - 1, top));
            right = Math.max(0, Math.min(originalWidth - 1, right));
            bottom = Math.max(0, Math.min(originalHeight - 1, bottom));
            
            // 獲取類別名稱
            if (maxClassIndex - 1 < COCO_CLASSES.length) {
                String className = COCO_CLASSES[maxClassIndex - 1];
                String chineseName = CLASS_NAMES_ZH.get(className);
                
                if (chineseName != null) {
                    Rect boundingBox = new Rect(left, top, right, bottom);
                    
                    // 驗證邊界框合理性：寬度和高度必須大於20像素
                    int width = right - left;
                    int height = bottom - top;
                    if (width >= 20 && height >= 20 && width > 0 && height > 0) {
                        results.add(new DetectionResult(className, chineseName, confidence, boundingBox));
                    } else {
                        Log.d(TAG, "濾除不合理邊界框: " + chineseName + " (" + width + "x" + height + ")");
                    }
                }
            }
        }
        
        // 應用 NMS
        results = applyNMS(results);
        
        // 按置信度排序
        Collections.sort(results, new Comparator<DetectionResult>() {
            @Override
            public int compare(DetectionResult a, DetectionResult b) {
                return Float.compare(b.getConfidence(), a.getConfidence());
            }
        });
        
        // 只返回置信度最高的3個物體
        if (results.size() > 3) {
            results = results.subList(0, 3);
            Log.d(TAG, "SSD檢測限制為3個物體");
        }
        
        return results;
    }
    
    /**
     * 後處理 YOLO 輸出（保留作為備用）
     */
    private List<DetectionResult> postProcessOutput(float[][] output, int originalWidth, int originalHeight) {
        List<DetectionResult> results = new ArrayList<>();
        
        for (int i = 0; i < output.length; i++) {
            float[] detection = output[i];
            
            // 提取邊界框座標 (x_center, y_center, width, height)
            float x_center = detection[0];
            float y_center = detection[1];
            float width = detection[2];
            float height = detection[3];
            
            // 找到最高置信度的類別
            int maxClassIndex = 4;
            float maxConfidence = detection[4];
            
            for (int j = 5; j < detection.length; j++) {
                if (detection[j] > maxConfidence) {
                    maxConfidence = detection[j];
                    maxClassIndex = j;
                }
            }
            
            // 計算總置信度
            float confidence = maxConfidence;
            
            // 過濾低置信度檢測
            if (confidence < AppConstants.CONFIDENCE_THRESHOLD) {
                continue;
            }
            
            // 轉換為邊界框座標
            float left = (x_center - width / 2) / AppConstants.INPUT_SIZE;
            float top = (y_center - height / 2) / AppConstants.INPUT_SIZE;
            float right = (x_center + width / 2) / AppConstants.INPUT_SIZE;
            float bottom = (y_center + height / 2) / AppConstants.INPUT_SIZE;
            
            // 確保座標在有效範圍內
            left = Math.max(0, Math.min(1, left));
            top = Math.max(0, Math.min(1, top));
            right = Math.max(0, Math.min(1, right));
            bottom = Math.max(0, Math.min(1, bottom));
            
            // 獲取類別名稱
            int classIndex = maxClassIndex - 4;
            if (classIndex >= 0 && classIndex < COCO_CLASSES.length) {
                String className = COCO_CLASSES[classIndex];
                String chineseName = CLASS_NAMES_ZH.get(className);
                
                // 創建邊界框 - 使用相對座標 (0-1)，保持浮點數精度
                Rect boundingBox = new Rect(
                    (int)(left * 1000), (int)(top * 1000), 
                    (int)(right * 1000), (int)(bottom * 1000)
                );
                
                results.add(new DetectionResult(className, chineseName, confidence, boundingBox));
            }
        }
        
        // 應用 NMS (Non-Maximum Suppression)
        results = applyNMS(results);
        
        // 按置信度排序
        Collections.sort(results, new Comparator<DetectionResult>() {
            @Override
            public int compare(DetectionResult a, DetectionResult b) {
                return Float.compare(b.getConfidence(), a.getConfidence());
            }
        });
        
        // 只返回置信度最高的2個物體
        if (results.size() > 2) {
            results = results.subList(0, 2);
            Log.d(TAG, "YOLO檢測限制為2個物體");
        }
        
        return results;
    }
    
    /**
     * 應用非極大值抑制 (NMS)
     */
    private List<DetectionResult> applyNMS(List<DetectionResult> detections) {
        List<DetectionResult> filtered = new ArrayList<>();
        
        for (DetectionResult detection : detections) {
            boolean shouldKeep = true;
            
            for (DetectionResult existing : filtered) {
                if (detection.getLabel().equals(existing.getLabel())) {
                    float iou = calculateIoU(detection.getBoundingBox(), existing.getBoundingBox());
                    if (iou > AppConstants.IOU_THRESHOLD) {
                        shouldKeep = false;
                        break;
                    }
                }
            }
            
            if (shouldKeep) {
                filtered.add(detection);
            }
        }
        
        return filtered;
    }
    
    /**
     * 計算 IoU (Intersection over Union)
     */
    private float calculateIoU(Rect box1, Rect box2) {
        // 轉換為浮點數座標
        float left1 = box1.left / 1000.0f;
        float top1 = box1.top / 1000.0f;
        float right1 = box1.right / 1000.0f;
        float bottom1 = box1.bottom / 1000.0f;
        
        float left2 = box2.left / 1000.0f;
        float top2 = box2.top / 1000.0f;
        float right2 = box2.right / 1000.0f;
        float bottom2 = box2.bottom / 1000.0f;
        
        // 計算交集
        float intersectionLeft = Math.max(left1, left2);
        float intersectionTop = Math.max(top1, top2);
        float intersectionRight = Math.min(right1, right2);
        float intersectionBottom = Math.min(bottom1, bottom2);
        
        if (intersectionRight <= intersectionLeft || intersectionBottom <= intersectionTop) {
            return 0.0f;
        }
        
        float intersection = (intersectionRight - intersectionLeft) * (intersectionBottom - intersectionTop);
        
        // 計算並集
        float area1 = (right1 - left1) * (bottom1 - top1);
        float area2 = (right2 - left2) * (bottom2 - top2);
        float union = area1 + area2 - intersection;
        
        return intersection / union;
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
            yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 85, out);
            byte[] imageBytes = out.toByteArray();
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        } catch (Exception e) {
            Log.e(TAG, "圖像轉換失敗: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 備用檢測方法（當 YOLO 模型不可用時使用）
     * 不使用簡陋的特徵檢測，直接返回空結果以避免誤報
     */
    private List<DetectionResult> getFallbackDetections(Bitmap bitmap) {
        Log.w(TAG, "模型未載入，無法進行準確檢測，返回空結果");
        return new ArrayList<>(); // 返回空列表，避免誤報
    }
    
    /**
     * 檢測人體（基於顏色和形狀特徵）
     */
    private boolean detectPerson(int[] pixels, int width, int height) {
        // 簡單的膚色檢測
        int skinPixels = 0;
        for (int pixel : pixels) {
            int r = (pixel >> 16) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = pixel & 0xFF;
            
            // 膚色範圍檢測
            if (r > 95 && g > 40 && b > 20 && 
                Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) > 15 &&
                Math.abs(r - g) > 15 && r > g && r > b) {
                skinPixels++;
            }
        }
        
        // 如果膚色像素超過一定比例，認為有人
        return (float)skinPixels / pixels.length > 0.05f;
    }
    
    /**
     * 檢測家具（基於邊緣和形狀特徵）
     */
    private boolean detectFurniture(int[] pixels, int width, int height) {
        // 簡單的邊緣檢測
        int edgePixels = 0;
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int center = pixels[y * width + x];
                int right = pixels[y * width + (x + 1)];
                int bottom = pixels[(y + 1) * width + x];
                
                // 計算梯度
                int gradX = Math.abs((center & 0xFF) - (right & 0xFF));
                int gradY = Math.abs((center & 0xFF) - (bottom & 0xFF));
                
                if (gradX > 30 || gradY > 30) {
                    edgePixels++;
                }
            }
        }
        
        // 如果邊緣像素超過一定比例，認為有家具
        return (float)edgePixels / pixels.length > 0.1f;
    }
    
    /**
     * 檢測電子產品（基於顏色和亮度特徵）
     */
    private boolean detectElectronics(int[] pixels, int width, int height) {
        int brightPixels = 0;
        int darkPixels = 0;
        
        for (int pixel : pixels) {
            int brightness = ((pixel >> 16) & 0xFF) + ((pixel >> 8) & 0xFF) + (pixel & 0xFF);
            brightness /= 3;
            
            if (brightness > 200) {
                brightPixels++;
            } else if (brightness < 50) {
                darkPixels++;
            }
        }
        
        // 電子產品通常有高對比度
        return (float)brightPixels / pixels.length > 0.1f && 
               (float)darkPixels / pixels.length > 0.1f;
    }
    
    /**
     * 格式化檢測結果為語音文本
     * 只播報物體名稱，不包含前綴、距離或位置信息
     */
    public String formatResultsForSpeech(List<DetectionResult> results) {
        if (results.isEmpty()) {
            return "未偵測到任何物體";
        }
        
        StringBuilder sb = new StringBuilder();
        // 最多播報2個物體
        int maxObjects = Math.min(results.size(), 2);
        for (int i = 0; i < maxObjects; i++) {
            DetectionResult result = results.get(i);
            sb.append(result.getLabelZh());
            if (i < maxObjects - 1) {
                sb.append("、");
            }
        }
        
        // 如果物體超過2個，添加總數
        if (results.size() > 2) {
            sb.append("等").append(results.size()).append("個");
        }
        
        return sb.toString();
    }
    
    /**
     * 獲取中文類別名稱
     */
    public static String getChineseLabel(String englishLabel) {
        String chineseLabel = CLASS_NAMES_ZH.get(englishLabel);
        return chineseLabel != null ? chineseLabel : englishLabel;
    }
    
    /**
     * 獲取檢測性能報告
     */
    public String getPerformanceReport() {
        if (performanceMonitor != null) {
            return performanceMonitor.getPerformanceReport();
        }
        return "性能監控未初始化";
    }
    
    /**
     * 檢查檢測性能是否良好
     */
    public boolean isPerformanceGood() {
        if (performanceMonitor != null) {
            return performanceMonitor.isPerformanceGood();
        }
        return false;
    }
    
    /**
     * 重置性能統計
     */
    public void resetPerformanceStats() {
        if (performanceMonitor != null) {
            performanceMonitor.reset();
        }
    }
    
    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
        isInitialized = false;
        Log.d(TAG, "真實AI檢測器資源已釋放");
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