package com.example.tonbo_app;

import android.content.Context;
import android.util.Log;

/**
 * Java類調用Kotlin代碼的示例
 * 展示Java和Kotlin的互操作性
 */
public class JavaKotlinInteropExample {
    private static final String TAG = "JavaKotlinInterop";
    
    /**
     * 在Java中調用Kotlin代碼
     */
    public static void demonstrateInterop(Context context) {
        Log.d(TAG, "=== Java調用Kotlin示例 ===");
        
        // 1. 調用Kotlin object的方法
        KotlinUtils.INSTANCE.performAsyncOperation(context, result -> {
            Log.d(TAG, "Java收到Kotlin回調: " + result);
            return null; // 明確返回null
        });
        
        // 2. 使用Kotlin數據類
        KotlinUtils.UserInfo userInfo = new KotlinUtils.UserInfo(
            "視障用戶", 
            "cantonese", 
            true
        );
        Log.d(TAG, "Kotlin數據類: " + userInfo);
        
        // 3. 使用Kotlin擴展函數 - 通過KotlinUtils調用
        String command = "打開環境識別";
        boolean isValid = KotlinUtils.INSTANCE.isValidCommand(command);
        Log.d(TAG, "命令 '" + command + "' 是否有效: " + isValid);
        
        // 4. 使用Kotlin高階函數
        java.util.List<String> commands = java.util.Arrays.asList(
            "打開環境識別", 
            "讀文件", 
            "緊急求助", 
            "ab" // 無效命令
        );
        
        java.util.List<String> validCommands = KotlinUtils.INSTANCE.processVoiceCommands(
            commands, 
            cmd -> cmd.length() > 3 // Java lambda
        );
        
        Log.d(TAG, "有效命令: " + validCommands);
        
        // 5. 使用Kotlin空安全函數
        String safeResult = KotlinUtils.INSTANCE.safeGetLanguage(context);
        Log.d(TAG, "空安全結果: " + safeResult);
        
        Log.d(TAG, "=== Java-Kotlin互操作完成 ===");
    }
}
