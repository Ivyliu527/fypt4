# YOLO 模型文件設置指南

## 📁 模型文件位置

將 YOLO 模型文件放置在以下位置：
```
app/src/main/assets/yolov8n.tflite
```

## 🔗 獲取模型文件

### 方法 1：從 Ultralytics 下載
1. 訪問 [Ultralytics YOLOv8](https://github.com/ultralytics/ultralytics)
2. 下載 `yolov8n.tflite` 模型文件
3. 將文件重命名為 `yolov8n.tflite`
4. 放置在 `app/src/main/assets/` 目錄中

### 方法 2：使用 Python 轉換
```python
from ultralytics import YOLO

# 下載並轉換為 TensorFlow Lite
model = YOLO('yolov8n.pt')
model.export(format='tflite', imgsz=640)
```

### 方法 3：使用現有模型
如果沒有 YOLO 模型文件，應用會自動使用備用檢測方法：
- 基於膚色檢測的人體識別
- 基於邊緣檢測的家具識別
- 基於對比度的電子產品識別

## ⚙️ 模型參數

當前配置：
- **輸入尺寸**: 640x640 像素
- **類別數量**: 80 (COCO 數據集)
- **置信度閾值**: 0.25
- **IoU 閾值**: 0.45

## 🔧 自定義配置

如需修改模型參數，請編輯 `YoloDetector.java` 中的常量：

```java
private static final String MODEL_FILE = "yolov8n.tflite";
private static final int INPUT_SIZE = 640;
private static final int NUM_CLASSES = 80;
private static final float CONFIDENCE_THRESHOLD = 0.25f;
private static final float IOU_THRESHOLD = 0.45f;
```

## 📱 測試

1. 將模型文件放置在正確位置
2. 編譯並運行應用
3. 進入環境識別功能
4. 檢查日誌輸出：
   - `YOLO 檢測器初始化成功` - 模型載入成功
   - `YOLO 模型未載入，使用備用檢測方法` - 使用備用方法

## 🚀 性能優化

### GPU 加速
如需 GPU 加速，請在 `build.gradle` 中添加：
```gradle
dependencies {
    implementation 'org.tensorflow:tensorflow-lite-gpu:2.14.0'
}
```

### 模型量化
使用量化模型可減少記憶體使用：
- `yolov8n-int8.tflite` - 8位量化
- `yolov8n-fp16.tflite` - 16位浮點

## 📊 支援的物體類別

應用支援 COCO 數據集的 80 個類別，包括：
- 人物：人
- 交通工具：汽車、公車、火車、飛機等
- 動物：貓、狗、鳥、馬等
- 家具：椅子、沙發、床、餐桌等
- 電子產品：手機、筆記本電腦、電視等
- 日常用品：瓶子、杯子、書、時鐘等

## 🔍 故障排除

### 模型載入失敗
1. 檢查文件路徑是否正確
2. 確認文件大小（應約 6MB）
3. 檢查文件權限

### 檢測結果不準確
1. 調整置信度閾值
2. 檢查光照條件
3. 確保相機對焦清晰

### 性能問題
1. 降低輸入解析度
2. 增加檢測間隔
3. 使用量化模型
