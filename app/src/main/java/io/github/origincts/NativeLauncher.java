package io.github.origincts;

import android.os.IBinder;
import de.robv.android.xposed.XposedHelpers;

/** Runs only in injected system processes; never starts a shell or requests superuser access. */
final class NativeLauncher {
    static void start(ClassLoader loader) { start(loader, 1); }
    static void start(ClassLoader loader, int entrypoint) {
        prepare(loader, entrypoint).run();
    }
    static Runnable prepare(ClassLoader loader, int entrypoint) {
        Class<?> services = XposedHelpers.findClass("android.os.ServiceManager", loader);
        IBinder binder = (IBinder) XposedHelpers.callStaticMethod(services, "getService", "contextual_search");
        if (binder == null) throw new IllegalStateException("contextual_search is unavailable; check the LSPosed log.");
        Class<?> stub = XposedHelpers.findClass("android.app.contextualsearch.IContextualSearchManager$Stub", loader);
        Object manager = XposedHelpers.callStaticMethod(stub, "asInterface", binder);
        int user = (int) XposedHelpers.callStaticMethod(
                XposedHelpers.findClass("android.app.ActivityManager", loader), "getCurrentUser");
        // Vivo adds an explicit-user call; AOSP/Neo 10 exposes MiCTS's one-argument Binder API.
        try {
            XposedHelpers.findMethodExact(manager.getClass(),
                    "startContextualSearchAsUser", int.class, int.class);
            return () -> XposedHelpers.callMethod(
                    manager, "startContextualSearchAsUser", entrypoint, user);
        } catch (NoSuchMethodError noUserVariant) {
            XposedHelpers.findMethodExact(manager.getClass(), "startContextualSearch", int.class);
            HookEntry.log("Using standard startContextualSearch(int) Binder API");
            return () -> XposedHelpers.callMethod(manager, "startContextualSearch", entrypoint);
        }
    }
}
