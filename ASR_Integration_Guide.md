# 🎙️ 第三方ASR集成指南

## 📋 **概述**

本指南將幫助你將先進的第三方語音識別（ASR）技術集成到Tonbo App中，大幅提升語音識別準確性和穩定性。

## 🎯 **推薦集成順序**

### 1️⃣ **sherpa-onnx** (首選 - 離線方案)
```bash
# 優勢
✅ 完全離線 - 無需網絡連接
✅ 高性能 - ONNX優化，速度快
✅ 多語言支持 - 優秀的中文支持
✅ Android友好 - 原生Android支持
✅ 體積小 - 模型文件相對較小
✅ 開源免費 - 無API限制
```

### 2️⃣ **Whisper.cpp** (備選 - 高精度方案)
```bash
# 優勢
✅ 高準確性 - OpenAI Whisper的高精度
✅ 多語言支持 - 支持100+語言
✅ 離線運行 - 本地處理
✅ 開源 - 免費使用
⚠️ 體積較大 - 模型文件較大
⚠️ 計算密集 - 需要較強處理能力
```

### 3️⃣ **Azure ASR** (雲端方案)
```bash
# 優勢
✅ 極高準確性 - 微軟頂級ASR技術
✅ 中文優化 - 對中文支持優秀
✅ 實時處理 - 低延遲
✅ 易於集成 - 豐富的SDK
⚠️ 需要網絡 - 雲端服務
⚠️ 有成本 - 按使用量收費
```

## 🛠️ **sherpa-onnx 集成步驟**

### Step 1: 添加依賴
```gradle
// app/build.gradle.kts
dependencies {
    implementation 'com.k2fsa:sherpa-onnx:1.9.7'
    
    // 中文模型依賴
    implementation 'com.k2fsa:sherpa-onnx-model-zh:1.0.0'
}
```

### Step 2: 下載模型文件
```bash
# 創建assets目錄結構
app/src/main/assets/
├── models/
│   ├── sherpa-onnx-streaming-zipformer-bilingual-zh-en-2023-02-20/
│   │   ├── model.onnx
│   │   ├── tokens.txt
│   │   └── config.yaml
│   └── sherpa-onnx-streaming-zipformer-zh-14M-2023-02-23/
│       ├── model.onnx
│       ├── tokens.txt
│       └── config.yaml
```

### Step 3: 實現sherpa-onnx ASR
```java
// SherpaOnnxASR.java
public class SherpaOnnxASR {
    private SherpaOnnxStreamingRecognizer recognizer;
    private boolean isListening = false;
    
    public void initialize() {
        // 配置模型路徑
        String modelDir = "models/sherpa-onnx-streaming-zipformer-zh-14M-2023-02-23/";
        
        // 創建配置
        SherpaOnnxOnlineRecognizerConfig config = new SherpaOnnxOnlineRecognizerConfig();
        config.setModelConfig(modelDir + "model.onnx");
        config.setTokensConfig(modelDir + "tokens.txt");
        
        // 初始化識別器
        recognizer = new SherpaOnnxStreamingRecognizer(config);
    }
    
    public void startRecognition(ASRCallback callback) {
        isListening = true;
        // 實現語音識別邏輯
    }
}
```

## 🛠️ **Whisper.cpp 集成步驟**

### Step 1: 添加依賴
```gradle
// app/build.gradle.kts
android {
    defaultConfig {
        ndk {
            abiFilters 'arm64-v8a', 'armeabi-v7a'
        }
    }
}

dependencies {
    implementation 'com.whispercpp:whispercpp:1.0.0'
}
```

### Step 2: 下載模型文件
```bash
# 下載Whisper模型（選擇合適的大小）
app/src/main/assets/models/
├── whisper-tiny.bin     # 39 MB - 最快
├── whisper-base.bin     # 74 MB - 平衡
├── whisper-small.bin    # 244 MB - 較準確
└── whisper-medium.bin   # 769 MB - 最準確
```

### Step 3: 實現Whisper.cpp ASR
```java
// WhisperCppASR.java
public class WhisperCppASR {
    private WhisperContext whisperContext;
    
    public void initialize() {
        // 加載模型
        String modelPath = "models/whisper-base.bin";
        whisperContext = WhisperContext.createFromAsset(context, modelPath);
    }
    
    public void recognizeAudio(byte[] audioData, ASRCallback callback) {
        // 實現音頻識別
        String result = whisperContext.transcribe(audioData);
        callback.onResult(result, 0.95f);
    }
}
```

## 🛠️ **Azure ASR 集成步驟**

### Step 1: 添加依賴
```gradle
// app/build.gradle.kts
dependencies {
    implementation 'com.microsoft.cognitiveservices.speech:client-sdk:1.34.0'
}
```

### Step 2: 配置API密鑰
```java
// AzureASR.java
public class AzureASR {
    private SpeechConfig speechConfig;
    
    public void initialize() {
        // 配置Azure Speech服務
        speechConfig = SpeechConfig.fromSubscription(
            "YOUR_AZURE_KEY", 
            "YOUR_AZURE_REGION"
        );
        
        // 設置語言
        speechConfig.setSpeechRecognitionLanguage("zh-CN");
    }
}
```

## 🎛️ **ASR引擎選擇策略**

### 智能引擎選擇
```java
public ASREngine selectBestEngine() {
    // 檢查網絡連接
    if (isNetworkAvailable()) {
        // 網絡可用，優先選擇雲端服務
        if (isAzureASRConfigured()) {
            return ASREngine.AZURE_ASR;
        }
    }
    
    // 離線情況，選擇本地引擎
    if (isSherpaOnnxAvailable()) {
        return ASREngine.SHERPA_ONNX;
    }
    
    if (isWhisperCppAvailable()) {
        return ASREngine.WHISPER_CPP;
    }
    
    // 最後選擇Android原生
    return ASREngine.ANDROID_NATIVE;
}
```

### 性能優化策略
```java
public class ASROptimizer {
    // 根據設備性能選擇引擎
    public ASREngine selectEngineByDevicePerformance() {
        if (isHighEndDevice()) {
            return ASREngine.WHISPER_CPP; // 高精度
        } else if (isMidRangeDevice()) {
            return ASREngine.SHERPA_ONNX; // 平衡性能
        } else {
            return ASREngine.ANDROID_NATIVE; // 兼容性
        }
    }
}
```

## 📱 **集成到現有代碼**

### 修改GlobalVoiceCommandManager
```java
public class GlobalVoiceCommandManager {
    private ASRManager asrManager;
    
    public GlobalVoiceCommandManager(Context context, TTSManager ttsManager) {
        this.context = context;
        this.ttsManager = ttsManager;
        
        // 初始化ASR管理器
        asrManager = new ASRManager(context);
        
        // 自動選擇最佳引擎
        ASREngine bestEngine = selectBestASREngine();
        asrManager.setASREngine(bestEngine);
    }
    
    public void startListening(VoiceCommandCallback callback) {
        asrManager.startRecognition(new ASRManager.ASRCallback() {
            @Override
            public void onResult(String text, float confidence) {
                callback.onCommandRecognized(text);
            }
            
            @Override
            public void onError(String error) {
                callback.onVoiceError(error);
            }
            
            @Override
            public void onPartialResult(String partialText) {
                // 處理部分結果
            }
        });
    }
}
```

## 🎯 **針對繁體中文的優化**

### 語言模型配置
```java
// sherpa-onnx中文優化
SherpaOnnxOnlineRecognizerConfig config = new SherpaOnnxOnlineRecognizerConfig();
config.setModelConfig("models/zh-model/model.onnx");
config.setTokensConfig("models/zh-model/tokens.txt");

// 設置中文相關參數
config.setDecodingMethod("greedy_search");
config.setMaxActivePaths(4);
config.setHotwordsFile("models/zh-model/hotwords.txt");
```

### Whisper中文優化
```java
// Whisper中文模型
WhisperParams params = new WhisperParams();
params.setLanguage("zh");
params.setTranslate(false);
params.setNoSpeech(false);
params.setMaxLength(224);
```

## 📊 **性能對比**

| ASR引擎 | 準確率 | 速度 | 離線 | 體積 | 中文支持 |
|---------|--------|------|------|------|----------|
| Android Native | 70% | 快 | ✅ | 小 | 一般 |
| sherpa-onnx | 85% | 快 | ✅ | 中 | 優秀 |
| Whisper.cpp | 95% | 中 | ✅ | 大 | 優秀 |
| Azure ASR | 98% | 快 | ❌ | 無 | 優秀 |

## 🚀 **實施建議**

### 階段1: 集成sherpa-onnx
1. 添加sherpa-onnx依賴
2. 下載中文模型文件
3. 實現基本識別功能
4. 測試繁體中文識別效果

### 階段2: 添加Whisper.cpp
1. 集成Whisper.cpp庫
2. 下載合適的模型大小
3. 實現高精度識別
4. 優化性能

### 階段3: 可選雲端服務
1. 配置Azure ASR
2. 實現網絡檢測
3. 添加智能引擎切換
4. 優化用戶體驗

## 💡 **最佳實踐**

1. **模型選擇**: 根據設備性能選擇合適的模型大小
2. **離線優先**: 優先使用離線方案保證可用性
3. **智能切換**: 根據網絡狀況自動切換引擎
4. **用戶選擇**: 提供用戶手動選擇ASR引擎的選項
5. **性能監控**: 監控各引擎的識別準確率和速度

這個集成方案將大幅提升你的語音識別準確性，特別是在繁體中文識別方面！🎉
