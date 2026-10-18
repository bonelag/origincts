package io.github.origincts;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.PowerManager;
import android.provider.Settings;
import de.robv.android.xposed.XposedHelpers;

/** Guards shared by navigation hooks; one accepted physical hold sends one request. */
final class NavigationLaunch {

    static boolean start(Context context, ClassLoader loader, int entrypoint, Runnable consumeInput) {
        return start(context, loader, entrypoint, null, consumeInput);
    }
    static boolean start(Context context, ClassLoader loader, int entrypoint, Runnable before, Runnable accepted) {
        if (!HookSettings.enabled(FeatureState.CTS)) return false;
        try {
            KeyguardManager keyguard = context.getSystemService(KeyguardManager.class);
            PowerManager power = context.getSystemService(PowerManager.class);
            ActivityManager activity = context.getSystemService(ActivityManager.class);
            if (keyguard == null || keyguard.isKeyguardLocked() || power == null
                    || !power.isInteractive() || activity == null
                    || activity.getLockTaskModeState() != ActivityManager.LOCK_TASK_MODE_NONE) {
                HookEntry.log("Navigation blocked by lock/power/pinning state");
                return false;
            }
            int user = (int) XposedHelpers.callStaticMethod(
                    XposedHelpers.findClass("android.app.ActivityManager", loader), "getCurrentUser");
            int setup = (int) XposedHelpers.callStaticMethod(Settings.Secure.class,
                    "getIntForUser", context.getContentResolver(), "user_setup_complete", 0, user);
            if (setup != 1) { HookEntry.log("Navigation blocked by incomplete user setup"); return false; }
            LaunchSequence.run(before, NativeLauncher.prepare(loader, entrypoint), accepted,
                    failure -> HookEntry.log("CTS accepted; input cleanup failed: " + failure));
            HookEntry.log("Navigation CTS request sent, entrypoint=" + entrypoint);
            return true;
        } catch (Throwable failure) {
            HookEntry.log("Navigation CTS unavailable: " + failure);
            return false;
        }
    }
}
