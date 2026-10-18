package io.github.origincts;

final class BridgeContract {
    static final String MODULE = "io.github.origincts";
    static final String SYSTEM_UI = "com.android.systemui";
    static final String ACTION = MODULE + ".START_CTS";
    static final String PERMISSION = MODULE + ".permission.START_CTS";

    static boolean trustedPermission(String owner, int protection, int grant) {
        return MODULE.equals(owner) && protection == 2 && grant == 0;
    }
}
