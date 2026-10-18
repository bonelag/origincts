package io.github.origincts;

import android.os.Build;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import java.lang.reflect.Method;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/** Buffer only a center-bar hold. Replay DOWN for a tap/swipe, never for an accepted CTS hold. */
final class VivoGestureHook {
    private static final String STATE = "origincts.hold";
    static void install(ClassLoader loader) throws Exception {
        Class<?> bar = XposedHelpers.findClass(
                "com.vivo.upslide.navigation.sideslide.view.SideSlideGestureBar", loader);
        Method touch = bar.getDeclaredMethod("onTouchEvent", MotionEvent.class);
        HookSupport.required(XposedBridge.hookMethod(touch, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    View view = (View) param.thisObject;
                    if (!(view.getLayoutParams() instanceof WindowManager.LayoutParams layout)
                            || !"SideSlideGestureBar-Bottom".contentEquals(layout.getTitle())) return;
                    Hold hold = (Hold) XposedHelpers.getAdditionalInstanceField(view, STATE);
                    if (hold == null) {
                        hold = new Hold(view, loader, touch);
                        XposedHelpers.setAdditionalInstanceField(view, STATE, hold);
                    }
                    if (hold.onTouch((MotionEvent) param.args[0])) param.setResult(true);
                } catch (Throwable failure) {
                    HookEntry.log("Vivo gesture routing failed: " + failure);
                    // Do not deliver a terminal event without its buffered DOWN.
                    param.setResult(true);
                }
            }
            @Override protected void afterHookedMethod(MethodHookParam param) {
                MotionEvent event = (MotionEvent) param.args[0];
                if (event.getActionMasked() == MotionEvent.ACTION_UP
                        || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                    Hold hold = (Hold) XposedHelpers.getAdditionalInstanceField(param.thisObject, STATE);
                    if (hold != null) hold.clear();
                }
            }
        }));
        HookEntry.log("Vivo buffered bottom-bar hold installed");
    }

    private static final class Hold implements Runnable, View.OnAttachStateChangeListener {
        final View view;
        final ClassLoader loader;
        final Method touch;
        final int slop;
        final HoldGate gate = new HoldGate();
        MotionEvent down;
        float x, y;
        Hold(View view, ClassLoader loader, Method touch) {
            this.view = view;
            this.loader = loader;
            this.touch = touch;
            slop = ViewConfiguration.get(view.getContext()).getScaledTouchSlop();
            view.addOnAttachStateChangeListener(this);
        }
        boolean onTouch(MotionEvent event) throws Throwable {
            int route;
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    clear();
                    x = event.getX();
                    y = event.getY();
                    float halfWidth = Math.min(view.getWidth() / 2f,
                            80 * view.getResources().getDisplayMetrics().density);
                    route = gate.down(HookSettings.enabled(FeatureState.CTS) && event.getPointerCount() == 1
                            && event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER
                            && Math.abs(x - view.getWidth() / 2f) <= halfWidth);
                    if (route == HoldGate.BUFFER) {
                        down = MotionEvent.obtain(event);
                        view.postDelayed(this, ViewConfiguration.getLongPressTimeout());
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    route = gate.move(event.getPointerCount() != 1
                            || Math.abs(event.getX() - x) > slop || Math.abs(event.getY() - y) > slop);
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    route = gate.move(true);
                    break;
                case MotionEvent.ACTION_UP:
                    route = gate.up();
                    break;
                case MotionEvent.ACTION_CANCEL:
                    route = gate.cancel();
                    break;
                default:
                    route = gate.move(false);
                    break;
            }
            if (route == HoldGate.REPLAY) {
                view.removeCallbacks(this);
                replayDown();
                return false;
            }
            return route == HoldGate.BUFFER || route == HoldGate.DROP;
        }
        private void replayDown() throws Throwable {
            if (down != null) XposedBridge.invokeOriginalMethod(touch, view, new Object[]{down});
        }
        @Override public void run() {
            if (!gate.claim()) return;
            if (!view.isAttachedToWindow() || !view.isShown()) { clear(); return; }
            if (NavigationLaunch.start(view.getContext(), loader, 1, () -> {
                try {
                    boolean android15 = Build.VERSION.SDK_INT == 35;
                    Object launcher = XposedHelpers.callStaticMethod(XposedHelpers.findClass(
                            "com.vivo.upslide.dependency.VivoLauncherHelper", loader),
                            android15 ? "getInstance" : "k", view.getContext());
                    XposedHelpers.callMethod(launcher, android15 ? "cancelRecentAnimation" : "j");
                    XposedHelpers.callMethod(launcher, android15 ? "onSideSlipStatusChanged" : "u", false);
                } catch (Throwable failure) {
                    HookEntry.log("Vivo launcher gesture cancellation failed: " + failure);
                }
            }, null)) {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            } else {
                gate.release();
                try { replayDown(); }
                catch (Throwable failure) { HookEntry.log("Vivo gesture replay failed: " + failure); }
            }
        }
        void clear() {
            view.removeCallbacks(this);
            gate.release();
            if (down != null) { down.recycle(); down = null; }
        }
        @Override public void onViewDetachedFromWindow(View ignored) { clear(); }
        @Override public void onViewAttachedToWindow(View ignored) { }
    }
}
