# 物体识别搜索功能集成指南

## 📋 功能概述

已创建三个核心模块来实现智能物体识别和搜索功能：

1. **ObjectSearchManager** - 物体搜索管理器
2. **SpatialDescriptionGenerator** - 空间描述生成器
3. **DynamicGuidanceManager** - 动态引导管理器

## 🎯 核心功能

### 1. 主动寻找物体
- 用户说出："我的钥匙在哪？"
- 系统识别关键词："钥匙"
- 实时搜索画面中的物体
- 找到后播报精确位置

### 2. 精确方位描述
- 使用非视觉性空间语言
- "钥匙在您右前方的茶几上，距离您大约1米"
- 支持：左前/右前/正左/正右、远处/近处、高处/低处

### 3. 动态引导
- 如果物体不在画面中，引导用户移动手机
- "请将手机向右移动...再稍微抬高一点"
- 实时跟踪物体位置

## 📁 已创建的文件

```
app/src/main/java/com/example/tonbo_app/
├── ObjectSearchManager.java          # 物体搜索管理器
├── SpatialDescriptionGenerator.java  # 空间描述生成器
└── DynamicGuidanceManager.java       # 动态引导管理器
```

## 🔧 集成步骤

### 步骤 1：在 EnvironmentActivity 中添加管理器

```java
public class EnvironmentActivity extends BaseAccessibleActivity {
    // ... 现有代码 ...
    
    // 添加新的管理器
    private ObjectSearchManager objectSearchManager;
    private SpatialDescriptionGenerator spatialDesc;
    private DynamicGuidanceManager guidanceManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ... 现有代码 ...
        
        // 初始化管理器
        objectSearchManager = new ObjectSearchManager();
        spatialDesc = new SpatialDescriptionGenerator();
        guidanceManager = new DynamicGuidanceManager();
        
        // 设置语言
        spatialDesc.setLanguage(currentLanguage);
        guidanceManager.setLanguage(currentLanguage);
        
        // 设置监听器
        setupSearchListeners();
    }
    
    private void setupSearchListeners() {
        // 物体搜索监听器
        objectSearchManager.setSearchResultListener(new ObjectSearchManager.SearchResultListener() {
            @Override
            public void onObjectFound(ObjectDetectorHelper.DetectionResult result, 
                                    String positionDescription) {
                // 找到物体，播报位置
                announceInfo(positionDescription);
                vibrationManager.vibrateSuccess();
                
                // 开始动态引导
                guidanceManager.startTracking(result.getLabelZh());
            }
            
            @Override
            public void onObjectNotFound(String guidance) {
                // 没找到，播报引导
                announceInfo(guidance);
                vibrationManager.vibrateNotification();
            }
            
            @Override
            public void onSearchStarted(String objectName) {
                announceInfo("正在搜索" + objectName);
            }
            
            @Override
            public void onSearchStopped() {
                guidanceManager.stopTracking();
            }
        });
        
        // 动态引导监听器
        guidanceManager.setGuidanceListener(new DynamicGuidanceManager.GuidanceListener() {
            @Override
            public void onGuidanceUpdate(String guidance, DynamicGuidanceManager.GuidanceState state) {
                announceInfo(guidance);
            }
            
            @Override
            public void onObjectFound(RectF position) {
                announceSuccess("找到了！物体在画面中央");
            }
            
            @Override
            public void onObjectLost() {
                announceInfo("物体已离开画面，请缓慢移动手机搜索");
            }
        });
    }
}
```

### 步骤 2：在检测结果中集成搜索功能

在 `analyzeImage` 方法中添加：

```java
private void analyzeImage(ImageProxy image) {
    // ... 现有检测代码 ...
    
    // 在检测结果中搜索目标物体
    if (objectSearchManager.isSearching()) {
        ObjectDetectorHelper.DetectionResult foundObject = 
            objectSearchManager.searchInDetections(results);
        
        if (foundObject != null) {
            // 找到目标物体
            float imageWidth = image.getWidth();
            float imageHeight = image.getHeight();
            String positionDesc = spatialDesc.describePosition(
                foundObject, imageWidth, imageHeight
            );
            
            // 更新动态引导
            guidanceManager.updatePosition(foundObject, imageWidth, imageHeight);
            
            // 播报位置
            runOnUiThread(() -> {
                announceInfo(positionDesc);
            });
        } else {
            // 没找到，更新引导
            guidanceManager.onObjectLost();
        }
    }
    
    // ... 现有代码 ...
}
```

### 步骤 3：添加语音命令支持

在 `VoiceCommandActivity` 或 `EnvironmentActivity` 中添加语音命令处理：

```java
// 处理语音命令："我的钥匙在哪？"
private void handleSearchCommand(String command) {
    // 提取物体名称
    String objectName = extractObjectName(command);
    
    if (objectName != null && !objectName.isEmpty()) {
        // 开始搜索
        objectSearchManager.startSearch(objectName);
        announceInfo("正在搜索" + objectName);
    }
}

// 提取物体名称
private String extractObjectName(String command) {
    // 移除问句词
    String cleaned = command
        .replaceAll("(我的|我的|where is|where's|在哪|在哪里|在哪里)", "")
        .replaceAll("(的|了|呢|吗|？|\\?)", "")
        .trim();
    
    return cleaned;
}
```

### 步骤 4：添加UI按钮（可选）

在布局文件中添加搜索按钮：

```xml
<Button
    android:id="@+id/searchObjectButton"
    android:layout_width="match_parent"
    android:layout_height="80dp"
    android:text="🔍 搜索物体"
    android:contentDescription="点击后说出要搜索的物体名称"
    android:onClick="onSearchObjectClick" />
```

在 Activity 中：

```java
public void onSearchObjectClick(View view) {
    vibrationManager.vibrateClick();
    
    // 启动语音识别
    VoiceCommandManager.getInstance(this).startListening(
        new VoiceCommandManager.VoiceCommandListener() {
            @Override
            public void onCommandRecognized(String command, String originalText) {
                handleSearchCommand(originalText);
            }
            
            // ... 其他回调方法 ...
        }
    );
}
```

## 🎤 使用示例

### 示例 1：搜索钥匙

1. **用户说**："我的钥匙在哪？"
2. **系统识别**：提取关键词"钥匙"
3. **开始搜索**：实时检测画面中的物体
4. **找到后播报**："钥匙在您右前方的茶几上，距离您大约1米"
5. **动态引导**："请将手机向右移动，再稍微抬高一点"

### 示例 2：搜索手机

1. **用户说**："找手机"
2. **系统识别**："手机"
3. **搜索并播报**："手机在您正前方的桌子上，距离您大约0.5米"

### 示例 3：物体不在画面中

1. **用户说**："我的钱包在哪？"
2. **系统搜索**：当前画面中没有钱包
3. **引导用户**："未找到钱包，请缓慢移动手机搜索"
4. **实时跟踪**：当钱包出现在画面中时，立即播报位置

## 🔍 支持的物体类型

当前支持以下物体（可扩展）：

- 钥匙 (key, 鑰匙, 钥匙)
- 手机 (cell phone, 手機, 手机)
- 钱包 (handbag, 錢包, 钱包)
- 眼镜 (eyeglasses, 眼鏡, 眼镜)
- 遥控器 (remote, 遙控器, 遥控器)
- 杯子 (cup, 杯子, 杯)
- 书本 (book, 書, 书)
- 笔 (pen, 筆, 笔)
- 笔记本电脑 (laptop, 筆記本, 笔记本)
- 鼠标 (mouse, 鼠標, 鼠标)
- 键盘 (keyboard, 鍵盤, 键盘)
- 瓶子 (bottle, 瓶子, 瓶)

## 🌐 多语言支持

所有功能都支持三种语言：
- **广东话**（默认）
- **英文**
- **普通话**

## 📝 下一步工作

1. **集成到 EnvironmentActivity**
   - 在现有检测流程中添加搜索功能
   - 添加语音命令处理

2. **扩展物体类型**
   - 在 `ObjectSearchManager` 中添加更多物体名称映射

3. **优化搜索算法**
   - 提高匹配准确率
   - 支持更多同义词

4. **添加UI界面**
   - 搜索按钮
   - 搜索状态显示
   - 搜索结果可视化

5. **性能优化**
   - 搜索频率控制
   - 内存管理
   - 电池优化

## 🐛 调试建议

1. **查看日志**：
   ```bash
   adb logcat | grep -E "ObjectSearchManager|SpatialDescriptionGenerator|DynamicGuidanceManager"
   ```

2. **测试搜索**：
   - 确保物体在画面中
   - 检查物体名称是否正确匹配
   - 验证位置描述是否准确

3. **测试引导**：
   - 将物体移出画面
   - 检查引导指令是否正确
   - 验证动态跟踪是否工作

## 📚 相关文档

- `ENVIRONMENT_RECOGNITION_ENHANCEMENT_PLAN.md` - 功能增强计划
- `ASR_Integration_Guide.md` - 语音识别集成指南

## 👤 作者

LUO Feiyang

## 📅 创建日期

2024年

