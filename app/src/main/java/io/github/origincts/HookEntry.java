package io.github.origincts;

import android.os.Build;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class HookEntry implements IXposedHookLoadPackage {
    static final String GOOGLE = "com.google.android.googlequicksearchbox";
    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam app) {
        if (Build.VERSION.SDK_INT != 35 && Build.VERSION.SDK_INT != 36) return;
        log("v0.10.3 sdk=" + Build.VERSION.SDK_INT + " package=" + app.packageName + " process=" + app.processName);
        if ("android".equals(app.packageName)
                && ("android".equals(app.processName) || "system_server".equals(app.processName))) {
            install("Settings", () -> HookSettings.install(app.classLoader, true));
            install("Framework", () -> FrameworkHooks.install(app.classLoader));
            install("Home", () -> HomeKeyHook.install(app.classLoader));
            install("Power", () -> PowerAssistantHook.install(app.classLoader));
            install("Voice", () -> VoiceWakeupHook.install(app.classLoader));
        } else if (BridgeContract.SYSTEM_UI.equals(app.packageName)
                && BridgeContract.SYSTEM_UI.equals(app.processName)) {
            install("Settings", () -> HookSettings.install(app.classLoader, false));
            install("Bridge", () -> SystemUiBridge.install(app.classLoader));
            install("SystemUI Home", () -> HomeKeyHook.installSystemUi(app.classLoader));
        } else if ("com.vivo.upslide".equals(app.packageName)
                && "com.vivo.upslide".equals(app.processName)) {
            install("Settings", () -> HookSettings.install(app.classLoader, false));
            install("Gestures", () -> VivoGestureHook.install(app.classLoader));
        } else if (GOOGLE.equals(app.packageName)
                && (GOOGLE.equals(app.processName) || (GOOGLE + ":search").equals(app.processName)
                || (GOOGLE + ":googleapp").equals(app.processName))) {
            install("Settings", () -> HookSettings.install(app.classLoader, false));
            install("Google", () -> GoogleHooks.install(app));
        }
    }
    private interface Installer { void run() throws Throwable; }
    private static void install(String name, Installer installer) {
        try { installer.run(); }
        catch (Throwable failure) { log(name + " hook unavailable: " + failure); }
    }
    static void log(String message) {
        android.util.Log.i("OriginCTS", message);
        XposedBridge.log("[OriginCTS] " + message);
    }
}
