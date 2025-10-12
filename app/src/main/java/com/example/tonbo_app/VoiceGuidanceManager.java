package com.example.tonbo_app;

import android.content.Context;
import android.util.Log;

public class VoiceGuidanceManager {
    private static final String TAG = "VoiceGuidanceManager";
    private static VoiceGuidanceManager instance;
    private Context context;
    private TTSManager ttsManager;
    private String currentLanguage = "cantonese";
    
    private VoiceGuidanceManager(Context context) {
        this.context = context.getApplicationContext();
        this.ttsManager = TTSManager.getInstance(context);
    }
    
    public static synchronized VoiceGuidanceManager getInstance(Context context) {
        if (instance == null) {
            instance = new VoiceGuidanceManager(context);
        }
        return instance;
    }
    
    public void setLanguage(String language) {
        this.currentLanguage = language;
    }
    
    public void speakNavigationInstruction(String instruction, double distance, String direction) {
        String cantoneseText = buildCantoneseInstruction(instruction, distance, direction);
        String englishText = buildEnglishInstruction(instruction, distance, direction);
        
        Log.d(TAG, "播報導航指令 - 廣東話: " + cantoneseText);
        Log.d(TAG, "播報導航指令 - 英文: " + englishText);
        
        ttsManager.speak(cantoneseText, englishText, true);
    }
    
    private String buildCantoneseInstruction(String instruction, double distance, String direction) {
        StringBuilder sb = new StringBuilder();
        
        // 添加方向指引
        if (direction != null && !direction.isEmpty()) {
            sb.append(direction).append("，");
        }
        
        // 添加距離信息
        if (distance > 0) {
            if (distance < 10) {
                sb.append("前進約").append(String.format("%.0f", distance)).append("米，");
            } else if (distance < 100) {
                sb.append("前進約").append(String.format("%.0f", distance)).append("米，");
            } else if (distance < 1000) {
                sb.append("前進約").append(String.format("%.0f", distance)).append("米，");
            } else {
                double km = distance / 1000;
                sb.append("前進約").append(String.format("%.1f", km)).append("公里，");
            }
        }
        
        // 添加具體指令
        if (instruction != null && !instruction.isEmpty()) {
            // 清理和簡化指令
            String cleanInstruction = cleanInstructionForSpeech(instruction);
            sb.append(cleanInstruction);
        } else {
            sb.append("繼續直行");
        }
        
        return sb.toString();
    }
    
    private String buildEnglishInstruction(String instruction, double distance, String direction) {
        StringBuilder sb = new StringBuilder();
        
        // Add direction guidance
        if (direction != null && !direction.isEmpty()) {
            sb.append("Go ").append(direction.toLowerCase().replace("向", "")).append(", ");
        }
        
        // Add distance information
        if (distance > 0) {
            if (distance < 10) {
                sb.append("continue for about ").append(String.format("%.0f", distance)).append(" meters, ");
            } else if (distance < 100) {
                sb.append("continue for about ").append(String.format("%.0f", distance)).append(" meters, ");
            } else if (distance < 1000) {
                sb.append("continue for about ").append(String.format("%.0f", distance)).append(" meters, ");
            } else {
                double km = distance / 1000;
                sb.append("continue for about ").append(String.format("%.1f", km)).append(" kilometers, ");
            }
        }
        
        // Add specific instruction
        if (instruction != null && !instruction.isEmpty()) {
            String cleanInstruction = cleanInstructionForSpeech(instruction);
            sb.append(cleanInstruction);
        } else {
            sb.append("continue straight");
        }
        
        return sb.toString();
    }
    
    private String cleanInstructionForSpeech(String instruction) {
        // 移除不需要的字符和詞彙
        String cleaned = instruction
                .replaceAll("[\\[\\]\\(\\)]", "") // 移除括號
                .replaceAll("\\d+米", "") // 移除距離信息（已經在前面處理）
                .replaceAll("約", "")
                .replaceAll("大約", "")
                .replaceAll("左右", "")
                .trim();
        
        // 簡化常見指令
        cleaned = cleaned
                .replace("直行", "繼續直行")
                .replace("左轉", "向左轉")
                .replace("右轉", "向右轉")
                .replace("調頭", "調頭行駛")
                .replace("迴轉", "調頭行駛");
        
        return cleaned;
    }
    
    public void speakArrivalAlert() {
        String cantoneseText = "即將到達目的地，請注意減速";
        String englishText = "You are approaching your destination, please slow down";
        ttsManager.speak(cantoneseText, englishText, true);
    }
    
    public void speakDestinationReached() {
        String cantoneseText = "已到達目的地";
        String englishText = "You have arrived at your destination";
        ttsManager.speak(cantoneseText, englishText, true);
    }
    
    public void speakRouteCalculating() {
        String cantoneseText = "正在計算路線，請稍候";
        String englishText = "Calculating route, please wait";
        ttsManager.speak(cantoneseText, englishText, true);
    }
    
    public void speakRouteFound() {
        String cantoneseText = "路線計算完成，準備開始導航";
        String englishText = "Route calculated, ready to start navigation";
        ttsManager.speak(cantoneseText, englishText, true);
    }
    
    public void speakLocationPermissionRequest() {
        String cantoneseText = "需要位置權限才能使用導航功能";
        String englishText = "Location permission is required for navigation";
        ttsManager.speak(cantoneseText, englishText, true);
    }
    
    public void speakLocationFound() {
        String cantoneseText = "位置定位成功，可以開始導航";
        String englishText = "Location found, ready to navigate";
        ttsManager.speak(cantoneseText, englishText, true);
    }
    
    public void speakLocationError() {
        String cantoneseText = "無法獲取位置信息，請檢查GPS設置";
        String englishText = "Cannot get location, please check GPS settings";
        ttsManager.speak(cantoneseText, englishText, true);
    }
    
    public void speakDestinationNotFound() {
        String cantoneseText = "找不到指定目的地，請重新輸入";
        String englishText = "Destination not found, please try again";
        ttsManager.speak(cantoneseText, englishText, true);
    }
    
    public void speakNavigationStarted() {
        String cantoneseText = "導航開始，請按照語音指引行走";
        String englishText = "Navigation started, please follow voice guidance";
        ttsManager.speak(cantoneseText, englishText, true);
    }
    
    public void speakNavigationStopped() {
        String cantoneseText = "導航已停止";
        String englishText = "Navigation stopped";
        ttsManager.speak(cantoneseText, englishText, true);
    }
    
    public void speakLandmarkInfo(String landmarkName, double distance) {
        String cantoneseText = String.format("前方%.0f米有%s", distance, landmarkName);
        String englishText = String.format("%.0f meters ahead: %s", distance, landmarkName);
        ttsManager.speak(cantoneseText, englishText, true);
    }
}
