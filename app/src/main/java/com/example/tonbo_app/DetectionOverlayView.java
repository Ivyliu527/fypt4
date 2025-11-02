package com.example.tonbo_app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 檢測結果覆蓋層視圖
 * 在相機預覽上繪製檢測框和標籤
 */
public class DetectionOverlayView extends View {
    private static final String TAG = "DetectionOverlayView";
    
    private List<ObjectDetectorHelper.DetectionResult> detections = new ArrayList<>();
    private Paint boxPaint;
    private Paint textPaint;
    private Paint backgroundPaint;
    private String currentLanguage = "cantonese"; // 當前語言
    
    // 繪製參數 - 優化為更高精度和清晰度
    private static final int BOX_COLOR = Color.BLUE;
    private static final int BOX_COLOR_ALT = Color.MAGENTA;
    private static final int TEXT_COLOR = Color.WHITE;
    private static final int BACKGROUND_COLOR = Color.BLACK;
    private static final int BOX_THICKNESS = 6;  // 適中的邊界框粗細
    private static final int TEXT_SIZE = 28;    // 適中的文字大小
    private static final int TEXT_PADDING = 16; // 適中的文字內邊距
    
    public DetectionOverlayView(Context context) {
        super(context);
        init();
    }
    
    public DetectionOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public DetectionOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        // 初始化邊界框畫筆
        boxPaint = new Paint();
        boxPaint.setColor(BOX_COLOR);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(BOX_THICKNESS);
        boxPaint.setAntiAlias(true);
        
        // 初始化文字畫筆
        textPaint = new Paint();
        textPaint.setColor(TEXT_COLOR);
        textPaint.setTextSize(TEXT_SIZE);
        textPaint.setAntiAlias(true);
        textPaint.setFakeBoldText(true);
        
        // 初始化背景畫筆
        backgroundPaint = new Paint();
        backgroundPaint.setColor(BACKGROUND_COLOR);
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setAlpha(128); // 半透明
        
        // 設置透明背景
        setBackgroundColor(Color.TRANSPARENT);
        
        // 確保視圖可點擊和可見（但點擊事件穿透）
        setClickable(false);
        setFocusable(false);
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.d(TAG, "onSizeChanged: " + w + "x" + h + " (之前: " + oldw + "x" + oldh + ")");
        
        // 當尺寸改變時，如果有檢測結果，觸發重繪
        if (w > 0 && h > 0 && !detections.isEmpty()) {
            Log.d(TAG, "視圖尺寸已確定，觸發重繪檢測框");
            postInvalidate();
        }
    }
    
    /**
     * 更新檢測結果
     */
    public void updateDetections(List<ObjectDetectorHelper.DetectionResult> newDetections) {
        Log.d(TAG, "updateDetections called with " + (newDetections != null ? newDetections.size() : 0) + " detections");
        
        // 過濾掉無效的檢測結果
        List<ObjectDetectorHelper.DetectionResult> validDetections = new ArrayList<>();
        if (newDetections != null) {
            for (ObjectDetectorHelper.DetectionResult detection : newDetections) {
                if (detection != null && detection.getBoundingBox() != null) {
                    validDetections.add(detection);
                } else {
                    Log.w(TAG, "跳過無效檢測結果: " + detection);
                }
            }
        }
        
        // 只顯示最多2個檢測結果
        if (validDetections.size() > 2) {
            // 按置信度排序並只取前2個
            List<ObjectDetectorHelper.DetectionResult> sortedDetections = new ArrayList<>(validDetections);
            Collections.sort(sortedDetections, new Comparator<ObjectDetectorHelper.DetectionResult>() {
                @Override
                public int compare(ObjectDetectorHelper.DetectionResult a, ObjectDetectorHelper.DetectionResult b) {
                    return Float.compare(b.getConfidence(), a.getConfidence());
                }
            });
            this.detections = new ArrayList<>(sortedDetections.subList(0, 2));
            Log.d(TAG, "限制檢測結果為2個，按置信度排序");
        } else {
            this.detections = validDetections;
        }
        
        Log.d(TAG, "Updated detections list size: " + this.detections.size());
        Log.d(TAG, "Current view visibility: " + getVisibility());
        Log.d(TAG, "Current view width: " + getWidth() + ", height: " + getHeight());
        
        // 確保視圖可見
        setVisibility(VISIBLE);
        setAlpha(1.0f); // 確保完全不透明
        
        // 強制重繪
        postInvalidate();
        invalidate();
        
        Log.d(TAG, "postInvalidate() and invalidate() called");
        
        // 打印檢測結果詳情
        for (int i = 0; i < this.detections.size(); i++) {
            ObjectDetectorHelper.DetectionResult detection = this.detections.get(i);
            if (detection != null && detection.getBoundingBox() != null) {
                Log.d(TAG, "Detection " + i + ": " + detection.getLabel() + " (" + detection.getConfidence() + ") at " + detection.getBoundingBox());
            }
        }
    }
    
    /**
     * 清除檢測結果
     */
    public void clearDetections() {
        this.detections.clear();
        postInvalidate(); // 觸發重繪
    }
    
    /**
     * 設置當前語言
     */
    public void setCurrentLanguage(String language) {
        this.currentLanguage = language;
        postInvalidate(); // 重新繪製以更新標籤語言
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // 獲取視圖尺寸（在檢查前獲取，以便記錄）
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        
        Log.d(TAG, "onDraw called - detections: " + detections.size() + 
                   ", view size: " + viewWidth + "x" + viewHeight +
                   ", visibility: " + getVisibility() + 
                   ", alpha: " + getAlpha());
        
        if (detections.isEmpty()) {
            Log.d(TAG, "No detections to draw");
            return;
        }
        
        // 檢查視圖尺寸是否有效
        if (viewWidth <= 0 || viewHeight <= 0) {
            Log.w(TAG, "視圖尺寸無效，無法繪製邊界框。將在尺寸確定後重繪。");
            // 如果視圖尺寸還未確定，稍後再試
            if (viewWidth == 0 || viewHeight == 0) {
                postDelayed(() -> {
                    if (getWidth() > 0 && getHeight() > 0 && !detections.isEmpty()) {
                        invalidate();
                    }
                }, 100);
            }
            return;
        }
        
        Log.d(TAG, "開始繪製 " + detections.size() + " 個檢測結果");
        
        // 繪製每個檢測結果，使用不同顏色
        int drawnCount = 0;
        for (int i = 0; i < detections.size(); i++) {
            ObjectDetectorHelper.DetectionResult detection = detections.get(i);
            if (detection == null || detection.getBoundingBox() == null) {
                Log.w(TAG, "檢測結果 " + i + " 或邊界框為 null，跳過");
                continue;
            }
            int color = (i % 2 == 0) ? BOX_COLOR : BOX_COLOR_ALT;
            Log.d(TAG, "繪製檢測 " + i + ": " + detection.getLabel() + " (" + detection.getConfidence() + ") at " + detection.getBoundingBox());
            try {
                drawDetection(canvas, detection, viewWidth, viewHeight, color);
                drawnCount++;
            } catch (Exception e) {
                Log.e(TAG, "繪製檢測 " + i + " 時出錯: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        Log.d(TAG, "繪製完成，成功繪製 " + drawnCount + " 個檢測框");
    }
    
    /**
     * 繪製單個檢測結果
     */
    private void drawDetection(Canvas canvas, ObjectDetectorHelper.DetectionResult detection, 
                             int viewWidth, int viewHeight, int boxColor) {
        
        // 獲取邊界框
        RectF boundingBox = detection.getBoundingBox();
        
        Log.d(TAG, "原始邊界框座標: " + boundingBox);
        Log.d(TAG, "視圖尺寸: " + viewWidth + "x" + viewHeight);
        
        // 檢查邊界框座標是否已經是像素座標還是相對座標
        float left, top, right, bottom;
        
        // 更精確的座標轉換邏輯
        // SSD檢測器返回的是相對座標 (0-1)，TensorFlow Lite Task Vision API也是相對座標
        // YOLO可能返回像素座標或縮放座標 (0-1000)
        
        // 判斷邏輯：
        // 1. 如果所有座標都在 [0, 1] 範圍內，視為相對座標
        // 2. 如果座標大於1但小於等於1000，視為縮放座標 (0-1000)
        // 3. 如果座標大於1000，視為像素座標
        
        boolean allInRange01 = (boundingBox.left >= 0 && boundingBox.left <= 1.0f &&
                                boundingBox.top >= 0 && boundingBox.top <= 1.0f &&
                                boundingBox.right >= 0 && boundingBox.right <= 1.0f &&
                                boundingBox.bottom >= 0 && boundingBox.bottom <= 1.0f);
        
        boolean allInRange1000 = (!allInRange01 && 
                                  boundingBox.left >= 0 && boundingBox.left <= 1000.0f &&
                                  boundingBox.top >= 0 && boundingBox.top <= 1000.0f &&
                                  boundingBox.right >= 0 && boundingBox.right <= 1000.0f &&
                                  boundingBox.bottom >= 0 && boundingBox.bottom <= 1000.0f);
        
        if (allInRange01) {
            // 相對座標 (0-1)，需要轉換為像素座標
            Log.d(TAG, "檢測到相對座標 (0-1)，轉換為像素座標");
            left = boundingBox.left * viewWidth;
            top = boundingBox.top * viewHeight;
            right = boundingBox.right * viewWidth;
            bottom = boundingBox.bottom * viewHeight;
        } else if (allInRange1000) {
            // 縮放座標 (0-1000)，需要轉換為相對座標再轉為像素座標
            Log.d(TAG, "檢測到縮放座標 (0-1000)，轉換為像素座標");
            left = (boundingBox.left / 1000.0f) * viewWidth;
            top = (boundingBox.top / 1000.0f) * viewHeight;
            right = (boundingBox.right / 1000.0f) * viewWidth;
            bottom = (boundingBox.bottom / 1000.0f) * viewHeight;
        } else {
            // 已經是像素座標，直接使用
            Log.d(TAG, "檢測到像素座標，直接使用");
            left = boundingBox.left;
            top = boundingBox.top;
            right = boundingBox.right;
            bottom = boundingBox.bottom;
        }
        
        // 確保 right >= left 且 bottom >= top
        if (right < left) {
            float temp = left;
            left = right;
            right = temp;
        }
        if (bottom < top) {
            float temp = top;
            top = bottom;
            bottom = temp;
        }
        
        // 精確的邊界檢查和調整
        left = Math.max(0, Math.min(left, viewWidth - 1));
        top = Math.max(0, Math.min(top, viewHeight - 1));
        right = Math.max(left + 1, Math.min(right, viewWidth));
        bottom = Math.max(top + 1, Math.min(bottom, viewHeight));
        
        // 驗證邊界框尺寸是否合理（至少20x20像素）
        float width = right - left;
        float height = bottom - top;
        if (width < 20 || height < 20) {
            Log.w(TAG, "邊界框尺寸過小，跳過繪製: " + width + "x" + height);
            return;
        }
        
        // 創建邊界框矩形
        RectF rect = new RectF(left, top, right, bottom);
        
        // 設置邊界框顏色
        boxPaint.setColor(boxColor);
        
        Log.d(TAG, "繪製邊界框: " + rect + ", 顏色: " + Integer.toHexString(boxColor) + ", 尺寸: " + width + "x" + height);
        
        // 繪製邊界框 - 先繪製填充，再繪製邊框
        Paint fillPaint = new Paint();
        fillPaint.setColor(boxColor);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAlpha(50); // 半透明填充
        
        // 繪製填充
        canvas.drawRect(rect, fillPaint);
        
        // 繪製邊框
        canvas.drawRect(rect, boxPaint);
        
        // 繪製角落標記
        drawCornerMarkers(canvas, rect, boxColor);
        
        // 準備標籤文字（根據當前語言選擇對應的標籤）
        String displayLabel = getDisplayLabel(detection);
        String label = String.format("%s %.2f", 
            displayLabel, 
            detection.getConfidence());
        
        // 計算文字尺寸
        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        float textHeight = fontMetrics.bottom - fontMetrics.top;
        float textWidth = textPaint.measureText(label);
        
        // 計算文字背景矩形
        float textLeft = left;
        float textTop = top - textHeight - TEXT_PADDING;
        float textRight = textLeft + textWidth + TEXT_PADDING * 2;
        float textBottom = textTop + textHeight + TEXT_PADDING * 2;
        
        // 確保文字背景不超出螢幕邊界
        if (textTop < 0) {
            textTop = bottom + TEXT_PADDING;
            textBottom = textTop + textHeight + TEXT_PADDING * 2;
        }
        
        // 繪製文字背景
        RectF textBackground = new RectF(textLeft, textTop, textRight, textBottom);
        canvas.drawRect(textBackground, backgroundPaint);
        
        // 繪製文字
        float textX = textLeft + TEXT_PADDING;
        float textY = textBottom - TEXT_PADDING;
        canvas.drawText(label, textX, textY, textPaint);
    }
    
    /**
     * 繪製邊界框角落標記
     */
    private void drawCornerMarkers(Canvas canvas, RectF rect, int color) {
        Paint markerPaint = new Paint();
        markerPaint.setColor(color);
        markerPaint.setStyle(Paint.Style.FILL);
        markerPaint.setAntiAlias(true);
        
        float radius = BOX_THICKNESS * 2;
        float[] corners = {
            rect.left, rect.top,      // 左上
            rect.right - radius * 2, rect.top,  // 右上
            rect.left, rect.bottom - radius * 2,  // 左下
            rect.right - radius * 2, rect.bottom - radius * 2  // 右下
        };
        
        for (int i = 0; i < corners.length; i += 2) {
            canvas.drawCircle(corners[i], corners[i + 1], radius, markerPaint);
        }
    }
    
    /**
     * 設置邊界框顏色
     */
    public void setBoxColor(int color) {
        boxPaint.setColor(color);
        invalidate();
    }
    
    /**
     * 設置文字大小
     */
    public void setTextSize(int size) {
        textPaint.setTextSize(size);
        invalidate();
    }
    
    /**
     * 設置邊界框粗細
     */
    public void setBoxThickness(int thickness) {
        boxPaint.setStrokeWidth(thickness);
        invalidate();
    }
    
    /**
     * 獲取檢測數量
     */
    public int getDetectionCount() {
        return detections.size();
    }
    
    /**
     * 根據當前語言獲取顯示標籤
     */
    private String getDisplayLabel(ObjectDetectorHelper.DetectionResult detection) {
        switch (currentLanguage) {
            case "english":
                return detection.getLabel() != null ? detection.getLabel() : detection.getLabelZh();
            case "mandarin":
                return detection.getLabelZh() != null ? detection.getLabelZh() : detection.getLabel();
            case "cantonese":
            default:
                return detection.getLabelZh() != null ? detection.getLabelZh() : detection.getLabel();
        }
    }
}
