package io.github.origincts;

import android.content.res.Resources;
import android.os.Binder;
import android.os.Build;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/** Keep CTS and MediaProjection's provider checks consistent inside system_server. */
final class ProviderConfig {
    static void install(ClassLoader loader) throws Exception {
        int id = Resources.getSystem().getIdentifier(
                "config_defaultContextualSearchPackageName", "string", "android");
        if (id == 0) throw new IllegalStateException("CTS provider resource not found");
        if (Build.VERSION.SDK_INT >= 36) {
            Class<?> projection = XposedHelpers.findClass(
                "com.android.server.media.projection.MediaProjectionManagerService$MediaProjection", loader);
            // The ROM reads the resource in this lambda, independently of the CTS provider getter.
            for (java.lang.reflect.Method method : projection.getDeclaredMethods()) {
                if (method.getName().contains("setRecordingOverlay")) HookSupport.deoptimize(method);
            }
        }
        HookSupport.required(XposedHelpers.findAndHookMethod(Resources.class, "getString",
                int.class, new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam param) {
                        if (!param.hasThrowable() && (int) param.args[0] == id) {
                            param.setResult(HookEntry.GOOGLE);
                        }
                    }
                }));
        HookEntry.log("Framework provider resource synchronized with Google");
        if (Build.VERSION.SDK_INT < 36) return;
        HookSupport.required(XposedHelpers.findAndHookMethod(
                "com.android.server.contextualsearch.ContextualSearchManagerService", loader,
                "getMediaProjection", int.class, String.class, new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam param) {
                        if (param.hasThrowable() || param.getResult() == null
                                || !HookEntry.GOOGLE.equals(param.args[1])) return;
                        long identity = Binder.clearCallingIdentity();
                        try {
                            HookEntry.log("Native CTS projection recordingOverlay="
                                    + XposedHelpers.callMethod(param.getResult(), "isRecordingOverlay"));
                        } catch (Throwable failure) {
                            HookEntry.log("Projection mode diagnostic unavailable: " + failure);
                        } finally {
                            Binder.restoreCallingIdentity(identity);
                        }
                    }
                }));
    }
}
