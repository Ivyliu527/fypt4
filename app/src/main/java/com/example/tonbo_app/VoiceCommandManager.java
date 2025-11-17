package com.example.tonbo_app;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 語音命令管理器
 * 負責語音識別和命令解析
 */
public class VoiceCommandManager {
    
    private static final String TAG = "VoiceCommandManager";
    private static final int REQUEST_CODE_SPEECH_INPUT = 1000;
    private static VoiceCommandManager instance;
    
    private Context context;
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private boolean isListening = false;
    
    // 命令映射表 - 廣東話
    private Map<String, String> cantoneseCommands = new HashMap<>();
    // 命令映射表 - 英文
    private Map<String, String> englishCommands = new HashMap<>();
    // 命令映射表 - 普通話
    private Map<String, String> mandarinCommands = new HashMap<>();
    
    // 當前語言
    private String currentLanguage = "cantonese";
    
    /**
     * 語音命令回調接口
     */
    public interface VoiceCommandListener {
        void onCommandRecognized(String command, String originalText);
        void onListeningStarted();
        void onListeningStopped();
        void onError(String error);
        void onPartialResult(String partialText);
    }
    
    private VoiceCommandListener commandListener;
    
    private VoiceCommandManager(Context context) {
        this.context = context.getApplicationContext();
        initializeCommands();
        initializeSpeechRecognizer();
    }
    
    public static synchronized VoiceCommandManager getInstance(Context context) {
        if (instance == null) {
            instance = new VoiceCommandManager(context);
        }
        return instance;
    }
    
    /**
     * 初始化命令映射表
     */
    private void initializeCommands() {
        cantoneseCommands = VoiceCommandBuilder.buildCantoneseCommands();
        englishCommands = VoiceCommandBuilder.buildEnglishCommands();
        mandarinCommands = VoiceCommandBuilder.buildMandarinCommands();
        
        Log.d(TAG, "命令映射表初始化完成");
    }
    
    /**
     * 初始化語音識別器
     */
    private void initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    Log.d(TAG, "準備接收語音");
                    isListening = true;
                    if (commandListener != null) {
                        commandListener.onListeningStarted();
                    }
                }
                
                @Override
                public void onBeginningOfSpeech() {
                    Log.d(TAG, "開始說話");
                }
                
                @Override
                public void onRmsChanged(float rmsdB) {
                    // 音量變化
                }
                
                @Override
                public void onBufferReceived(byte[] buffer) {
                    // 接收緩衝區數據
                }
                
                @Override
                public void onEndOfSpeech() {
                    Log.d(TAG, "說話結束");
                    isListening = false;
                }
                
                @Override
                public void onError(int error) {
                    Log.e(TAG, "語音識別錯誤: " + getErrorText(error));
                    isListening = false;
                    if (commandListener != null) {
                        commandListener.onError(getErrorText(error));
                        commandListener.onListeningStopped();
                    }
                }
                
                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        // 改進：使用多個識別結果提高準確率
                        String bestMatch = findBestMatchFromMultipleResults(matches);
                        if (bestMatch != null) {
                            Log.d(TAG, "最佳匹配結果: " + bestMatch + " (從 " + matches.size() + " 個候選中選擇)");
                            processCommand(bestMatch);
                        } else {
                            // 如果多個結果都無法匹配，使用第一個結果
                            String recognizedText = matches.get(0);
                            Log.d(TAG, "使用第一個識別結果: " + recognizedText);
                            processCommand(recognizedText);
                        }
                    } else {
                        Log.w(TAG, "沒有識別結果");
                        if (commandListener != null) {
                            commandListener.onError("沒有識別結果，請重試");
                        }
                    }
                    isListening = false;
                    if (commandListener != null) {
                        commandListener.onListeningStopped();
                    }
                }
                
                @Override
                public void onPartialResults(Bundle partialResults) {
                    ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String partialText = matches.get(0);
                        Log.d(TAG, "部分識別結果: " + partialText);
                        if (commandListener != null) {
                            commandListener.onPartialResult(partialText);
                        }
                    }
                }
                
                @Override
                public void onEvent(int eventType, Bundle params) {
                    // 其他事件
                }
            });
            
            Log.d(TAG, "語音識別器初始化成功");
        } else {
            Log.e(TAG, "設備不支持語音識別");
        }
    }
    
    /**
     * 設置語言
     */
    public void setLanguage(String language) {
        this.currentLanguage = language;
        Log.d(TAG, "語言已設置為: " + language);
    }
    
    /**
     * 開始監聽語音命令
     */
    public void startListening() {
        if (speechRecognizer == null) {
            initializeSpeechRecognizer();
        }
        
        if (speechRecognizer == null) {
            Log.e(TAG, "語音識別器初始化失敗");
            if (commandListener != null) {
                commandListener.onError("語音識別器初始化失敗");
            }
            return;
        }
        
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "錄音權限未授予");
            if (commandListener != null) {
                commandListener.onError("需要錄音權限");
            }
            return;
        }
        
        if (isListening) {
            Log.w(TAG, "已經在監聽中");
            return;
        }
        
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        
        // 根據當前語言設置識別語言
        Locale locale;
        switch (currentLanguage) {
            case "english":
                locale = Locale.ENGLISH;
                break;
            case "mandarin":
                locale = Locale.SIMPLIFIED_CHINESE;
                break;
            case "cantonese":
            default:
                locale = new Locale("zh", "HK");
                break;
        }
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, locale);
        
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        
        speechRecognizer.startListening(recognizerIntent);
        Log.d(TAG, "開始監聽語音命令 - 語言: " + currentLanguage);
    }
    
    /**
     * 停止監聽
     */
    public void stopListening() {
        if (speechRecognizer != null && isListening) {
            speechRecognizer.stopListening();
            isListening = false;
            Log.d(TAG, "停止監聽");
        }
    }
    
    /**
     * 從多個識別結果中找出最佳匹配
     */
    private String findBestMatchFromMultipleResults(ArrayList<String> matches) {
        if (matches == null || matches.isEmpty()) {
            return null;
        }
        
        String bestMatch = null;
        double bestScore = 0.0;
        
        // 遍歷所有識別結果，找出匹配度最高的
        for (String match : matches) {
            String command = matchCommand(match.toLowerCase());
            if (command != null) {
                // 計算匹配質量（第一個結果權重更高）
                double quality = 1.0 / (matches.indexOf(match) + 1);
                
                // 如果找到精確匹配，直接返回
                if (match.toLowerCase().contains(command.toLowerCase())) {
                    Log.d(TAG, "在多個結果中找到精確匹配: " + match + " -> " + command);
                    return match;
                }
                
                // 記錄最佳匹配
                if (quality > bestScore) {
                    bestScore = quality;
                    bestMatch = match;
                }
            }
        }
        
        return bestMatch;
    }
    
    /**
     * 處理命令
     */
    private void processCommand(String recognizedText) {
        String command = matchCommand(recognizedText.toLowerCase());
        
        if (command != null) {
            Log.d(TAG, "匹配到命令: " + command + " (原始文本: " + recognizedText + ")");
            if (commandListener != null) {
                commandListener.onCommandRecognized(command, recognizedText);
            }
        } else {
            Log.d(TAG, "未匹配到命令: " + recognizedText);
            if (commandListener != null) {
                commandListener.onError("未識別的命令: " + recognizedText);
            }
        }
    }
    
    /**
     * 匹配命令 - 改進版，支持模糊匹配
     */
    private String matchCommand(String text) {
        Map<String, String> commandMap;
        
        switch (currentLanguage) {
            case "english":
                commandMap = englishCommands;
                break;
            case "mandarin":
                commandMap = mandarinCommands;
                break;
            case "cantonese":
            default:
                commandMap = cantoneseCommands;
                break;
        }
        
        // 預處理：移除常見的語音識別干擾詞
        String processedText = preprocessText(text);
        String lowerText = processedText.toLowerCase().trim();
        
        // 1. 精確匹配（優先）
        for (Map.Entry<String, String> entry : commandMap.entrySet()) {
            String key = entry.getKey().toLowerCase();
            if (lowerText.contains(key)) {
                Log.d(TAG, "精確匹配: " + key + " -> " + entry.getValue());
                return entry.getValue();
            }
        }
        
        // 2. 模糊匹配（容錯）- 改進版
        String bestMatch = null;
        double bestScore = 0.0;
        double threshold = 0.55; // 降低閾值到0.55，提高容錯率
        
        // 收集所有可能的匹配（相似度 > 閾值）
        List<MatchCandidate> candidates = new ArrayList<>();
        
        for (Map.Entry<String, String> entry : commandMap.entrySet()) {
            String key = entry.getKey().toLowerCase();
            double similarity = calculateSimilarity(lowerText, key);
            
            if (similarity >= threshold) {
                candidates.add(new MatchCandidate(entry.getValue(), key, similarity));
                Log.d(TAG, "模糊匹配候選: " + key + " (相似度: " + String.format("%.2f", similarity) + ") -> " + entry.getValue());
            }
        }
        
        // 如果有多個候選，選擇相似度最高的
        if (!candidates.isEmpty()) {
            // 按相似度排序
            candidates.sort((a, b) -> Double.compare(b.similarity, a.similarity));
            bestMatch = candidates.get(0).command;
            bestScore = candidates.get(0).similarity;
            
            // 如果最高分和次高分差距很小（< 0.1），可能需要進一步判斷
            if (candidates.size() > 1) {
                double scoreDiff = candidates.get(0).similarity - candidates.get(1).similarity;
                if (scoreDiff < 0.1 && bestScore < 0.75) {
                    // 相似度太接近，可能不確定，但還是返回最高分
                    Log.w(TAG, "警告：多個候選相似度接近，選擇: " + bestMatch + " (分數: " + String.format("%.2f", bestScore) + ")");
                }
            }
            
            Log.d(TAG, "模糊匹配成功: " + bestMatch + " (相似度: " + String.format("%.2f", bestScore) + ")");
            return bestMatch;
        }
        
        // 3. 部分匹配（最後嘗試）
        for (Map.Entry<String, String> entry : commandMap.entrySet()) {
            String key = entry.getKey().toLowerCase();
            // 檢查是否包含關鍵詞（至少3個字符）
            if (key.length() >= 3 && lowerText.contains(key.substring(0, Math.min(3, key.length())))) {
                Log.d(TAG, "部分匹配: " + key + " -> " + entry.getValue());
                return entry.getValue();
            }
        }
        
        return null;
    }
    
    /**
     * 計算兩個字符串的相似度（改進版 - 使用 Levenshtein 距離和多重策略）
     */
    private double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        if (s1.equals(s2)) return 1.0;
        
        // 移除空格和標點符號進行比較
        String clean1 = s1.replaceAll("[\\s\\p{Punct}]", "");
        String clean2 = s2.replaceAll("[\\s\\p{Punct}]", "");
        
        if (clean1.equals(clean2)) return 0.95; // 幾乎完全匹配
        
        // 1. 計算 Levenshtein 距離相似度（最準確）
        double levenshteinScore = calculateLevenshteinSimilarity(clean1, clean2);
        
        // 2. 計算最長公共子串相似度
        double lcsScore = calculateLCSimilarity(clean1, clean2);
        
        // 3. 計算字符集合相似度（容錯同音字）
        double charSetScore = calculateCharSetSimilarity(clean1, clean2);
        
        // 4. 計算開頭匹配度（語音識別通常開頭較準確）
        double prefixScore = calculatePrefixSimilarity(clean1, clean2);
        
        // 加權平均（Levenshtein 權重最高）
        double finalScore = (levenshteinScore * 0.4) + 
                           (lcsScore * 0.3) + 
                           (charSetScore * 0.2) + 
                           (prefixScore * 0.1);
        
        return finalScore;
    }
    
    /**
     * 計算 Levenshtein 距離相似度（編輯距離）
     */
    private double calculateLevenshteinSimilarity(String s1, String s2) {
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;
        
        int distance = levenshteinDistance(s1, s2);
        return 1.0 - ((double) distance / maxLen);
    }
    
    /**
     * Levenshtein 距離算法（動態規劃）
     */
    private int levenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        
        int[][] dp = new int[len1 + 1][len2 + 1];
        
        // 初始化
        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }
        
        // 動態規劃計算
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1,      // 刪除
                                dp[i][j - 1] + 1),      // 插入
                        dp[i - 1][j - 1] + 1            // 替換
                    );
                }
            }
        }
        
        return dp[len1][len2];
    }
    
    /**
     * 計算最長公共子串相似度
     */
    private double calculateLCSimilarity(String s1, String s2) {
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;
        
        int lcsLength = longestCommonSubstring(s1, s2);
        return (double) lcsLength / maxLen;
    }
    
    /**
     * 計算最長公共子串長度
     */
    private int longestCommonSubstring(String s1, String s2) {
        int maxLen = 0;
        int len1 = s1.length();
        int len2 = s2.length();
        
        for (int i = 0; i < len1; i++) {
            for (int j = 0; j < len2; j++) {
                int k = 0;
                while (i + k < len1 && j + k < len2 && 
                       s1.charAt(i + k) == s2.charAt(j + k)) {
                    k++;
                }
                maxLen = Math.max(maxLen, k);
            }
        }
        
        return maxLen;
    }
    
    /**
     * 計算字符集合相似度（容錯同音字和順序錯誤）
     */
    private double calculateCharSetSimilarity(String s1, String s2) {
        if (s1.length() == 0 && s2.length() == 0) return 1.0;
        if (s1.length() == 0 || s2.length() == 0) return 0.0;
        
        // 計算字符頻率
        Map<Character, Integer> freq1 = new HashMap<>();
        Map<Character, Integer> freq2 = new HashMap<>();
        
        for (char c : s1.toCharArray()) {
            freq1.put(c, freq1.getOrDefault(c, 0) + 1);
        }
        for (char c : s2.toCharArray()) {
            freq2.put(c, freq2.getOrDefault(c, 0) + 1);
        }
        
        // 計算共同字符數
        int commonChars = 0;
        int totalChars = Math.max(s1.length(), s2.length());
        
        for (char c : freq1.keySet()) {
            if (freq2.containsKey(c)) {
                commonChars += Math.min(freq1.get(c), freq2.get(c));
            }
        }
        
        return (double) commonChars / totalChars;
    }
    
    /**
     * 計算前綴相似度（語音識別通常開頭較準確）
     */
    private double calculatePrefixSimilarity(String s1, String s2) {
        int minLen = Math.min(s1.length(), s2.length());
        if (minLen == 0) return 0.0;
        
        int matchLen = 0;
        for (int i = 0; i < minLen; i++) {
            if (s1.charAt(i) == s2.charAt(i)) {
                matchLen++;
            } else {
                break;
            }
        }
        
        return (double) matchLen / minLen;
    }
    
    /**
     * 設置命令監聽器
     */
    public void setCommandListener(VoiceCommandListener listener) {
        this.commandListener = listener;
    }
    
    /**
     * 是否正在監聽
     */
    public boolean isListening() {
        return isListening;
    }
    
    /**
     * 獲取錯誤文本
     */
    private String getErrorText(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "音頻錯誤";
            case SpeechRecognizer.ERROR_CLIENT:
                return "客戶端錯誤";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "權限不足";
            case SpeechRecognizer.ERROR_NETWORK:
                return "網絡錯誤";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "網絡超時";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "沒有匹配結果";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "識別器忙碌";
            case SpeechRecognizer.ERROR_SERVER:
                return "服務器錯誤";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "語音超時";
            default:
                return "未知錯誤";
        }
    }
    
    /**
     * 預處理文本，移除語音識別常見的干擾詞和錯誤
     */
    private String preprocessText(String text) {
        if (text == null) return "";
        
        // 移除常見的語音識別干擾詞（中文）
        String processed = text
            .replaceAll("(請|幫|幫我|幫你|幫他)", "")  // 移除"請"、"幫"等前綴
            .replaceAll("(一下|一下下|一下兒)", "")     // 移除"一下"等後綴
            .replaceAll("(的|地|得)", "")              // 移除助詞
            .replaceAll("(啊|呀|呢|吧|嗎)", "")        // 移除語氣詞
            .replaceAll("\\s+", " ")                   // 多個空格合併為一個
            .trim();
        
        // 處理常見的同音字替換（針對中文）
        if (currentLanguage.equals("cantonese") || currentLanguage.equals("mandarin")) {
            processed = processed
                .replace("識別", "识别")  // 簡繁轉換容錯
                .replace("識", "识")
                .replace("設", "设")
                .replace("開", "开")
                .replace("關", "关");
        }
        
        return processed;
    }
    
    /**
     * 匹配候選類（用於排序和選擇最佳匹配）
     */
    private static class MatchCandidate {
        String command;
        String key;
        double similarity;
        
        MatchCandidate(String command, String key, double similarity) {
            this.command = command;
            this.key = key;
            this.similarity = similarity;
        }
    }
    
    /**
     * 釋放資源
     */
    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
            Log.d(TAG, "語音識別器已釋放");
        }
        isListening = false;
    }
}