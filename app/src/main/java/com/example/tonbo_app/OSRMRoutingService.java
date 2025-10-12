package com.example.tonbo_app;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * OSRM (Open Source Routing Machine) 路線規劃服務
 * 使用免費的OpenStreetMap路線規劃API
 */
public class OSRMRoutingService {
    private static final String TAG = "OSRMRoutingService";
    private static final String OSRM_API_BASE = "https://router.project-osrm.org/route/v1/foot/";
    
    private final OkHttpClient client;
    private final Gson gson;
    
    public OSRMRoutingService() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        gson = new Gson();
    }
    
    public interface RoutingCallback {
        void onSuccess(RouteResponse routeResponse);
        void onError(String error);
    }
    
    /**
     * 獲取路線
     * @param startLat 起點緯度
     * @param startLon 起點經度
     * @param endLat 終點緯度
     * @param endLon 終點經度
     * @param callback 回調接口
     */
    public void getRoute(double startLat, double startLon, double endLat, double endLon, 
                        RoutingCallback callback) {
        // 構建OSRM API URL
        // 格式: {service}/{version}/{profile}/{coordinates}?{options}
        String url = String.format("%s%f,%f;%f,%f?overview=full&geometries=geojson&steps=true",
                OSRM_API_BASE, startLon, startLat, endLon, endLat);
        
        Log.d(TAG, "請求路線: " + url);
        
        Request request = new Request.Builder()
                .url(url)
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "路線請求失敗", e);
                runOnMainThread(() -> callback.onError("網絡請求失敗：" + e.getMessage()));
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnMainThread(() -> callback.onError("服務器錯誤：" + response.code()));
                    return;
                }
                
                String responseBody = response.body().string();
                Log.d(TAG, "路線響應: " + responseBody.substring(0, Math.min(200, responseBody.length())));
                
                try {
                    RouteResponse routeResponse = gson.fromJson(responseBody, RouteResponse.class);
                    
                    if (routeResponse != null && routeResponse.code.equals("Ok") 
                            && routeResponse.routes != null && routeResponse.routes.size() > 0) {
                        runOnMainThread(() -> callback.onSuccess(routeResponse));
                    } else {
                        runOnMainThread(() -> callback.onError("未找到路線"));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析路線響應失敗", e);
                    runOnMainThread(() -> callback.onError("解析路線數據失敗"));
                }
            }
        });
    }
    
    /**
     * 地址搜索（使用Nominatim API）
     * 支持多種搜索策略以提高成功率
     */
    public void searchLocation(String query, SearchCallback callback) {
        // 優化查詢字符串
        String optimizedQuery = optimizeSearchQuery(query);
        
        // 策略1：精確搜索（限定香港）
        searchWithStrategy(optimizedQuery, "hk", callback, new Runnable() {
            @Override
            public void run() {
                // 策略2：如果策略1失敗，擴大到整個香港地區搜索
                searchWithStrategy(optimizedQuery + ", Hong Kong", null, callback, new Runnable() {
                    @Override
                    public void run() {
                        // 策略3：如果還是失敗，只用關鍵詞搜索
                        String simplifiedQuery = simplifyQuery(query);
                        searchWithStrategy(simplifiedQuery + ", HK", null, callback, new Runnable() {
                            @Override
                            public void run() {
                                // 所有策略都失敗
                                runOnMainThread(() -> callback.onError("未找到地點：" + query));
                            }
                        });
                    }
                });
            }
        });
    }
    
    /**
     * 優化搜索查詢
     */
    private String optimizeSearchQuery(String query) {
        // 移除不必要的字符
        String optimized = query.trim()
                .replace("香港", "")
                .replace("Hong Kong", "")
                .replace("HK", "")
                .trim();
        
        // 如果是純中文，保持原樣
        // 如果包含英文，轉換為標準格式
        return optimized;
    }
    
    /**
     * 簡化查詢（提取關鍵詞）
     */
    private String simplifyQuery(String query) {
        // 移除常見的後綴詞
        return query.replace("站", "")
                   .replace("區", "")
                   .replace("道", "")
                   .replace("街", "")
                   .replace("路", "")
                   .trim();
    }
    
    /**
     * 使用特定策略搜索
     */
    private void searchWithStrategy(String query, String countryCode, SearchCallback callback, Runnable fallback) {
        // 構建URL
        String url;
        if (countryCode != null) {
            url = String.format("https://nominatim.openstreetmap.org/search?q=%s&format=json&limit=5&countrycodes=%s&addressdetails=1",
                    query.replace(" ", "+"), countryCode);
        } else {
            url = String.format("https://nominatim.openstreetmap.org/search?q=%s&format=json&limit=5&addressdetails=1",
                    query.replace(" ", "+"));
        }
        
        Log.d(TAG, "搜索地址: " + url);
        
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "TonboApp/1.0")
                .addHeader("Accept-Language", "zh-HK,zh-CN,en")
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "地址搜索失敗", e);
                if (fallback != null) {
                    fallback.run();
                } else {
                    runOnMainThread(() -> callback.onError("網絡請求失敗：" + e.getMessage()));
                }
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    if (fallback != null) {
                        fallback.run();
                    } else {
                        runOnMainThread(() -> callback.onError("服務器錯誤：" + response.code()));
                    }
                    return;
                }
                
                String responseBody = response.body().string();
                Log.d(TAG, "搜索響應: " + responseBody.substring(0, Math.min(200, responseBody.length())));
                
                try {
                    SearchResult[] results = gson.fromJson(responseBody, SearchResult[].class);
                    
                    if (results != null && results.length > 0) {
                        // 過濾結果，優先選擇香港的地點
                        SearchResult[] filteredResults = filterHongKongResults(results);
                        if (filteredResults.length > 0) {
                            runOnMainThread(() -> callback.onSuccess(filteredResults));
                        } else if (results.length > 0) {
                            // 如果沒有香港地點，但有其他結果，也返回
                            runOnMainThread(() -> callback.onSuccess(results));
                        } else if (fallback != null) {
                            fallback.run();
                        } else {
                            runOnMainThread(() -> callback.onError("未找到地點"));
                        }
                    } else if (fallback != null) {
                        // 嘗試下一個策略
                        fallback.run();
                    } else {
                        runOnMainThread(() -> callback.onError("未找到地點"));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析搜索響應失敗", e);
                    if (fallback != null) {
                        fallback.run();
                    } else {
                        runOnMainThread(() -> callback.onError("解析搜索數據失敗"));
                    }
                }
            }
        });
    }
    
    /**
     * 過濾出香港的搜索結果
     */
    private SearchResult[] filterHongKongResults(SearchResult[] results) {
        List<SearchResult> filtered = new ArrayList<>();
        for (SearchResult result : results) {
            if (result.displayName != null && 
                (result.displayName.contains("Hong Kong") || 
                 result.displayName.contains("香港") ||
                 result.displayName.contains("Kowloon") ||
                 result.displayName.contains("九龍") ||
                 result.displayName.contains("New Territories") ||
                 result.displayName.contains("新界"))) {
                filtered.add(result);
            }
        }
        return filtered.toArray(new SearchResult[0]);
    }
    
    public interface SearchCallback {
        void onSuccess(SearchResult[] results);
        void onError(String error);
    }
    
    private void runOnMainThread(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }
    
    // OSRM API 響應數據模型
    public static class RouteResponse {
        @SerializedName("code")
        public String code;
        
        @SerializedName("routes")
        public List<Route> routes;
        
        @SerializedName("waypoints")
        public List<Waypoint> waypoints;
    }
    
    public static class Route {
        @SerializedName("distance")
        public double distance; // 米
        
        @SerializedName("duration")
        public double duration; // 秒
        
        @SerializedName("geometry")
        public Geometry geometry;
        
        @SerializedName("legs")
        public List<Leg> legs;
    }
    
    public static class Geometry {
        @SerializedName("coordinates")
        public List<List<Double>> coordinates; // [lon, lat]
        
        @SerializedName("type")
        public String type;
    }
    
    public static class Leg {
        @SerializedName("steps")
        public List<Step> steps;
        
        @SerializedName("distance")
        public double distance;
        
        @SerializedName("duration")
        public double duration;
    }
    
    public static class Step {
        @SerializedName("geometry")
        public Geometry geometry;
        
        @SerializedName("maneuver")
        public Maneuver maneuver;
        
        @SerializedName("mode")
        public String mode;
        
        @SerializedName("name")
        public String name;
        
        @SerializedName("distance")
        public double distance;
        
        @SerializedName("duration")
        public double duration;
    }
    
    public static class Maneuver {
        @SerializedName("location")
        public List<Double> location; // [lon, lat]
        
        @SerializedName("type")
        public String type; // turn, new name, depart, arrive, etc.
        
        @SerializedName("modifier")
        public String modifier; // left, right, straight, etc.
        
        @SerializedName("instruction")
        public String instruction;
    }
    
    public static class Waypoint {
        @SerializedName("location")
        public List<Double> location; // [lon, lat]
        
        @SerializedName("name")
        public String name;
    }
    
    // Nominatim 搜索結果數據模型
    public static class SearchResult {
        @SerializedName("place_id")
        public long placeId;
        
        @SerializedName("lat")
        public String lat;
        
        @SerializedName("lon")
        public String lon;
        
        @SerializedName("display_name")
        public String displayName;
        
        @SerializedName("type")
        public String type;
        
        @SerializedName("importance")
        public double importance;
    }
}
