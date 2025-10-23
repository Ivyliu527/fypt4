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
    }
    
    /**
     * 更新檢測結果
     */
    public void updateDetections(List<ObjectDetectorHelper.DetectionResult> newDetections) {
        Log.d(TAG, "updateDetections called with " + (newDetections != null ? newDetections.size() : 0) + " detections");
        this.detections = newDetections != null ? new ArrayList<>(newDetections) : new ArrayList<>();
        Log.d(TAG, "Updated detections list size: " + this.detections.size());
        
        // 確保視圖可見
        setVisibility(VISIBLE);
        
        // 強制重繪
        postInvalidate();
        invalidate();
        
        Log.d(TAG, "postInvalidate() and invalidate() called");
        
        // 打印檢測結果詳情
        for (int i = 0; i < this.detections.size(); i++) {
            ObjectDetectorHelper.DetectionResult detection = this.detections.get(i);
            Log.d(TAG, "Detection " + i + ": " + detection.getLabel() + " (" + detection.getConfidence() + ") at " + detection.getBoundingBox());
        }
    }
    
    /**
     * 清除檢測結果
     */
    public void clearDetections() {
        this.detections.clear();
        postInvalidate(); // 觸發重繪
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        Log.d(TAG, "onDraw called, detections size: " + detections.size());
        
        if (detections.isEmpty()) {
            Log.d(TAG, "No detections to draw");
            return;
        }
        
        // 獲取視圖尺寸
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        
        Log.d(TAG, "View size: " + viewWidth + "x" + viewHeight);
        
        // 繪製每個檢測結果，使用不同顏色
        for (int i = 0; i < detections.size(); i++) {
            ObjectDetectorHelper.DetectionResult detection = detections.get(i);
            int color = (i % 2 == 0) ? BOX_COLOR : BOX_COLOR_ALT;
            Log.d(TAG, "Drawing detection " + i + ": " + detection.getLabel() + " at " + detection.getBoundingBox());
            drawDetection(canvas, detection, viewWidth, viewHeight, color);
        }
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
        boolean isScaledCoords = (boundingBox.left > 1.0f || boundingBox.top > 1.0f ||
                                boundingBox.right > 1.0f || boundingBox.bottom > 1.0f);
        
        if (isScaledCoords) {
            // 縮放座標 (0-1000)，需要轉換為相對座標再轉為像素座標
            Log.d(TAG, "檢測到縮放座標，轉換為像素座標");
            left = (boundingBox.left / 1000.0f) * viewWidth;
            top = (boundingBox.top / 1000.0f) * viewHeight;
            right = (boundingBox.right / 1000.0f) * viewWidth;
            bottom = (boundingBox.bottom / 1000.0f) * viewHeight;
        } else if (boundingBox.left <= 1.0f && boundingBox.top <= 1.0f &&
                  boundingBox.right <= 1.0f && boundingBox.bottom <= 1.0f) {
            // 相對座標 (0-1)，需要轉換為像素座標
            Log.d(TAG, "檢測到相對座標，轉換為像素座標");
            left = boundingBox.left * viewWidth;
            top = boundingBox.top * viewHeight;
            right = boundingBox.right * viewWidth;
            bottom = boundingBox.bottom * viewHeight;
        } else {
            // 已經是像素座標，直接使用
            Log.d(TAG, "檢測到像素座標，直接使用");
            left = boundingBox.left;
            top = boundingBox.top;
            right = boundingBox.right;
            bottom = boundingBox.bottom;
        }
        
        // 精確的邊界檢查和調整
        left = Math.max(0, Math.min(left, viewWidth - 1));
        top = Math.max(0, Math.min(top, viewHeight - 1));
        right = Math.max(left + 1, Math.min(right, viewWidth));
        bottom = Math.max(top + 1, Math.min(bottom, viewHeight));
        
        // 創建邊界框矩形
        RectF rect = new RectF(left, top, right, bottom);
        
        // 設置邊界框顏色
        boxPaint.setColor(boxColor);
        
        Log.d(TAG, "繪製邊界框: " + rect + ", 顏色: " + Integer.toHexString(boxColor));
        
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
        
        // 準備標籤文字（類似圖片中的格式）
        String label = String.format("%s %.2f", 
            detection.getLabel(), 
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
}
