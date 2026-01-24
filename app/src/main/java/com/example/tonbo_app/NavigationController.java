package com.example.tonbo_app;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

/**
 * 導航控制器
 * 負責處理導航相關功能：獲取位置、路線規劃、導航狀態播報
 */
public class NavigationController {
    private static final String TAG = "NavigationController";
    
    private Context context;
    private FusedLocationProviderClient fusedLocationClient;
    private TTSManager ttsManager;
    private NavigationListener listener;
    
    // 導航狀態
    private NavigationState currentState = NavigationState.IDLE;
    private Location currentLocation;
    private String destination;
    
    /**
     * 導航狀態枚舉
     */
    public enum NavigationState {
        IDLE,                    // 空閒
        GETTING_LOCATION,        // 正在獲取位置
        PLANNING_ROUTE,          // 正在規劃路線
        NAVIGATING,              // 導航中
        ARRIVED,                 // 已到達
        ERROR                    // 錯誤
    }
    
    /**
     * 導航監聽器接口
     */
    public interface NavigationListener {
        void onLocationObtained(Location location);
        void onRoutePlanned(String routeInfo);
        void onNavigationStarted();
        void onNavigationStateChanged(NavigationState state);
        void onError(String error);
    }
    
    /**
     * 構造函數
     */
    public NavigationController(Context context) {
        this.context = context.getApplicationContext();
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.context);
        this.ttsManager = TTSManager.getInstance(this.context);
    }
    
    /**
     * 設置導航監聽器
     */
    public void setNavigationListener(NavigationListener listener) {
        this.listener = listener;
    }
    
    /**
     * 開始導航
     * @param destination 目的地字符串
     */
    public void startNavigation(String destination) {
        if (destination == null || destination.trim().isEmpty()) {
            String errorMsg = getLocalizedString("navigation_error_no_destination");
            Log.e(TAG, errorMsg);
            announceStatus(errorMsg);
            if (listener != null) {
                listener.onError(errorMsg);
            }
            return;
        }
        
        this.destination = destination.trim();
        Log.d(TAG, "開始導航到: " + this.destination);
        
        // 更新狀態
        setState(NavigationState.GETTING_LOCATION);
        
        // 播報開始獲取位置
        String statusMsg = getLocalizedString("navigation_getting_location");
        announceStatus(statusMsg);
        
        // 獲取當前位置
        getCurrentLocation();
    }
    
    /**
     * 獲取當前位置
     */
    private void getCurrentLocation() {
        try {
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                    .setMaxUpdateDelayMillis(5000)
                    .build();
            
            Task<Location> locationTask = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY, null);
            
            locationTask.addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        currentLocation = location;
                        Log.d(TAG, "位置獲取成功: " + location.getLatitude() + ", " + location.getLongitude());
                        
                        // 通知監聽器
                        if (listener != null) {
                            listener.onLocationObtained(location);
                        }
                        
                        // 開始規劃路線
                        planRoute();
                    } else {
                        String errorMsg = getLocalizedString("navigation_error_location_unavailable");
                        Log.e(TAG, errorMsg);
                        setState(NavigationState.ERROR);
                        announceStatus(errorMsg);
                        if (listener != null) {
                            listener.onError(errorMsg);
                        }
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    String errorMsg = getLocalizedString("navigation_error_location_failed") + ": " + e.getMessage();
                    Log.e(TAG, errorMsg, e);
                    setState(NavigationState.ERROR);
                    announceStatus(errorMsg);
                    if (listener != null) {
                        listener.onError(errorMsg);
                    }
                }
            });
            
        } catch (SecurityException e) {
            String errorMsg = getLocalizedString("navigation_error_permission");
            Log.e(TAG, errorMsg, e);
            setState(NavigationState.ERROR);
            announceStatus(errorMsg);
            if (listener != null) {
                listener.onError(errorMsg);
            }
        } catch (Exception e) {
            String errorMsg = getLocalizedString("navigation_error_unknown") + ": " + e.getMessage();
            Log.e(TAG, errorMsg, e);
            setState(NavigationState.ERROR);
            announceStatus(errorMsg);
            if (listener != null) {
                listener.onError(errorMsg);
            }
        }
    }
    
    /**
     * 規劃路線（模擬實現）
     */
    private void planRoute() {
        setState(NavigationState.PLANNING_ROUTE);
        
        // 播報正在規劃路線
        String statusMsg = getLocalizedString("navigation_planning_route");
        announceStatus(statusMsg);
        
        // 模擬路線規劃（實際應用中可以調用 Google Maps API 或其他路線規劃服務）
        // 這裡使用延遲來模擬規劃過程
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // 生成模擬路線信息
                String routeInfo = generateMockRouteInfo();
                Log.d(TAG, "路線規劃完成: " + routeInfo);
                
                // 通知監聽器
                if (listener != null) {
                    listener.onRoutePlanned(routeInfo);
                }
                
                // 開始導航
                startNavigating();
            }
        }, 2000); // 延遲2秒模擬規劃過程
    }
    
    /**
     * 生成模擬路線信息
     */
    private String generateMockRouteInfo() {
        if (currentLocation == null) {
            return "路線規劃完成";
        }
        
        // 模擬路線信息
        String routeInfo = String.format(
            getLocalizedString("navigation_route_info"),
            destination,
            String.format("%.2f", currentLocation.getLatitude()),
            String.format("%.2f", currentLocation.getLongitude()),
            "5.2", // 模擬距離（公里）
            "15"   // 模擬時間（分鐘）
        );
        
        return routeInfo;
    }
    
    /**
     * 開始導航
     */
    private void startNavigating() {
        setState(NavigationState.NAVIGATING);
        
        // 播報導航開始
        String statusMsg = getLocalizedString("navigation_started");
        announceStatus(statusMsg);
        
        // 通知監聽器
        if (listener != null) {
            listener.onNavigationStarted();
        }
        
        Log.d(TAG, "導航已開始，目的地: " + destination);
    }
    
    /**
     * 停止導航
     */
    public void stopNavigation() {
        if (currentState == NavigationState.NAVIGATING) {
            setState(NavigationState.IDLE);
            String statusMsg = getLocalizedString("navigation_stopped");
            announceStatus(statusMsg);
            Log.d(TAG, "導航已停止");
        }
    }
    
    /**
     * 設置導航狀態
     */
    private void setState(NavigationState state) {
        if (currentState != state) {
            NavigationState oldState = currentState;
            currentState = state;
            Log.d(TAG, "導航狀態變化: " + oldState + " -> " + state);
            
            if (listener != null) {
                listener.onNavigationStateChanged(state);
            }
        }
    }
    
    /**
     * 獲取當前狀態
     */
    public NavigationState getCurrentState() {
        return currentState;
    }
    
    /**
     * 獲取當前位置
     */
    public Location getCurrentLocationSync() {
        return currentLocation;
    }
    
    /**
     * 獲取目的地
     */
    public String getDestination() {
        return destination;
    }
    
    /**
     * 播報狀態信息
     */
    private void announceStatus(String message) {
        if (ttsManager != null) {
            String currentLang = LocaleManager.getInstance(context).getCurrentLanguage();
            if ("english".equals(currentLang)) {
                ttsManager.speak(null, message, true);
            } else if ("mandarin".equals(currentLang)) {
                ttsManager.speak(message, null, true);
            } else {
                ttsManager.speak(message, message, true);
            }
        }
    }
    
    /**
     * 根據當前語言獲取本地化字符串
     */
    private String getLocalizedString(String key) {
        String currentLang = LocaleManager.getInstance(context).getCurrentLanguage();
        
        switch (key) {
            case "navigation_getting_location":
                if ("english".equals(currentLang)) {
                    return "Getting current location";
                } else if ("mandarin".equals(currentLang)) {
                    return "正在获取当前位置";
                } else {
                    return "正在獲取當前位置";
                }
            case "navigation_planning_route":
                if ("english".equals(currentLang)) {
                    return "Planning route";
                } else if ("mandarin".equals(currentLang)) {
                    return "正在规划路线";
                } else {
                    return "正在規劃路線";
                }
            case "navigation_started":
                if ("english".equals(currentLang)) {
                    return "Navigation started";
                } else if ("mandarin".equals(currentLang)) {
                    return "导航开始";
                } else {
                    return "導航開始";
                }
            case "navigation_stopped":
                if ("english".equals(currentLang)) {
                    return "Navigation stopped";
                } else if ("mandarin".equals(currentLang)) {
                    return "导航已停止";
                } else {
                    return "導航已停止";
                }
            case "navigation_route_info":
                if ("english".equals(currentLang)) {
                    return "Route to %s planned. Distance: %s km, Estimated time: %s minutes";
                } else if ("mandarin".equals(currentLang)) {
                    return "前往%s的路线已规划。距离：%s公里，预计时间：%s分钟";
                } else {
                    return "前往%s的路線已規劃。距離：%s公里，預計時間：%s分鐘";
                }
            case "navigation_error_no_destination":
                if ("english".equals(currentLang)) {
                    return "Destination not specified";
                } else if ("mandarin".equals(currentLang)) {
                    return "未指定目的地";
                } else {
                    return "未指定目的地";
                }
            case "navigation_error_location_unavailable":
                if ("english".equals(currentLang)) {
                    return "Location unavailable";
                } else if ("mandarin".equals(currentLang)) {
                    return "位置不可用";
                } else {
                    return "位置不可用";
                }
            case "navigation_error_location_failed":
                if ("english".equals(currentLang)) {
                    return "Failed to get location";
                } else if ("mandarin".equals(currentLang)) {
                    return "获取位置失败";
                } else {
                    return "獲取位置失敗";
                }
            case "navigation_error_permission":
                if ("english".equals(currentLang)) {
                    return "Location permission denied";
                } else if ("mandarin".equals(currentLang)) {
                    return "位置权限被拒绝";
                } else {
                    return "位置權限被拒絕";
                }
            case "navigation_error_unknown":
                if ("english".equals(currentLang)) {
                    return "Unknown error";
                } else if ("mandarin".equals(currentLang)) {
                    return "未知错误";
                } else {
                    return "未知錯誤";
                }
            default:
                return "";
        }
    }
}
