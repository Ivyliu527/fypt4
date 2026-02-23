package com.example.tonbo_app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * 開始出行頁面：語音目的地輸入 + 確認 + 導航
 * 使用 VoiceCommandManager 識別（非原生 SpeechRecognizer）
 */
public class StartTravelActivity extends BaseAccessibleActivity {
    private static final String TAG = "StartTravel";
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 100;
    private static final int PERMISSION_REQUEST_LOCATION = 101;

    private TextView promptText;
    private TextView statusText;
    private EditText recognitionEdit;
    private Button confirmButton;
    private Button micButton;

    private VoiceCommandManager voiceCommandManager;
    private LocationService locationService;
    private String pendingDestination = null;
    private boolean isListening = false;
    private boolean hasAnnouncedPageTitle = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_travel);

        promptText = findViewById(R.id.prompt_text);
        statusText = findViewById(R.id.status_text);
        recognitionEdit = findViewById(R.id.recognition_edit);
        confirmButton = findViewById(R.id.confirm_button);
        micButton = findViewById(R.id.mic_button);

        android.widget.ImageButton backButton = findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                vibrationManager.vibrateClick();
                if (ttsManager != null) ttsManager.stopSpeaking();
                if (isListening) {
                    voiceCommandManager.stopListening();
                    isListening = false;
                }
                finish();
            });
        }

        voiceCommandManager = VoiceCommandManager.getInstance(this);
        voiceCommandManager.setLanguage(currentLanguage);
        locationService = new LocationService(this);
        setupVoiceListener();
        updateLanguageUI();

        String destination = getIntent() != null ? getIntent().getStringExtra("destination") : null;
        if (!TextUtils.isEmpty(destination)) {
            recognitionEdit.setText(destination);
            recognitionEdit.setSelection(destination.length());
            statusText.setText(getLocalizedString("voice_status_recognized"));
        }

        micButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            if (isListening) {
                voiceCommandManager.stopListening();
                isListening = false;
                updateMicUI(false);
            } else {
                startListening();
            }
        });

        confirmButton.setOnClickListener(v -> {
            vibrationManager.vibrateClick();
            String dest = recognitionEdit != null ? recognitionEdit.getText().toString().trim() : "";
            if (TextUtils.isEmpty(dest)) {
                announceInfo(getLocalizedString("please_enter_destination"));
                return;
            }
            if (ttsManager != null) ttsManager.speak("现在为您导航去 " + dest, "现在为您导航去 " + dest, true);
            performNavigationWithGuards(dest);
        });
    }

    @Override
    protected void onDestroy() {
        if (voiceCommandManager != null) voiceCommandManager.stopListening();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (ttsManager != null) ttsManager.stopSpeaking();
        if (isListening) {
            voiceCommandManager.stopListening();
            isListening = false;
        }
    }

    @Override
    protected void announcePageTitle() {
        if (!hasAnnouncedPageTitle && ttsManager != null) {
            hasAnnouncedPageTitle = true;
            ttsManager.speak("开始出行，请说出目的地，例如：我要去天水围", "开始出行，请说出目的地，例如：我要去天水围", true);
            new Handler(Looper.getMainLooper()).postDelayed(this::startListening, 2500);
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void setupVoiceListener() {
        voiceCommandManager.setCommandListener(new VoiceCommandManager.ExtendedVoiceCommandListener() {
            @Override
            public void onTravelDestinationRecognized(TravelParseResult parseResult) {
                runOnUiThread(() -> {
                    if (parseResult == null || !parseResult.hasDestination()) return;
                    vibrationManager.vibrateSuccess();
                    isListening = false;
                    updateMicUI(false);
                    String raw = parseResult.destination;
                    statusText.setText("識別成功");
                    recognitionEdit.setText(raw);
                    recognitionEdit.setSelection(raw.length());
                    ttsSpeak("目的地已识别：" + raw);
                });
            }

            @Override
            public void onCommandRecognized(String command, String originalText) {
                runOnUiThread(() -> {
                    isListening = false;
                    updateMicUI(false);
                    ttsSpeak("未听清目的地，请再说一次");
                });
            }

            @Override
            public void onTextRecognized(String text) {
                runOnUiThread(() -> {
                    TravelParseResult parsed = voiceCommandManager.parseDestinationFromTravelPhrase(text);
                    if (parsed != null && parsed.hasDestination()) {
                        onTravelDestinationRecognized(parsed);
                        return;
                    }
                    isListening = false;
                    updateMicUI(false);
                    ttsSpeak("未听清目的地，请再说一次");
                });
            }

            @Override
            public void onListeningStarted() {
                runOnUiThread(() -> {
                    isListening = true;
                    updateMicUI(true);
                    statusText.setText("正在聆聽...");
                });
            }

            @Override
            public void onListeningStopped() {
                runOnUiThread(() -> {
                    isListening = false;
                    updateMicUI(false);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    isListening = false;
                    updateMicUI(false);
                    ttsSpeak("未听清目的地，请再说一次");
                });
            }

            @Override
            public void onPartialResult(String partialText) {
                runOnUiThread(() -> statusText.setText("識別中: " + partialText));
            }

            @Override
            public void onContinuousCommandsRecognized(java.util.List<VoiceCommandManager.CommandPair> commands, String originalText) {
                runOnUiThread(() -> {
                    isListening = false;
                    updateMicUI(false);
                    ttsSpeak("未听清目的地，请再说一次");
                });
            }
        });
    }

    private void startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ttsSpeak("请先授予录音权限");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_RECORD_AUDIO);
            return;
        }
        if (ttsManager != null) ttsManager.stopSpeaking();
        voiceCommandManager.startListening();
    }

    private void updateMicUI(boolean listening) {
        if (micButton == null) return;
        if (listening) {
            micButton.setText("⏸️");
            micButton.setContentDescription("停止聆聽");
        } else {
            micButton.setText("🎤");
            micButton.setContentDescription("開始說話");
        }
    }

    private void ttsSpeak(String text) {
        if (ttsManager != null) ttsManager.speak(text, text, true);
    }

    /**
     * 導航前守衛：跨區檢查 + 歧義 POI 處理
     */
    private void performNavigationWithGuards(String destination) {
        if (!hasLocationPermission()) {
            pendingDestination = destination;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_REQUEST_LOCATION);
            return;
        }

        locationService.checkCrossRegion(destination, (isCrossRegion, error) -> {
            runOnUiThread(() -> {
                if (error != null) {
                    doStartNavigation(destination);
                    return;
                }
                if (isCrossRegion) {
                    ttsSpeak("当前位置与目的地不在同一地区，暂不支持跨境导航");
                    statusText.setText("暫不支持跨境導航");
                    return;
                }

                locationService.searchNearbyPOIs(destination, (pois, poiError) -> {
                    runOnUiThread(() -> {
                        if (poiError != null || pois == null || pois.isEmpty()) {
                            doStartNavigation(destination);
                            return;
                        }
                        LocationService.POICandidate nearest = pois.get(0);
                        String resolved = nearest.name + (nearest.address != null && !nearest.address.isEmpty() ? " " + nearest.address : "");
                        if (pois.size() > 1) {
                            ttsSpeak("将为您导航到最近的 " + destination);
                        }
                        doStartNavigation(resolved);
                    });
                });
            });
        });
    }

    private void doStartNavigation(String destination) {
        Intent intent = new Intent(this, NavigationActivity.class);
        intent.putExtra("destination", destination);
        intent.putExtra("language", currentLanguage != null ? currentLanguage : LocaleManager.getInstance(this).getCurrentLanguage());
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListening();
        }
        if (requestCode == PERMISSION_REQUEST_LOCATION && grantResults.length > 0) {
            boolean granted = (grantResults[0] == PackageManager.PERMISSION_GRANTED) || (grantResults.length > 1 && grantResults[1] == PackageManager.PERMISSION_GRANTED);
            if (granted && pendingDestination != null) {
                performNavigationWithGuards(pendingDestination);
                pendingDestination = null;
            } else if (!granted) {
                pendingDestination = null;
                announceInfo(getLocalizedString("location_permission_denied"));
            }
        }
    }

    private void updateStatus(String status) {
        if (statusText != null) statusText.setText(status);
    }

    private void updateLanguageUI() {
        promptText.setText(getLocalizedString("say_destination_prompt"));
        statusText.setText(getLocalizedString("voice_status_ready"));
    }

    private String getLocalizedString(String key) {
        String lang = LocaleManager.getInstance(this).getCurrentLanguage();
        switch (key) {
            case "say_destination_prompt":
                return "mandarin".equals(lang) ? "请说出你要去的位置" : "請說出你要去的位置";
            case "voice_status_ready":
                return "mandarin".equals(lang) ? "就绪" : "就緒";
            case "voice_status_recognized":
                return "mandarin".equals(lang) ? "已识别" : "已識別";
            case "confirm_depart":
                return "mandarin".equals(lang) ? "确认出发" : "確認出發";
            case "please_enter_destination":
                return "mandarin".equals(lang) ? "请输入目的地" : "請輸入目的地";
            case "location_permission_denied":
                return "mandarin".equals(lang) ? "位置权限被拒绝，无法开始导航" : "位置權限被拒絕，無法開始導航";
            case "voice_status_getting_location":
                return "mandarin".equals(lang) ? "正在获取位置..." : "正在獲取位置...";
            case "voice_status_planning_route":
                return "mandarin".equals(lang) ? "正在规划路线..." : "正在規劃路線...";
            case "voice_status_navigating":
                return "mandarin".equals(lang) ? "导航中..." : "導航中...";
            case "voice_status_route_planned":
                return "mandarin".equals(lang) ? "路线已规划" : "路線已規劃";
            default:
                return getString(R.string.app_name);
        }
    }
}
