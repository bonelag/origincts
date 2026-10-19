package io.github.origincts;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.SystemClock;
import java.lang.reflect.Method;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/** Intercepts Vivo's native voice wakeup (Hi Jovi / Xiao V) and redirects to Google Assistant / Gemini. */
final class VoiceWakeupHook {
    static final String ACTION_WAKEUP_BY_VOICE = "com.vivo.agent.action.wakeup_by_voice";

    static void install(ClassLoader loader) {
        try {
            Class<?> ams = XposedHelpers.findClass("com.android.server.am.ActivityManagerService", loader);
            XC_MethodHook hook = new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        if (param.args == null || param.args.length < 2) return;
                        Object arg = param.args[1];
                        if (!(arg instanceof Intent)) return;
                        Intent intent = (Intent) arg;
                        if (!ACTION_WAKEUP_BY_VOICE.equals(intent.getAction())) return;

                        if (!HookSettings.enabled(FeatureState.VOICE)) {
                            HookEntry.log("Voice wakeup: disabled in OriginCTS, allowing native Jovi");
                            return;
                        }

                        // Wake up the screen if it is currently off
                        Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        if (context != null) {
                            try {
                                PowerManager pm = context.getSystemService(PowerManager.class);
                                if (pm != null && !pm.isInteractive()) {
                                    XposedHelpers.callMethod(pm, "wakeUp", SystemClock.uptimeMillis(), 2, "OriginCTS:VoiceWakeup");
                                }
                            } catch (Throwable failure) {
                                HookEntry.log("Voice wakeup screen turn on: " + failure);
                            }
                        }

                        // Trigger Google Assistant / Gemini
                        if (PowerAssistantHook.showGoogleAssistant(loader)) {
                            // Cancel starting Jovi AgentService by returning a ComponentName directly
                            ComponentName target = intent.getComponent();
                            param.setResult(target != null ? target
                                    : new ComponentName("com.vivo.agent", "com.vivo.agent.AgentService"));
                            HookEntry.log("Voice wakeup: successfully redirected to Google Assistant/Gemini");
                        } else {
                            HookEntry.log("Voice wakeup: Google Assistant not accepted, falling back to native Jovi");
                        }
                    } catch (Throwable failure) {
                        HookEntry.log("Voice wakeup hook error: " + failure);
                    }
                }
            };

            boolean hooked = false;
            for (Method method : ams.getDeclaredMethods()) {
                if ("startService".equals(method.getName())) {
                    Class<?>[] types = method.getParameterTypes();
                    if (types.length >= 2 && Intent.class.isAssignableFrom(types[1])) {
                        HookSupport.deoptimize(method);
                        HookSupport.required(XposedBridge.hookMethod(method, hook));
                        hooked = true;
                    }
                }
            }
            if (hooked) {
                HookEntry.log("Voice wakeup hook installed on ActivityManagerService");
            } else {
                HookEntry.log("Voice wakeup hook: no matching startService method on ActivityManagerService");
            }
        } catch (Throwable failure) {
            HookEntry.log("Voice wakeup hook unavailable: " + failure);
        }
    }
}
