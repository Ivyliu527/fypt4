package com.example.tonbo_app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 登入Activity
 * 提供用戶登入功能，為Firebase集成做準備
 */
public class LoginActivity extends BaseAccessibleActivity {
    private static final String TAG = "LoginActivity";
    
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button registerButton;
    private Button guestButton;
    private TextView statusText;
    private LinearLayout loginForm;
    
    private UserManager userManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        initViews();
        setupButtons();
        announcePageTitle();
        
        // 初始化用戶管理器
        userManager = UserManager.getInstance(this);
    }
    
    private void initViews() {
        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        loginButton = findViewById(R.id.login_button);
        registerButton = findViewById(R.id.register_button);
        guestButton = findViewById(R.id.guest_button);
        statusText = findViewById(R.id.status_text);
        loginForm = findViewById(R.id.login_form);
        
        // 設置返回按鈕
        android.widget.ImageButton backButton = findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                vibrationManager.vibrateClick();
                announceInfo(getString(R.string.going_back_to_home));
                finish();
            });
        }
        
        // 設置無障礙內容描述
        emailEditText.setContentDescription(getString(R.string.email_input_desc));
        passwordEditText.setContentDescription(getString(R.string.password_input_desc));
    }
    
    private void setupButtons() {
        // 登入按鈕
        loginButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            attemptLogin();
        });
        
        // 註冊按鈕
        registerButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            announceInfo(getString(R.string.register_feature_coming_soon));
            // TODO: 實現註冊功能
        });
        
        // 訪客登入按鈕
        guestButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            loginAsGuest();
        });
    }
    
    private void attemptLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        
        // 驗證輸入
        if (TextUtils.isEmpty(email)) {
            announceError(getString(R.string.email_required));
            emailEditText.requestFocus();
            return;
        }
        
        if (TextUtils.isEmpty(password)) {
            announceError(getString(R.string.password_required));
            passwordEditText.requestFocus();
            return;
        }
        
        if (!isValidEmail(email)) {
            announceError(getString(R.string.invalid_email));
            emailEditText.requestFocus();
            return;
        }
        
        // 顯示登入狀態
        updateStatus(getString(R.string.logging_in));
        announceInfo(getString(R.string.logging_in));
        
        // 模擬登入過程（之後會替換為Firebase認證）
        performLogin(email, password);
    }
    
    private void performLogin(String email, String password) {
        // TODO: 替換為Firebase Authentication
        // 目前使用模擬登入
        Log.d(TAG, "嘗試登入: " + email);
        
        // 模擬網絡延遲
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            // 模擬登入成功（之後會根據Firebase結果判斷）
            boolean loginSuccess = simulateLoginResult(email, password);
            
            if (loginSuccess) {
                // 保存用戶登入狀態
                userManager.setUserLoggedIn(true);
                userManager.setCurrentUserEmail(email);
                
                // 登入成功，跳轉到主頁
                announceSuccess(getString(R.string.login_success));
                navigateToMainActivity();
            } else {
                // 登入失敗
                announceError(getString(R.string.login_failed));
                updateStatus(getString(R.string.login_failed));
            }
        }, 2000); // 2秒延遲模擬網絡請求
    }
    
    private boolean simulateLoginResult(String email, String password) {
        // 模擬登入邏輯（之後會替換為Firebase）
        // 簡單的測試：如果密碼長度大於5就成功
        return password.length() > 5;
    }
    
    private void loginAsGuest() {
        announceInfo(getString(R.string.logging_in_as_guest));
        
        // 設置為訪客模式
        userManager.setUserLoggedIn(false);
        userManager.setCurrentUserEmail(null);
        userManager.setGuestMode(true);
        
        // 跳轉到主頁
        navigateToMainActivity();
    }
    
    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("language", currentLanguage);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
    
    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
    
    private void updateStatus(String message) {
        if (statusText != null) {
            statusText.setText(message);
        }
    }
    
    @Override
    protected void announcePageTitle() {
        String title = getString(R.string.login_title);
        String description = getString(R.string.login_description);
        String fullAnnouncement = title + "。" + description;
        
        Log.d(TAG, "🔊 播報頁面標題: " + fullAnnouncement);
        
        // 根據當前語言播報
        String currentLang = LocaleManager.getInstance(this).getCurrentLanguage();
        switch (currentLang) {
            case "english":
                ttsManager.speak(null, "Login Page. " + getEnglishDescription(), true);
                break;
            case "mandarin":
                ttsManager.speak(getSimplifiedChineseDescription(), null, true);
                break;
            case "cantonese":
            default:
                ttsManager.speak(fullAnnouncement, null, true);
                break;
        }
    }
    
    private String getEnglishDescription() {
        return "Please enter your email and password to login, or continue as guest.";
    }
    
    private String getSimplifiedChineseDescription() {
        return "请输入您的邮箱和密码登录，或继续以访客身份使用。";
    }
    
    @Override
    protected void startEnvironmentActivity() {
        // 重寫父類方法，避免語音命令衝突
    }
    
    @Override
    protected void startDocumentCurrencyActivity() {
        // 重寫父類方法，避免語音命令衝突
    }
    
    @Override
    protected void startFindItemsActivity() {
        // 重寫父類方法，避免語音命令衝突
    }
    
    @Override
    protected void startSettingsActivity() {
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra("language", currentLanguage);
        startActivity(intent);
    }
    
    @Override
    protected void handleEmergencyCommand() {
        announceInfo(getString(R.string.emergency_location_feature_coming_soon));
    }
    
    @Override
    protected void goToHome() {
        navigateToMainActivity();
    }
    
    @Override
    protected void handleLanguageSwitch() {
        announceInfo(getString(R.string.language_switch_feature_coming_soon));
    }
    
    @Override
    protected void stopCurrentOperation() {
        announceInfo(getString(R.string.operation_stopped));
    }
}

