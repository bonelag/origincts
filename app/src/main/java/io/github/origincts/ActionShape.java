package io.github.origincts;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Structural validation adapted from MiuiBackGestureHook (Apache-2.0). */
final class ActionShape {
    static final int ACTION_ID = 271520;
    static boolean isLiveTranslateFeature(Object feature) {
        return "com.google.android.feature.CONTEXTUAL_SEARCH_LIVE_TRANSLATE".equals(feature);
    }

    static Method capability(Class<?> action) {
        if (booleanMethod(action, "i") == null) return null;
        Constructor<?> found = null;
        for (Constructor<?> constructor : action.getDeclaredConstructors()) {
            if (constructor.getParameterCount() != 3) continue;
            if (found != null) return null;
            found = constructor;
        }
        return found == null ? null : booleanMethod(found.getParameterTypes()[1], "a");
    }

    private static Method booleanMethod(Class<?> owner, String name) {
        try {
            Method method = owner.getDeclaredMethod(name);
            return method.getReturnType() == boolean.class && !Modifier.isStatic(method.getModifiers())
                    && !Modifier.isAbstract(method.getModifiers()) ? method : null;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
