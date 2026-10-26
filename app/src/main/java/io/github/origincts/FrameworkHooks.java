package io.github.origincts;

import android.content.Context;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;

final class FrameworkHooks {
    static void install(ClassLoader loader) throws Exception {
        ProviderConfig.install(loader);
        CaptureExclusion.install(loader);
        NativeVoiceHook.install(loader);
        Class<?> server = XposedHelpers.findClass("com.android.server.SystemServer", loader);
        boolean foundBootCaller = false;
        for (java.lang.reflect.Method method : server.getDeclaredMethods()) {
            if (method.getName().equals("startOtherServices")) {
                HookSupport.deoptimize(method);
                foundBootCaller = true;
            }
        }
        if (!foundBootCaller) throw new NoSuchMethodException("SystemServer.startOtherServices");
        String service = "com.android.server.contextualsearch.ContextualSearchManagerService";
        int vendorGate = android.os.Build.VERSION.SDK_INT == 35
                ? android.content.res.Resources.getSystem().getIdentifier(
                        "config_ethernet_tcp_buffers", "string", "android") : 0;
        // Provider MUST be installed first; a failed lookup must never force service startup.
        XC_MethodHook.Unhook provider = HookSupport.required(XposedHelpers.findAndHookMethod(service, loader,
                "getContextualSearchPackageName", XC_MethodReplacement.returnConstant(HookEntry.GOOGLE)));
        try {
            HookSupport.required(XposedHelpers.findAndHookMethod("com.android.server.SystemServer", loader,
                    "deviceHasConfigString", Context.class, int.class, new XC_MethodHook() {
                        @Override protected void afterHookedMethod(MethodHookParam param) {
                            if (param.hasThrowable()) return;
                            try {
                                Context context = (Context) param.args[0];
                                int resource = context.getResources().getIdentifier(
                                        "config_defaultContextualSearchPackageName", "string", "android");
                                int requested = (int) param.args[1];
                                if ((resource != 0 && requested == resource)
                                        || (vendorGate != 0 && requested == vendorGate)) {
                                    param.setResult(true);
                                    HookEntry.log(vendorGate != 0 && requested == vendorGate
                                            && requested != resource
                                            ? "CTS boot condition enabled via Android 15 vendor resource"
                                            : "CTS boot condition enabled");
                                }
                            } catch (Throwable failure) {
                                HookEntry.log("Boot condition left unchanged: " + failure);
                            }
                        }
                    }));
        } catch (Throwable failure) {
            provider.unhook();
            throw failure;
        }
        SystemUiPermission.install(service, loader);
        HookEntry.log("Framework hooks installed; awaiting normal SystemServer boot path");
    }
}
