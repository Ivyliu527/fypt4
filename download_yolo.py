#!/usr/bin/env python3
"""
YOLOv8 Nano TensorFlow Lite 模型下載腳本
使用 ultralytics 自動下載並轉換模型
"""

import os
import sys
from pathlib import Path

try:
    from ultralytics import YOLO
    print("✅ Ultralytics 已安裝")
except ImportError:
    print("❌ 未安裝 Ultralytics")
    print("正在安裝 ultralytics...")
    os.system(f"{sys.executable} -m pip install ultralytics --quiet")
    from ultralytics import YOLO
    print("✅ Ultralytics 安裝完成")

def download_and_convert_yolo():
    """下載並轉換 YOLOv8 Nano 模型"""
    
    print("\n🚀 開始下載 YOLOv8 Nano 模型...")
    print("=" * 50)
    
    try:
        # 加載 YOLOv8 Nano 模型（會自動下載）
        print("📥 下載 yolov8n.pt...")
        model = YOLO('yolov8n.pt')
        print("✅ yolov8n.pt 下載完成")
        
        # 轉換為 ONNX 格式（更穩定）
        print("\n🔄 轉換為 ONNX 格式（更穩定，可後續轉換為 TFLite）...")
        success = model.export(
            format='onnx',        # ONNX 格式（更穩定）
            imgsz=640,           # 輸入尺寸 640x640
            simplify=True,        # 簡化模型
            opset=12             # ONNX opset 版本
        )
        
        if not success:
            print("❌ 轉換失敗")
            return False
            
        # 查找轉換後的模型文件
        converted_file = None
        for filename in os.listdir('.'):
            if filename.endswith('.onnx') and 'yolov8n' in filename:
                converted_file = filename
                break
        
        # 也查找 TFLite 文件
        if not converted_file:
            for filename in os.listdir('.'):
                if filename.endswith('.tflite') and 'yolov8n' in filename:
                    converted_file = filename
                    break
        
        if not converted_file or not os.path.exists(converted_file):
            print("❌ 找不到轉換後的模型文件")
            return False
        
        # 獲取文件大小
        file_size = os.path.getsize(converted_file) / (1024 * 1024)  # MB
        print(f"\n✅ 模型轉換成功！")
        print(f"📁 文件: {converted_file}")
        print(f"📏 大小: {file_size:.2f} MB")
        
        # 檢查文件大小是否合理（應該在 4-8 MB 之間）
        if file_size < 1:
            print("⚠️  警告：模型文件太小，可能轉換失敗")
            return False
        
        # 複製到 assets 目錄
        assets_dir = Path('app/src/main/assets')
        if not assets_dir.exists():
            print(f"❌ Assets 目錄不存在: {assets_dir}")
            return False
        
        # 根據轉換的文件類型設置目標文件名
        if converted_file.endswith('.onnx'):
            target_path = assets_dir / 'yolov8n.onnx'
            print("📦 使用 ONNX 格式模型")
        else:
            target_path = assets_dir / 'yolov8n.tflite'
            print("📦 使用 TFLite 格式模型")
        
        # 備份舊文件（如果存在）
        if target_path.exists():
            backup_path = assets_dir / 'yolov8n.tflite.backup'
            print(f"💾 備份舊文件: {target_path} → {backup_path}")
            os.rename(target_path, backup_path)
        
        # 複製新文件
        print(f"📋 複製到: {target_path}")
        import shutil
        shutil.copy2(converted_file, target_path)
        
        final_size = os.path.getsize(target_path) / (1024 * 1024)
        print(f"✅ 成功！模型已放置到 assets 目錄")
        print(f"📏 最終大小: {final_size:.2f} MB")
        
        return True
        
    except Exception as e:
        print(f"❌ 錯誤: {e}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == '__main__':
    print("🎯 YOLOv8 Nano 模型下載工具")
    print("=" * 50)
    
    # 切換到腳本所在目錄
    script_dir = os.path.dirname(os.path.abspath(__file__))
    os.chdir(script_dir)
    print(f"📂 工作目錄: {os.getcwd()}")
    
    # 執行下載和轉換
    success = download_and_convert_yolo()
    
    if success:
        print("\n" + "=" * 50)
        print("🎉 完成！模型已準備就緒")
        print("\n📝 下一步：")
        print("1. 在 Android Studio 中 Rebuild Project")
        print("2. 運行 APP 測試環境識別功能")
        print("3. 檢查日誌確認模型載入成功")
        sys.exit(0)
    else:
        print("\n" + "=" * 50)
        print("❌ 下載失敗")
        print("\n💡 備選方案：")
        print("1. 手動下載: https://github.com/ultralytics/assets")
        print("2. 檢查網絡連接")
        print("3. 查看 YOLO_MODEL_DOWNLOAD_GUIDE.md 獲取更多幫助")
        sys.exit(1)

