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
            .addCommands("open_environment", "打開環境識別", "環境識別", "睇下周圍")
            .addCommands("open_document", "打開閱讀助手", "閱讀助手", "讀文件", "掃描文件")
            .addCommands("open_find", "尋找物品", "搵嘢")
            .addCommands("open_assistance", "即時協助", "幫手")
            .addCommands("emergency", "緊急求助", "救命", "幫我")
            .addCommands("go_home", "返回主頁", "主頁")
            .addCommands("go_back", "返回")
            .addCommands("switch_language", "切換語言", "轉語言")
            .addCommands("open_settings", "打開設定", "設定")
            .addCommands("tell_time", "現在幾點", "幾點")
            .addCommands("stop_listening", "停止", "收聲")
            .build();
    }
    
    /**
     * 創建英文命令映射
     */
    public static Map<String, String> buildEnglishCommands() {
        return new VoiceCommandBuilder()
            .addCommands("open_environment", "open environment", "environment recognition", "look around")
            .addCommands("open_document", "open document", "document assistant", "read document", "scan document")
            .addCommands("open_find", "find items", "find object")
            .addCommands("open_assistance", "live assistance", "help")
            .addCommands("emergency", "emergency", "help me")
            .addCommands("go_home", "go home", "home")
            .addCommands("go_back", "go back")
            .addCommands("switch_language", "switch language", "change language")
            .addCommands("open_settings", "open settings", "settings")
            .addCommands("tell_time", "what time is it", "tell me the time")
            .addCommands("stop_listening", "stop", "stop listening")
            .build();
    }
    
    /**
     * 創建普通話命令映射
     */
    public static Map<String, String> buildMandarinCommands() {
        return new VoiceCommandBuilder()
            .addCommands("open_environment", "打開環境識別", "環境識別", "看看周圍")
            .addCommands("open_document", "打開閱讀助手", "閱讀助手", "讀文件", "掃描文件")
            .addCommands("open_find", "尋找物品", "找東西")
            .addCommands("open_assistance", "即時協助", "幫助")
            .addCommands("emergency", "緊急求助", "救命", "幫我")
            .addCommands("go_home", "返回主頁", "主頁")
            .addCommands("go_back", "返回")
            .addCommands("switch_language", "切換語言", "轉換語言")
            .addCommands("open_settings", "打開設置", "設置")
            .addCommands("tell_time", "現在幾點", "幾點了")
            .addCommands("stop_listening", "停止", "停")
            .build();
    }
}
