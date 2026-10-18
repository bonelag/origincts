package io.github.origincts;

import android.content.Context;
import android.os.Build;
import android.view.KeyEvent;
import android.view.HapticFeedbackConstants;
import android.view.View;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/** Reuses Android's long-press recognition and consumed-key handling on the inspected Vivo ROM. */
final class HomeKeyHook {
    static void install(ClassLoader loader) throws Exception {
        Class<?> handler = XposedHelpers.findClass(
                "com.android.server.policy.PhoneWindowManager$DisplayHomeButtonHandler", loader);
        for (java.lang.reflect.Method method : handler.getDeclaredMethods()) {
            if (method.getName().contains("handleHomeButton")) HookSupport.deoptimize(method);
        }
        HookSupport.required(XposedHelpers.findAndHookMethod(handler, "handleLongPressOnHome",
                KeyEvent.class, new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            KeyEvent event = (KeyEvent) param.args[0];
                            if (event.getKeyCode() != KeyEvent.KEYCODE_HOME || event.isCanceled()
                                    || XposedHelpers.getIntField(param.thisObject, "mDisplayId") != 0
                                    || XposedHelpers.getBooleanField(param.thisObject, "mHomeConsumed")) return;
                            Object policy = XposedHelpers.getObjectField(param.thisObject, "this$0");
                            Context context = (Context) XposedHelpers.getObjectField(policy, "mContext");
                            NavigationLaunch.start(context, loader, 2, () -> {
                                param.setResult(null);
                                XposedHelpers.setBooleanField(param.thisObject, "mHomeConsumed", true);
                            });
                        } catch (Throwable failure) {
                            HookEntry.log("Home hook left original behavior: " + failure);
                        }
                    }
                }));
        HookEntry.log("Home key long-press hook installed");
    }
    static void installSystemUi(ClassLoader loader) throws Exception {
        if (Build.VERSION.SDK_INT == 35) {
            installSystemUi35(loader);
            return;
        }
        // Catch the key's own hold timer before it branches into an assistant or injected key event.
        HookSupport.required(XposedHelpers.findAndHookMethod(
                "com.android.systemui.navigationbar.views.buttons.KeyButtonView$1", loader,
                "run", new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            View view = (View) XposedHelpers.getObjectField(param.thisObject, "this$0");
                            if (XposedHelpers.getIntField(view, "mCode") != KeyEvent.KEYCODE_HOME
                                    || !view.isPressed() || view.getDisplay() == null
                                    || view.getDisplay().getDisplayId() != 0) return;
                            if (NavigationLaunch.start(view.getContext(), loader, 2, () -> {
                                XposedHelpers.setBooleanField(view, "mLongClicked", true);
                                XposedHelpers.callMethod(view, "abortCurrentGesture");
                                param.setResult(null);
                            })) {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                            }
                        } catch (Throwable failure) {
                            HookEntry.log("Home hold timer hook: " + failure);
                        }
                    }
                }));
        Class<?> callback = XposedHelpers.findClass(
                "com.android.systemui.navigationbar.views.NavigationBar$$ExternalSyntheticLambda8", loader);
        HookSupport.deoptimize(callback.getDeclaredMethod("onLongClick", View.class));
        HookSupport.required(XposedHelpers.findAndHookMethod(
                "com.android.systemui.navigationbar.views.NavigationBar", loader,
                "onHomeLongClick", View.class, new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            View view = (View) param.args[0];
                            if (view == null || view.getDisplay() == null || view.getDisplay().getDisplayId() != 0
                                    || XposedHelpers.getBooleanField(param.thisObject, "mScreenPinningActive")
                                    || (XposedHelpers.getIntField(param.thisObject, "mDisabledFlags1") & 33554432) != 0) return;
                            NavigationLaunch.start(view.getContext(), loader, 2, () -> {
                                param.setResult(true);
                                XposedHelpers.callMethod(XposedHelpers.getObjectField(param.thisObject, "mView"),
                                        "abortCurrentGesture");
                            });
                        } catch (Throwable failure) {
                            HookEntry.log("SystemUI Home callback: " + failure);
                        }
                    }
                }));
        HookEntry.log("SystemUI Home long-click hook installed");
    }
    private static void installSystemUi35(ClassLoader loader) throws Exception {
        HookSupport.required(XposedHelpers.findAndHookMethod(
                "com.android.systemui.navigationbar.buttons.KeyButtonView$1", loader,
                "run", new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            View view = (View) XposedHelpers.getObjectField(param.thisObject, "this$0");
                            if (XposedHelpers.getIntField(view, "mCode") != KeyEvent.KEYCODE_HOME
                                    || !view.isPressed() || view.getDisplay() == null
                                    || view.getDisplay().getDisplayId() != 0) return;
                            if (NavigationLaunch.start(view.getContext(), loader, 2, () -> {
                                XposedHelpers.setBooleanField(view, "mLongClicked", true);
                                XposedHelpers.callMethod(view, "abortCurrentGesture");
                                param.setResult(null);
                            })) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        } catch (Throwable failure) {
                            HookEntry.log("Android 15 Home hold timer: " + failure);
                        }
                    }
                }));
        Class<?> callback = XposedHelpers.findClass(
                "com.android.systemui.navigationbar.NavigationBar$$ExternalSyntheticLambda12", loader);
        HookSupport.deoptimize(callback.getDeclaredMethod("onLongClick", View.class));
        HookSupport.required(XposedHelpers.findAndHookMethod(
                "com.android.systemui.navigationbar.NavigationBar", loader,
                "onHomeLongClick", View.class, new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            View view = (View) param.args[0];
                            if (view == null || view.getDisplay() == null
                                    || view.getDisplay().getDisplayId() != 0
                                    || XposedHelpers.getBooleanField(param.thisObject, "mScreenPinningActive")
                                    || (XposedHelpers.getIntField(param.thisObject, "mDisabledFlags1") & 33554432) != 0) return;
                            NavigationLaunch.start(view.getContext(), loader, 2, () -> {
                                param.setResult(true);
                                XposedHelpers.callMethod(XposedHelpers.getObjectField(param.thisObject, "mView"),
                                        "abortCurrentGesture");
                            });
                        } catch (Throwable failure) {
                            HookEntry.log("Android 15 SystemUI Home callback: " + failure);
                        }
                    }
                }));
        HookEntry.log("Android 15 SystemUI Home long-click hook installed");
    }
}
