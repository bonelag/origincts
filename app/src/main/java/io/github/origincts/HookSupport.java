package io.github.origincts;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

final class HookSupport {
    static XC_MethodHook.Unhook required(XC_MethodHook.Unhook handle) {
        if (handle == null) throw new IllegalStateException("LSPosed returned a null hook handle");
        return handle;
    }
    static void deoptimize(Member member) throws Exception {
        Method api = XposedBridge.class.getMethod("deoptimizeMethod", Member.class);
        api.invoke(null, member);
    }
}
