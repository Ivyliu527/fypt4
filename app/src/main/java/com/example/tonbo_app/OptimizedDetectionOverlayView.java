package com.example.tonbo_app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import java.util.List;

/**
 * 優化的檢測結果顯示器
 * 提供更好的視覺效果和用戶體驗
 */
public class OptimizedDetectionOverlayView extends View {
    private static final String TAG = "DetectionOverlay";
    
    private List<YoloDetector.DetectionResult> detectionResults;
    private Paint boxPaint;
    private Paint textPaint;
    private Paint backgroundPaint;
    private int viewWidth;
    private int viewHeight;
    
    public OptimizedDetectionOverlayView(Context context) {
        super(context);
        init();
    }
    
    public OptimizedDetectionOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public OptimizedDetectionOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        // 初始化繪畫工具
        boxPaint = new Paint();
        boxPaint.setColor(Color.RED);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(4f);
        boxPaint.setAntiAlias(true);
        
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(32f);
        textPaint.setAntiAlias(true);
        textPaint.setFakeBoldText(true);
        
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.BLACK);
        backgroundPaint.setAlpha(128);
        backgroundPaint.setAntiAlias(true);
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (detectionResults == null || detectionResults.isEmpty()) {
            return;
        }
        
        // 繪製檢測結果
        for (int i = 0; i < detectionResults.size(); i++) {
            YoloDetector.DetectionResult result = detectionResults.get(i);
            drawDetectionResult(canvas, result, i);
        }
    }
    
    private void drawDetectionResult(Canvas canvas, YoloDetector.DetectionResult result, int index) {
        Rect boundingBox = result.getBoundingBox();
        if (boundingBox == null) {
            return;
        }
        
        // 根據檢測結果調整顏色
        int color = getColorForConfidence(result.getConfidence());
        boxPaint.setColor(color);
        
        // 繪製邊界框
        canvas.drawRect(boundingBox, boxPaint);
        
        // 準備標籤文本
        String label = result.getLabelZh();
        String confidence = String.format("%.1f%%", result.getConfidence() * 100);
        String displayText = label + " " + confidence;
        
        // 計算文本位置
        float textWidth = textPaint.measureText(displayText);
        float textHeight = textPaint.getTextSize();
        
        // 繪製文本背景
        float textX = boundingBox.left;
        float textY = boundingBox.top - 10;
        
        // 確保文本不超出屏幕
        if (textY < textHeight) {
            textY = boundingBox.bottom + textHeight + 10;
        }
        
        if (textX + textWidth > viewWidth) {
            textX = viewWidth - textWidth - 10;
        }
        
        // 繪製文本背景矩形
        canvas.drawRect(
            textX - 5, textY - textHeight - 5,
            textX + textWidth + 5, textY + 5,
            backgroundPaint
        );
        
        // 繪製文本
        canvas.drawText(displayText, textX, textY, textPaint);
        
        // 繪製檢測索引
        if (index < 3) { // 只顯示前3個檢測結果的索引
            String indexText = String.valueOf(index + 1);
            float indexX = boundingBox.right - 30;
            float indexY = boundingBox.top + 30;
            
            // 繪製索引背景圓圈
            canvas.drawCircle(indexX, indexY, 15, backgroundPaint);
            
            // 繪製索引文本
            Paint indexPaint = new Paint(textPaint);
            indexPaint.setTextSize(24f);
            indexPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(indexText, indexX, indexY + 8, indexPaint);
        }
    }
    
    private int getColorForConfidence(float confidence) {
        if (confidence >= 0.8f) {
            return Color.GREEN; // 高置信度 - 綠色
        } else if (confidence >= 0.6f) {
            return Color.YELLOW; // 中等置信度 - 黃色
        } else {
            return Color.RED; // 低置信度 - 紅色
        }
    }
    
    /**
     * 更新檢測結果
     */
    public void updateDetectionResults(List<YoloDetector.DetectionResult> results) {
        this.detectionResults = results;
        invalidate(); // 觸發重繪
    }
    
    /**
     * 清除檢測結果
     */
    public void clearDetectionResults() {
        this.detectionResults = null;
        invalidate();
    }
    
    /**
     * 設置檢測結果的邊界框座標
     * 將相對座標轉換為絕對座標
     */
    public void setDetectionResultsWithRelativeCoords(List<YoloDetector.DetectionResult> results) {
        if (results == null) {
            clearDetectionResults();
            return;
        }
        
        // 轉換相對座標為絕對座標
        for (YoloDetector.DetectionResult result : results) {
            Rect boundingBox = result.getBoundingBox();
            if (boundingBox != null) {
                // 轉換相對座標 (0-1000) 為絕對座標
                int left = (int) (boundingBox.left * viewWidth / 1000f);
                int top = (int) (boundingBox.top * viewHeight / 1000f);
                int right = (int) (boundingBox.right * viewWidth / 1000f);
                int bottom = (int) (boundingBox.bottom * viewHeight / 1000f);
                
                // 確保座標在有效範圍內
                left = Math.max(0, Math.min(viewWidth - 1, left));
                top = Math.max(0, Math.min(viewHeight - 1, top));
                right = Math.max(0, Math.min(viewWidth - 1, right));
                bottom = Math.max(0, Math.min(viewHeight - 1, bottom));
                
                // 更新邊界框
                result.getBoundingBox().set(left, top, right, bottom);
            }
        }
        
        updateDetectionResults(results);
    }
    
    /**
     * 獲取檢測結果數量
     */
    public int getDetectionCount() {
        return detectionResults != null ? detectionResults.size() : 0;
    }
    
    /**
     * 檢查是否有檢測結果
     */
    public boolean hasDetections() {
        return detectionResults != null && !detectionResults.isEmpty();
    }
}
