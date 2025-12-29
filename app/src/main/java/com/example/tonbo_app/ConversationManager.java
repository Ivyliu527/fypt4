package com.example.tonbo_app;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;

/**
 * 對話管理器
 * 管理對話上下文、歷史記錄和會話狀態
 */
public class ConversationManager {
    private static final String TAG = "ConversationManager";
    private static final int MAX_HISTORY_SIZE = 20; // 最多保存20輪對話
    
    // 對話歷史記錄
    private List<ConversationTurn> conversationHistory;
    
    // 當前會話狀態
    private ConversationState currentState;
    
    // 用戶信息（可擴展）
    private String userName;
    
    public enum ConversationState {
        IDLE,           // 空閒狀態
        LISTENING,      // 正在監聽
        PROCESSING,     // 正在處理
        RESPONDING      // 正在回應
    }
    
    /**
     * 對話輪次
     */
    public static class ConversationTurn {
        public String userInput;      // 用戶輸入
        public String assistantResponse; // 助手回應
        public boolean isCommand;      // 是否為命令
        public String commandType;    // 命令類型（如果為命令）
        public long timestamp;        // 時間戳
        
        public ConversationTurn(String userInput, String assistantResponse, boolean isCommand, String commandType) {
            this.userInput = userInput;
            this.assistantResponse = assistantResponse;
            this.isCommand = isCommand;
            this.commandType = commandType;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    public ConversationManager() {
        conversationHistory = new ArrayList<>();
        currentState = ConversationState.IDLE;
    }
    
    /**
     * 添加對話輪次
     */
    public void addTurn(String userInput, String assistantResponse, boolean isCommand, String commandType) {
        ConversationTurn turn = new ConversationTurn(userInput, assistantResponse, isCommand, commandType);
        conversationHistory.add(turn);
        
        // 限制歷史記錄大小
        if (conversationHistory.size() > MAX_HISTORY_SIZE) {
            conversationHistory.remove(0);
        }
        
        Log.d(TAG, "添加對話輪次: " + userInput + " -> " + assistantResponse);
    }
    
    /**
     * 獲取最近的對話歷史
     */
    public List<ConversationTurn> getRecentHistory(int count) {
        int start = Math.max(0, conversationHistory.size() - count);
        return new ArrayList<>(conversationHistory.subList(start, conversationHistory.size()));
    }
    
    /**
     * 獲取所有對話歷史
     */
    public List<ConversationTurn> getAllHistory() {
        return new ArrayList<>(conversationHistory);
    }
    
    /**
     * 獲取上下文信息（用於生成回應）
     */
    public String getContextSummary() {
        if (conversationHistory.isEmpty()) {
            return "";
        }
        
        StringBuilder summary = new StringBuilder();
        List<ConversationTurn> recent = getRecentHistory(3); // 最近3輪對話
        
        for (ConversationTurn turn : recent) {
            summary.append("用戶: ").append(turn.userInput).append("\n");
            if (turn.assistantResponse != null) {
                summary.append("助手: ").append(turn.assistantResponse).append("\n");
            }
        }
        
        return summary.toString();
    }
    
    /**
     * 檢查是否在討論某個話題
     */
    public boolean isDiscussingTopic(String topic) {
        if (conversationHistory.isEmpty()) {
            return false;
        }
        
        List<ConversationTurn> recent = getRecentHistory(3);
        for (ConversationTurn turn : recent) {
            if (turn.userInput.toLowerCase().contains(topic.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 獲取當前狀態
     */
    public ConversationState getCurrentState() {
        return currentState;
    }
    
    /**
     * 設置當前狀態
     */
    public void setCurrentState(ConversationState state) {
        this.currentState = state;
        Log.d(TAG, "對話狀態變更: " + state);
    }
    
    /**
     * 設置用戶名稱
     */
    public void setUserName(String name) {
        this.userName = name;
    }
    
    /**
     * 獲取用戶名稱
     */
    public String getUserName() {
        return userName != null ? userName : "朋友";
    }
    
    /**
     * 清除對話歷史
     */
    public void clearHistory() {
        conversationHistory.clear();
        Log.d(TAG, "對話歷史已清除");
    }
    
    /**
     * 獲取對話統計
     */
    public ConversationStats getStats() {
        int totalTurns = conversationHistory.size();
        int commandCount = 0;
        int chatCount = 0;
        
        for (ConversationTurn turn : conversationHistory) {
            if (turn.isCommand) {
                commandCount++;
            } else {
                chatCount++;
            }
        }
        
        return new ConversationStats(totalTurns, commandCount, chatCount);
    }
    
    /**
     * 對話統計
     */
    public static class ConversationStats {
        public int totalTurns;
        public int commandCount;
        public int chatCount;
        
        public ConversationStats(int totalTurns, int commandCount, int chatCount) {
            this.totalTurns = totalTurns;
            this.commandCount = commandCount;
            this.chatCount = chatCount;
        }
    }
}

