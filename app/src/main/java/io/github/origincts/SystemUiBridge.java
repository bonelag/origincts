package io.github.origincts;

import android.app.Activity;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.os.Handler;
import android.os.Looper;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/** Signature-protected request channel; only platform-signed SystemUI receives the narrow CTS permission exception. */
final class SystemUiBridge {
    private static boolean registered;

    static void install(ClassLoader loader) {
        Handler main = new Handler(Looper.getMainLooper());
        // Observe the concrete application, not an optimizable boot-class attach method.
        HookSupport.required(XposedHelpers.findAndHookMethod(
                "com.android.systemui.SystemUIApplication", loader, "onCreate", new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam param) {
                        if (!param.hasThrowable()) {
                            main.post(() -> tryRegister((Context) param.thisObject, loader));
                        }
                    }
                }));
        // Late loads must also handle an Application that has already been attached/created.
        main.post(() -> {
            try {
                Class<?> thread = XposedHelpers.findClass("android.app.ActivityThread", loader);
                Context app = (Context) XposedHelpers.callStaticMethod(thread, "currentApplication");
                if (app != null) tryRegister(app, loader);
                else HookEntry.log("SystemUI bridge waiting for Application.onCreate");
            } catch (Throwable failure) {
                HookEntry.log("SystemUI current Application lookup failed: " + failure);
            }
        });
        HookEntry.log("SystemUI bridge lifecycle hooks installed");
    }

    private static void tryRegister(Context context, ClassLoader loader) {
        if (registered) return;
        try {
            if (!BridgeContract.SYSTEM_UI.equals(context.getPackageName())) {
                HookEntry.log("SystemUI bridge unexpected context: " + context.getPackageName());
                return;
            }
            HookSettings.attach(context);
            register(context, loader);
            registered = true;
            HookEntry.log("SystemUI CTS bridge ready (signature-protected, no shell)");
        } catch (Throwable failure) {
            HookEntry.log("SystemUI bridge unavailable: " + failure);
        }
    }
    private static void register(Context context, ClassLoader loader) throws Exception {
        PackageManager pm = context.getPackageManager();
        PermissionInfo permission = pm.getPermissionInfo(BridgeContract.PERMISSION, 0);
        if (!BridgeContract.trustedPermission(permission.packageName, permission.protectionLevel,
                pm.checkPermission(BridgeContract.PERMISSION, BridgeContract.MODULE))) {
            throw new SecurityException("OriginCTS signature permission unavailable or owned by another package");
        }
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override public void onReceive(Context ignored, Intent intent) {
                if (!BridgeContract.ACTION.equals(intent.getAction())) return;
                try {
                    KeyguardManager keyguard = context.getSystemService(KeyguardManager.class);
                    if (keyguard == null || keyguard.isKeyguardLocked()) {
                        throw new SecurityException("Unlock the screen first.");
                    }

                    long requested = intent.getLongExtra("requested_at", 0);
                    if (requested > 0) HookEntry.log("Bridge dispatch delay ms="
                            + (android.os.SystemClock.uptimeMillis() - requested));
                    HookSettings.attach(context);
                    if (!HookSettings.enabled(FeatureState.MASTER)) {
                        throw new IllegalStateException("Features are disabled or settings are still loading.");
                    }
                    if (intent.getBooleanExtra("gemini", false)) {
                        if (!PowerAssistantHook.showGoogleAssistant(loader)) {
                            throw new IllegalStateException("Select Google as the default assistant and Gemini in Google App.");
                        }
                    } else {
                        NativeLauncher.start(loader);
                    }
                    setResultCode(Activity.RESULT_OK);
                    setResultData("Request sent.");
                } catch (Throwable failure) {
                    setResultCode(Activity.RESULT_CANCELED);
                    setResultData("Could not open assistant: " + failure.getMessage());
                    HookEntry.log("SystemUI launch failed: " + failure);
                }
            }
        };
        context.registerReceiver(receiver, new IntentFilter(BridgeContract.ACTION),
                BridgeContract.PERMISSION, new Handler(Looper.getMainLooper()), Context.RECEIVER_EXPORTED);
    }
}
