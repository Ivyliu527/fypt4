# Google Maps API 設置指南

## 🗺️ 如何獲取Google Maps API Key

### 1. 前往Google Cloud Console
1. 打開 [Google Cloud Console](https://console.cloud.google.com/)
2. 登入你的Google帳號
3. 創建新專案或選擇現有專案

### 2. 啟用必要的API
在Google Cloud Console中啟用以下API：
- **Maps SDK for Android**
- **Geocoding API**
- **Directions API**
- **Places API** (可選)

### 3. 創建API Key
1. 在左側選單中選擇「憑證」(Credentials)
2. 點擊「+ 創建憑證」→「API 金鑰」
3. 複製生成的API Key

### 4. 配置API Key限制（建議）
為了安全起見，建議設置API Key限制：
- **應用程式限制**：選擇「Android 應用程式」
- **API 限制**：只啟用必要的API

### 5. 在應用程式中配置API Key

#### 方法1：在AndroidManifest.xml中配置
```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_ACTUAL_API_KEY_HERE" />
```

#### 方法2：在NavigationManager.java中配置
```java
private void initGeoApiContext() {
    geoApiContext = new GeoApiContext.Builder()
            .apiKey("YOUR_ACTUAL_API_KEY_HERE") // 替換為你的實際API Key
            .build();
}
```

## 🚀 當前狀態

目前應用程式使用**模擬數據**運行，包含以下功能：

### 模擬目的地
- 中環 (Central)
- 銅鑼灣 (Causeway Bay)
- 尖沙咀 (Tsim Sha Tsui)
- 旺角 (Mong Kok)
- 香港國際機場 (Airport)
- 維多利亞港 (Victoria Harbour)

### 模擬導航功能
- 基本的步行路線規劃
- 語音導航指引
- 距離和方向計算

## 📝 測試步驟

1. **安裝應用程式**
2. **授予位置權限**
3. **輸入測試目的地**：
   - 輸入「中環」或「Central」
   - 輸入「銅鑼灣」或「Causeway Bay」
   - 輸入「機場」或「Airport」
4. **點擊搜索**查看模擬結果
5. **開始導航**體驗語音指引

## ⚠️ 注意事項

- 目前使用模擬數據，不會產生實際的地圖顯示
- 語音導航功能正常工作
- 位置權限和GPS功能正常
- 設置真實API Key後將獲得完整功能

## 🔧 故障排除

### 如果遇到「Invalid API key」錯誤：
1. 檢查API Key是否正確
2. 確認已啟用必要的API
3. 檢查API Key限制設置
4. 確認專案計費帳戶已設置

### 如果模擬數據不工作：
1. 檢查位置權限是否已授予
2. 確認GPS已開啟
3. 檢查網絡連接

## 📞 技術支持

如有問題，請檢查：
1. Google Cloud Console中的API配額
2. Android Studio中的Logcat輸出
3. 設備的網絡連接狀態
