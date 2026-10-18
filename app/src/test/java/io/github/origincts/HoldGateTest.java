package io.github.origincts;

import org.junit.Test;
import static org.junit.Assert.*;

public class HoldGateTest {
    @Test public void heldGestureNeverReachesHomeHandler() {
        HoldGate gate = new HoldGate();
        assertEquals(HoldGate.BUFFER, gate.down(true));
        assertEquals(HoldGate.BUFFER, gate.move(false));
        assertTrue(gate.claim());
        assertEquals(HoldGate.DROP, gate.move(true));
        assertEquals(HoldGate.DROP, gate.up());
        assertFalse(gate.claim());
    }
    @Test public void ordinarySwipeReplaysDownOnlyOnceAndNeverClaims() {
        HoldGate gate = new HoldGate();
        gate.down(true);
        assertEquals(HoldGate.REPLAY, gate.move(true));
        assertEquals(HoldGate.PASS, gate.move(true));
        assertFalse(gate.claim());
        assertEquals(HoldGate.PASS, gate.up());
    }
    @Test public void shortTapReplaysItsOriginalStream() {
        HoldGate gate = new HoldGate();
        gate.down(true);
        assertEquals(HoldGate.REPLAY, gate.up());
        assertFalse(gate.claim());
    }
    @Test public void cancelNeverStartsAnOriginalGesture() {
        HoldGate gate = new HoldGate();
        gate.down(true);
        assertEquals(HoldGate.DROP, gate.cancel());
        assertFalse(gate.claim());
    }
    @Test public void failedLaunchRestoresNormalRelease() {
        HoldGate gate = new HoldGate();
        gate.down(true);
        assertTrue(gate.claim());
        gate.release();
        assertEquals(HoldGate.PASS, gate.up());
    }
    @Test public void outsideHoldAreaPassesThrough() {
        HoldGate gate = new HoldGate();
        assertEquals(HoldGate.PASS, gate.down(false));
        assertEquals(HoldGate.PASS, gate.move(false));
        assertEquals(HoldGate.PASS, gate.up());
        assertFalse(gate.claim());
    }
}
