package com.example.tonbo_app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;

/**
 * 手勢管理Activity
 * 用於創建、管理和使用手勢
 */
public class GestureManagementActivity extends BaseAccessibleActivity {
    private static final String TAG = "GestureManagement";
    
    private GestureDrawView gestureDrawView;
    private Button clearButton;
    private Button backButton;
    private TextView pageTitle;
    private LinearLayout savedGesturesList;
    
    private GestureRecognitionManager gestureManager;
    private TTSManager ttsManager;
    private VibrationManager vibrationManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gesture_management);
        
        gestureManager = GestureRecognitionManager.getInstance(this);
        ttsManager = TTSManager.getInstance(this);
        vibrationManager = VibrationManager.getInstance(this);
        
        initViews();
        setupListeners();
        updateLanguageUI();
        loadSavedGestures();
        announcePageTitle();
    }
    
    private void initViews() {
        gestureDrawView = findViewById(R.id.gesture_draw_view);
        clearButton = findViewById(R.id.clear_button);
        backButton = findViewById(R.id.back_button);
        pageTitle = findViewById(R.id.page_title);
        savedGesturesList = findViewById(R.id.saved_gestures_list);
    }
    
    private void setupListeners() {
        // 返回按鈕
        backButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            finish();
        });
        
        // 清除手勢
        clearButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            gestureDrawView.clear();
            announceInfo("手勢已清除");
        });
        
        // 手勢繪畫完成檢測（簡化實現）- 使用延遲檢測
        gestureDrawView.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                // 手勢繪畫完成，延遲檢查
                new android.os.Handler().postDelayed(() -> {
                    if (gestureDrawView.hasDrawing()) {
                        checkAndRecognizeGesture();
                    }
                }, 500);
            }
            return false;
        });
    }
    
    /**
     * 檢查並識別手勢
     */
    private void checkAndRecognizeGesture() {
        if (!gestureDrawView.hasDrawing()) {
            return;
        }
        
        // 識別手勢
        String recognizedGesture = gestureManager.recognizeGesture(
            gestureDrawView.getPaths()
        );
        
        if (recognizedGesture != null) {
            String functionName = gestureManager.getFunctionForGesture(recognizedGesture);
            
            if (functionName != null) {
                announceInfo("識別到手勢：" + recognizedGesture + "，跳轉到：" + functionName);
                
                // 根據功能名稱跳轉
                navigateToFunction(functionName);
            }
        } else {
            // 顯示創建新手勢的選項
            showCreateGestureDialog();
        }
    }
    
    /**
     * 顯示創建新手勢對話框
     */
    private void showCreateGestureDialog() {
        announceInfo("未識別到已保存的手勢，是否創建新手勢？");
        
        // TODO: 顯示創建手勢對話框，讓用戶選擇綁定功能
        // 簡化實現：直接提示
        Toast.makeText(this, "請繪製手勢並綁定功能", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 導航到指定功能
     */
    private void navigateToFunction(String functionName) {
        Intent intent = null;
        
        switch (functionName) {
            case "尋找物品":
            case "Find Items":
            case "FindItems":
                intent = new Intent(this, FindItemsActivity.class);
                break;
                
            case "環境識別":
            case "Environment":
            case "EnvironmentActivity":
                intent = new Intent(this, EnvironmentActivity.class);
                break;
                
            case "文檔助手":
            case "Document":
            case "DocumentAssistant":
                intent = new Intent(this, DocumentCurrencyActivity.class);
                break;
                
            case "出行協助":
            case "Travel":
            case "TravelAssistant":
                intent = new Intent(this, TravelAssistantActivity.class);
                break;
                
            case "即時協助":
            case "InstantAssistance":
                intent = new Intent(this, InstantAssistanceActivity.class);
                break;
                
            default:
                Log.w(TAG, "未知功能: " + functionName);
                break;
        }
        
        if (intent != null) {
            startActivity(intent);
        }
    }
    
    /**
     * 載入已保存的手勢列表
     */
    private void loadSavedGestures() {
        savedGesturesList.removeAllViews();
        
        Map<String, String> gestures = gestureManager.getAllGestures();
        
        for (Map.Entry<String, String> entry : gestures.entrySet()) {
            addGestureItem(entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * 添加手勢項到列表
     */
    private void addGestureItem(String gestureName, String functionName) {
        View itemView = getLayoutInflater().inflate(R.layout.item_gesture, null);
        
        TextView gestureNameText = itemView.findViewById(R.id.gesture_name);
        TextView functionNameText = itemView.findViewById(R.id.function_name);
        Button deleteButton = itemView.findViewById(R.id.delete_button);
        
        gestureNameText.setText("手勢: " + gestureName);
        functionNameText.setText("功能: " + functionName);
        
        deleteButton.setOnClickListener(v -> {
            gestureManager.deleteGesture(gestureName);
            loadSavedGestures();
            announceInfo("已刪除手勢：" + gestureName);
        });
        
        savedGesturesList.addView(itemView);
    }
    
    /**
     * 更新語言UI
     */
    private void updateLanguageUI() {
        if (pageTitle != null) {
            String title = getLocalizedString("gesture_management_title");
            pageTitle.setText(title);
        }
        
        if (clearButton != null) {
            String text = getLocalizedString("clear");
            clearButton.setText(text);
        }
    }
    
    /**
     * 獲取本地化字符串
     */
    private String getLocalizedString(String key) {
        String currentLang = LocaleManager.getInstance(this).getCurrentLanguage();
        
        switch (key) {
            case "gesture_management_title":
                if ("english".equals(currentLang)) {
                    return "Gesture Management";
                } else if ("mandarin".equals(currentLang)) {
                    return "手势管理";
                } else {
                    return "手勢管理";
                }
            case "clear":
                if ("english".equals(currentLang)) {
                    return "Clear";
                } else if ("mandarin".equals(currentLang)) {
                    return "清除";
                } else {
                    return "清除";
                }
            default:
                return key;
        }
    }
    
    @Override
    protected void announcePageTitle() {
        new android.os.Handler().postDelayed(() -> {
            String description = getLocalizedString("gesture_management_title") + "頁面。可以繪製手勢並綁定功能。";
            ttsManager.speak(description, null, true);
        }, 500);
    }
}
