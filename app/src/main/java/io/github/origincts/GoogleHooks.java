package io.github.origincts;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

/** Adapted from MiuiBackGestureHook's Live Translate resolver; see THIRD_PARTY_NOTICES. */
final class GoogleHooks {
    static void install(XC_LoadPackage.LoadPackageParam app) throws Exception {
        System.loadLibrary("dexkit");
        List<String> paths = new ArrayList<>();
        paths.add(app.appInfo.sourceDir);
        if (app.appInfo.splitSourceDirs != null) {
            java.util.Collections.addAll(paths, app.appInfo.splitSourceDirs);
        }
        Set<Class<?>> candidates = new LinkedHashSet<>();
        Set<java.lang.reflect.Member> callers = new LinkedHashSet<>();
        // ponytail: rescan on each process start; add a version-keyed cache only if measured slow.
        // Scanning every code split also prevents selecting an ambiguous base-only match.
        for (String path : paths) {
            try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(path)) {
                if (zip.getEntry("classes.dex") == null) continue;
            }
            try (DexKitBridge bridge = DexKitBridge.create(path)) {
                for (MethodData match : bridge.findMethod(FindMethod.create().matcher(
                        MethodMatcher.create().paramCount(0).returnType("int")
                                .usingNumbers(Integer.valueOf(ActionShape.ACTION_ID))))) {
                    Class<?> owner = match.getClassInstance(app.classLoader);
                    Method gate = ActionShape.capability(owner);
                    if (gate == null) continue;
                    candidates.add(owner);
                    for (MethodData target : bridge.findMethod(FindMethod.create().matcher(
                            MethodMatcher.create().declaredClass(gate.getDeclaringClass().getName())
                                    .name(gate.getName()).paramCount(0).returnType("boolean")))) {
                        callers.add(gate);
                        for (MethodData caller : target.getCallers()) {
                            if (caller.isConstructor()) callers.add(caller.getConstructorInstance(app.classLoader));
                            else if (caller.isMethod()) callers.add(caller.getMethodInstance(app.classLoader));
                        }
                    }
                }
            }
        }
        if (candidates.size() != 1) {
            HookEntry.log("Live Translate unchanged: valid action candidates=" + candidates.size());
            return;
        }
        Class<?> action = candidates.iterator().next();
        Method capability = ActionShape.capability(action);
        // Required on optimized Google builds: otherwise View/Compose may use an inlined gate.
        if (callers.isEmpty()) throw new IllegalStateException("Capability caller resolution failed");
        for (java.lang.reflect.Member caller : callers) HookSupport.deoptimize(caller);
        List<XC_MethodHook.Unhook> handles = new ArrayList<>();
        try {
            handles.add(HookSupport.required(XposedBridge.hookMethod(capability, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    // Preserve original failures; only change a successfully computed boolean.
                    if (!param.hasThrowable() && HookSettings.enabled(FeatureState.TRANSLATE)) param.setResult(true);
                }
            })));
            Class<?> pm = XposedHelpers.findClass("android.app.ApplicationPackageManager", app.classLoader);
            XC_MethodHook featureHook = new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    if (!param.hasThrowable() && HookSettings.enabled(FeatureState.TRANSLATE) && ActionShape.isLiveTranslateFeature(param.args[0])) {
                        param.setResult(true);
                    }
                }
            };
            handles.add(HookSupport.required(XposedHelpers.findAndHookMethod(pm, "hasSystemFeature", String.class, featureHook)));
            handles.add(HookSupport.required(XposedHelpers.findAndHookMethod(pm, "hasSystemFeature", String.class, int.class, featureHook)));
        } catch (Throwable failure) {
            for (XC_MethodHook.Unhook handle : handles) handle.unhook();
            throw failure;
        }
        HookEntry.log("Live Translate ready: action=" + action.getName() + ", capability=" + capability
                + ", process=" + app.processName + "; Google visibility/capture flow preserved");
    }
}
