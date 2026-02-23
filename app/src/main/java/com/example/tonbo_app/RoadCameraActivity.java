package com.example.tonbo_app;

import android.graphics.Rect;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.TextView;

import android.content.res.Configuration;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 獨立的路面相機頁：僅用 CameraX + YOLO 檢測並打 Log，不播報、不影響導航。
 */
public class RoadCameraActivity extends AppCompatActivity {
    private static final String TAG = "RoadCameraActivity";

    private PreviewView previewView;
    private OverlayView overlayView;
    private TextView statusText;
    private YoloDetector yoloDetector;
    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;

    private TextToSpeech tts;
    private long lastSpeakTime = 0;
    private String lastSpokenLabel = "";
    private String lastPathState = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_road_camera);

        previewView = findViewById(R.id.previewView);
        overlayView = findViewById(R.id.overlayView);
        statusText = findViewById(R.id.road_camera_status);
        if (statusText != null) {
            statusText.setText("Road Detection Running");
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 100);
            return;
        }

        cameraExecutor = Executors.newSingleThreadExecutor();
        try {
            yoloDetector = new YoloDetector(this);
        } catch (Exception e) {
            Log.e(TAG, "YOLO init failed: " + e.getMessage());
            if (statusText != null) statusText.setText("YOLO init failed");
            return;
        }

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Locale systemLocale = getResources().getConfiguration().getLocales().get(0);
                tts.setLanguage(systemLocale);
            }
        });

        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                bindCamera();
            } catch (Exception e) {
                Log.e(TAG, "CameraProvider failed: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private boolean isObjectClose(Detection det) {
        float screenHeight = overlayView.getHeight();
        float boxHeight = det.getBottom() - det.getTop();
        float ratio = boxHeight / screenHeight;
        return ratio > 0.20f;
    }

    private boolean isWalkingObstacle(String label) {
        return label.equals("person")
                || label.equals("car")
                || label.equals("truck")
                || label.equals("bus")
                || label.equals("motorcycle")
                || label.equals("bicycle")
                || label.equals("tree")
                || label.equals("pole")
                || label.equals("bench")
                || label.equals("chair")
                || label.equals("traffic light")
                || label.equals("stop sign")
                || label.equals("crosswalk");
    }

    private String getDirectionHint(Detection det) {
        float screenWidth = overlayView.getWidth();
        float centerX = (det.getLeft() + det.getRight()) / 2f;
        float leftBoundary = screenWidth * 0.35f;
        float rightBoundary = screenWidth * 0.65f;
        if (centerX < leftBoundary) {
            return "right";
        } else if (centerX > rightBoundary) {
            return "left";
        } else {
            return "center";
        }
    }

    private Detection getMostDangerous(List<Detection> detections) {
        if (detections == null || detections.isEmpty()) return null;

        Detection mostDangerous = null;
        float highestScore = 0f;

        float screenWidth = overlayView.getWidth();
        float screenHeight = overlayView.getHeight();

        for (Detection det : detections) {
            if (!isWalkingObstacle(det.getLabel())) continue;
            if (det.getConfidence() < 0.6f) continue;

            float boxWidth = det.getRight() - det.getLeft();
            float boxHeight = det.getBottom() - det.getTop();
            float area = boxWidth * boxHeight;

            float centerX = (det.getLeft() + det.getRight()) / 2f;
            float centerY = (det.getTop() + det.getBottom()) / 2f;

            float horizontalCenterWeight =
                    1f - Math.abs(centerX - screenWidth / 2f) / (screenWidth / 2f);

            float verticalWeight = centerY / screenHeight;

            float areaWeight = area / (screenWidth * screenHeight);

            float score = (horizontalCenterWeight * 0.4f)
                    + (verticalWeight * 0.3f)
                    + (areaWeight * 0.3f);

            if (score > highestScore) {
                highestScore = score;
                mostDangerous = det;
            }
        }

        return mostDangerous;
    }

    private String analyzeWalkablePath(List<Detection> detections) {
        if (detections == null || detections.isEmpty()) {
            return "clear";
        }

        float screenWidth = overlayView.getWidth();
        float screenHeight = overlayView.getHeight();

        float leftBlocked = 0f;
        float centerBlocked = 0f;
        float rightBlocked = 0f;

        float leftBoundary = screenWidth / 3f;
        float rightBoundary = screenWidth * 2f / 3f;

        for (Detection det : detections) {
            if (!isWalkingObstacle(det.getLabel())) continue;
            if (det.getConfidence() < 0.6f) continue;

            float boxWidth = det.getRight() - det.getLeft();
            float boxHeight = det.getBottom() - det.getTop();
            float area = boxWidth * boxHeight;

            float centerY = (det.getTop() + det.getBottom()) / 2f;

            if (centerY < screenHeight * 0.5f) continue;

            float areaRatio = area / (screenWidth * (screenHeight / 2f));

            float leftEdge = det.getLeft();
            float rightEdge = det.getRight();

            float leftOverlap = Math.max(0, Math.min(rightEdge, leftBoundary) - leftEdge);
            float centerOverlap = Math.max(0,
                    Math.min(rightEdge, rightBoundary) - Math.max(leftEdge, leftBoundary));
            float rightOverlap = Math.max(0, rightEdge - Math.max(leftEdge, rightBoundary));

            float totalWidth = rightEdge - leftEdge;
            if (totalWidth <= 0) continue;

            float leftRatio = leftOverlap / totalWidth;
            float centerRatio = centerOverlap / totalWidth;
            float rightRatio = rightOverlap / totalWidth;

            leftBlocked += areaRatio * leftRatio;
            centerBlocked += areaRatio * centerRatio;
            rightBlocked += areaRatio * rightRatio;

            if (det.getLabel().equals("crosswalk")) {
                return "crosswalk";
            }

            if (det.getLabel().equals("traffic light")) {
                return "traffic_light";
            }
        }

        float threshold = 0.08f;

        boolean leftFree = leftBlocked < threshold;
        boolean centerFree = centerBlocked < threshold;
        boolean rightFree = rightBlocked < threshold;

        if (centerFree) return "center_clear";
        if (!centerFree && leftFree) return "move_left";
        if (!centerFree && rightFree) return "move_right";

        return "blocked";
    }

    private void bindCamera() {
        if (cameraProvider == null || previewView == null) return;

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        imageAnalysis.setAnalyzer(cameraExecutor, this::analyze);

        CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;
        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, selector, preview, imageAnalysis);
        } catch (Exception e) {
            Log.e(TAG, "bindToLifecycle failed: " + e.getMessage());
        }
    }

    private void analyze(@NonNull ImageProxy image) {
        if (yoloDetector == null) {
            image.close();
            return;
        }
        try {
            List<YoloDetector.DetectionResult> results = yoloDetector.detect(image);
            int count = results != null ? results.size() : 0;
            Log.d(TAG, "RoadCamera detect count: " + count);

            final int imageWidth = image.getWidth();
            final int imageHeight = image.getHeight();
            final List<YoloDetector.DetectionResult> resultList = results;
            runOnUiThread(() -> {
                if (overlayView == null) return;
                int vw = overlayView.getWidth();
                int vh = overlayView.getHeight();
                if (resultList == null || vw <= 0 || vh <= 0) {
                    overlayView.setDetections(null);
                    return;
                }
                float sx = (float) vw / imageWidth;
                float sy = (float) vh / imageHeight;
                List<Detection> overlayList = new ArrayList<>();
                for (YoloDetector.DetectionResult r : resultList) {
                    Rect box = r.getBoundingBox();
                    if (box == null) continue;
                    float left   = box.left   * sx;
                    float top    = box.top    * sy;
                    float right  = box.right  * sx;
                    float bottom = box.bottom * sy;
                    String label = r.getLabel() != null ? r.getLabel() : "";
                    float conf = r.getConfidence();
                    overlayList.add(new Detection(left, top, right, bottom, label, conf));
                }
                Collections.sort(overlayList, (a, b) -> Float.compare(b.getConfidence(), a.getConfidence()));
                overlayView.setDetections(overlayList);

                String pathState = analyzeWalkablePath(overlayList);

                if (!pathState.equals(lastPathState)) {
                    long now = System.currentTimeMillis();

                    if (now - lastSpeakTime > 1500) {
                        String speechText = "";

                        switch (pathState) {
                            case "center_clear":
                                speechText = "前方道路畅通";
                                break;
                            case "move_left":
                                speechText = "中间受阻，请向左慢行";
                                break;
                            case "move_right":
                                speechText = "中间受阻，请向右慢行";
                                break;
                            case "blocked":
                                speechText = "前方受阻，请停下";
                                break;
                            case "crosswalk":
                                speechText = "前方有斑马线";
                                break;
                            case "traffic_light":
                                speechText = "前方红绿灯";
                                break;
                        }

                        if (tts != null && !speechText.isEmpty()) {
                            tts.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, null);
                            lastSpeakTime = now;
                            lastPathState = pathState;
                        }
                    }
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "analyze: " + e.getMessage());
        } finally {
            image.close();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (cameraExecutor != null && !cameraExecutor.isShutdown()) {
            cameraExecutor.shutdown();
        }
        if (yoloDetector != null) {
            yoloDetector.close();
        }
    }
}
