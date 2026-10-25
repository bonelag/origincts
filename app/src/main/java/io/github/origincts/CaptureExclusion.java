package io.github.origincts;

import android.view.WindowManager;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * Google draws Live Translate into a full-screen TYPE_APPLICATION_OVERLAY window. When the user
 * shares the whole display, MediaProjection mirrors that window back into Google's own ImageReader,
 * so every frame it scans already contains the previous translation and it re-scans endlessly: the
 * overlay is replaced, briefly missing, replaced again, which reads as flicker over the original
 * text. Task-scoped shares mirror only the app surface, which is why they look perfect.
 *
 * SurfaceFlinger drops layers flagged eLayerSkipScreenshot from display mirroring as well as from
 * screenshots (Layer::isInternalDisplayOverlay skips them in traverseLayersInLayerStack and
 * commitMirrorDisplays). WindowState.setSkipScreenshot already exists on this ROM but only fires for
 * Vivo's own VIVO_FLAG_SKIP_SCREEN_SHOT, so apply it ourselves when Google's overlay surface is
 * created.
 */
final class CaptureExclusion {
    private static final int OVERLAY_TYPE = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
    private static boolean reported;

    static void install(ClassLoader loader) throws Exception {
        HookSupport.required(XposedHelpers.findAndHookMethod(
                "com.android.server.wm.WindowStateAnimator", loader, "createSurfaceLocked",
                new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam param) {
                        if (param.hasThrowable() || param.getResult() == null) return;
                        try {
                            Object window = XposedHelpers.getObjectField(param.thisObject, "mWin");
                            Object attrs = XposedHelpers.getObjectField(window, "mAttrs");
                            if (XposedHelpers.getIntField(attrs, "type") != OVERLAY_TYPE) return;
                            if (!HookEntry.GOOGLE.equals(XposedHelpers.getObjectField(attrs, "packageName")))
                                return;
                            if (!HookSettings.enabled(FeatureState.TRANSLATE)) return;
                            java.lang.reflect.Method skip = window.getClass()
                                    .getDeclaredMethod("setSkipScreenshot", boolean.class);
                            skip.setAccessible(true);
                            skip.invoke(window, true);
                            if (!reported) {
                                reported = true;
                                HookEntry.log("Live Translate overlay excluded from display capture");
                            }
                        } catch (Throwable failure) {
                            HookEntry.log("Overlay capture exclusion failed: " + failure);
                        }
                    }
                }));
    }
}
