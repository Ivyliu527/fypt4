# 🎯 YOLO 模型下載與配置指南

## 📋 **問題分析**

目前您的環境識別出現**誤報問題**（牆壁被檢測為人），原因：
1. ✅ **已修復**：置信度閾值太低（0.3 → 0.6）
2. ✅ **已修復**：備用檢測邏輯過於簡陋
3. ⚠️ **待解決**：缺少真正的 YOLOv8 模型文件

---

## 🔗 **獲取 YOLOv8 模型**

### 方法 1：直接下載（推薦）

```bash
# 訪問官方下載頁面
https://github.com/ultralytics/assets/releases/tag/v0.0.0

# 下載 yolov8n.tflite（約4-6MB）
# 注意：需要註冊 GitHub 才能下載
```

### 方法 2：使用 Python 轉換

如果無法直接下載，可以使用 Python 轉換：

```bash
# 1. 安裝 Ultralytics
pip install ultralytics

# 2. 創建轉換腳本 convert_yolo.py
cat > convert_yolo.py << 'EOF'
from ultralytics import YOLO
import os

# 下載並轉換 YOLOv8 Nano 為 TensorFlow Lite
model = YOLO('yolov8n.pt')  # 自動下載 yolo8n.pt
success = model.export(
    format='tflite',      # 轉換為 TensorFlow Lite
    imgsz=640,            # 輸入尺寸 640x640
    int8=True,            # 使用 8-bit 量化（更小更快）
    data='coco.yaml'      # 使用 COCO 數據集
)

if success:
    print("✅ 轉換成功！")
    print(f"模型文件位置: {os.path.abspath('yolov8n.tflite')}")
else:
    print("❌ 轉換失敗")
EOF

# 3. 運行轉換
python convert_yolo.py

# 4. 複製到項目
cp yolov8n.tflite /path/to/Tonbo_App/app/src/main/assets/
```

### 方法 3：使用替代模型

如果上述方法都無法使用，可以使用 **TensorFlow Hub** 的模型：

```bash
# 訪問
https://tfhub.dev/s?deployment-format=lite

# 搜索 "Object Detection"
# 推薦模型：
- EfficientDet-Lite0 (約 2MB，速度快)
- EfficientDet-Lite1 (約 3MB，平衡)
- EfficientDet-Lite2 (約 4MB，高精度)
```

---

## 📦 **文件放置**

將下載的模型文件放置在：

```
app/src/main/assets/
├── ssd_mobilenet_v1.tflite  # 現有模型（保留）
└── yolov8n.tflite           # 新模型（4-6MB）
```

---

## ⚙️ **切換到 YOLO 模型**

編輯 `AppConstants.java`：

```java
// 模型參數
public static final String MODEL_FILE = "yolov8n.tflite";  // 切換到 YOLO
// public static final String YOLO_MODEL_FILE = "yolov8n.tflite";
public static final int INPUT_SIZE = 640;  // YOLO 使用 640x640
public static final int NUM_CLASSES = 80;   // COCO 80 類別
```

---

## 🧪 **測試**

1. **編譯運行**
   ```bash
   ./gradlew assembleDebug
   ```

2. **檢查日誌**
   ```bash
   # 應看到：
   ✅ "真實AI檢測器初始化成功 - 使用YOLO模型"
   ✅ "YOLO 檢測器初始化成功"
   ```

3. **測試檢測**
   - 開啟環境識別功能
   - 對準牆壁 → **不應該有**檢測結果
   - 對準真實物體 → **應該有**準確檢測

---

## 📊 **預期改進**

| 問題 | 之前 | 現在 |
|------|------|------|
| 牆壁誤報 | ❌ 檢測為「人」 | ✅ 無檢測 |
| 置信度門檻 | ❌ 0.3（太鬆） | ✅ 0.6（嚴格） |
| 邊界框驗證 | ❌ 無驗證 | ✅ 最小20x20px |
| 備用檢測 | ❌ 誤報嚴重 | ✅ 返回空結果 |

---

## 🚀 **可選：更強的模型**

如果需要更高準確度（但速度稍慢）：

```bash
# YOLOv8 Small (約10MB)
model = YOLO('yolov8s.pt')
model.export(format='tflite', imgsz=640)

# YOLOv8 Medium (約25MB)
model = YOLO('yolov8m.pt')
model.export(format='tflite', imgsz=640)

# 注意：更大模型會增加記憶體使用和處理時間
```

---

## 🔧 **故障排查**

### 問題：模型載入失敗
```bash
# 檢查文件是否正確
ls -lh app/src/main/assets/yolov8n.tflite
# 應該顯示約 4-6 MB

# 如果文件太小（<1MB），說明下載失敗
```

### 問題：檢測仍然誤報
```bash
# 提高置信度閾值（在 AppConstants.java）
public static final float CONFIDENCE_THRESHOLD = 0.7f;  # 從 0.6 提高到 0.7
```

### 問題：記憶體不足
```bash
# 使用量化模型（更小）
# 在下載時使用 int8=True（已包含在轉換腳本中）
```

---

## 📞 **需要幫助？**

如果遇到問題：
1. 檢查日誌輸出：`logcat | grep YoloDetector`
2. 確認模型文件大小 > 4MB
3. 確認 Android 版本 >= Android 5.0 (API 21)

