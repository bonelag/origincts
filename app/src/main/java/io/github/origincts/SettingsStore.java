package io.github.origincts;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

final class SettingsStore {
    static final Uri URI = Uri.parse("content://io.github.origincts.settings/features");
    static final String COLUMN = "features";
    private static SharedPreferences prefs(Context context) {
        return context.createDeviceProtectedStorageContext().getSharedPreferences("features", Context.MODE_PRIVATE);
    }
    static int read(Context context) { return prefs(context).getInt(COLUMN, FeatureState.ALL) & FeatureState.ALL; }
    static boolean write(Context context, int state) {
        if (!prefs(context).edit().putInt(COLUMN, state & FeatureState.ALL).commit()) return false;
        context.getContentResolver().notifyChange(URI, null);
        return true;
    }
}
