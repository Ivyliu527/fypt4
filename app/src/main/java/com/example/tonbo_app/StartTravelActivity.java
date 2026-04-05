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
            statusText.setText(getString(R.string.start_travel_recognition_ok));
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
                String msg = getString(R.string.please_enter_destination);
                if (ttsManager != null) ttsManager.speak(msg, msg, true);
                vibrationManager.vibrateError();
                return;
            }
            if (ttsManager != null) {
                String navMsg = getString(R.string.navigating_to_tts, dest);
                ttsManager.speak(navMsg, navMsg, true);
            }
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
            String intro = getString(R.string.start_travel_tts_intro);
            ttsManager.speak(intro, intro, true);
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
                    statusText.setText(getString(R.string.start_travel_recognition_ok));
                    recognitionEdit.setText(raw);
                    recognitionEdit.setSelection(raw.length());
                    ttsSpeak(getString(R.string.start_travel_dest_recognized, raw));
                });
            }

            @Override
            public void onCommandRecognized(String command, String originalText) {
                runOnUiThread(() -> {
                    isListening = false;
                    updateMicUI(false);
                    ttsSpeak(getString(R.string.start_travel_retry_dest));
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
                    ttsSpeak(getString(R.string.start_travel_retry_dest));
                });
            }

            @Override
            public void onListeningStarted() {
                runOnUiThread(() -> {
                    isListening = true;
                    updateMicUI(true);
                    statusText.setText(getString(R.string.voice_status_listening));
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
                    ttsSpeak(getString(R.string.start_travel_retry_dest));
                });
            }

            @Override
            public void onPartialResult(String partialText) {
                runOnUiThread(() -> statusText.setText(getString(R.string.recognizing_partial, partialText)));
            }

            @Override
            public void onContinuousCommandsRecognized(java.util.List<VoiceCommandManager.CommandPair> commands, String originalText) {
                runOnUiThread(() -> {
                    isListening = false;
                    updateMicUI(false);
                    ttsSpeak(getString(R.string.start_travel_retry_dest));
                });
            }
        });
    }

    private void startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ttsSpeak(getString(R.string.start_travel_need_mic_permission));
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
            micButton.setContentDescription(getString(R.string.mic_stop_listening));
        } else {
            micButton.setText("🎤");
            micButton.setContentDescription(getString(R.string.mic_start_speaking));
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
                    ttsSpeak(getString(R.string.start_travel_cross_region));
                    statusText.setText(getString(R.string.start_travel_cross_region_status));
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
                            ttsSpeak(getString(R.string.start_travel_nav_nearest, destination));
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
                String denied = getString(R.string.location_permission_denied_nav);
                if (ttsManager != null) ttsManager.speak(denied, denied, true);
                vibrationManager.vibrateError();
            }
        }
    }

    private void updateStatus(String status) {
        if (statusText != null) statusText.setText(status);
    }

    private void updateLanguageUI() {
        promptText.setText(getString(R.string.say_destination_prompt));
        statusText.setText(getString(R.string.voice_status_ready));
        TextView pageTitle = findViewById(R.id.page_title);
        if (pageTitle != null) {
            pageTitle.setText(getString(R.string.start_travel_voice));
        }
        if (confirmButton != null) {
            confirmButton.setText(getString(R.string.confirm_depart));
        }
        updateMicUI(isListening);
    }
}
