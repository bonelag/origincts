package io.github.origincts;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/** Exception applies only to the CTS start operation and the platform-signed SystemUI UID. */
final class SystemUiPermission {
    static void install(String service, ClassLoader loader) throws Exception {
        Class<?> stub = XposedHelpers.findClass(service + "$ContextualSearchManagerStub", loader);
        for (java.lang.reflect.Method method : stub.getDeclaredMethods()) {
            if (method.getName().equals("startContextualSearch")
                    || method.getName().equals("startContextualSearchAsUser")) {
                HookSupport.deoptimize(method);
            }
        }
        HookSupport.required(XposedHelpers.findAndHookMethod(service, loader,
                "enforcePermission", String.class, new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam param) {
                        if (!(param.getThrowable() instanceof SecurityException)
                                || !"startContextualSearch".equals(param.args[0])) return;
                        try {
                            Context context = (Context) XposedHelpers.callMethod(param.thisObject, "getContext");
                            PackageManager pm = context.getPackageManager();
                            String[] packages = pm.getPackagesForUid(Binder.getCallingUid());
                            // Reject unrelated UIDs before checking SystemUI's certificate.
                            if (!CallerPolicy.allows((String) param.args[0], packages, true, true)) return;
                            boolean signed = pm.checkSignatures("android", BridgeContract.SYSTEM_UI)
                                    == PackageManager.SIGNATURE_MATCH;
                            boolean system = (pm.getApplicationInfo(BridgeContract.SYSTEM_UI, 0).flags
                                    & ApplicationInfo.FLAG_SYSTEM) != 0;
                            if (CallerPolicy.allows((String) param.args[0], packages, signed, system)) {
                                param.setResult(null);
                            }
                        } catch (Throwable failure) {
                            HookEntry.log("CTS permission unchanged: " + failure);
                        }
                    }
                }));
    }
}
