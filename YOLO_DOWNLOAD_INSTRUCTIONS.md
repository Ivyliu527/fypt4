# 🎯 YOLOv8 TFLite 模型下載指南

## 📋 當前狀態

- ❌ `yolov8n.tflite` (9字節) - **損壞，需要重新下載**
- ✅ `yolov8n.pt` (6.2 MB) - 可用，可轉換為 TFLite
- ✅ `yolov8n.onnx` (12 MB) - 可用

---

## 🚀 方法 1：使用 Python 轉換（推薦）

### 步驟 1：安裝依賴

```bash
# 安裝 ultralytics
pip3 install ultralytics

# 如果失敗，嘗試：
pip install ultralytics --upgrade
```

### 步驟 2：運行轉換腳本

```bash
cd /Users/charlie.ppy/Documents/GitHub/Tonbo_App/Tonbo_App/Tonbo_App
python3 convert_yolo_to_tflite.py
```

腳本會自動：
1. 使用現有的 `yolov8n.pt` 文件
2. 轉換為 TFLite 格式（8-bit 量化）
3. 輸出到 `app/src/main/assets/yolov8n.tflite`

### 步驟 3：驗證文件

```bash
ls -lh app/src/main/assets/yolov8n.tflite
# 應該顯示約 4-6 MB（不是 9 字節）
```

---

## 🌐 方法 2：手動下載（無需 Python）

### 選項 A：從 GitHub 下載

1. **訪問 Ultralytics Assets**
   - URL: https://github.com/ultralytics/assets/releases
   - 或搜索 "ultralytics assets releases"

2. **下載模型**
   - 找到 `yolov8n.tflite` 文件（約 4-6 MB）
   - 或下載 `yolov8n.pt` 然後使用在線轉換工具

3. **放置文件**
   ```bash
   # 將下載的文件複製到：
   cp ~/Downloads/yolov8n.tflite app/src/main/assets/yolov8n.tflite
   ```

### 選項 B：使用 Hugging Face

1. **訪問 Hugging Face**
   - URL: https://huggingface.co/models?search=yolov8
   - 搜索 "yolov8n tflite"

2. **下載並放置到項目**

---

## 🔧 方法 3：使用在線轉換工具

如果有 `yolov8n.pt` 或 `yolov8n.onnx` 文件：

1. **Netron（查看模型）**
   - https://netron.app/
   - 上傳模型查看結構

2. **Google Colab（轉換）**
   - 訪問: https://colab.research.google.com/
   - 創建新 notebook
   - 運行：
   ```python
   !pip install ultralytics
   from ultralytics import YOLO
   model = YOLO('yolov8n.pt')
   model.export(format='tflite', imgsz=640, int8=True)
   ```

---

## ✅ 轉換後驗證

轉換完成後，運行：

```bash
# 檢查文件大小（應該是 4-6 MB）
ls -lh app/src/main/assets/yolov8n.tflite

# 檢查文件類型
file app/src/main/assets/yolov8n.tflite
# 應該顯示: "data" 或 "Zip archive"（TFLite 實際是 zip 格式）

# 重新編譯應用
./gradlew clean assembleDebug

# 檢查日誌
adb logcat | grep -i "yolo\|tflite"
# 應該看到: "✅ YOLO檢測器初始化成功" 或 "真實AI檢測器初始化成功"
```

---

## 🆘 故障排查

### 問題：pip install 失敗
```bash
# 嘗試使用 Python3
python3 -m pip install ultralytics

# 或使用 conda
conda install -c conda-forge ultralytics
```

### 問題：轉換失敗
1. 確保 `yolov8n.pt` 文件完整（6.2 MB）
2. 檢查磁盤空間
3. 嘗試不使用量化：
   ```python
   model.export(format='tflite', imgsz=640, int8=False)
   ```

### 問題：找不到轉換後的文件
```bash
# Ultralytics 通常在以下位置生成：
find . -name "*.tflite" -type f
# 或
ls -R runs/export/
```

---

## 📝 快速命令參考

```bash
# 安裝依賴
pip3 install ultralytics

# 運行轉換（已創建腳本）
python3 convert_yolo_to_tflite.py

# 或手動轉換
python3 << EOF
from ultralytics import YOLO
model = YOLO('app/src/main/assets/yolov8n.pt')
model.export(format='tflite', imgsz=640, int8=True)
EOF

# 找到生成的文件
find . -name "yolov8n*.tflite" -type f

# 複製到 assets
cp runs/export/*/yolov8n.tflite app/src/main/assets/yolov8n.tflite
```

---

## 🎯 當前優先級

由於您已經有 **SSD MobileNet** 模型（4.0 MB）正常工作，**YOLO 模型是可選的**：

- ✅ **SSD MobileNet** - 主要檢測器（已正常工作）
- ⚠️ **YOLOv8** - 備用檢測器（需要修復）

如果 SSD 檢測效果良好，可以先使用 SSD，YOLO 模型可以稍後添加。

