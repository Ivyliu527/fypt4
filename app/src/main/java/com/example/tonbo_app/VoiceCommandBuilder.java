package com.example.tonbo_app;

import java.util.HashMap;
import java.util.Map;

/**
 * 語音命令映射構建器
 * 使用Builder模式優化命令初始化
 */
public class VoiceCommandBuilder {
    
    private final Map<String, String> commandMap = new HashMap<>();
    
    /**
     * 添加命令映射
     */
    public VoiceCommandBuilder addCommand(String voiceCommand, String action) {
        commandMap.put(voiceCommand, action);
        return this;
    }
    
    /**
     * 批量添加命令
     */
    public VoiceCommandBuilder addCommands(String action, String... voiceCommands) {
        for (String voiceCommand : voiceCommands) {
            commandMap.put(voiceCommand, action);
        }
        return this;
    }
    
    /**
     * 構建命令映射
     */
    public Map<String, String> build() {
        return new HashMap<>(commandMap);
    }
    
    /**
     * 創建廣東話命令映射
     */
    public static Map<String, String> buildCantoneseCommands() {
        return new VoiceCommandBuilder()
            // 環境識別相關
            .addCommands("open_environment", "打開環境識別", "環境識別", "睇下周圍", "環境", "識別環境", "打開環境", "環境檢測")
            // 環境識別控制
            .addCommands("start_detection", "開始檢測", "開始識別", "開始掃描", "開始環境識別")
            .addCommands("stop_detection", "停止檢測", "停止識別", "停止掃描", "停止環境識別")
            .addCommands("describe_environment", "描述環境", "描述周圍", "講下周圍", "周圍有咩")
            // 文件閱讀
            .addCommands("open_document", "打開閱讀助手", "閱讀助手", "讀文件", "掃描文件", "文件助手", "閱讀", "讀文件", "掃描")
            // 尋找物品
            .addCommands("open_find", "尋找物品", "搵嘢", "找東西", "尋找", "搵物品")
            // 即時協助
            .addCommands("open_assistance", "即時協助", "幫手", "協助", "即時幫助", "需要幫助")
            // 緊急求助
            .addCommands("emergency", "緊急求助", "救命", "幫我", "緊急", "求助", "緊急情況")
            // 導航
            .addCommands("go_home", "返回主頁", "主頁", "回到主頁", "去主頁", "主畫面")
            .addCommands("go_back", "返回", "返回上一頁", "上一頁", "後退")
            // 語言切換
            .addCommands("switch_language", "切換語言", "轉語言", "換語言", "改變語言")
            // 設定
            .addCommands("open_settings", "打開設定", "設定", "設置", "系統設定", "打開設置")
            // 時間查詢
            .addCommands("tell_time", "現在幾點", "幾點", "時間", "現在時間", "幾點鐘")
            // 控制命令
            .addCommands("stop_listening", "停止", "收聲", "停止監聽", "停止語音", "停止識別")
            .addCommands("repeat", "重複", "再說一次", "重複剛才", "再說一遍")
            .addCommands("volume_up", "增大音量", "音量增大", "大聲啲", "提高音量")
            .addCommands("volume_down", "減小音量", "音量減小", "細聲啲", "降低音量")
            // 聊天記錄命令
            .addCommands("view_chat_history", "查看聊天記錄", "聊天記錄", "對話記錄", "查看記錄", "歷史記錄")
            .addCommands("previous_message", "上一句", "上一條", "上一條消息", "前一句")
            .addCommands("next_message", "下一句", "下一條", "下一條消息", "後一句")
            .addCommands("repeat_last_message", "重複上一句", "再說上一句", "重複剛才的話")
            .addCommands("clear_chat_history", "清除聊天記錄", "清空記錄", "清除記錄")
            .build();
    }
    
    /**
     * 創建英文命令映射
     */
    public static Map<String, String> buildEnglishCommands() {
        return new VoiceCommandBuilder()
            // 環境識別相關
            .addCommands("open_environment", "open environment", "environment recognition", "look around", "environment", "detect environment", "scan environment")
            // 環境識別控制
            .addCommands("start_detection", "start detection", "start scanning", "begin detection", "start recognizing")
            .addCommands("stop_detection", "stop detection", "stop scanning", "end detection", "stop recognizing")
            .addCommands("describe_environment", "describe environment", "describe surroundings", "what's around", "tell me what's around")
            // 文件閱讀
            .addCommands("open_document", "open document", "document assistant", "read document", "scan document", "document", "read", "scan")
            // 尋找物品
            .addCommands("open_find", "find items", "find object", "find things", "search items", "locate items")
            // 即時協助
            .addCommands("open_assistance", "live assistance", "help", "assistance", "need help", "get help")
            // 緊急求助
            .addCommands("emergency", "emergency", "help me", "emergency help", "urgent help", "call emergency")
            // 導航
            .addCommands("go_home", "go home", "home", "return home", "back to home", "main screen")
            .addCommands("go_back", "go back", "back", "previous page", "return")
            // 語言切換
            .addCommands("switch_language", "switch language", "change language", "change to", "set language")
            // 設定
            .addCommands("open_settings", "open settings", "settings", "system settings", "preferences")
            // 時間查詢
            .addCommands("tell_time", "what time is it", "tell me the time", "time", "current time", "what's the time")
            // 控制命令
            .addCommands("stop_listening", "stop", "stop listening", "stop voice", "stop recognition", "cancel")
            .addCommands("repeat", "repeat", "say again", "repeat that", "say it again")
            .addCommands("volume_up", "volume up", "increase volume", "louder", "turn up volume")
            .addCommands("volume_down", "volume down", "decrease volume", "quieter", "turn down volume")
            // 聊天記錄命令
            .addCommands("view_chat_history", "view chat history", "chat history", "conversation history", "show history", "history")
            .addCommands("previous_message", "previous message", "last message", "previous", "go back")
            .addCommands("next_message", "next message", "next", "go forward")
            .addCommands("repeat_last_message", "repeat last message", "say last message again", "repeat previous")
            .addCommands("clear_chat_history", "clear chat history", "clear history", "delete history")
            .build();
    }
    
    /**
     * 創建普通話命令映射
     */
    public static Map<String, String> buildMandarinCommands() {
        return new VoiceCommandBuilder()
            // 環境識別相關
            .addCommands("open_environment", "打開環境識別", "環境識別", "看看周圍", "環境", "識別環境", "打開環境", "環境檢測")
            // 環境識別控制
            .addCommands("start_detection", "開始檢測", "開始識別", "開始掃描", "開始環境識別")
            .addCommands("stop_detection", "停止檢測", "停止識別", "停止掃描", "停止環境識別")
            .addCommands("describe_environment", "描述環境", "描述周圍", "說說周圍", "周圍有什麼")
            // 文件閱讀
            .addCommands("open_document", "打開閱讀助手", "閱讀助手", "讀文件", "掃描文件", "文件助手", "閱讀", "掃描")
            // 尋找物品
            .addCommands("open_find", "尋找物品", "找東西", "尋找", "找物品", "搜索物品")
            // 即時協助
            .addCommands("open_assistance", "即時協助", "幫助", "協助", "即時幫助", "需要幫助")
            // 緊急求助
            .addCommands("emergency", "緊急求助", "救命", "幫我", "緊急", "求助", "緊急情況")
            // 導航
            .addCommands("go_home", "返回主頁", "主頁", "回到主頁", "去主頁", "主畫面")
            .addCommands("go_back", "返回", "返回上一頁", "上一頁", "後退")
            // 語言切換
            .addCommands("switch_language", "切換語言", "轉換語言", "換語言", "改變語言")
            // 設定
            .addCommands("open_settings", "打開設置", "設置", "系統設置", "打開設定")
            // 時間查詢
            .addCommands("tell_time", "現在幾點", "幾點了", "時間", "現在時間", "幾點鐘")
            // 控制命令
            .addCommands("stop_listening", "停止", "停", "停止監聽", "停止語音", "停止識別")
            .addCommands("repeat", "重複", "再說一次", "重複剛才", "再說一遍")
            .addCommands("volume_up", "增大音量", "音量增大", "大聲點", "提高音量")
            .addCommands("volume_down", "減小音量", "音量減小", "小聲點", "降低音量")
            // 聊天記錄命令
            .addCommands("view_chat_history", "查看聊天記錄", "聊天記錄", "對話記錄", "查看記錄", "歷史記錄")
            .addCommands("previous_message", "上一句", "上一條", "上一條消息", "前一句")
            .addCommands("next_message", "下一句", "下一條", "下一條消息", "後一句")
            .addCommands("repeat_last_message", "重複上一句", "再說上一句", "重複剛才的話")
            .addCommands("clear_chat_history", "清除聊天記錄", "清空記錄", "清除記錄")
            .build();
    }
}
