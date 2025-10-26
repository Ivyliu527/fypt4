package com.example.tonbo_app;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;

import java.io.IOException;
import java.util.List;

/**
 * 傳統Camera API的備用實現
 * 當CameraX不兼容時使用
 */
public class LegacyCameraHelper implements Camera.PreviewCallback, SurfaceHolder.Callback {
    private static final String TAG = "LegacyCameraHelper";
    
    private Camera camera;
    private SurfaceView surfaceView;
    private TextureView textureView;
    private Context context;
    private boolean isPreviewing = false;
    
    public LegacyCameraHelper(Context context) {
        this.context = context;
    }
    
    /**
     * 初始化相機
     */
    public boolean initializeCamera() {
        try {
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            if (camera == null) {
                Log.e(TAG, "無法打開後置相機");
                return false;
            }
            
            Camera.Parameters parameters = camera.getParameters();
            
            // 設置預覽大小
            List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
            Camera.Size bestSize = getBestPreviewSize(previewSizes);
            if (bestSize != null) {
                parameters.setPreviewSize(bestSize.width, bestSize.height);
            }
            
            // 設置對焦模式
            List<String> focusModes = parameters.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
            
            camera.setParameters(parameters);
            Log.d(TAG, "傳統相機初始化成功");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "傳統相機初始化失敗: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 設置SurfaceView
     */
    public void setSurfaceView(SurfaceView surfaceView) {
        this.surfaceView = surfaceView;
        if (surfaceView != null) {
            surfaceView.getHolder().addCallback(this);
        }
    }
    
    /**
     * 開始預覽
     */
    public void startPreview() {
        if (camera != null && !isPreviewing) {
            try {
                camera.startPreview();
                isPreviewing = true;
                Log.d(TAG, "傳統相機預覽已開始");
            } catch (Exception e) {
                Log.e(TAG, "開始預覽失敗: " + e.getMessage());
            }
        }
    }
    
    /**
     * 停止預覽
     */
    public void stopPreview() {
        if (camera != null && isPreviewing) {
            try {
                camera.stopPreview();
                isPreviewing = false;
                Log.d(TAG, "傳統相機預覽已停止");
            } catch (Exception e) {
                Log.e(TAG, "停止預覽失敗: " + e.getMessage());
            }
        }
    }
    
    /**
     * 釋放相機資源
     */
    public void releaseCamera() {
        if (camera != null) {
            stopPreview();
            camera.setPreviewCallback(null);
            camera.release();
            camera = null;
            isPreviewing = false;
            Log.d(TAG, "傳統相機資源已釋放");
        }
    }
    
    /**
     * 設置預覽回調
     */
    public void setPreviewCallback(Camera.PreviewCallback callback) {
        if (camera != null) {
            camera.setPreviewCallback(callback);
        }
    }
    
    /**
     * 獲取最佳預覽大小
     */
    private Camera.Size getBestPreviewSize(List<Camera.Size> sizes) {
        Camera.Size bestSize = null;
        for (Camera.Size size : sizes) {
            if (bestSize == null) {
                bestSize = size;
            } else {
                int bestArea = bestSize.width * bestSize.height;
                int currentArea = size.width * size.height;
                if (currentArea > bestArea && currentArea < 1920 * 1080) {
                    bestSize = size;
                }
            }
        }
        return bestSize;
    }
    
    // SurfaceHolder.Callback implementation
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "Surface已創建");
        if (camera != null) {
            try {
                camera.setPreviewDisplay(holder);
            } catch (IOException e) {
                Log.e(TAG, "設置預覽顯示失敗: " + e.getMessage());
            }
        }
    }
    
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "Surface已更改: " + width + "x" + height);
        if (camera != null) {
            startPreview();
        }
    }
    
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "Surface已銷毀");
        stopPreview();
    }
    
    // Camera.PreviewCallback implementation
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        // 處理預覽幀數據
        Log.d(TAG, "收到預覽幀數據，大小: " + data.length);
    }
}
