#!/usr/bin/env python3
"""
直接下載預轉換的 YOLOv8 TFLite 模型
"""

import urllib.request
import os
from pathlib import Path

def download_pre_converted_yolo():
    """直接下載預轉換的 TFLite 模型"""
    
    print("🎯 直接下載 YOLOv8 Nano TFLite 模型")
    print("=" * 50)
    
    # 幾個可能的下載源
    download_urls = [
        "https://github.com/ultralytics/assets/releases/download/v8.3.0/yolov8n.tflite",
        "https://github.com/ultralytics/assets/releases/download/v8.2.0/yolov8n.tflite",
        "https://github.com/ultralytics/assets/releases/download/v8.1.0/yolov8n.tflite",
    ]
    
    assets_dir = Path('app/src/main/assets')
    target_path = assets_dir / 'yolov8n.tflite'
    
    # 備份舊文件
    if target_path.exists():
        backup_path = assets_dir / 'yolov8n.tflite.backup'
        print(f"💾 備份舊文件...")
        os.rename(target_path, backup_path)
    
    for url in download_urls:
        try:
            print(f"\n📥 嘗試從: {url}")
            print("下載中...", end="", flush=True)
            
            urllib.request.urlretrieve(url, target_path, reporthook=lambda n, _, size: print(".", end="", flush=True) if n % (size // 10) == 0 else None)
            print()  # 換行
            
            # 檢查文件大小
            file_size = os.path.getsize(target_path) / (1024 * 1024)
            
            if file_size > 4:  # 應該 > 4MB
                print(f"\n✅ 下載成功！")
                print(f"📏 大小: {file_size:.2f} MB")
                return True
            else:
                print(f"❌ 文件太小 ({file_size:.2f} MB)，嘗試下一個源...")
                os.remove(target_path)
                
        except Exception as e:
            print(f"\n❌ 失敗: {e}")
            if target_path.exists():
                os.remove(target_path)
            continue
    
    print("\n" + "=" * 50)
    print("❌ 所有下載源都失敗了")
    print("\n💡 手動下載方案：")
    print("1. 訪問: https://github.com/ultralytics/assets/releases")
    print("2. 下載 yolov8n.tflite (約 4-6 MB)")
    print("3. 放置到: app/src/main/assets/")
    return False

if __name__ == '__main__':
    success = download_pre_converted_yolo()
    
    if success:
        print("\n" + "=" * 50)
        print("🎉 完成！模型已準備就緒")
        print("\n📝 下一步：")
        print("1. 在 Android Studio 中 Rebuild Project")
        print("2. 運行 APP 測試環境識別功能")
        sys.exit(0)
    else:
        sys.exit(1)

