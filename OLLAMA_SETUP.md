# Ollama AI API 集成指南

## 📋 概述

本應用已集成 Ollama AI API，可以為語音助手提供更智能的對話能力。Ollama 是一個免費開源的本地 AI 模型運行平台。

## 🚀 快速開始

### 1. 安裝 Ollama

#### Windows:
```bash
# 下載並安裝 Ollama
# 訪問: https://ollama.com/download
# 或使用 winget:
winget install Ollama.Ollama
```

#### macOS:
```bash
# 使用 Homebrew
brew install ollama
```

#### Linux:
```bash
# 使用官方安裝腳本
curl -fsSL https://ollama.com/install.sh | sh
```

### 2. 啟動 Ollama 服務

```bash
# 啟動 Ollama 服務（默認端口 11434）
ollama serve
```

### 3. 下載支持中文的模型

推薦使用以下模型（支持中文）：

```bash
# Qwen 2.5 7B（推薦，中文支持好，體積適中）
ollama pull qwen2.5:7b

# 或使用更小的模型（如果設備性能較低）
ollama pull qwen2.5:1.5b

# 或使用更大的模型（如果設備性能強）
ollama pull qwen2.5:14b
```

其他推薦的中文模型：
- `qwen2.5:7b` - 平衡性能和質量（推薦）
- `qwen2.5:14b` - 更高質量，需要更多資源
- `qwen2.5:1.5b` - 輕量級，適合低端設備
- `chatglm3:6b` - 另一個優秀的中文模型

### 4. 配置應用

#### 方法 1: 使用默認配置（本地）
- 默認 API URL: `http://localhost:11434`
- 默認模型: `qwen2.5:7b`

如果 Ollama 運行在本地且使用默認端口，應用會自動連接。

#### 方法 2: 自定義配置（遠程服務器）
如果需要連接到遠程 Ollama 服務器：

1. 在應用中進入「系統設定」
2. 找到「Ollama 配置」選項
3. 設置：
   - API URL: `http://你的服務器IP:11434`
   - 模型名稱: `qwen2.5:7b`

## 📱 在 Android 設備上使用

### 本地部署（同一網絡）

如果 Ollama 運行在電腦上，Android 設備需要：

1. **確保設備和電腦在同一 Wi-Fi 網絡**
2. **獲取電腦的 IP 地址**：
   - Windows: `ipconfig` 查看 IPv4 地址
   - macOS/Linux: `ifconfig` 或 `ip addr`
3. **在應用中設置 API URL**：
   - 例如: `http://192.168.1.100:11434`

### 遠程服務器部署

如果 Ollama 運行在遠程服務器上：

1. **確保服務器防火牆開放 11434 端口**
2. **在應用中設置 API URL**：
   - 例如: `http://your-server.com:11434`

## 🔧 配置選項

### API URL 格式
- 本地: `http://localhost:11434`
- 同網絡: `http://192.168.1.100:11434`
- 遠程: `http://your-server.com:11434`

### 模型選擇建議

| 模型 | 大小 | 適用場景 | 推薦設備 |
|------|------|----------|----------|
| qwen2.5:1.5b | ~1GB | 快速響應，低資源 | 低端設備 |
| qwen2.5:7b | ~4.5GB | 平衡性能和質量 | 中高端設備（推薦）|
| qwen2.5:14b | ~8GB | 高質量回應 | 高端設備/服務器 |

## 🧪 測試連接

應用會在首次使用時自動測試連接。如果連接失敗，會自動回退到關鍵詞匹配模式。

## 💡 使用提示

1. **首次使用**: 應用會自動嘗試連接 Ollama，如果失敗會使用關鍵詞匹配
2. **網絡要求**: 需要確保設備可以訪問 Ollama 服務器
3. **性能優化**: 
   - 使用較小的模型可以獲得更快的響應
   - 本地部署可以減少延遲
4. **隱私保護**: Ollama 在本地運行，所有對話數據不會上傳到雲端

## 🔍 故障排除

### 問題 1: 無法連接到 Ollama
- 檢查 Ollama 服務是否運行: `ollama list`
- 檢查防火牆設置
- 確認 API URL 正確
- 檢查網絡連接

### 問題 2: 響應速度慢
- 嘗試使用更小的模型（如 `qwen2.5:1.5b`）
- 確保設備和服務器在同一網絡
- 檢查服務器性能

### 問題 3: 回應質量不佳
- 嘗試使用更大的模型（如 `qwen2.5:14b`）
- 檢查模型是否正確下載: `ollama list`

## 📚 更多資源

- Ollama 官方文檔: https://ollama.com/docs
- Ollama GitHub: https://github.com/ollama/ollama
- 模型列表: https://ollama.com/library

## 🎯 功能說明

### 自動回退機制
- 如果 Ollama API 不可用，應用會自動使用關鍵詞匹配模式
- 確保即使沒有 Ollama，語音助手也能正常工作

### 多語言支持
- 支持廣東話、普通話、英文
- 系統會根據當前語言自動調整提示詞

### 上下文理解
- Ollama API 可以理解對話上下文
- 提供更自然、更智能的回應

