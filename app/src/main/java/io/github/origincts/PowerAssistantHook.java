package io.github.origincts;

import android.content.ComponentName;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/** Uses Vivo's existing 500ms callback; the native detector retains key-up and power-menu handling. */
final class PowerAssistantHook {
    static void install(ClassLoader loader) {
        try {
            Class<?> policy = XposedHelpers.findClass("com.android.server.policy.VivoInputPolicy", loader);
            // The vendor wrapper can inline this small callback in an optimized system_server.
            Class<?> wrapper = XposedHelpers.findClass(
                    "com.android.server.policy.VivoPhoneWindowManagerImpl", loader);
            HookSupport.deoptimize(wrapper.getDeclaredMethod("startJoviVoiceByPowerKey"));
            HookSupport.required(XposedHelpers.findAndHookMethod(policy, "startJoviVoiceByPowerKey",
                    new XC_MethodHook() {
                        @Override protected void beforeHookedMethod(MethodHookParam param) {
                            try {
                                if (!HookSettings.enabled(FeatureState.GEMINI)) return;
                                if (!allowed(param.thisObject, loader)) return;
                                if (!showGoogleAssistant(loader)) return;
                                XposedHelpers.setBooleanField(param.thisObject, "mPowerKeyHandledByJovi", true);
                                // Native detector consumes key-up only if a session was accepted.
                                param.setResult(true);
                                HookEntry.log("Power hold: Google assistant session accepted");
                            } catch (Throwable failure) {
                                HookEntry.log("Power hold kept native behavior: " + failure);
                            }
                        }
                    }));
            HookEntry.log("Power assistant hook installed; native Vivo hold timer retained");
        } catch (Throwable failure) {
            HookEntry.log("Power assistant hook unavailable: " + failure);
        }
    }

    private static boolean allowed(Object policy, ClassLoader loader) {
        Object pwm = XposedHelpers.getObjectField(policy, "mPhoneWindowManager");
        if (!(boolean) XposedHelpers.callMethod(pwm, "isUserSetupComplete")
                || XposedHelpers.getBooleanField(policy, "mIsSupportAiKey")) return false;
        if (XposedHelpers.getBooleanField(policy, "mInUnderWaterCamera")
                && (boolean) XposedHelpers.callMethod(policy, "topActivityIsCamera")) return false;
        // Preserve the same special-mode restrictions as Vivo's startJoviVoice on both ROMs.
        Class<?> services = XposedHelpers.findClass("com.android.server.LocalServices", loader);
        Object notification = XposedHelpers.callStaticMethod(services, "getService",
                XposedHelpers.findClass("com.android.server.notification.NotificationManagerInternal", loader));
        Object power = XposedHelpers.callStaticMethod(services, "getService",
                XposedHelpers.findClass("android.os.PowerManagerInternal", loader));
        return notification != null && power != null
                && !(boolean) XposedHelpers.callMethod(notification, "isMeditationMode")
                && (int) XposedHelpers.callMethod(power, "getLastModeStatus") != 1;
    }

    static boolean showGoogleAssistant(ClassLoader loader) {
        long identity = Binder.clearCallingIdentity();
        try {
            Class<?> services = XposedHelpers.findClass("android.os.ServiceManager", loader);
            IBinder binder = (IBinder) XposedHelpers.callStaticMethod(services, "getService", "voiceinteraction");
            if (binder == null) return false;
            Class<?> stub = XposedHelpers.findClass(
                    "com.android.internal.app.IVoiceInteractionManagerService$Stub", loader);
            Object manager = XposedHelpers.callStaticMethod(stub, "asInterface", binder);
            ComponentName active = (ComponentName) XposedHelpers.callMethod(manager, "getActiveServiceComponentName");
            if (active == null || !HookEntry.GOOGLE.equals(active.getPackageName())) {
                HookEntry.log("Power hold: select Google as the default digital assistant and Gemini in Google App");
                return false;
            }
            Bundle args = new Bundle();
            args.putInt("invocation_type", 6); // Native PhoneWindowManager power-assistant source.
            args.putLong("invocation_time_ms", SystemClock.elapsedRealtime());
            // Both inspected ROMs use the same five-argument API; Google owns the overlay and consent.
            return (boolean) XposedHelpers.callMethod(manager, "showSessionForActiveService",
                    args, 3, null, null, null);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }
}
