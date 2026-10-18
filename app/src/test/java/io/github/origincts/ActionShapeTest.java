package io.github.origincts;

import org.junit.Test;
import static org.junit.Assert.*;

public class ActionShapeTest {
    static class Capability { public boolean a() { return false; } }
    static class Action {
        Action(Object first, Capability capability, Object last) {}
        public boolean i() { return false; }
    }
    static class WrongGate { public int a() { return 0; } }
    static class WrongAction {
        WrongAction(Object first, WrongGate capability, Object last) {}
        public boolean i() { return false; }
    }
    static class Ambiguous {
        Ambiguous(Object first, Capability capability, Object last) {}
        Ambiguous(String first, Capability capability, Object last) {}
        public boolean i() { return false; }
    }
    static class StaticGate { public static boolean a() { return false; } }
    static class StaticAction {
        StaticAction(Object first, StaticGate capability, Object last) {}
        public boolean i() { return false; }
    }
    @Test public void rejectsStaticCapability() {
        assertNull(ActionShape.capability(StaticAction.class));
    }
    @Test public void acceptsOnlyTheValidatedShape() {
        assertNotNull(ActionShape.capability(Action.class));
        assertNull(ActionShape.capability(WrongAction.class));
        assertNull(ActionShape.capability(Ambiguous.class));
        assertNull(ActionShape.capability(Object.class));
    }
    @Test public void featureOverrideIsExact() {
        assertTrue(ActionShape.isLiveTranslateFeature("com.google.android.feature.CONTEXTUAL_SEARCH_LIVE_TRANSLATE"));
        assertFalse(ActionShape.isLiveTranslateFeature(null));
        assertFalse(ActionShape.isLiveTranslateFeature("com.google.android.feature.CONTEXTUAL_SEARCH"));
    }
}
