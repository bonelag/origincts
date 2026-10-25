package io.github.origincts;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/** Refresh on a worker, never start another process or perform disk IO on an input callback. */
final class HookSettings {
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "OriginCTS-settings"); thread.setDaemon(true); return thread;
    });
    private static final AtomicBoolean loading = new AtomicBoolean();
    private static final AtomicBoolean dirty = new AtomicBoolean();
    private static volatile long retryAt;
    private static volatile Context context;
    // Vivo's cached-app freezer can make the provider unreachable seconds after start, so an
    // unreadable provider must not disable features: keep the last known state and start permissive.
    private static volatile int state = FeatureState.ALL;
    private static volatile boolean ready;
    private static volatile boolean reported;

    static boolean enabled(int feature) {
        if (!ready) {
            long now = android.os.SystemClock.uptimeMillis();
            if (now >= retryAt) {
                retryAt = now + 1000; // One attempt per second at most, even on an input callback.
                Integer global = SettingsStore.readGlobal();
                if (global != null) {
                    state = global;
                    ready = true;
                } else if (context != null) {
                    refresh();
                }
            }
            if (!ready) return FeatureState.enabled(state, feature); // Permissive until proven otherwise.
        }
        return FeatureState.enabled(state, feature);
    }
    static void install(ClassLoader loader, boolean system) {
        if (system) {
            HookSupport.required(XposedHelpers.findAndHookMethod(
                    "com.android.server.policy.PhoneWindowManager", loader, "systemReady", new XC_MethodHook() {
                        @Override protected void afterHookedMethod(MethodHookParam param) {
                            attach((Context) XposedHelpers.getObjectField(param.thisObject, "mContext"));
                        }
                    }));
        } else {
            HookSupport.required(XposedHelpers.findAndHookMethod(Instrumentation.class,
                    "callApplicationOnCreate", Application.class, new XC_MethodHook() {
                        @Override protected void beforeHookedMethod(MethodHookParam param) {
                            attach((Application) param.args[0]);
                        }
                    }));
            Object current = XposedHelpers.callStaticMethod(
                    XposedHelpers.findClass("android.app.ActivityThread", loader), "currentApplication");
            if (current instanceof Context app) attach(app);
        }
    }
    static synchronized void attach(Context app) {
        if (context != null) return;
        context = app.getApplicationContext() == null ? app : app.getApplicationContext();
        try {
            // Settings.Global is served by system_server, so this observer survives the app being frozen.
            context.getContentResolver().registerContentObserver(
                    android.provider.Settings.Global.getUriFor(SettingsStore.GLOBAL_KEY), false,
                    new ContentObserver(new Handler(Looper.getMainLooper())) {
                        @Override public void onChange(boolean selfChange) { refresh(); }
                    });
        } catch (Throwable failure) {
            HookEntry.log("Settings observer unavailable: " + failure);
        }
        refresh();
    }
    private static void refresh() {
        if (context == null) return;
        dirty.set(true);
        if (!loading.compareAndSet(false, true)) return;
        WORKER.execute(() -> {
            dirty.set(false);
            Integer global = SettingsStore.readGlobal();
            if (global != null) {
                state = global;
                ready = true;
            }
            try (Cursor cursor = context.getContentResolver().query(SettingsStore.URI, null, null, null, null)) {
                if (cursor == null || !cursor.moveToFirst()) throw new IllegalStateException("Empty settings response");
                state = cursor.getInt(0) & FeatureState.ALL;
                ready = true;
                reported = false;
            } catch (Throwable failure) {
                // The provider is unreachable while Vivo freezes the app; the Global copy is authoritative then.
                retryAt = android.os.SystemClock.uptimeMillis() + 1000;
                if (global == null) {
                    ready = false;
                    if (!reported) {
                        reported = true;
                        HookEntry.log("Settings unavailable; keeping previous state: " + failure);
                    }
                }
            } finally {
                loading.set(false);
                if (dirty.get()) refresh();
            }
        });
    }
}
