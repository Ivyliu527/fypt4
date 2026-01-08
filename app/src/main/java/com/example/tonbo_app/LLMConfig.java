package com.example.tonbo_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * LLM configuration manager
 * Manages LLM API keys and settings
 */
public class LLMConfig {
    private static final String TAG = "LLMConfig";
    private static final String PREFS_NAME = "LLMConfig";
    
    // Configuration keys
    private static final String KEY_PROVIDER = "llm_provider";
    private static final String KEY_API_KEY = "llm_api_key";
    private static final String KEY_ENABLED = "llm_enabled";
    
    // Default values
    private static final String DEFAULT_PROVIDER = "zhipu";  // Use GLM-4-Flash as default (free)
    private static final boolean DEFAULT_ENABLED = true;  // Enable by default
    
    // API keys should be configured in LLMConfig.local.java (not committed to Git)
    // Template: Create LLMConfig.local.java with:
    //   public static final String DEEPSEEK_API_KEY = "your-deepseek-key";
    //   public static final String ZHIPU_API_KEY = "your-zhipu-key";
    
    // Fallback: Try to load from local config class
    private static String DEEPSEEK_API_KEY = getLocalApiKey("DEEPSEEK_API_KEY", "");
    private static String ZHIPU_API_KEY = getLocalApiKey("ZHIPU_API_KEY", "");
    
    /**
     * Get API key from local config class (if exists)
     */
    private static String getLocalApiKey(String keyName, String defaultValue) {
        try {
            // Try to load from LLMConfigLocal class (not in Git)
            Class<?> localConfigClass = Class.forName("com.example.tonbo_app.LLMConfigLocal");
            java.lang.reflect.Field field = localConfigClass.getField(keyName);
            return (String) field.get(null);
        } catch (Exception e) {
            // LLMConfigLocal not found, return default
            return defaultValue;
        }
    }
    
    private Context context;
    private SharedPreferences prefs;
    
    public LLMConfig(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Initialize with default API keys if not set
        initializeDefaultKeys();
    }
    
    /**
     * Initialize default API keys
     */
    private void initializeDefaultKeys() {
        String provider = prefs.getString(KEY_PROVIDER, null);
        String apiKey = prefs.getString(KEY_API_KEY, null);
        
        // Always use GLM-4-Flash as default (free, reliable)
        // If previously configured with DeepSeek, switch to GLM-4-Flash
        if (provider == null || apiKey == null || "deepseek".equals(provider)) {
            // Set default to GLM-4-Flash (free, no balance issues)
            setProvider("zhipu");
            setApiKey(ZHIPU_API_KEY);
            setEnabled(true);
            Log.d(TAG, "Initialized with GLM-4-Flash API key (free, reliable)");
        }
    }
    
    /**
     * Get current provider
     */
    public String getProvider() {
        return prefs.getString(KEY_PROVIDER, DEFAULT_PROVIDER);
    }
    
    /**
     * Set provider (deepseek or zhipu)
     */
    public void setProvider(String provider) {
        prefs.edit().putString(KEY_PROVIDER, provider).apply();
        
        // Auto-set API key based on provider
        if ("deepseek".equals(provider)) {
            if (DEEPSEEK_API_KEY != null && !DEEPSEEK_API_KEY.isEmpty()) {
                setApiKey(DEEPSEEK_API_KEY);
            } else {
                Log.w(TAG, "DeepSeek API key not configured in LLMConfigLocal.java");
            }
        } else if ("zhipu".equals(provider)) {
            if (ZHIPU_API_KEY != null && !ZHIPU_API_KEY.isEmpty()) {
                setApiKey(ZHIPU_API_KEY);
            } else {
                Log.w(TAG, "Zhipu API key not configured in LLMConfigLocal.java");
            }
        }
        
        Log.d(TAG, "Provider set to: " + provider);
    }
    
    /**
     * Get API key
     */
    public String getApiKey() {
        return prefs.getString(KEY_API_KEY, "");
    }
    
    /**
     * Set API key
     */
    public void setApiKey(String apiKey) {
        prefs.edit().putString(KEY_API_KEY, apiKey).apply();
        Log.d(TAG, "API key updated");
    }
    
    /**
     * Check if LLM is enabled
     */
    public boolean isEnabled() {
        return prefs.getBoolean(KEY_ENABLED, DEFAULT_ENABLED);
    }
    
    /**
     * Enable or disable LLM
     */
    public void setEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
        Log.d(TAG, "LLM enabled: " + enabled);
    }
    
    /**
     * Switch to DeepSeek
     */
    public void useDeepSeek() {
        setProvider("deepseek");
        if (DEEPSEEK_API_KEY != null && !DEEPSEEK_API_KEY.isEmpty()) {
            setApiKey(DEEPSEEK_API_KEY);
            Log.d(TAG, "Switched to DeepSeek");
        } else {
            Log.w(TAG, "DeepSeek API key not configured, switching to GLM-4-Flash");
            useGLM4Flash();
        }
    }
    
    /**
     * Switch to GLM-4-Flash (Zhipu)
     */
    public void useGLM4Flash() {
        setProvider("zhipu");
        if (ZHIPU_API_KEY != null && !ZHIPU_API_KEY.isEmpty()) {
            setApiKey(ZHIPU_API_KEY);
            Log.d(TAG, "Switched to GLM-4-Flash");
        } else {
            Log.w(TAG, "Zhipu API key not configured, LLM will be disabled");
            setEnabled(false);
        }
    }
    
    /**
     * Get configuration summary
     */
    public String getConfigSummary() {
        return String.format(
            "LLM Config:\n" +
            "  Enabled: %s\n" +
            "  Provider: %s\n" +
            "  API Key: %s...",
            isEnabled(),
            getProvider(),
            getApiKey().length() > 10 ? getApiKey().substring(0, 10) : "Not set"
        );
    }
}

