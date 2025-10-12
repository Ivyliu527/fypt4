package com.example.tonbo_app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
// 使用自定義的LatLng類

public class NavigationActivity extends BaseAccessibleActivity {
    private static final String TAG = "NavigationActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    
    // UI組件
    private EditText destinationInput;
    private Button searchButton;
    private Button startNavigationButton;
    private Button stopNavigationButton;
    private TextView statusText;
    private TextView currentLocationText;
    private TextView navigationInstructions;
    private LinearLayout savedDestinationsContainer;
    
    // 位置相關
    private Location currentLocation;
    private LatLng destination;
    private boolean isNavigating = false;
    
    // 位置管理
    private LocationManager locationManager;
    private LocationPermissionHelper permissionHelper;
    
    // 導航相關
    private NavigationManager navigationManager;
    private VoiceGuidanceManager voiceGuidanceManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        
        // 初始化管理器
        navigationManager = NavigationManager.getInstance(this);
        voiceGuidanceManager = VoiceGuidanceManager.getInstance(this);
        locationManager = LocationManager.getInstance(this);
        permissionHelper = new LocationPermissionHelper(this);
        
        initViews();
        checkLocationPermissionAndSetup();
        
        // 頁面標題播報
        announcePageTitle();
    }
    
    private void initViews() {
        // 獲取UI組件
        destinationInput = findViewById(R.id.destinationInput);
        searchButton = findViewById(R.id.searchButton);
        startNavigationButton = findViewById(R.id.startNavigationButton);
        stopNavigationButton = findViewById(R.id.stopNavigationButton);
        statusText = findViewById(R.id.statusText);
        currentLocationText = findViewById(R.id.currentLocationText);
        navigationInstructions = findViewById(R.id.navigationInstructions);
        savedDestinationsContainer = findViewById(R.id.savedDestinationsContainer);
        
        // 設置按鈕點擊事件
        searchButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            searchDestination();
        });
        
        startNavigationButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            startNavigation();
        });
        
        stopNavigationButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            stopNavigation();
        });
        
        // 初始狀態
        updateUI();
    }
    
    // 移除了Google Maps相關的設置方法
    
    private void checkLocationPermissionAndSetup() {
        // 檢查權限狀態
        if (!permissionHelper.hasLocationPermission()) {
            // 請求位置權限
            permissionHelper.requestLocationPermission(new LocationPermissionHelper.OnPermissionResultListener() {
                @Override
                public void onPermissionGranted() {
                    Log.d(TAG, "位置權限已授予");
                    setupLocationServices();
                }
                
                @Override
                public void onPermissionDenied() {
                    Log.d(TAG, "位置權限被拒絕");
                    handleLocationPermissionDenied();
                }
                
                @Override
                public void onPermissionDeniedPermanently() {
                    Log.d(TAG, "位置權限被永久拒絕");
                    handleLocationPermissionDeniedPermanently();
                }
            });
        } else {
            // 已有權限，直接設置位置服務
            setupLocationServices();
        }
    }
    
    private void setupLocationServices() {
        // 檢查定位服務狀態
        if (!permissionHelper.isAnyLocationProviderEnabled()) {
            announceError("定位服務已關閉，請開啟GPS或網絡定位");
            statusText.setText("定位服務已關閉");
            return;
        }
        
        // 開始獲取位置
        getCurrentLocation();
        
        // 開始位置更新
        startLocationUpdates();
        
        announceInfo("位置服務已啟動");
    }
    
    private void handleLocationPermissionDenied() {
        announceError("需要位置權限才能使用導航功能，請在設置中允許位置權限");
        statusText.setText("需要位置權限");
    }
    
    private void handleLocationPermissionDeniedPermanently() {
        announceError("位置權限被永久拒絕，請在應用設置中手動開啟位置權限");
        statusText.setText("位置權限被拒絕");
        // 可以考慮顯示一個對話框引導用戶到設置頁面
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionHelper.handlePermissionResult(requestCode, permissions, grantResults);
    }
    
    private void getCurrentLocation() {
        locationManager.getLastKnownLocation(new LocationManager.OnLocationReceivedListener() {
            @Override
            public void onLocationReceived(Location location) {
                currentLocation = location;
                updateCurrentLocationDisplay();
                
                // 播報位置信息
                String locationInfo = locationManager.formatLocationInfo(location);
                String accuracyDesc = locationManager.getLocationAccuracyDescription(location);
                announceInfo("位置已獲取：" + accuracyDesc + "，" + locationInfo);
                
                Log.d(TAG, "當前位置: " + locationInfo);
            }
            
            @Override
            public void onLocationError(String error) {
                announceError("獲取位置失敗：" + error);
                statusText.setText("位置獲取失敗：" + error);
            }
        });
    }
    
    private void startLocationUpdates() {
        locationManager.startLocationUpdates(new LocationManager.OnLocationUpdateListener() {
            @Override
            public void onLocationUpdate(Location location) {
                currentLocation = location;
                updateCurrentLocationDisplay();
                
                // 在導航過程中更新位置
                if (isNavigating) {
                    // 這裡可以添加導航過程中的位置更新邏輯
                    Log.d(TAG, "導航中位置更新: " + locationManager.formatLocationInfo(location));
                }
            }
            
            @Override
            public void onLocationError(String error) {
                Log.e(TAG, "位置更新錯誤: " + error);
            }
        });
    }
    
    private void searchDestination() {
        String destinationText = destinationInput.getText().toString().trim();
        if (destinationText.isEmpty()) {
            announceError("請輸入目的地");
            return;
        }
        
        announceInfo("正在搜索目的地：" + destinationText);
        statusText.setText("正在搜索目的地...");
        
        // 使用Geocoding API搜索目的地
        navigationManager.searchDestination(destinationText, new NavigationManager.OnDestinationFoundListener() {
            @Override
            public void onDestinationFound(LatLng destinationLatLng, String address) {
                destination = destinationLatLng;
                updateUI();
                
                // 目的地已找到，準備導航
                
                String cantoneseText = "找到目的地：" + address + "。點擊開始導航按鈕開始導航";
                String englishText = "Destination found: " + address + ". Tap start navigation to begin";
                ttsManager.speak(cantoneseText, englishText, true);
                
                statusText.setText("目的地已找到：" + address);
            }
            
            @Override
            public void onDestinationNotFound(String error) {
                announceError("找不到目的地：" + error);
                statusText.setText("找不到目的地");
            }
        });
    }
    
    private void startNavigation() {
        if (destination == null) {
            announceError("請先搜索目的地");
            return;
        }
        
        if (currentLocation == null) {
            announceError("無法獲取當前位置");
            return;
        }
        
        isNavigating = true;
        updateUI();
        
        // 開始導航
        navigationManager.startNavigation(currentLocation, destination, new NavigationManager.OnNavigationUpdateListener() {
            @Override
            public void onNavigationUpdate(String instruction, double distance, String direction) {
                updateNavigationInstructions(instruction, distance, direction);
                
                // 語音播報導航指令
                voiceGuidanceManager.speakNavigationInstruction(instruction, distance, direction);
            }
            
            @Override
            public void onNavigationComplete() {
                isNavigating = false;
                updateUI();
                announceInfo("已到達目的地");
                statusText.setText("已到達目的地");
            }
            
            @Override
            public void onNavigationError(String error) {
                announceError("導航錯誤：" + error);
                statusText.setText("導航錯誤：" + error);
            }
        });
        
        // 路線規劃完成
        
        announceInfo("導航已開始，請按照語音指引行走");
        statusText.setText("導航進行中...");
    }
    
    private void stopNavigation() {
        isNavigating = false;
        navigationManager.stopNavigation();
        updateUI();
        
        announceInfo("導航已停止");
        statusText.setText("導航已停止");
    }
    
    // 移除了地圖路線繪製功能
    
    private void updateCurrentLocationDisplay() {
        if (currentLocation != null) {
            String locationText = locationManager.formatLocationInfo(currentLocation);
            currentLocationText.setText(locationText);
        }
    }
    
    private void updateNavigationInstructions(String instruction, double distance, String direction) {
        String instructionText = String.format("%s，距離%.0f米", instruction, distance);
        navigationInstructions.setText(instructionText);
    }
    
    private void updateUI() {
        // 更新按鈕狀態
        searchButton.setEnabled(!isNavigating);
        startNavigationButton.setEnabled(destination != null && !isNavigating);
        stopNavigationButton.setEnabled(isNavigating);
        
        // 更新輸入框狀態
        destinationInput.setEnabled(!isNavigating);
    }
    
    // 移除了Google Maps相關的回調方法
    
    @Override
    protected void announcePageTitle() {
        new android.os.Handler().postDelayed(() -> {
            String cantoneseText = "智能導航系統。請輸入目的地開始導航。";
            String englishText = "Smart Navigation System. Please enter destination to start navigation.";
            ttsManager.speak(cantoneseText, englishText, true);
        }, 500);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isNavigating) {
            navigationManager.stopNavigation();
        }
        if (locationManager != null) {
            locationManager.cleanup();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // 暫停位置更新以節省電量
        if (locationManager != null) {
            locationManager.stopLocationUpdates();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 恢復位置更新
        if (permissionHelper.canUseLocation() && !isNavigating) {
            startLocationUpdates();
        }
    }
}
