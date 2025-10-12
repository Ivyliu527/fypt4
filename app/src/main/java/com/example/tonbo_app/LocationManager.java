package com.example.tonbo_app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnSuccessListener;

public class LocationManager {
    private static final String TAG = "LocationManager";
    private static final long UPDATE_INTERVAL = 10000; // 10秒更新一次
    private static final long FASTEST_INTERVAL = 5000; // 最快5秒更新一次
    private static final float SMALLEST_DISPLACEMENT = 10.0f; // 最小位移10米
    
    private static LocationManager instance;
    private Context context;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location currentLocation;
    private boolean isLocationUpdatesActive = false;
    
    // 位置更新監聽器接口
    public interface OnLocationUpdateListener {
        void onLocationUpdate(Location location);
        void onLocationError(String error);
    }
    
    // 位置獲取監聽器接口
    public interface OnLocationReceivedListener {
        void onLocationReceived(Location location);
        void onLocationError(String error);
    }
    
    private OnLocationUpdateListener locationUpdateListener;
    private OnLocationReceivedListener locationReceivedListener;
    
    private LocationManager(Context context) {
        this.context = context.getApplicationContext();
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        initLocationCallback();
    }
    
    public static synchronized LocationManager getInstance(Context context) {
        if (instance == null) {
            instance = new LocationManager(context);
        }
        return instance;
    }
    
    private void initLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.w(TAG, "位置結果為空");
                    return;
                }
                
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    currentLocation = location;
                    Log.d(TAG, "位置更新: " + location.getLatitude() + ", " + location.getLongitude() + 
                          ", 精度: " + location.getAccuracy() + "米");
                    
                    if (locationUpdateListener != null) {
                        locationUpdateListener.onLocationUpdate(location);
                    }
                }
            }
        };
    }
    
    /**
     * 檢查位置權限
     */
    public boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED &&
               ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * 獲取最後已知位置
     */
    public void getLastKnownLocation(OnLocationReceivedListener listener) {
        this.locationReceivedListener = listener;
        
        if (!hasLocationPermission()) {
            if (listener != null) {
                listener.onLocationError("沒有位置權限");
            }
            return;
        }
        
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            currentLocation = location;
                            Log.d(TAG, "獲取最後已知位置: " + location.getLatitude() + ", " + location.getLongitude());
                            
                            if (locationReceivedListener != null) {
                                locationReceivedListener.onLocationReceived(location);
                            }
                        } else {
                            Log.w(TAG, "最後已知位置為空，開始請求位置更新");
                            // 如果最後已知位置為空，開始請求位置更新
                            requestLocationUpdates();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "獲取最後已知位置失敗", e);
                    if (locationReceivedListener != null) {
                        locationReceivedListener.onLocationError("獲取位置失敗: " + e.getMessage());
                    }
                });
    }
    
    /**
     * 開始位置更新
     */
    public void startLocationUpdates(OnLocationUpdateListener listener) {
        this.locationUpdateListener = listener;
        
        if (!hasLocationPermission()) {
            if (listener != null) {
                listener.onLocationError("沒有位置權限");
            }
            return;
        }
        
        requestLocationUpdates();
    }
    
    private void requestLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL)
                .setMinUpdateIntervalMillis(FASTEST_INTERVAL)
                .setMinUpdateDistanceMeters(SMALLEST_DISPLACEMENT)
                .build();
        
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "沒有精確位置權限");
            return;
        }
        
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "位置更新請求成功");
                    isLocationUpdatesActive = true;
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "位置更新請求失敗", e);
                    if (locationUpdateListener != null) {
                        locationUpdateListener.onLocationError("位置更新失敗: " + e.getMessage());
                    }
                });
    }
    
    /**
     * 停止位置更新
     */
    public void stopLocationUpdates() {
        if (isLocationUpdatesActive) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "位置更新已停止");
                        isLocationUpdatesActive = false;
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "停止位置更新失敗", e);
                    });
        }
    }
    
    /**
     * 獲取當前位置
     */
    public Location getCurrentLocation() {
        return currentLocation;
    }
    
    /**
     * 檢查GPS是否啟用
     */
    public boolean isGPSEnabled() {
        android.location.LocationManager locationManager = 
                (android.location.LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        
        if (locationManager == null) {
            return false;
        }
        
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
    }
    
    /**
     * 獲取位置精度描述
     */
    public String getLocationAccuracyDescription(Location location) {
        if (location == null) {
            return "位置未知";
        }
        
        float accuracy = location.getAccuracy();
        if (accuracy < 5) {
            return "位置精度很高";
        } else if (accuracy < 20) {
            return "位置精度良好";
        } else if (accuracy < 100) {
            return "位置精度一般";
        } else {
            return "位置精度較低";
        }
    }
    
    /**
     * 格式化位置信息為可讀字符串
     */
    public String formatLocationInfo(Location location) {
        if (location == null) {
            return "位置未知";
        }
        
        return String.format("緯度: %.6f, 經度: %.6f, 精度: %.1f米", 
                location.getLatitude(), 
                location.getLongitude(), 
                location.getAccuracy());
    }
    
    /**
     * 計算兩點間距離（米）
     */
    public static float calculateDistance(Location location1, Location location2) {
        if (location1 == null || location2 == null) {
            return 0;
        }
        return location1.distanceTo(location2);
    }
    
    /**
     * 計算兩點間方位角
     */
    public static float calculateBearing(Location from, Location to) {
        if (from == null || to == null) {
            return 0;
        }
        
        double lat1 = Math.toRadians(from.getLatitude());
        double lat2 = Math.toRadians(to.getLatitude());
        double deltaLng = Math.toRadians(to.getLongitude() - from.getLongitude());
        
        double y = Math.sin(deltaLng) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - 
                  Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLng);
        
        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (float) ((bearing + 360) % 360);
    }
    
    /**
     * 獲取方位描述
     */
    public static String getBearingDescription(float bearing) {
        if (bearing >= 337.5 || bearing < 22.5) return "正北";
        else if (bearing >= 22.5 && bearing < 67.5) return "東北";
        else if (bearing >= 67.5 && bearing < 112.5) return "正東";
        else if (bearing >= 112.5 && bearing < 157.5) return "東南";
        else if (bearing >= 157.5 && bearing < 202.5) return "正南";
        else if (bearing >= 202.5 && bearing < 247.5) return "西南";
        else if (bearing >= 247.5 && bearing < 292.5) return "正西";
        else return "西北";
    }
    
    /**
     * 清理資源
     */
    public void cleanup() {
        stopLocationUpdates();
        locationUpdateListener = null;
        locationReceivedListener = null;
    }
}
