package com.example.tonbo_app;

import android.content.Context;
import android.location.Location;
import android.util.Log;

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
    private OSRMRoutingService routingService;
    private VoiceGuidanceManager voiceGuidanceManager;
    private android.location.Location lastKnownLocation;
    
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
        this.routingService = new OSRMRoutingService();
        this.voiceGuidanceManager = VoiceGuidanceManager.getInstance(context);
    }
    
    public static synchronized NavigationManager getInstance(Context context) {
        if (instance == null) {
            instance = new NavigationManager(context);
        }
        return instance;
    }
    
    // 不再需要Google Maps API初始化，直接使用模擬數據
    
    public void searchDestination(String query, OnDestinationFoundListener listener) {
        Log.d(TAG, "使用Nominatim API搜索目的地: " + query);
        
        routingService.searchLocation(query, new OSRMRoutingService.SearchCallback() {
            @Override
            public void onSuccess(OSRMRoutingService.SearchResult[] results) {
                if (results.length > 0) {
                    OSRMRoutingService.SearchResult result = results[0];
                    try {
                        double lat = Double.parseDouble(result.lat);
                        double lon = Double.parseDouble(result.lon);
                        LatLng destination = new LatLng(lat, lon);
                        listener.onDestinationFound(destination, result.displayName);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "解析坐標失敗", e);
                        listener.onDestinationNotFound("坐標解析失敗");
                    }
                } else {
                    listener.onDestinationNotFound("未找到地點");
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "搜索失敗: " + error);
                // 如果API失敗，回退到模擬數據
                simulateDestinationSearch(query, listener);
            }
        });
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
        this.lastKnownLocation = startLocation;
        
        Log.d(TAG, "使用OSRM API計算路線");
        
        // 播報開始計算路線
        voiceGuidanceManager.speakRouteCalculating();
        
        routingService.getRoute(
            startLocation.getLatitude(), 
            startLocation.getLongitude(),
            destination.latitude,
            destination.longitude,
            new OSRMRoutingService.RoutingCallback() {
                @Override
                public void onSuccess(OSRMRoutingService.RouteResponse routeResponse) {
                    if (routeResponse.routes != null && routeResponse.routes.size() > 0) {
                        OSRMRoutingService.Route route = routeResponse.routes.get(0);
                        navigationSteps = parseOSRMRoute(route);
                        
                        // 播報路線計算完成
                        voiceGuidanceManager.speakRouteFound();
                        
                        // 延遲1秒後開始導航，讓用戶聽完語音
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            voiceGuidanceManager.speakNavigationStarted();
                            startNavigationLoop();
                        }, 1000);
                        
                        Log.d(TAG, "路線計算成功，共 " + navigationSteps.size() + " 個步驟");
                    } else {
                        listener.onNavigationError("未找到路線");
                    }
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "路線計算失敗: " + error + "，使用模擬路線");
                    // 如果API失敗，回退到模擬數據
                    simulateRouteGeneration(startLocation, destination);
                }
            }
        );
    }
    
    private List<NavigationStep> parseOSRMRoute(OSRMRoutingService.Route route) {
        List<NavigationStep> steps = new ArrayList<>();
        
        if (route.legs != null && route.legs.size() > 0) {
            for (OSRMRoutingService.Leg leg : route.legs) {
                if (leg.steps != null) {
                    for (OSRMRoutingService.Step step : leg.steps) {
                        String instruction = getChineseInstruction(step);
                        String direction = getDirectionFromModifier(step.maneuver.modifier);
                        
                        // 獲取步驟的起點和終點坐標
                        LatLng startLatLng = null;
                        LatLng endLatLng = null;
                        
                        if (step.maneuver.location != null && step.maneuver.location.size() >= 2) {
                            startLatLng = new LatLng(
                                step.maneuver.location.get(1), 
                                step.maneuver.location.get(0)
                            );
                        }
                        
                        if (step.geometry != null && step.geometry.coordinates != null 
                                && step.geometry.coordinates.size() > 0) {
                            List<Double> lastCoord = step.geometry.coordinates.get(
                                step.geometry.coordinates.size() - 1
                            );
                            if (lastCoord.size() >= 2) {
                                endLatLng = new LatLng(lastCoord.get(1), lastCoord.get(0));
                            }
                        }
                        
                        steps.add(new NavigationStep(
                            instruction,
                            step.distance,
                            direction,
                            startLatLng != null ? startLatLng : new LatLng(0, 0),
                            endLatLng != null ? endLatLng : new LatLng(0, 0)
                        ));
                    }
                }
            }
        }
        
        return steps;
    }
    
    private String getChineseInstruction(OSRMRoutingService.Step step) {
        String type = step.maneuver.type;
        String modifier = step.maneuver.modifier != null ? step.maneuver.modifier : "";
        String name = step.name != null && !step.name.isEmpty() ? step.name : "前方";
        
        switch (type) {
            case "depart":
                return "出發，沿" + name + "行走";
            case "arrive":
                return "到達目的地";
            case "turn":
                if (modifier.contains("left")) {
                    return "左轉進入" + name;
                } else if (modifier.contains("right")) {
                    return "右轉進入" + name;
                }
                return "轉彎進入" + name;
            case "new name":
                return "繼續直行進入" + name;
            case "continue":
                return "繼續沿" + name + "行走";
            case "roundabout":
                return "進入環島";
            case "rotary":
                return "進入迴旋處";
            case "end of road":
                if (modifier.contains("left")) {
                    return "道路盡頭左轉";
                } else if (modifier.contains("right")) {
                    return "道路盡頭右轉";
                }
                return "道路盡頭轉彎";
            default:
                return step.maneuver.instruction != null ? 
                       step.maneuver.instruction : "繼續行走";
        }
    }
    
    private String getDirectionFromModifier(String modifier) {
        if (modifier == null) return "直行";
        
        switch (modifier) {
            case "left":
            case "sharp left":
                return "向左";
            case "right":
            case "sharp right":
                return "向右";
            case "straight":
                return "直行";
            case "slight left":
                return "稍微向左";
            case "slight right":
                return "稍微向右";
            case "uturn":
                return "掉頭";
            default:
                return "直行";
        }
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
                // 播報導航指令
                voiceGuidanceManager.speakNavigationInstruction(
                    currentStep.instruction,
                    currentStep.distance,
                    currentStep.direction
                );
                
                // 通知監聽器
                if (navigationListener != null) {
                    navigationListener.onNavigationUpdate(
                            currentStep.instruction,
                            currentStep.distance,
                            currentStep.direction
                    );
                }
            });
            
            // 如果接近最後一步，提前播報到達提示
            if (currentStepIndex == navigationSteps.size() - 2) {
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    voiceGuidanceManager.speakArrivalAlert();
                }, 5000); // 5秒後播報即將到達
            }
            
            // 模擬導航進度（實際應用中應該根據GPS位置更新）
            new android.os.Handler().postDelayed(() -> {
                currentStepIndex++;
                if (currentStepIndex >= navigationSteps.size()) {
                    // 導航完成
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        voiceGuidanceManager.speakDestinationReached();
                        
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
        
        // 播報導航停止
        voiceGuidanceManager.speakNavigationStopped();
    }
    
    /**
     * 更新當前位置（用於實時導航）
     */
    public void updateLocation(Location location) {
        this.lastKnownLocation = location;
        
        if (!isNavigating || navigationSteps == null || currentStepIndex >= navigationSteps.size()) {
            return;
        }
        
        // 計算到下一個轉向點的距離
        NavigationStep currentStep = navigationSteps.get(currentStepIndex);
        if (currentStep.endLocation != null) {
            float[] results = new float[1];
            android.location.Location.distanceBetween(
                location.getLatitude(), location.getLongitude(),
                currentStep.endLocation.latitude, currentStep.endLocation.longitude,
                results
            );
            
            double distanceToNext = results[0];
            
            // 如果接近轉向點（50米內），準備進入下一步
            if (distanceToNext < 50 && currentStepIndex < navigationSteps.size() - 1) {
                Log.d(TAG, "接近轉向點，距離: " + distanceToNext + "米");
                // 可以在這裡提前播報下一步指令
            }
            
            // 如果已經過了轉向點（距離開始變大），進入下一步
            // 這裡的邏輯需要更複雜的實現，暫時保持定時更新
        }
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
