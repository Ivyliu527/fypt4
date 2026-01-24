package com.example.tonbo_app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 環境識別控制器
 * 負責處理相機拍照、獲取圖像、圖像識別和語音播報等環境識別相關功能
 * 
 * <p><b>權限要求：</b></p>
 * <ul>
 *   <li>android.permission.CAMERA：用於拍照功能</li>
 * </ul>
 * 
 * <p>請確保在 AndroidManifest.xml 中聲明權限：</p>
 * <pre>{@code
 * <uses-permission android:name="android.permission.CAMERA" />
 * }</pre>
 * 
 * <p>如果目標 SDK >= 23，還需要在運行時請求權限。</p>
 * 
 * <p><b>使用示例：</b></p>
 * <pre>{@code
 * // 在 Activity 中初始化
 * EnvironmentRecognitionController controller = 
 *     new EnvironmentRecognitionController(this);
 * 
 * // 設置監聽器
 * controller.setEnvironmentRecognitionListener(
 *     new EnvironmentRecognitionController.EnvironmentRecognitionListener() {
 *         @Override
 *         public void onImageCaptured(Bitmap bitmap) {
 *             // 處理圖片
 *         }
 *         
 *         @Override
 *         public void onError(String error) {
 *             // 處理錯誤
 *         }
 *     }
 * );
 * 
 * // 開始識別
 * controller.startRecognition();
 * 
 * // 在 onDestroy() 中清理資源
 * @Override
 * protected void onDestroy() {
 *     super.onDestroy();
 *     if (controller != null) {
 *         controller.cleanup();
 *     }
 * }
 * }</pre>
 */
public class EnvironmentRecognitionController {
    private static final String TAG = "EnvironmentRecognition";
    
    private AppCompatActivity activity;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private EnvironmentRecognitionListener listener;
    
    // 臨時文件用於保存拍照結果
    private File tempImageFile;
    private Uri imageUri;
    
    // ML Kit Image Labeling
    private com.google.mlkit.vision.label.ImageLabeler imageLabeler;
    
    // 標籤到中文描述的映射表
    private Map<String, String> labelToDescriptionMap;
    
    // TTS 管理器
    private TTSManager ttsManager;
    
    /**
     * 環境識別監聽器接口
     */
    public interface EnvironmentRecognitionListener {
        /**
         * 拍照成功，返回 Bitmap
         * @param bitmap 拍攝的圖片
         */
        void onImageCaptured(Bitmap bitmap);
        
        /**
         * 拍照失敗或取消
         * @param error 錯誤信息
         */
        void onError(String error);
    }
    
    /**
     * 構造函數
     * @param activity 用於註冊 ActivityResultLauncher 的 Activity
     * 
     * 注意：使用此控制器需要以下權限：
     * - android.permission.CAMERA：用於拍照功能
     * 請確保在 AndroidManifest.xml 中聲明並在運行時請求權限
     */
    public EnvironmentRecognitionController(AppCompatActivity activity) {
        this.activity = activity;
        initializeCameraLauncher();
        initializeImageLabeler();
        initializeLabelMapping();
        initializeTTSManager();
    }
    
    /**
     * 初始化 TTS 管理器
     */
    private void initializeTTSManager() {
        ttsManager = TTSManager.getInstance(activity);
        Log.d(TAG, "TTS 管理器初始化完成");
    }
    
    /**
     * 初始化標籤到中文描述的映射表
     */
    private void initializeLabelMapping() {
        labelToDescriptionMap = new HashMap<>();
        
        // 道路相關
        labelToDescriptionMap.put("road", "前方是道路");
        labelToDescriptionMap.put("street", "前方是道路");
        labelToDescriptionMap.put("highway", "前方是道路");
        labelToDescriptionMap.put("sidewalk", "前方是人行道");
        labelToDescriptionMap.put("pavement", "前方是人行道");
        labelToDescriptionMap.put("crosswalk", "前方是斑馬線");
        labelToDescriptionMap.put("intersection", "前方是路口");
        
        // 車輛相關
        labelToDescriptionMap.put("car", "前方有車輛");
        labelToDescriptionMap.put("vehicle", "前方有車輛");
        labelToDescriptionMap.put("automobile", "前方有車輛");
        labelToDescriptionMap.put("truck", "前方有卡車");
        labelToDescriptionMap.put("bus", "前方有巴士");
        labelToDescriptionMap.put("motorcycle", "前方有摩托車");
        labelToDescriptionMap.put("bicycle", "前方有自行車");
        labelToDescriptionMap.put("bike", "前方有自行車");
        
        // 人員相關
        labelToDescriptionMap.put("person", "前方有行人");
        labelToDescriptionMap.put("people", "前方有行人");
        labelToDescriptionMap.put("pedestrian", "前方有行人");
        labelToDescriptionMap.put("crowd", "前方有人群");
        
        // 建築物相關
        labelToDescriptionMap.put("building", "前方有建築物");
        labelToDescriptionMap.put("house", "前方有房屋");
        labelToDescriptionMap.put("skyscraper", "前方有高樓");
        labelToDescriptionMap.put("office building", "前方有辦公樓");
        labelToDescriptionMap.put("store", "前方有商店");
        labelToDescriptionMap.put("shop", "前方有商店");
        
        // 自然環境
        labelToDescriptionMap.put("tree", "前方有樹木");
        labelToDescriptionMap.put("grass", "前方是草地");
        labelToDescriptionMap.put("park", "前方是公園");
        labelToDescriptionMap.put("garden", "前方是花園");
        labelToDescriptionMap.put("water", "前方有水");
        labelToDescriptionMap.put("lake", "前方有湖泊");
        labelToDescriptionMap.put("river", "前方有河流");
        
        // 障礙物
        labelToDescriptionMap.put("pole", "前方有柱子");
        labelToDescriptionMap.put("sign", "前方有標誌");
        labelToDescriptionMap.put("fence", "前方有圍欄");
        labelToDescriptionMap.put("barrier", "前方有障礙物");
        labelToDescriptionMap.put("wall", "前方有牆壁");
        
        // 其他常見物體
        labelToDescriptionMap.put("door", "前方有門");
        labelToDescriptionMap.put("window", "前方有窗戶");
        labelToDescriptionMap.put("stairs", "前方有樓梯");
        labelToDescriptionMap.put("elevator", "前方有電梯");
        labelToDescriptionMap.put("bench", "前方有長椅");
        labelToDescriptionMap.put("table", "前方有桌子");
        labelToDescriptionMap.put("chair", "前方有椅子");
        
        Log.d(TAG, "標籤映射表初始化完成，共 " + labelToDescriptionMap.size() + " 個映射");
    }
    
    /**
     * 初始化圖像標籤識別器（使用本地模型）
     */
    private void initializeImageLabeler() {
        // 使用本地模型進行圖像標籤識別
        ImageLabelerOptions options = new ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.5f) // 設置置信度閾值
                .build();
        
        imageLabeler = ImageLabeling.getClient(options);
        Log.d(TAG, "ML Kit Image Labeling 初始化完成（本地模型）");
    }
    
    /**
     * 初始化相機啟動器
     */
    private void initializeCameraLauncher() {
        cameraLauncher = activity.registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    handleCameraResult(result);
                }
            }
        );
        
        Log.d(TAG, "相機啟動器初始化完成");
    }
    
    /**
     * 開始環境識別（拍照）
     * 這是外部調用的主要入口方法
     */
    public void startRecognition() {
        Log.d(TAG, "開始環境識別 - 啟動相機");
        
        // 檢查相機是否可用
        if (!isCameraAvailable()) {
            String errorMsg = "相機不可用";
            Log.e(TAG, errorMsg);
            if (listener != null) {
                listener.onError(errorMsg);
            }
            return;
        }
        
        // 創建拍照 Intent
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        
        // 檢查是否有應用可以處理拍照 Intent
        if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
            try {
                // 創建臨時文件保存拍照結果
                tempImageFile = createTempImageFile();
                if (tempImageFile != null) {
                    imageUri = Uri.fromFile(tempImageFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    
                    // 啟動相機
                    cameraLauncher.launch(takePictureIntent);
                    Log.d(TAG, "相機已啟動，等待拍照結果");
                } else {
                    String errorMsg = "無法創建臨時圖片文件";
                    Log.e(TAG, errorMsg);
                    if (listener != null) {
                        listener.onError(errorMsg);
                    }
                }
            } catch (Exception e) {
                String errorMsg = "啟動相機失敗: " + e.getMessage();
                Log.e(TAG, errorMsg, e);
                if (listener != null) {
                    listener.onError(errorMsg);
                }
            }
        } else {
            String errorMsg = "沒有可用的相機應用";
            Log.e(TAG, errorMsg);
            if (listener != null) {
                listener.onError(errorMsg);
            }
        }
    }
    
    /**
     * 處理相機拍照結果
     */
    private void handleCameraResult(ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_OK) {
            Log.d(TAG, "拍照成功，處理圖片");
            
            // 從 Intent 中獲取縮略圖（如果有的話）
            Intent data = result.getData();
            Bitmap thumbnailBitmap = null;
            
            if (data != null && data.getExtras() != null) {
                thumbnailBitmap = (Bitmap) data.getExtras().get("data");
            }
            
            // 優先使用保存的文件，如果沒有則使用縮略圖
            Bitmap finalBitmap = null;
            
            if (tempImageFile != null && tempImageFile.exists()) {
                try {
                    // 從文件讀取完整圖片
                    finalBitmap = BitmapFactory.decodeFile(tempImageFile.getAbsolutePath());
                    Log.d(TAG, "從文件讀取圖片成功: " + tempImageFile.getAbsolutePath());
                } catch (Exception e) {
                    Log.e(TAG, "讀取圖片文件失敗: " + e.getMessage(), e);
                }
            }
            
            // 如果文件讀取失敗，使用縮略圖
            if (finalBitmap == null && thumbnailBitmap != null) {
                finalBitmap = thumbnailBitmap;
                Log.d(TAG, "使用縮略圖");
            }
            
            // 通知監聽器
            if (finalBitmap != null) {
                if (listener != null) {
                    listener.onImageCaptured(finalBitmap);
                }
                Log.d(TAG, "圖片處理完成，尺寸: " + finalBitmap.getWidth() + "x" + finalBitmap.getHeight());
                
                // 進行圖像識別
                performImageLabeling(finalBitmap);
            } else {
                String errorMsg = "無法獲取圖片";
                Log.e(TAG, errorMsg);
                if (listener != null) {
                    listener.onError(errorMsg);
                }
            }
            
            // 清理臨時文件
            cleanupTempFile();
            
        } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
            Log.d(TAG, "用戶取消了拍照");
            if (listener != null) {
                listener.onError("拍照已取消");
            }
            cleanupTempFile();
        } else {
            String errorMsg = "拍照失敗，結果碼: " + result.getResultCode();
            Log.e(TAG, errorMsg);
            if (listener != null) {
                listener.onError(errorMsg);
            }
            cleanupTempFile();
        }
    }
    
    /**
     * 創建臨時圖片文件
     */
    private File createTempImageFile() {
        try {
            // 使用應用緩存目錄
            File cacheDir = activity.getCacheDir();
            String imageFileName = "environment_recognition_" + System.currentTimeMillis() + ".jpg";
            File imageFile = new File(cacheDir, imageFileName);
            
            // 確保父目錄存在
            if (!imageFile.getParentFile().exists()) {
                imageFile.getParentFile().mkdirs();
            }
            
            // 創建文件
            if (imageFile.createNewFile()) {
                Log.d(TAG, "臨時圖片文件創建成功: " + imageFile.getAbsolutePath());
                return imageFile;
            } else {
                Log.e(TAG, "無法創建臨時圖片文件");
                return null;
            }
        } catch (IOException e) {
            Log.e(TAG, "創建臨時圖片文件失敗: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 清理臨時文件
     */
    private void cleanupTempFile() {
        if (tempImageFile != null && tempImageFile.exists()) {
            boolean deleted = tempImageFile.delete();
            if (deleted) {
                Log.d(TAG, "臨時文件已刪除: " + tempImageFile.getAbsolutePath());
            } else {
                Log.w(TAG, "無法刪除臨時文件: " + tempImageFile.getAbsolutePath());
            }
            tempImageFile = null;
        }
        imageUri = null;
    }
    
    /**
     * 檢查相機是否可用
     */
    private boolean isCameraAvailable() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        return takePictureIntent.resolveActivity(activity.getPackageManager()) != null;
    }
    
    /**
     * 設置環境識別監聽器
     * @param listener 監聽器
     */
    public void setEnvironmentRecognitionListener(EnvironmentRecognitionListener listener) {
        this.listener = listener;
    }
    
    /**
     * 執行圖像標籤識別
     * @param bitmap 要識別的圖片
     */
    private void performImageLabeling(Bitmap bitmap) {
        if (imageLabeler == null) {
            Log.e(TAG, "圖像標籤識別器未初始化");
            return;
        }
        
        try {
            // 將 Bitmap 轉換為 InputImage
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            Log.d(TAG, "開始進行圖像標籤識別...");
            
            // 執行識別
            imageLabeler.process(image)
                    .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<List<ImageLabel>>() {
                        @Override
                        public void onSuccess(List<ImageLabel> labels) {
                            // 識別成功，打印結果到 Log
                            Log.d(TAG, "圖像識別成功，識別到 " + labels.size() + " 個標籤：");
                            
                            for (ImageLabel label : labels) {
                                String text = label.getText();
                                float confidence = label.getConfidence();
                                int index = label.getIndex();
                                
                                Log.d(TAG, String.format(
                                    "  標籤[%d]: %s (置信度: %.2f%%)",
                                    index, text, confidence * 100
                                ));
                            }
                            
                            // 打印前3個最可能的標籤
                            if (!labels.isEmpty()) {
                                Log.d(TAG, "--- 主要識別結果 ---");
                                int count = Math.min(3, labels.size());
                                for (int i = 0; i < count; i++) {
                                    ImageLabel label = labels.get(i);
                                    Log.d(TAG, String.format(
                                        "%d. %s (%.1f%%)",
                                        i + 1, label.getText(), label.getConfidence() * 100
                                    ));
                                }
                                
                                // 生成中文環境描述
                                String description = generateChineseDescription(labels);
                                Log.d(TAG, "--- 環境描述 ---");
                                Log.d(TAG, description);
                                
                                // 使用 TTS 播報環境描述
                                announceEnvironmentDescription(description);
                            }
                        }
                    })
                    .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "圖像識別失敗: " + e.getMessage(), e);
                        }
                    });
                    
        } catch (Exception e) {
            Log.e(TAG, "圖像識別處理失敗: " + e.getMessage(), e);
        }
    }
    
    /**
     * 生成中文環境描述
     * 將識別到的英文標籤映射為適合視障用戶的中文描述
     * 
     * @param labels 識別到的標籤列表
     * @return 完整、自然的中文環境描述字符串
     */
    private String generateChineseDescription(List<ImageLabel> labels) {
        if (labels == null || labels.isEmpty()) {
            return "未識別到環境信息";
        }
        
        // 過濾高置信度的標籤（置信度 >= 0.5）
        List<String> descriptions = new ArrayList<>();
        List<String> processedLabels = new ArrayList<>(); // 避免重複處理相同類型的標籤
        
        for (ImageLabel label : labels) {
            if (label.getConfidence() < 0.5f) {
                continue; // 跳過低置信度標籤
            }
            
            String labelText = label.getText().toLowerCase().trim();
            
            // 檢查是否已經處理過相同類型的標籤
            boolean isDuplicate = false;
            for (String processed : processedLabels) {
                if (isSimilarLabel(labelText, processed)) {
                    isDuplicate = true;
                    break;
                }
            }
            
            if (isDuplicate) {
                continue;
            }
            
            // 查找映射
            String description = findDescriptionForLabel(labelText);
            if (description != null && !description.isEmpty()) {
                descriptions.add(description);
                processedLabels.add(labelText);
            }
        }
        
        // 生成完整描述
        if (descriptions.isEmpty()) {
            return "環境識別完成，但未識別到常見物體";
        }
        
        return combineDescriptions(descriptions);
    }
    
    /**
     * 查找標籤對應的中文描述
     * 
     * @param labelText 標籤文本（小寫）
     * @return 中文描述，如果未找到則返回 null
     */
    private String findDescriptionForLabel(String labelText) {
        // 直接查找
        if (labelToDescriptionMap.containsKey(labelText)) {
            return labelToDescriptionMap.get(labelText);
        }
        
        // 部分匹配（處理複合標籤，如 "office building"）
        for (Map.Entry<String, String> entry : labelToDescriptionMap.entrySet()) {
            if (labelText.contains(entry.getKey()) || entry.getKey().contains(labelText)) {
                return entry.getValue();
            }
        }
        
        return null;
    }
    
    /**
     * 判斷兩個標籤是否相似（避免重複描述）
     * 
     * @param label1 標籤1
     * @param label2 標籤2
     * @return 是否相似
     */
    private boolean isSimilarLabel(String label1, String label2) {
        // 定義相似標籤組
        String[][] similarGroups = {
            {"road", "street", "highway"},
            {"car", "vehicle", "automobile"},
            {"person", "people", "pedestrian"},
            {"building", "house", "skyscraper"},
            {"sidewalk", "pavement"},
            {"bicycle", "bike"},
            {"store", "shop"}
        };
        
        for (String[] group : similarGroups) {
            boolean inGroup1 = false;
            boolean inGroup2 = false;
            
            for (String item : group) {
                if (label1.contains(item) || item.contains(label1)) {
                    inGroup1 = true;
                }
                if (label2.contains(item) || item.contains(label2)) {
                    inGroup2 = true;
                }
            }
            
            if (inGroup1 && inGroup2) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 組合多個描述為一條完整、自然的中文描述
     * 
     * @param descriptions 描述列表
     * @return 組合後的完整描述
     */
    private String combineDescriptions(List<String> descriptions) {
        if (descriptions.size() == 1) {
            return descriptions.get(0);
        }
        
        if (descriptions.size() == 2) {
            return descriptions.get(0) + "，" + descriptions.get(1);
        }
        
        // 3個或更多描述
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < descriptions.size(); i++) {
            if (i == 0) {
                sb.append(descriptions.get(i));
            } else if (i == descriptions.size() - 1) {
                sb.append("，還有").append(descriptions.get(i));
            } else {
                sb.append("，").append(descriptions.get(i));
            }
        }
        
        return sb.toString();
    }
    
    /**
     * 播報環境描述
     * 使用 TTS 管理器將環境描述轉換為語音播報
     * 
     * @param description 環境描述文本
     */
    private void announceEnvironmentDescription(String description) {
        if (ttsManager == null || description == null || description.trim().isEmpty()) {
            Log.w(TAG, "無法播報環境描述：TTS管理器未初始化或描述為空");
            return;
        }
        
        // 獲取當前語言設置
        String currentLang = LocaleManager.getInstance(activity).getCurrentLanguage();
        
        // 根據語言設置播報內容
        if ("english".equals(currentLang)) {
            // 英文模式：將中文描述翻譯為英文（簡化處理）
            String englishDescription = translateToEnglish(description);
            ttsManager.speak(null, englishDescription, false);
        } else if ("mandarin".equals(currentLang)) {
            // 普通話模式：直接播報中文描述
            ttsManager.speak(description, null, false);
        } else {
            // 廣東話模式（默認）：直接播報中文描述
            ttsManager.speak(description, null, false);
        }
        
        Log.d(TAG, "環境描述已發送到 TTS 播報");
    }
    
    /**
     * 將中文描述翻譯為英文（簡化版本）
     * 注意：這是簡化實現，實際應用中可以集成專業翻譯服務
     * 
     * @param chineseDescription 中文描述
     * @return 英文描述
     */
    private String translateToEnglish(String chineseDescription) {
        // 簡化的翻譯映射（實際應用中可以使用專業翻譯 API）
        String translation = chineseDescription
            .replace("前方是道路", "Road ahead")
            .replace("前方是人行道", "Sidewalk ahead")
            .replace("前方是斑馬線", "Crosswalk ahead")
            .replace("前方是路口", "Intersection ahead")
            .replace("前方有車輛", "Vehicle ahead")
            .replace("前方有卡車", "Truck ahead")
            .replace("前方有巴士", "Bus ahead")
            .replace("前方有摩托車", "Motorcycle ahead")
            .replace("前方有自行車", "Bicycle ahead")
            .replace("前方有行人", "Pedestrian ahead")
            .replace("前方有人群", "Crowd ahead")
            .replace("前方有建築物", "Building ahead")
            .replace("前方有房屋", "House ahead")
            .replace("前方有高樓", "Skyscraper ahead")
            .replace("前方有辦公樓", "Office building ahead")
            .replace("前方有商店", "Store ahead")
            .replace("前方有樹木", "Tree ahead")
            .replace("前方是草地", "Grass ahead")
            .replace("前方是公園", "Park ahead")
            .replace("前方是花園", "Garden ahead")
            .replace("前方有水", "Water ahead")
            .replace("前方有湖泊", "Lake ahead")
            .replace("前方有河流", "River ahead")
            .replace("前方有柱子", "Pole ahead")
            .replace("前方有標誌", "Sign ahead")
            .replace("前方有圍欄", "Fence ahead")
            .replace("前方有障礙物", "Barrier ahead")
            .replace("前方有牆壁", "Wall ahead")
            .replace("前方有門", "Door ahead")
            .replace("前方有窗戶", "Window ahead")
            .replace("前方有樓梯", "Stairs ahead")
            .replace("前方有電梯", "Elevator ahead")
            .replace("前方有長椅", "Bench ahead")
            .replace("前方有桌子", "Table ahead")
            .replace("前方有椅子", "Chair ahead")
            .replace("，", ", ")
            .replace("還有", "and ");
        
        return translation;
    }
    
    /**
     * 清理資源
     * 在 Activity 銷毀時調用，確保正確釋放所有資源
     * 
     * 注意：此方法必須在宿主 Activity 的 onDestroy() 中調用
     */
    public void cleanup() {
        Log.d(TAG, "開始清理環境識別控制器資源");
        
        // 清理臨時文件
        cleanupTempFile();
        
        // 關閉圖像標籤識別器
        if (imageLabeler != null) {
            try {
                imageLabeler.close();
                imageLabeler = null;
                Log.d(TAG, "圖像標籤識別器已關閉");
            } catch (Exception e) {
                Log.e(TAG, "關閉圖像標籤識別器時出錯: " + e.getMessage(), e);
            }
        }
        
        // 清理 TTS（不需要關閉，因為是單例）
        // TTSManager 由應用統一管理，不需要在這裡關閉
        
        // 清理引用
        listener = null;
        activity = null;
        labelToDescriptionMap = null;
        
        Log.d(TAG, "環境識別控制器資源已清理完成");
    }
}

