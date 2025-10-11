package com.example.tonbo_app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DocumentCurrencyActivity extends BaseAccessibleActivity {
    private Button startScanButton;
    private Button backButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupUI();
        announceNavigation("閱讀助手功能正在開發中，請稍後返回主頁");
    }
    
    @Override
    protected void announcePageTitle() {
        announcePageTitle("閱讀助手頁面");
    }
    
    private void setupUI() {
        // 創建簡單的UI布局
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);
        
        // 標題
        TextView title = new TextView(this);
        title.setText("閱讀助手");
        title.setTextSize(24);
        title.setContentDescription("閱讀助手功能頁面標題");
        layout.addView(title);
        
        // 描述
        TextView desc = new TextView(this);
        desc.setText("此功能正在開發中，將能夠掃描文件和識別貨幣");
        desc.setTextSize(16);
        desc.setContentDescription("功能描述：此功能正在開發中，將能夠掃描文件和識別貨幣");
        layout.addView(desc);
        
        // 開始掃描按鈕
        startScanButton = new Button(this);
        startScanButton.setText("開始掃描");
        startScanButton.setContentDescription("開始掃描文件按鈕");
        startScanButton.setOnClickListener(v -> {
            announceInfo("閱讀助手功能正在開發中");
        });
        provideTouchFeedback(startScanButton);
        layout.addView(startScanButton);
        
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
