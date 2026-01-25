# 路線規劃 API 配置說明

## 概述

本應用支持兩種路線規劃 API：
1. **Google Maps Directions API**（默認）
2. **高德地圖 API**（中國大陸地區推薦）

## 配置步驟

### 1. Google Maps Directions API

#### 獲取 API Key
1. 訪問 [Google Cloud Console](https://console.cloud.google.com/google/maps-apis)
2. 創建或選擇項目
3. 啟用 "Directions API"
4. 創建憑證（API Key）
5. 設置 API Key 限制（可選，建議限制為 Android 應用）

#### 配置 API Key
在 `RoutePlanner.java` 文件中，找到以下行：
```java
private static final String GOOGLE_MAPS_API_KEY = "YOUR_GOOGLE_MAPS_API_KEY";
```
將 `YOUR_GOOGLE_MAPS_API_KEY` 替換為您獲取的實際 API Key。

### 2. 高德地圖 API

#### 獲取 API Key
1. 訪問 [高德開放平台](https://console.amap.com/dev/key/app)
2. 註冊/登錄賬號
3. 創建應用
4. 獲取 Key（API Key）
5. 添加服務：路線規劃、地理編碼

#### 配置 API Key
在 `RoutePlanner.java` 文件中，找到以下行：
```java
private static final String AMAP_API_KEY = "YOUR_AMAP_API_KEY";
```
將 `YOUR_AMAP_API_KEY` 替換為您獲取的實際 API Key。

#### 切換 API 提供商
在 `NavigationController` 初始化後，可以設置使用的 API：
```java
navigationController.setRouteApiProvider("amap"); // 使用高德地圖
// 或
navigationController.setRouteApiProvider("google"); // 使用 Google Maps（默認）
```

## API 費用說明

### Google Maps Directions API
- 按請求次數收費
- 有免費額度（每月 $200 免費額度）
- 詳細價格：https://developers.google.com/maps/billing-and-pricing/pricing

### 高德地圖 API
- 個人開發者有一定免費額度
- 企業用戶需要付費
- 詳細價格：https://lbs.amap.com/api/webservice/price

## 注意事項

1. **API Key 安全**：
   - 不要將 API Key 提交到公開的 Git 倉庫
   - 建議使用環境變量或配置文件（不提交到版本控制）
   - 設置 API Key 限制（IP、應用包名等）

2. **網絡權限**：
   - 確保應用有 `INTERNET` 權限（已在 AndroidManifest.xml 中聲明）

3. **錯誤處理**：
   - API 調用失敗時會自動回調錯誤信息
   - 建議在生產環境中添加重試機制

4. **位置權限**：
   - 路線規劃需要獲取當前位置
   - 確保已請求 `ACCESS_FINE_LOCATION` 或 `ACCESS_COARSE_LOCATION` 權限

## 測試

配置完成後，可以通過以下方式測試：
1. 運行應用
2. 進入「出行協助」頁面
3. 點擊「開始出行（語音）」
4. 說出目的地（如「我要去中環」）
5. 查看 Log 輸出，確認 API 請求是否成功

## 故障排查

### API 請求失敗
- 檢查 API Key 是否正確
- 檢查網絡連接
- 查看 Log 中的錯誤信息
- 確認 API 服務是否已啟用

### 地理編碼失敗（高德地圖）
- 確認地址格式正確
- 檢查是否啟用了地理編碼服務
- 查看 API 響應中的錯誤信息

### 路線規劃失敗
- 確認起點和終點是否有效
- 檢查 API 配額是否用完
- 查看 API 響應狀態碼

