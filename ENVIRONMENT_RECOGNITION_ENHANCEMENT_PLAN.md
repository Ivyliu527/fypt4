# 环境识别功能增强计划

## 功能概述
实现一个智能环境识别系统，支持语音命令启动、主动寻找物体、动态引导等功能。

## 核心功能模块

### 1. 语音命令启动模块
**功能**：通过语音命令启动环境识别
- "嘿Siri，帮我看看周围"（iOS Shortcuts）
- "打开环境识别"（应用内语音识别）
- 物理按钮：双击音量下键

**实现方案**：
```java
// 使用现有的 ASRManager 或集成 Sherpa-Onnx
// 监听语音命令："打开环境识别"、"帮我看看"、"环境识别"
// 支持快捷方式：音量键双击
```

**开源资源**：
- Sherpa-Onnx（已集成）
- Android Accessibility Service（用于音量键监听）

### 2. 主动寻找与精确定位模块
**功能**：用户说出目标物体，系统主动寻找并引导

**实现方案**：
```java
// 1. 语音识别用户问题："我的钥匙在哪？"
// 2. 提取关键词："钥匙"
// 3. 实时检测画面中的物体
// 4. 匹配关键词与检测结果
// 5. 如果找到，播报位置；如果没找到，引导用户移动手机
```

**核心代码结构**：
```java
public class ObjectSearchManager {
    // 搜索特定物体
    public void searchObject(String objectName) {
        // 1. 实时检测
        // 2. 匹配物体名称
        // 3. 计算位置
        // 4. 生成引导语音
    }
    
    // 生成方位描述
    private String generatePositionDescription(DetectionResult result) {
        // "钥匙在您右前方的茶几上，距离您大约1米"
        // 使用：左前/右前/正左/正右、远处/近处、高处/低处
    }
}
```

### 3. 动态引导模块
**功能**：实时跟踪物体位置，引导用户移动手机

**实现方案**：
```java
public class DynamicGuidanceManager {
    private String targetObject;
    private RectF lastKnownPosition;
    
    // 实时跟踪物体
    public void trackObject(String objectName) {
        // 1. 检测物体是否在画面中
        // 2. 如果在画面中：计算位置并播报
        // 3. 如果不在画面中：引导用户移动
        //    - "请将手机向右移动"
        //    - "再稍微抬高一点"
        //    - "好了，现在钥匙出现在画面中央了"
    }
    
    // 生成引导指令
    private String generateGuidance(RectF currentPos, RectF targetPos) {
        // 计算需要移动的方向和距离
        // 生成自然语言引导
    }
}
```

### 4. 精确方位描述模块
**功能**：使用非视觉性空间语言描述物体位置

**实现方案**：
```java
public class SpatialDescriptionGenerator {
    // 生成方位描述
    public String describePosition(DetectionResult result, float imageWidth, float imageHeight) {
        float centerX = result.getBoundingBox().centerX();
        float centerY = result.getBoundingBox().centerY();
        
        // 水平方向：左前/右前/正左/正右/正前方
        String horizontal = getHorizontalDirection(centerX, imageWidth);
        
        // 垂直方向：高处/低处/中间
        String vertical = getVerticalDirection(centerY, imageHeight);
        
        // 距离估算：基于物体大小
        String distance = estimateDistance(result.getBoundingBox());
        
        return String.format("%s在您%s%s，距离您大约%s", 
            result.getLabelZh(), horizontal, vertical, distance);
    }
    
    private String getHorizontalDirection(float x, float width) {
        float ratio = x / width;
        if (ratio < 0.3) return "左前方";
        if (ratio > 0.7) return "右前方";
        if (ratio < 0.4) return "正左方";
        if (ratio > 0.6) return "正右方";
        return "正前方";
    }
    
    private String estimateDistance(RectF bbox) {
        float area = bbox.width() * bbox.height();
        // 基于物体大小估算距离
        if (area > 0.1) return "1米";
        if (area > 0.05) return "2米";
        if (area > 0.02) return "3米";
        return "较远处";
    }
}
```

## 实现步骤

### 阶段1：语音命令启动（1-2周）
1. 集成语音识别监听
2. 添加音量键双击监听
3. 实现快捷启动

### 阶段2：主动寻找功能（2-3周）
1. 实现物体搜索管理器
2. 添加关键词匹配
3. 实现位置描述生成

### 阶段3：动态引导（2-3周）
1. 实现物体跟踪
2. 添加引导指令生成
3. 优化语音播报

### 阶段4：优化与测试（1-2周）
1. 性能优化
2. 无障碍测试
3. 用户体验优化

## 技术栈

### 已使用
- ✅ TensorFlow Lite（物体检测）
- ✅ YOLO（物体检测）
- ✅ TTSManager（语音播报）
- ✅ CameraX（摄像头）

### 需要添加
- 🔄 ASRManager（语音识别）- 已有基础
- 🔄 物体跟踪算法（OpenCV 或 MediaPipe）
- 🔄 空间定位算法（基于物体位置和手机姿态）

## 开源资源推荐

### 1. 语音识别
- **Sherpa-Onnx**（已集成）：离线语音识别
- **Vosk**：轻量级离线语音识别
- **Whisper.cpp**：高质量多语言识别

### 2. 物体跟踪
- **OpenCV Android**：物体跟踪、光流法
- **MediaPipe**：Google 实时感知框架
- **TensorFlow Lite Object Tracking**：轻量级跟踪

### 3. 空间理解
- **ARCore**（可选）：空间理解，但需要 Google Play Services
- **Android Sensor API**：陀螺仪、加速度计、磁力计

## 示例代码结构

```java
// 主控制器
public class EnhancedEnvironmentRecognitionActivity extends BaseAccessibleActivity {
    private ObjectSearchManager searchManager;
    private DynamicGuidanceManager guidanceManager;
    private SpatialDescriptionGenerator spatialDesc;
    private ASRManager asrManager;
    
    // 语音命令处理
    private void handleVoiceCommand(String command) {
        if (command.contains("钥匙") || command.contains("key")) {
            searchManager.searchObject("钥匙");
        } else if (command.contains("打开") || command.contains("开始")) {
            startEnvironmentRecognition();
        }
    }
    
    // 主动寻找
    private void searchForObject(String objectName) {
        searchManager.searchObject(objectName);
    }
}
```

## 注意事项

1. **性能优化**：实时检测和跟踪需要优化，避免卡顿
2. **电池消耗**：摄像头和AI处理会消耗大量电量
3. **准确性**：物体识别和位置估算的准确性需要不断优化
4. **无障碍**：所有功能都要有语音反馈和震动反馈

## 总结

这个功能**完全可以实现**，而且您的项目已经有了很好的基础。主要需要：
1. 增强语音识别功能（已有ASRManager基础）
2. 添加物体搜索和跟踪功能
3. 优化空间描述算法
4. 实现动态引导逻辑

预计开发时间：**6-10周**（根据团队规模）

