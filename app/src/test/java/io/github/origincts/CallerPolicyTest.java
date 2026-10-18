package io.github.origincts;

import org.junit.Test;
import static org.junit.Assert.*;

public class CallerPolicyTest {
    @Test public void onlyPlatformSystemUiCanStart() {
        String[] ui = {"com.android.systemui"};
        assertTrue(CallerPolicy.allows("startContextualSearch", ui, true, true));
        assertFalse(CallerPolicy.allows("getContextualSearchState", ui, true, true));
        assertFalse(CallerPolicy.allows("startContextualSearch", null, true, true));
        assertFalse(CallerPolicy.allows("startContextualSearch", new String[]{"io.github.origincts"}, true, true));
        assertFalse(CallerPolicy.allows("startContextualSearch", ui, false, true));
        assertFalse(CallerPolicy.allows("startContextualSearch", ui, true, false));
    }
    @Test public void permissionOwnerAndProtectionMustMatch() {
        assertTrue(BridgeContract.trustedPermission("io.github.origincts", 2, 0));
        assertFalse(BridgeContract.trustedPermission("other.app", 2, 0));
        assertFalse(BridgeContract.trustedPermission("io.github.origincts", 0, 0));
        assertFalse(BridgeContract.trustedPermission("io.github.origincts", 18, 0));
        assertFalse(BridgeContract.trustedPermission("io.github.origincts", 2, -1));
    }
}
