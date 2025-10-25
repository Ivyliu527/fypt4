package com.example.tonbo_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * 尋找物品頁面
 * 支持標記物品和尋找已標記的物品
 */
public class FindItemsActivity extends BaseAccessibleActivity {
    private static final String TAG = "FindItemsActivity";
    private static final String PREFS_NAME = "find_items";
    private static final String KEY_ITEMS = "marked_items";

    // UI組件
    private PreviewView cameraPreview;
    private ImageView capturedImage;
    private EditText itemNameInput;
    private Button captureButton;
    private Button markItemButton;
    private Button findItemButton;
    private Button backButton;
    private TextView statusText;
    private LinearLayout itemsListLayout;
    private TextView itemsListTitle;
    private TextView pageTitle;

    // 相機相關
    private ImageCapture imageCapture;
    private Camera camera;
    private File currentImageFile;

    // 數據存儲
    private SharedPreferences prefs;
    private List<MarkedItem> markedItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_items);

        // 初始化SharedPreferences
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        markedItems = new ArrayList<>();
        loadMarkedItems();

        initViews();
        setupCamera();
        updateItemsList();
        
        // 播放頁面進入語音
        announcePageTitle();
    }

    private void initViews() {
        cameraPreview = findViewById(R.id.camera_preview);
        capturedImage = findViewById(R.id.captured_image);
        itemNameInput = findViewById(R.id.item_name_input);
        captureButton = findViewById(R.id.capture_button);
        markItemButton = findViewById(R.id.mark_item_button);
        findItemButton = findViewById(R.id.find_item_button);
        backButton = findViewById(R.id.back_button);
        statusText = findViewById(R.id.status_text);
        itemsListLayout = findViewById(R.id.items_list_layout);
        itemsListTitle = findViewById(R.id.items_list_title);
        pageTitle = findViewById(R.id.page_title);

        // 設置按鈕點擊事件
        backButton.setOnClickListener(v -> {
            ttsManager.speak(null, getString(R.string.going_back_to_home), true);
            finish();
        });

        captureButton.setOnClickListener(v -> capturePhoto());
        markItemButton.setOnClickListener(v -> markItem());
        findItemButton.setOnClickListener(v -> findItem());

        // 設置內容描述
        setupAccessibility();
        
        // 根據當前語言更新界面文字
        updateLanguageUI();
    }
    
    /**
     * 更新語言UI
     */
    private void updateLanguageUI() {
        if (pageTitle != null) {
            pageTitle.setText(getLocalizedString("find_items_title"));
        }
        
        if (captureButton != null) {
            captureButton.setText(getLocalizedString("capture_photo"));
        }
        
        if (markItemButton != null) {
            markItemButton.setText(getLocalizedString("mark_item"));
        }
        
        if (findItemButton != null) {
            findItemButton.setText(getLocalizedString("find_items"));
        }
        
        if (statusText != null) {
            statusText.setText(getLocalizedString("camera_ready"));
        }
        
        if (itemNameInput != null) {
            itemNameInput.setHint(getLocalizedString("enter_item_name"));
        }
        
        // 更新物品名稱標籤
        TextView itemNameLabel = findViewById(R.id.item_name_label);
        if (itemNameLabel != null) {
            itemNameLabel.setText(getLocalizedString("item_name_label"));
        }
    }
    
    /**
     * 根據當前語言獲取本地化字符串
     */
    private String getLocalizedString(String key) {
        switch (key) {
            case "find_items_title":
                if ("english".equals(currentLanguage)) {
                    return "Find Items";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "查找物品";
                } else {
                    return "尋找物品";
                }
            case "capture_photo":
                if ("english".equals(currentLanguage)) {
                    return "Capture Photo";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "拍照";
                } else {
                    return "拍照";
                }
            case "mark_item":
                if ("english".equals(currentLanguage)) {
                    return "Mark Item";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "标记物品";
                } else {
                    return "標記物品";
                }
            case "find_items":
                if ("english".equals(currentLanguage)) {
                    return "Find Items";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "查找物品";
                } else {
                    return "尋找物品";
                }
            case "camera_ready":
                if ("english".equals(currentLanguage)) {
                    return "Camera ready";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "相机就绪";
                } else {
                    return "相機就緒";
                }
            case "enter_item_name":
                if ("english".equals(currentLanguage)) {
                    return "Enter item name";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "输入物品名称";
                } else {
                    return "輸入物品名稱";
                }
            case "item_name_label":
                if ("english".equals(currentLanguage)) {
                    return "Item Name:";
                } else if ("mandarin".equals(currentLanguage)) {
                    return "物品名称：";
                } else {
                    return "物品名稱：";
                }
            default:
                return "";
        }
    }
    
    private void setupAccessibility() {
        cameraPreview.setContentDescription(getString(R.string.camera_preview_desc));
        capturedImage.setContentDescription(getString(R.string.captured_image_desc));
        itemNameInput.setContentDescription(getString(R.string.item_name_input_desc));
        captureButton.setContentDescription(getString(R.string.capture_button_desc));
        markItemButton.setContentDescription(getString(R.string.mark_item_button_desc));
        findItemButton.setContentDescription(getString(R.string.find_item_button_desc));
        backButton.setContentDescription(getString(R.string.back_button_desc));
        statusText.setContentDescription(getString(R.string.status_text_desc));
    }

    private void setupCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
            ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                
                Preview preview = new Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .build();

                imageCapture = new ImageCapture.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture);
                
                Log.d(TAG, "相機初始化成功");
                updateStatus(getString(R.string.camera_ready));
                
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "相機初始化失敗: " + e.getMessage());
                updateStatus(getString(R.string.camera_setup_failed));
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void capturePhoto() {
        if (imageCapture == null) {
            updateStatus(getString(R.string.camera_not_ready_status));
            return;
        }

        try {
            // 創建圖片文件
            currentImageFile = createImageFile();
            
            ImageCapture.OutputFileOptions outputOptions = 
                new ImageCapture.OutputFileOptions.Builder(currentImageFile).build();

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(ImageCapture.OutputFileResults output) {
                        Log.d(TAG, "照片拍攝成功: " + currentImageFile.getAbsolutePath());
                        displayCapturedImage();
                        updateStatus(getString(R.string.photo_captured_successfully));
                        
                        // 播放語音確認
                        ttsManager.speak(null, getString(R.string.photo_captured_successfully), true);
                    }

                    @Override
                    public void onError(ImageCaptureException exception) {
                        Log.e(TAG, "照片拍攝失敗: " + exception.getMessage());
                        updateStatus(getString(R.string.photo_capture_failed));
                        ttsManager.speak(null, getString(R.string.photo_capture_failed), true);
                    }
                }
            );
        } catch (Exception e) {
            Log.e(TAG, "創建圖片文件失敗: " + e.getMessage());
            updateStatus(getString(R.string.photo_capture_failed));
        }
    }

    private void displayCapturedImage() {
        if (currentImageFile != null && currentImageFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(currentImageFile.getAbsolutePath());
            if (bitmap != null) {
                capturedImage.setImageBitmap(bitmap);
                capturedImage.setVisibility(View.VISIBLE);
                markItemButton.setEnabled(true);
                
                // 隱藏相機預覽
                cameraPreview.setVisibility(View.GONE);
            }
        }
    }

    private void markItem() {
        String itemName = itemNameInput.getText().toString().trim();
        
        if (itemName.isEmpty()) {
            updateStatus(getString(R.string.please_enter_item_name));
            ttsManager.speak(null, getString(R.string.please_enter_item_name), true);
            return;
        }

        if (currentImageFile == null || !currentImageFile.exists()) {
            updateStatus(getString(R.string.no_photo_to_mark));
            ttsManager.speak(null, getString(R.string.no_photo_to_mark), true);
            return;
        }

        // 創建標記物品
        MarkedItem item = new MarkedItem(itemName, currentImageFile.getAbsolutePath(), new Date());
        markedItems.add(item);
        saveMarkedItems();

        updateStatus(getString(R.string.item_marked_successfully, itemName));
        ttsManager.speak(null, getString(R.string.item_marked_successfully, itemName), true);
        
        // 重置界面
        resetCapture();
        updateItemsList();
    }

    private void findItem() {
        if (markedItems.isEmpty()) {
            updateStatus(getString(R.string.no_marked_items));
            ttsManager.speak(null, getString(R.string.no_marked_items), true);
            return;
        }

        // 顯示已標記物品列表
        showMarkedItemsList();
        
        String message = getString(R.string.found_items_count, markedItems.size());
        updateStatus(message);
        ttsManager.speak(null, message, true);
    }

    private void showMarkedItemsList() {
        itemsListLayout.removeAllViews();
        
        for (int i = 0; i < markedItems.size(); i++) {
            MarkedItem item = markedItems.get(i);
            
            // 創建物品項目視圖
            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            itemLayout.setPadding(16, 8, 16, 8);
            
            TextView nameText = new TextView(this);
            nameText.setText(item.getName());
            nameText.setTextSize(16);
            nameText.setTextColor(getResources().getColor(android.R.color.white));
            
            TextView dateText = new TextView(this);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            dateText.setText(sdf.format(item.getDate()));
            dateText.setTextSize(12);
            dateText.setTextColor(getResources().getColor(android.R.color.darker_gray));
            
            itemLayout.addView(nameText);
            itemLayout.addView(dateText);
            
            // 設置點擊事件
            final int index = i;
            itemLayout.setOnClickListener(v -> {
                selectItem(index);
            });
            
            itemsListLayout.addView(itemLayout);
        }
        
        itemsListLayout.setVisibility(View.VISIBLE);
        itemsListTitle.setText(getString(R.string.marked_items_list, markedItems.size()));
    }

    private void selectItem(int index) {
        if (index >= 0 && index < markedItems.size()) {
            MarkedItem item = markedItems.get(index);
            
            // 顯示選中的物品圖片
            displayItemImage(item);
            
            String message = getString(R.string.selected_item, item.getName());
            updateStatus(message);
            ttsManager.speak(null, message, true);
        }
    }

    private void displayItemImage(MarkedItem item) {
        File imageFile = new File(item.getImagePath());
        if (imageFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            if (bitmap != null) {
                capturedImage.setImageBitmap(bitmap);
                capturedImage.setVisibility(View.VISIBLE);
                cameraPreview.setVisibility(View.GONE);
            }
        }
    }

    private void updateItemsList() {
        if (!markedItems.isEmpty()) {
            itemsListTitle.setText(getString(R.string.marked_items_list, markedItems.size()));
            itemsListTitle.setVisibility(View.VISIBLE);
        } else {
            itemsListTitle.setVisibility(View.GONE);
        }
    }

    private void resetCapture() {
        capturedImage.setVisibility(View.GONE);
        cameraPreview.setVisibility(View.VISIBLE);
        itemNameInput.setText("");
        markItemButton.setEnabled(false);
        currentImageFile = null;
    }

    private void updateStatus(String message) {
        statusText.setText(message);
        Log.d(TAG, "狀態更新: " + message);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir("Pictures");
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        return image;
    }

    private void loadMarkedItems() {
        String itemsJson = prefs.getString(KEY_ITEMS, "");
        if (!itemsJson.isEmpty()) {
            // 簡單的JSON解析（實際應用中建議使用Gson等庫）
            try {
                markedItems.clear();
                String[] items = itemsJson.split("\\|");
                for (String itemStr : items) {
                    if (!itemStr.isEmpty()) {
                        String[] parts = itemStr.split("\\$");
                        if (parts.length >= 3) {
                            MarkedItem item = new MarkedItem(
                                parts[0], 
                                parts[1], 
                                new Date(Long.parseLong(parts[2]))
                            );
                            markedItems.add(item);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "載入標記物品失敗: " + e.getMessage());
                markedItems.clear();
            }
        }
    }

    private void saveMarkedItems() {
        StringBuilder sb = new StringBuilder();
        for (MarkedItem item : markedItems) {
            sb.append(item.getName()).append("$")
              .append(item.getImagePath()).append("$")
              .append(item.getDate().getTime()).append("|");
        }
        prefs.edit().putString(KEY_ITEMS, sb.toString()).apply();
    }

    @Override
    protected void announcePageTitle() {
        String title = getLocalizedString("find_items_title");
        String description;
        
        if ("english".equals(currentLanguage)) {
            description = "Find Items page. You can take photos, mark items, and find previously marked items.";
            ttsManager.speak(null, description, true);
        } else if ("mandarin".equals(currentLanguage)) {
            description = "查找物品页面。您可以拍照、标记物品，并查找之前标记的物品。";
            ttsManager.speak(description, null, true);
        } else {
            description = "尋找物品頁面。您可以拍照、標記物品，並查找之前標記的物品。";
            ttsManager.speak(description, "Find Items page. You can take photos, mark items, and find previously marked items.", true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (camera != null) {
            camera = null;
        }
    }

    /**
     * 標記物品數據類
     */
    private static class MarkedItem {
        private String name;
        private String imagePath;
        private Date date;

        public MarkedItem(String name, String imagePath, Date date) {
            this.name = name;
            this.imagePath = imagePath;
            this.date = date;
        }

        public String getName() { return name; }
        public String getImagePath() { return imagePath; }
        public Date getDate() { return date; }
    }
}
