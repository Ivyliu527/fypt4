package com.example.tonbo_app;

import android.content.Context;
import android.location.Location;
import android.util.Log;
// 使用自定義的LatLng類

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NavigationManager {
    private static final String TAG = "NavigationManager";
    private static NavigationManager instance;
    private Context context;
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
        // 不再需要Google Maps API初始化
    }
    
    public static synchronized NavigationManager getInstance(Context context) {
        if (instance == null) {
            instance = new NavigationManager(context);
        }
        return instance;
    }
    
    // 不再需要Google Maps API初始化，直接使用模擬數據
    
    public void searchDestination(String query, OnDestinationFoundListener listener) {
        new Thread(() -> {
            Log.d(TAG, "使用模擬數據搜索目的地: " + query);
            // 直接使用模擬數據
            simulateDestinationSearch(query, listener);
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
            Log.d(TAG, "使用模擬路線數據");
            // 直接使用模擬路線數據
            simulateRouteGeneration(startLocation, destination);
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
    
    // 移除了Google Maps相關的解析方法，直接使用模擬數據
    
    // 移除了Google Maps相關的解析方法，直接使用模擬數據
    
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
