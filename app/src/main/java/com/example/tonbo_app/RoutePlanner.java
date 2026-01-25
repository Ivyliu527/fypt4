package com.example.tonbo_app;

import android.location.Location;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 路線規劃器
 * 支持 Google Maps Directions API 和高德地圖 API
 */
public class RoutePlanner {
    private static final String TAG = "RoutePlanner";
    
    // API 配置
    // 注意：請替換為您實際的 API Key
    // Google Maps API Key 獲取：https://console.cloud.google.com/google/maps-apis
    // 高德地圖 API Key 獲取：https://console.amap.com/dev/key/app
    private static final String GOOGLE_MAPS_API_KEY = "YOUR_GOOGLE_MAPS_API_KEY"; // 需要替換為實際的 API Key
    private static final String AMAP_API_KEY = "7b649dda456d9085654e3c5d181dcb71"; // 高德地圖 API Key
    
    // API 端點
    private static final String GOOGLE_DIRECTIONS_API_URL = "https://maps.googleapis.com/maps/api/directions/json";
    private static final String GOOGLE_GEOCODING_API_URL = "https://maps.googleapis.com/maps/api/geocode/json";
    private static final String AMAP_DIRECTIONS_API_URL = "https://restapi.amap.com/v3/direction/driving";
    private static final String AMAP_GEOCODING_API_URL = "https://restapi.amap.com/v3/geocode/geo";
    
    // 使用的 API 提供商（google 或 amap）
    private String apiProvider = "google"; // 默認使用 Google Maps
    
    private OkHttpClient httpClient;
    private Gson gson;
    private ExecutorService executorService;
    
    /**
     * 路線規劃結果
     */
    public static class RouteResult {
        public String distance;      // 距離（公里）
        public String duration;      // 時間（分鐘）
        public String routeSummary;  // 路線摘要
        public String fullRouteInfo; // 完整路線信息
        public boolean success;
        public String errorMessage;
    }
    
    /**
     * 路線規劃回調
     */
    public interface RoutePlanningCallback {
        void onRoutePlanned(RouteResult result);
        void onError(String error);
    }
    
    public RoutePlanner() {
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
        this.executorService = Executors.newSingleThreadExecutor();
    }
    
    /**
     * 設置 API 提供商
     * @param provider "google" 或 "amap"
     */
    public void setApiProvider(String provider) {
        this.apiProvider = provider;
    }
    
    /**
     * 規劃路線
     * @param origin 起點位置
     * @param destination 終點位置（地址字符串）
     * @param callback 回調接口
     */
    public void planRoute(Location origin, String destination, RoutePlanningCallback callback) {
        if (origin == null) {
            callback.onError("起點位置不可用");
            return;
        }
        
        if (destination == null || destination.trim().isEmpty()) {
            callback.onError("目的地不可用");
            return;
        }
        
        executorService.execute(() -> {
            if ("google".equals(apiProvider)) {
                planRouteWithGoogle(origin, destination, callback);
            } else if ("amap".equals(apiProvider)) {
                planRouteWithAmap(origin, destination, callback);
            } else {
                callback.onError("不支持的 API 提供商: " + apiProvider);
            }
        });
    }
    
    /**
     * 使用 Google Maps Directions API 規劃路線
     */
    private void planRouteWithGoogle(Location origin, String destination, RoutePlanningCallback callback) {
        try {
            // 構建請求 URL
            // Google Maps 支持直接使用地址字符串作為目的地
            String originStr = origin.getLatitude() + "," + origin.getLongitude();
            String url = String.format(
                "%s?origin=%s&destination=%s&key=%s&language=zh-CN&region=CN",
                GOOGLE_DIRECTIONS_API_URL,
                originStr,
                java.net.URLEncoder.encode(destination, "UTF-8"),
                GOOGLE_MAPS_API_KEY
            );
            
            Log.d(TAG, "Google Maps API 請求 URL: " + url);
            
            Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
            
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Google Maps API 請求失敗: " + e.getMessage(), e);
                    callback.onError("路線規劃失敗: " + e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        Log.e(TAG, "Google Maps API 響應失敗: " + response.code());
                        callback.onError("路線規劃失敗: HTTP " + response.code());
                        return;
                    }
                    
                    String responseBody = response.body().string();
                    Log.d(TAG, "Google Maps API 響應: " + responseBody);
                    
                    try {
                        RouteResult result = parseGoogleResponse(responseBody);
                        callback.onRoutePlanned(result);
                    } catch (Exception e) {
                        Log.e(TAG, "解析 Google Maps 響應失敗: " + e.getMessage(), e);
                        callback.onError("解析路線信息失敗: " + e.getMessage());
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Google Maps API 請求異常: " + e.getMessage(), e);
            callback.onError("路線規劃異常: " + e.getMessage());
        }
    }
    
    /**
     * 使用高德地圖 API 規劃路線
     */
    private void planRouteWithAmap(Location origin, String destination, RoutePlanningCallback callback) {
        try {
            // 先進行地理編碼，將目的地地址轉換為坐標
            geocodeDestinationWithAmap(destination, new GeocodingCallback() {
                @Override
                public void onGeocoded(double lat, double lng) {
                    // 地理編碼成功，使用坐標規劃路線
                    String originStr = origin.getLongitude() + "," + origin.getLatitude(); // 高德使用經度,緯度格式
                    String destStr = lng + "," + lat;
                    
                    String url = String.format(
                        "%s?origin=%s&destination=%s&key=%s&extensions=base",
                        AMAP_DIRECTIONS_API_URL,
                        originStr,
                        destStr,
                        AMAP_API_KEY
                    );
                    
                    Log.d(TAG, "高德地圖路線規劃 API 請求 URL: " + url);
                    
                    Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();
                    
                    httpClient.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            Log.e(TAG, "高德地圖 API 請求失敗: " + e.getMessage(), e);
                            callback.onError("路線規劃失敗: " + e.getMessage());
                        }
                        
                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            if (!response.isSuccessful()) {
                                Log.e(TAG, "高德地圖 API 響應失敗: " + response.code());
                                callback.onError("路線規劃失敗: HTTP " + response.code());
                                return;
                            }
                            
                            String responseBody = response.body().string();
                            Log.d(TAG, "高德地圖 API 響應: " + responseBody);
                            
                            try {
                                RouteResult result = parseAmapResponse(responseBody);
                                callback.onRoutePlanned(result);
                            } catch (Exception e) {
                                Log.e(TAG, "解析高德地圖響應失敗: " + e.getMessage(), e);
                                callback.onError("解析路線信息失敗: " + e.getMessage());
                            }
                        }
                    });
                }
                
                @Override
                public void onError(String error) {
                    callback.onError("地理編碼失敗: " + error);
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "高德地圖 API 請求異常: " + e.getMessage(), e);
            callback.onError("路線規劃異常: " + e.getMessage());
        }
    }
    
    /**
     * 地理編碼回調接口
     */
    private interface GeocodingCallback {
        void onGeocoded(double lat, double lng);
        void onError(String error);
    }
    
    /**
     * 使用高德地圖進行地理編碼（地址轉坐標）
     */
    private void geocodeDestinationWithAmap(String address, GeocodingCallback callback) {
        try {
            String url = String.format(
                "%s?address=%s&key=%s",
                AMAP_GEOCODING_API_URL,
                java.net.URLEncoder.encode(address, "UTF-8"),
                AMAP_API_KEY
            );
            
            Log.d(TAG, "高德地圖地理編碼 API 請求 URL: " + url);
            
            Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
            
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "高德地圖地理編碼請求失敗: " + e.getMessage(), e);
                    callback.onError(e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        Log.e(TAG, "高德地圖地理編碼響應失敗: " + response.code());
                        callback.onError("HTTP " + response.code());
                        return;
                    }
                    
                    String responseBody = response.body().string();
                    Log.d(TAG, "高德地圖地理編碼響應: " + responseBody);
                    
                    try {
                        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                        String status = json.get("status").getAsString();
                        
                        if (!"1".equals(status)) {
                            String info = json.has("info") ? json.get("info").getAsString() : "未知錯誤";
                            callback.onError(info);
                            return;
                        }
                        
                        JsonArray geocodes = json.getAsJsonArray("geocodes");
                        if (geocodes == null || geocodes.size() == 0) {
                            callback.onError("未找到地址對應的坐標");
                            return;
                        }
                        
                        JsonObject geocode = geocodes.get(0).getAsJsonObject();
                        String location = geocode.get("location").getAsString(); // 格式：經度,緯度
                        String[] coords = location.split(",");
                        
                        if (coords.length != 2) {
                            callback.onError("坐標格式錯誤");
                            return;
                        }
                        
                        double lng = Double.parseDouble(coords[0]);
                        double lat = Double.parseDouble(coords[1]);
                        
                        Log.d(TAG, "地理編碼成功: " + address + " -> " + lat + ", " + lng);
                        callback.onGeocoded(lat, lng);
                        
                    } catch (Exception e) {
                        Log.e(TAG, "解析地理編碼響應失敗: " + e.getMessage(), e);
                        callback.onError("解析響應失敗: " + e.getMessage());
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "高德地圖地理編碼請求異常: " + e.getMessage(), e);
            callback.onError(e.getMessage());
        }
    }
    
    /**
     * 解析 Google Maps API 響應
     */
    private RouteResult parseGoogleResponse(String jsonResponse) {
        RouteResult result = new RouteResult();
        
        try {
            JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();
            String status = json.get("status").getAsString();
            
            if (!"OK".equals(status)) {
                result.success = false;
                result.errorMessage = "路線規劃失敗: " + status;
                return result;
            }
            
            JsonArray routes = json.getAsJsonArray("routes");
            if (routes == null || routes.size() == 0) {
                result.success = false;
                result.errorMessage = "未找到路線";
                return result;
            }
            
            JsonObject route = routes.get(0).getAsJsonObject();
            JsonArray legs = route.getAsJsonArray("legs");
            
            if (legs == null || legs.size() == 0) {
                result.success = false;
                result.errorMessage = "路線信息不完整";
                return result;
            }
            
            JsonObject leg = legs.get(0).getAsJsonObject();
            
            // 解析距離
            JsonObject distance = leg.getAsJsonObject("distance");
            double distanceMeters = distance.get("value").getAsDouble();
            double distanceKm = distanceMeters / 1000.0;
            result.distance = String.format("%.1f", distanceKm);
            
            // 解析時間
            JsonObject duration = leg.getAsJsonObject("duration");
            int durationSeconds = duration.get("value").getAsInt();
            int durationMinutes = durationSeconds / 60;
            result.duration = String.valueOf(durationMinutes);
            
            // 路線摘要
            String summary = route.has("summary") ? route.get("summary").getAsString() : "";
            result.routeSummary = summary;
            
            // 完整路線信息
            result.fullRouteInfo = String.format(
                "距離：%s公里，預計時間：%s分鐘",
                result.distance,
                result.duration
            );
            
            result.success = true;
            
        } catch (Exception e) {
            Log.e(TAG, "解析 Google Maps 響應異常: " + e.getMessage(), e);
            result.success = false;
            result.errorMessage = "解析響應失敗: " + e.getMessage();
        }
        
        return result;
    }
    
    /**
     * 解析高德地圖 API 響應
     */
    private RouteResult parseAmapResponse(String jsonResponse) {
        RouteResult result = new RouteResult();
        
        try {
            JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();
            String status = json.get("status").getAsString();
            
            if (!"1".equals(status)) {
                result.success = false;
                String info = json.has("info") ? json.get("info").getAsString() : "未知錯誤";
                result.errorMessage = "路線規劃失敗: " + info;
                return result;
            }
            
            JsonObject route = json.getAsJsonObject("route");
            if (route == null) {
                result.success = false;
                result.errorMessage = "未找到路線";
                return result;
            }
            
            JsonArray paths = route.getAsJsonArray("paths");
            if (paths == null || paths.size() == 0) {
                result.success = false;
                result.errorMessage = "路線信息不完整";
                return result;
            }
            
            JsonObject path = paths.get(0).getAsJsonObject();
            
            // 解析距離（米）
            double distanceMeters = path.get("distance").getAsDouble();
            double distanceKm = distanceMeters / 1000.0;
            result.distance = String.format("%.1f", distanceKm);
            
            // 解析時間（秒）
            double durationSeconds = path.get("duration").getAsDouble();
            int durationMinutes = (int)(durationSeconds / 60.0);
            result.duration = String.valueOf(durationMinutes);
            
            // 路線摘要
            result.routeSummary = path.has("strategy") ? path.get("strategy").getAsString() : "";
            
            // 完整路線信息
            result.fullRouteInfo = String.format(
                "距離：%s公里，預計時間：%s分鐘",
                result.distance,
                result.duration
            );
            
            result.success = true;
            
        } catch (Exception e) {
            Log.e(TAG, "解析高德地圖響應異常: " + e.getMessage(), e);
            result.success = false;
            result.errorMessage = "解析響應失敗: " + e.getMessage();
        }
        
        return result;
    }
    
    /**
     * 清理資源
     */
    public void cleanup() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
