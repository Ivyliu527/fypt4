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
    
    // 繪製參數
    private static final int BOX_COLOR = Color.BLUE;
    private static final int BOX_COLOR_ALT = Color.MAGENTA;
    private static final int TEXT_COLOR = Color.WHITE;
    private static final int BACKGROUND_COLOR = Color.BLACK;
    private static final int BOX_THICKNESS = 6;
    private static final int TEXT_SIZE = 28;
    private static final int TEXT_PADDING = 12;
    
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
        postInvalidate(); // 觸發重繪
        Log.d(TAG, "postInvalidate() called");
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
        
        // 獲取邊界框（模型輸出的是0-1的相對座標）
        RectF boundingBox = detection.getBoundingBox();
        
        // 轉換為實際像素座標
        float left = boundingBox.left * viewWidth;
        float top = boundingBox.top * viewHeight;
        float right = boundingBox.right * viewWidth;
        float bottom = boundingBox.bottom * viewHeight;
        
        // 創建邊界框矩形
        RectF rect = new RectF(left, top, right, bottom);
        
        // 設置邊界框顏色
        boxPaint.setColor(boxColor);
        
        // 繪製邊界框
        canvas.drawRect(rect, boxPaint);
        
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
        
        // 在邊界框角落繪製小圓點（視覺增強）
        drawCornerMarkers(canvas, rect, boxColor);
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
