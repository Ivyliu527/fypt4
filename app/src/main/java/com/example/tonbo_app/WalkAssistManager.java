package com.example.tonbo_app;

import android.speech.tts.TextToSpeech;

import java.util.List;

public class WalkAssistManager {

    private String lastPathState = "";
    private long lastSpeakTime = 0;
    private TextToSpeech tts;

    public WalkAssistManager(TextToSpeech tts) {
        this.tts = tts;
    }

    public String analyzeWalkablePath(List<Detection> detections,
                                      float screenWidth,
                                      float screenHeight) {

        if (detections == null || detections.isEmpty()) {
            return "clear";
        }

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
            float totalWidth = rightEdge - leftEdge;
            if (totalWidth <= 0) continue;

            float leftOverlap = Math.max(0,
                    Math.min(rightEdge, leftBoundary) - leftEdge);

            float centerOverlap = Math.max(0,
                    Math.min(rightEdge, rightBoundary)
                            - Math.max(leftEdge, leftBoundary));

            float rightOverlap = Math.max(0,
                    rightEdge - Math.max(leftEdge, rightBoundary));

            leftBlocked += areaRatio * (leftOverlap / totalWidth);
            centerBlocked += areaRatio * (centerOverlap / totalWidth);
            rightBlocked += areaRatio * (rightOverlap / totalWidth);

            if (det.getLabel().equals("crosswalk")) return "crosswalk";
            if (det.getLabel().equals("traffic light")) return "traffic_light";
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

    public void speak(String pathState) {

        long now = System.currentTimeMillis();
        if (pathState.equals(lastPathState)) return;
        if (now - lastSpeakTime < 1500) return;

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
}
