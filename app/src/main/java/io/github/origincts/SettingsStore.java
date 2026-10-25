package io.github.origincts;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;

/**
 * Feature flags live in Settings.Global so every hooked process can read them without depending on
 * the OriginCTS process staying unfrozen. SharedPreferences remains the pre-grant fallback.
 */
final class SettingsStore {
    static final Uri URI = Uri.parse("content://io.github.origincts.settings/features");
    static final String COLUMN = "features";
    static final String GLOBAL_KEY = "origincts_features";
    private static final int UNSET = -1;

    private static SharedPreferences prefs(Context context) {
        return context.createDeviceProtectedStorageContext().getSharedPreferences("features", Context.MODE_PRIVATE);
    }

    /** Global settings resolver; the system context keeps this usable from system_server too. */
    private static ContentResolver resolver() {
        try {
            Class<?> threadClass = Class.forName("android.app.ActivityThread");
            Object thread = threadClass.getMethod("currentActivityThread").invoke(null);
            if (thread == null) return null;
            Context system = (Context) threadClass.getMethod("getSystemContext").invoke(thread);
            return system == null ? null : system.getContentResolver();
        } catch (Throwable failure) {
            return null;
        }
    }
    static Integer readGlobal() {
        ContentResolver resolver = resolver();
        if (resolver == null) return null;
        try {
            int value = Settings.Global.getInt(resolver, GLOBAL_KEY, UNSET);
            return value == UNSET ? null : value & FeatureState.ALL;
        } catch (Throwable failure) {
            return null;
        }
    }
    static int read(Context context) {
        Integer global = readGlobal();
        return global != null ? global : prefs(context).getInt(COLUMN, FeatureState.ALL) & FeatureState.ALL;
    }
    static boolean write(Context context, int state) {
        int value = state & FeatureState.ALL;
        ContentResolver resolver = context.getContentResolver();
        boolean saved = false;
        try {
            // Granted by the installer (WRITE_SECURE_SETTINGS), so no shell is needed at runtime.
            saved = Settings.Global.putInt(resolver, GLOBAL_KEY, value);
        } catch (Throwable denied) {
            HookEntry.log("Global feature settings write denied: " + denied);
        }
        // Mirror into prefs so a revoked permission degrades to the provider instead of losing the choice.
        boolean stored = prefs(context).edit().putInt(COLUMN, value).commit();
        if (!saved && !stored) return false;
        context.getContentResolver().notifyChange(URI, null);
        return true;
    }
}
