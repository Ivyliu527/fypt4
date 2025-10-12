package com.example.tonbo_app;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.GeocodingResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NavigationManager {
    private static final String TAG = "NavigationManager";
    private static NavigationManager instance;
    private Context context;
    private GeoApiContext geoApiContext;
    private boolean isNavigating = false;
    private List<NavigationStep> navigationSteps;
    private int currentStepIndex = 0;
    private OnNavigationUpdateListener navigationListener;
    
    // 導航步驟類
    public static class NavigationStep {
        public String instruction;
        public double distance;
        public String direction;
        public LatLng startLocation;
        public LatLng endLocation;
        
        public NavigationStep(String instruction, double distance, String direction, 
                            LatLng startLocation, LatLng endLocation) {
            this.instruction = instruction;
            this.distance = distance;
            this.direction = direction;
            this.startLocation = startLocation;
            this.endLocation = endLocation;
        }
    }
    
    // 目的地搜索回調接口
    public interface OnDestinationFoundListener {
        void onDestinationFound(LatLng destination, String address);
        void onDestinationNotFound(String error);
    }
    
    // 導航更新回調接口
    public interface OnNavigationUpdateListener {
        void onNavigationUpdate(String instruction, double distance, String direction);
        void onNavigationComplete();
        void onNavigationError(String error);
    }
    
    private NavigationManager(Context context) {
        this.context = context.getApplicationContext();
        initGeoApiContext();
    }
    
    public static synchronized NavigationManager getInstance(Context context) {
        if (instance == null) {
            instance = new NavigationManager(context);
        }
        return instance;
    }
    
    private void initGeoApiContext() {
        // 初始化Google Maps API上下文
        // 注意：需要在Google Cloud Console獲取API Key
        // 暫時使用模擬數據，避免API Key錯誤
        try {
            geoApiContext = new GeoApiContext.Builder()
                    .apiKey("DEMO_API_KEY") // 使用模擬API Key
                    .build();
        } catch (Exception e) {
            Log.w(TAG, "Google Maps API未配置，將使用模擬數據");
            geoApiContext = null;
        }
    }
    
    public void searchDestination(String query, OnDestinationFoundListener listener) {
        new Thread(() -> {
            try {
                // 檢查API是否可用
                if (geoApiContext == null) {
                    Log.d(TAG, "使用模擬數據搜索目的地: " + query);
                    // 使用模擬數據
                    simulateDestinationSearch(query, listener);
                    return;
                }
                
                GeocodingResult[] results = GeocodingApi.geocode(geoApiContext, query).await();
                
                if (results != null && results.length > 0) {
                    GeocodingResult result = results[0];
                    LatLng destination = new LatLng(
                            result.geometry.location.lat,
                            result.geometry.location.lng
                    );
                    String address = result.formattedAddress;
                    
                    // 在主線程中調用回調
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        listener.onDestinationFound(destination, address);
                    });
                } else {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        listener.onDestinationNotFound("找不到指定地點");
                    });
                }
            } catch (ApiException | InterruptedException | IOException e) {
                Log.e(TAG, "搜索目的地失敗，使用模擬數據", e);
                // 如果API失敗，使用模擬數據
                simulateDestinationSearch(query, listener);
            }
        }).start();
    }
    
    private void simulateDestinationSearch(String query, OnDestinationFoundListener listener) {
        // 模擬一些常見目的地
        LatLng destination;
        String address;
        
        if (query.contains("中環") || query.contains("Central")) {
            destination = new LatLng(22.2813, 114.1586);
            address = "香港中環";
        } else if (query.contains("銅鑼灣") || query.contains("Causeway Bay")) {
            destination = new LatLng(22.2791, 114.1838);
            address = "香港銅鑼灣";
        } else if (query.contains("尖沙咀") || query.contains("Tsim Sha Tsui")) {
            destination = new LatLng(22.2967, 114.1724);
            address = "香港尖沙咀";
        } else if (query.contains("旺角") || query.contains("Mong Kok")) {
            destination = new LatLng(22.3193, 114.1694);
            address = "香港旺角";
        } else if (query.contains("機場") || query.contains("Airport")) {
            destination = new LatLng(22.3080, 113.9185);
            address = "香港國際機場";
        } else {
            // 默認位置（維多利亞港）
            destination = new LatLng(22.2915, 114.1778);
            address = "香港維多利亞港（模擬位置）";
        }
        
        // 在主線程中調用回調
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            listener.onDestinationFound(destination, address);
        });
    }
    
    public void startNavigation(Location startLocation, LatLng destination, 
                              OnNavigationUpdateListener listener) {
        this.navigationListener = listener;
        this.isNavigating = true;
        this.currentStepIndex = 0;
        
        new Thread(() -> {
            try {
                // 檢查API是否可用
                if (geoApiContext == null) {
                    Log.d(TAG, "使用模擬路線數據");
                    // 使用模擬路線數據
                    simulateRouteGeneration(startLocation, destination);
                    return;
                }
                
                // 計算路線
                LatLng startLatLng = new LatLng(startLocation.getLatitude(), startLocation.getLongitude());
                DirectionsResult result = DirectionsApi.newRequest(geoApiContext)
                        .origin(new com.google.maps.model.LatLng(startLatLng.latitude, startLatLng.longitude))
                        .destination(new com.google.maps.model.LatLng(destination.latitude, destination.longitude))
                        .mode(com.google.maps.model.TravelMode.WALKING)
                        .await();
                
                if (result.routes != null && result.routes.length > 0) {
                    DirectionsRoute route = result.routes[0];
                    navigationSteps = parseRouteSteps(route);
                    
                    // 開始導航
                    startNavigationLoop();
                } else {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        listener.onNavigationError("無法計算路線");
                    });
                }
            } catch (ApiException | InterruptedException | IOException e) {
                Log.e(TAG, "開始導航失敗，使用模擬路線", e);
                // 如果API失敗，使用模擬路線
                simulateRouteGeneration(startLocation, destination);
            }
        }).start();
    }
    
    private void simulateRouteGeneration(Location startLocation, LatLng destination) {
        // 創建模擬導航步驟
        LatLng startLatLng = new LatLng(startLocation.getLatitude(), startLocation.getLongitude());
        
        navigationSteps = new ArrayList<>();
        
        // 計算距離和方向
        double distance = calculateDistance(startLatLng, destination);
        String direction = getDirectionFromBearing(startLatLng, destination);
        
        // 創建模擬導航步驟
        if (distance > 100) {
            // 如果距離較遠，創建多個步驟
            navigationSteps.add(new NavigationStep(
                "向北步行", 200, "向北", 
                startLatLng, 
                new LatLng(startLatLng.latitude + 0.002, startLatLng.longitude)
            ));
            
            navigationSteps.add(new NavigationStep(
                "向東步行", 300, "向東", 
                new LatLng(startLatLng.latitude + 0.002, startLatLng.longitude),
                new LatLng(startLatLng.latitude + 0.002, startLatLng.longitude + 0.003)
            ));
            
            navigationSteps.add(new NavigationStep(
                "到達目的地", 0, "直行", 
                new LatLng(startLatLng.latitude + 0.002, startLatLng.longitude + 0.003),
                destination
            ));
        } else {
            // 距離較近，直接導航
            navigationSteps.add(new NavigationStep(
                "步行到目的地", (float)distance, direction,
                startLatLng, destination
            ));
        }
        
        // 開始導航
        startNavigationLoop();
    }
    
    private double calculateDistance(LatLng from, LatLng to) {
        float[] results = new float[1];
        android.location.Location.distanceBetween(
            from.latitude, from.longitude,
            to.latitude, to.longitude,
            results
        );
        return results[0];
    }
    
    private String getDirectionFromBearing(LatLng start, LatLng end) {
        double bearing = calculateBearing(start.latitude, start.longitude, end.latitude, end.longitude);
        
        if (bearing >= 337.5 || bearing < 22.5) return "向北";
        else if (bearing >= 22.5 && bearing < 67.5) return "向東北";
        else if (bearing >= 67.5 && bearing < 112.5) return "向東";
        else if (bearing >= 112.5 && bearing < 157.5) return "向東南";
        else if (bearing >= 157.5 && bearing < 202.5) return "向南";
        else if (bearing >= 202.5 && bearing < 247.5) return "向西南";
        else if (bearing >= 247.5 && bearing < 292.5) return "向西";
        else return "向西北";
    }
    
    private List<NavigationStep> parseRouteSteps(DirectionsRoute route) {
        List<NavigationStep> steps = new ArrayList<>();
        
        for (DirectionsStep step : route.legs[0].steps) {
            String instruction = cleanInstruction(step.htmlInstructions);
            double distance = step.distance.inMeters;
            String direction = getDirectionFromBearing(step.startLocation, step.endLocation);
            
            LatLng startLatLng = new LatLng(step.startLocation.lat, step.startLocation.lng);
            LatLng endLatLng = new LatLng(step.endLocation.lat, step.endLocation.lng);
            
            steps.add(new NavigationStep(instruction, distance, direction, startLatLng, endLatLng));
        }
        
        return steps;
    }
    
    private String cleanInstruction(String htmlInstruction) {
        // 移除HTML標籤
        return htmlInstruction.replaceAll("<[^>]*>", "");
    }
    
    private String getDirectionFromBearing(com.google.maps.model.LatLng start, com.google.maps.model.LatLng end) {
        double bearing = calculateBearing(start.lat, start.lng, end.lat, end.lng);
        
        if (bearing >= 337.5 || bearing < 22.5) return "向北";
        else if (bearing >= 22.5 && bearing < 67.5) return "向東北";
        else if (bearing >= 67.5 && bearing < 112.5) return "向東";
        else if (bearing >= 112.5 && bearing < 157.5) return "向東南";
        else if (bearing >= 157.5 && bearing < 202.5) return "向南";
        else if (bearing >= 202.5 && bearing < 247.5) return "向西南";
        else if (bearing >= 247.5 && bearing < 292.5) return "向西";
        else return "向西北";
    }
    
    private double calculateBearing(double lat1, double lng1, double lat2, double lng2) {
        double dLng = Math.toRadians(lng2 - lng1);
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        
        double y = Math.sin(dLng) * Math.cos(lat2Rad);
        double x = Math.cos(lat1Rad) * Math.sin(lat2Rad) - 
                  Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(dLng);
        
        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (bearing + 360) % 360;
    }
    
    private void startNavigationLoop() {
        if (navigationSteps != null && currentStepIndex < navigationSteps.size()) {
            NavigationStep currentStep = navigationSteps.get(currentStepIndex);
            
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (navigationListener != null) {
                    navigationListener.onNavigationUpdate(
                            currentStep.instruction,
                            currentStep.distance,
                            currentStep.direction
                    );
                }
            });
            
            // 模擬導航進度（實際應用中應該根據GPS位置更新）
            new android.os.Handler().postDelayed(() -> {
                currentStepIndex++;
                if (currentStepIndex >= navigationSteps.size()) {
                    // 導航完成
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        if (navigationListener != null) {
                            navigationListener.onNavigationComplete();
                        }
                    });
                    isNavigating = false;
                } else {
                    // 繼續下一步
                    startNavigationLoop();
                }
            }, 10000); // 每10秒更新一次（實際應用中應該根據距離和速度調整）
        }
    }
    
    public void stopNavigation() {
        isNavigating = false;
        navigationSteps = null;
        currentStepIndex = 0;
        navigationListener = null;
    }
    
    public boolean isNavigating() {
        return isNavigating;
    }
    
    public List<NavigationStep> getNavigationSteps() {
        return navigationSteps;
    }
    
    public int getCurrentStepIndex() {
        return currentStepIndex;
    }
}
