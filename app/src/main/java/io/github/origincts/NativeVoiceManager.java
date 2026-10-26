package io.github.origincts;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

final class NativeVoiceManager {
    static final String ACTION_HOTWORD_SETTINGS =
            "com.google.android.googlequicksearchbox.action.HOTWORD_DETECTION_SETTINGS";

    static void apply(Context context, boolean enabled) {
        new Thread(() -> {
            try {
                if (enabled) {
                    String[] cmds = {
                        "pm grant com.google.android.googlequicksearchbox android.permission.RECORD_AUDIO",
                        "pm grant com.google.android.googlequicksearchbox android.permission.CAPTURE_AUDIO_HOTWORD",
                        "pm grant com.google.android.googlequicksearchbox android.permission.MANAGE_VOICE_KEYPHRASES",
                        "pm grant com.google.android.googlequicksearchbox android.permission.MANAGE_SOUND_TRIGGER",
                        "pm grant com.google.android.googlequicksearchbox android.permission.SOUND_TRIGGER_RUN_IN_BATTERY_SAVER",
                        "settings put secure voice_interaction_service com.google.android.googlequicksearchbox/com.google.android.voiceinteraction.GsaVoiceInteractionService",
                        "settings put secure assistant com.google.android.googlequicksearchbox/com.google.android.voiceinteraction.GsaVoiceInteractionService"
                    };
                    for (String cmd : cmds) {
                        runSu(cmd);
                    }
                }
            } catch (Throwable t) {
                HookEntry.log("NativeVoiceManager error: " + t);
            }
        }).start();
    }

    static void openSettings(Context context) {
        try {
            Intent intent = new Intent(ACTION_HOTWORD_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Throwable t) {
            Toast.makeText(context, "Could not open Voice Match settings: " + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private static void runSu(String command) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
            process.waitFor();
        } catch (Throwable ignored) {}
    }
}
