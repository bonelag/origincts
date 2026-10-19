package io.github.origincts;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class SettingsAndLaunchTest {
    @Test public void masterDisableKeepsIndividualChoicesForReenable() {
        int state = FeatureState.set(FeatureState.ALL, FeatureState.GEMINI, false);
        state = FeatureState.set(state, FeatureState.MASTER, false);
        assertFalse(FeatureState.enabled(state, FeatureState.CTS));
        assertFalse(FeatureState.enabled(state, FeatureState.TRANSLATE));
        assertFalse(FeatureState.enabled(state, FeatureState.VOICE));
        state = FeatureState.set(state, FeatureState.MASTER, true);
        assertTrue(FeatureState.enabled(state, FeatureState.CTS));
        assertTrue(FeatureState.enabled(state, FeatureState.TRANSLATE));
        assertTrue(FeatureState.enabled(state, FeatureState.VOICE));
        assertFalse(FeatureState.enabled(state, FeatureState.GEMINI));
        assertEquals(FeatureState.ALL, FeatureState.set(FeatureState.ALL, 64, true));
        assertFalse(FeatureState.enabled(FeatureState.ALL, 64));
    }
    @Test public void failedRequestDoesNotConsumeHome() {
        List<String> events = new ArrayList<>();
        try {
            LaunchSequence.run(null, () -> { throw new IllegalStateException("service unavailable"); },
                    () -> events.add("consume"), failure -> events.add("cleanup error"));
            fail("Request failure must propagate to native fallback");
        } catch (IllegalStateException expected) { assertTrue(events.isEmpty()); }
    }
    @Test public void launcherCancellationPrecedesRequestAndHomeConsumptionFollowsIt() {
        List<String> events = new ArrayList<>();
        LaunchSequence.run(() -> events.add("cancel launcher"), () -> events.add("CTS"),
                () -> events.add("consume"), failure -> fail("Unexpected failure"));
        assertEquals(List.of("cancel launcher", "CTS", "consume"), events);
    }
    @Test public void cleanupFailureDoesNotReplayNativeGestureAfterSuccessfulLaunch() {
        List<String> events = new ArrayList<>();
        LaunchSequence.run(null, () -> events.add("CTS"), () -> { throw new IllegalStateException(); },
                failure -> events.add("reported"));
        assertEquals(List.of("CTS", "reported"), events);
    }
    @Test public void preparationFailureNeverStartsCtsOrConsumesHome() {
        List<String> events = new ArrayList<>();
        try {
            LaunchSequence.run(() -> { throw new IllegalStateException(); }, () -> events.add("CTS"),
                    () -> events.add("consume"), failure -> fail("Unexpected failure"));
            fail("Preparation failure must propagate");
        } catch (IllegalStateException expected) { assertTrue(events.isEmpty()); }
    }
}
