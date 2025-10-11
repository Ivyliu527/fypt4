package com.example.tonbo_app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class EnvironmentActivity extends BaseAccessibleActivity {
    private Button startCameraButton;
    private Button backButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupUI();
        announceNavigation("環境識別功能正在開發中，請稍後返回主頁");
    }
    
    @Override
    protected void announcePageTitle() {
        announcePageTitle("環境識別頁面");
    }
    
    private void setupUI() {
        // 創建簡單的UI布局
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);
        
        // 標題
        TextView title = new TextView(this);
        title.setText("環境識別");
        title.setTextSize(24);
        title.setContentDescription("環境識別功能頁面標題");
        layout.addView(title);
        
        // 描述
        TextView desc = new TextView(this);
        desc.setText("此功能正在開發中，將能夠識別周圍環境和物體");
        desc.setTextSize(16);
        desc.setContentDescription("功能描述：此功能正在開發中，將能夠識別周圍環境和物體");
        layout.addView(desc);
        
        // 開始按鈕
        startCameraButton = new Button(this);
        startCameraButton.setText("開始識別");
        startCameraButton.setContentDescription("開始環境識別按鈕");
        startCameraButton.setOnClickListener(v -> {
            announceInfo("環境識別功能正在開發中");
        });
        provideTouchFeedback(startCameraButton);
        layout.addView(startCameraButton);
        
        // 返回按鈕
        backButton = new Button(this);
        backButton.setText("返回");
        backButton.setContentDescription("返回主頁按鈕");
        backButton.setOnClickListener(v -> {
            announceNavigation("返回主頁");
            finish();
        });
        provideTouchFeedback(backButton);
        layout.addView(backButton);
        
        setContentView(layout);
    }
}
