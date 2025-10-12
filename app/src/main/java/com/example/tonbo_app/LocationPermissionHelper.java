package com.example.tonbo_app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class LocationPermissionHelper {
    private static final String TAG = "LocationPermissionHelper";
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    public static final int LOCATION_SETTINGS_REQUEST_CODE = 1002;
    
    private Activity activity;
    private OnPermissionResultListener permissionListener;
    
    // 權限結果監聽器接口
    public interface OnPermissionResultListener {
        void onPermissionGranted();
        void onPermissionDenied();
        void onPermissionDeniedPermanently();
    }
    
    public LocationPermissionHelper(Activity activity) {
        this.activity = activity;
    }
    
    /**
     * 檢查是否已有位置權限
     */
    public boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * 請求位置權限
     */
    public void requestLocationPermission(OnPermissionResultListener listener) {
        this.permissionListener = listener;
        
        if (hasLocationPermission()) {
            Log.d(TAG, "已有位置權限");
            if (permissionListener != null) {
                permissionListener.onPermissionGranted();
            }
            return;
        }
        
        // 檢查是否應該顯示權限說明
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
            // 用戶之前拒絕了權限，解釋為什麼需要權限
            showPermissionRationale();
        } else {
            // 直接請求權限
            requestPermissions();
        }
    }
    
    /**
     * 顯示權限說明
     */
    private void showPermissionRationale() {
        Log.d(TAG, "顯示位置權限說明");
        
        // 這裡可以顯示一個對話框解釋為什麼需要位置權限
        // 為了簡化，我們直接請求權限
        requestPermissions();
    }
    
    /**
     * 請求權限
     */
    private void requestPermissions() {
        Log.d(TAG, "請求位置權限");
        ActivityCompat.requestPermissions(activity,
                new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST_CODE);
    }
    
    /**
     * 處理權限請求結果
     */
    public void handlePermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }
        
        boolean allPermissionsGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }
        
        if (allPermissionsGranted) {
            Log.d(TAG, "位置權限已授予");
            if (permissionListener != null) {
                permissionListener.onPermissionGranted();
            }
        } else {
            Log.d(TAG, "位置權限被拒絕");
            
            // 檢查是否永久拒絕
            boolean shouldShowRationale = false;
            for (String permission : permissions) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                    shouldShowRationale = true;
                    break;
                }
            }
            
            if (shouldShowRationale) {
                // 用戶拒絕但沒有選擇"不再詢問"
                if (permissionListener != null) {
                    permissionListener.onPermissionDenied();
                }
            } else {
                // 用戶選擇了"不再詢問"，需要引導到設置頁面
                if (permissionListener != null) {
                    permissionListener.onPermissionDeniedPermanently();
                }
            }
        }
    }
    
    /**
     * 檢查GPS是否啟用
     */
    public boolean isGPSEnabled() {
        LocationManager locationManager = 
                (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        
        if (locationManager == null) {
            return false;
        }
        
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
    
    /**
     * 檢查網絡定位是否啟用
     */
    public boolean isNetworkLocationEnabled() {
        LocationManager locationManager = 
                (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        
        if (locationManager == null) {
            return false;
        }
        
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
    
    /**
     * 檢查是否有任何定位服務啟用
     */
    public boolean isAnyLocationProviderEnabled() {
        return isGPSEnabled() || isNetworkLocationEnabled();
    }
    
    /**
     * 打開位置設置頁面
     */
    public void openLocationSettings() {
        Log.d(TAG, "打開位置設置頁面");
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        activity.startActivityForResult(intent, LOCATION_SETTINGS_REQUEST_CODE);
    }
    
    /**
     * 打開應用設置頁面
     */
    public void openAppSettings() {
        Log.d(TAG, "打開應用設置頁面");
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivity(intent);
    }
    
    /**
     * 獲取定位服務狀態描述
     */
    public String getLocationServiceStatusDescription() {
        if (!isAnyLocationProviderEnabled()) {
            return "定位服務已關閉";
        }
        
        StringBuilder status = new StringBuilder("定位服務狀態：");
        if (isGPSEnabled()) {
            status.append("GPS已啟用");
        }
        if (isNetworkLocationEnabled()) {
            if (isGPSEnabled()) {
                status.append("，");
            }
            status.append("網絡定位已啟用");
        }
        
        return status.toString();
    }
    
    /**
     * 獲取權限狀態描述
     */
    public String getPermissionStatusDescription() {
        if (hasLocationPermission()) {
            return "位置權限已授予";
        } else {
            return "位置權限未授予";
        }
    }
    
    /**
     * 獲取完整的定位狀態描述
     */
    public String getFullLocationStatusDescription() {
        StringBuilder status = new StringBuilder();
        
        // 權限狀態
        status.append(getPermissionStatusDescription());
        status.append("；");
        
        // 服務狀態
        status.append(getLocationServiceStatusDescription());
        
        return status.toString();
    }
    
    /**
     * 檢查是否可以使用定位功能
     */
    public boolean canUseLocation() {
        return hasLocationPermission() && isAnyLocationProviderEnabled();
    }
    
    /**
     * 獲取需要解決的問題描述
     */
    public String getIssuesDescription() {
        if (canUseLocation()) {
            return "定位功能正常";
        }
        
        StringBuilder issues = new StringBuilder();
        
        if (!hasLocationPermission()) {
            issues.append("缺少位置權限");
        }
        
        if (!isAnyLocationProviderEnabled()) {
            if (issues.length() > 0) {
                issues.append("；");
            }
            issues.append("定位服務已關閉");
        }
        
        return issues.toString();
    }
}
