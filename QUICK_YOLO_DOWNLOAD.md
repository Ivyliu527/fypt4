# ⚡ 快速下載 YOLOv8 TFLite 模型

## 🎯 最簡單的方法：直接下載

### 方法 1：從 Hugging Face 下載（推薦，最快）

1. **訪問 Hugging Face**
   ```
   https://huggingface.co/datasets/falahgs/YOLOv8/tree/main
   或
   https://huggingface.co/models?search=yolov8n+tflite
   ```

2. **下載文件**
   - 找到 `yolov8n.tflite` 文件
   - 點擊下載（約 4-6 MB）

3. **放置到項目**
   ```bash
   # 將下載的文件複製到：
   cp ~/Downloads/yolov8n.tflite app/src/main/assets/yolov8n.tflite
   
   # 驗證文件大小（應該是 4-6 MB，不是 9 字節）
   ls -lh app/src/main/assets/yolov8n.tflite
   ```

---

### 方法 2：使用 curl 直接下載

嘗試以下鏈接（可能需要替換為實際可用的 URL）：

```bash
cd app/src/main/assets

# 備份損壞的文件
mv yolov8n.tflite yolov8n.tflite.broken

# 嘗試下載（如果找到有效鏈接）
# curl -L -o yolov8n.tflite <實際下載URL>
```

---

### 方法 3：手動從 GitHub 下載

1. **訪問 Ultralytics Assets**
   ```
   https://github.com/ultralytics/assets/releases
   ```

2. **查找 YOLOv8 相關發布**
   - 可能需要註冊 GitHub 帳號
   - 下載 `yolov8n.tflite` 或 `yolov8n.pt`

3. **如果是 .pt 文件，使用在線轉換**
   - https://colab.research.google.com/
   - 或使用其他在線 TFLite 轉換工具

---

## 📋 當前狀態

- ✅ **SSD MobileNet** (4.0 MB) - **正常工作**，是主要檢測器
- ❌ **YOLOv8** (9 字節) - 損壞，可選的備用檢測器

**重要提示**：由於 SSD MobileNet 已經正常工作，**YOLO 模型不是必須的**。您可以：

1. **繼續使用 SSD** - 當前的主要檢測器（已修復所有問題）
2. **稍後下載 YOLO** - 作為性能提升的可選方案

---

## ✅ 驗證下載

下載完成後，運行：

```bash
# 檢查文件大小
ls -lh app/src/main/assets/yolov8n.tflite

# 應該顯示約 4-6 MB
# 如果是 9 字節，說明下載失敗或文件損壞

# 檢查文件類型
file app/src/main/assets/yolov8n.tflite
# 正常應該顯示 "data" 或 "Zip archive"

# 重新編譯
./gradlew clean assembleDebug
```

---

## 🆘 如果下載失敗

**當前環境識別功能使用 SSD MobileNet 已經完全正常工作**，YOLO 只是可選的備用檢測器。

如果無法下載 YOLO，應用仍然可以正常使用：
- ✅ 環境識別功能正常
- ✅ 檢測框正常顯示
- ✅ 真實 AI 檢測正常工作

YOLO 可以稍後再添加。

