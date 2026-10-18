package io.github.origincts;

final class CallerPolicy {
    static boolean allows(String operation, String[] uidPackages, boolean platformSigned, boolean systemApp) {
        if (!"startContextualSearch".equals(operation) || !platformSigned || !systemApp || uidPackages == null) {
            return false;
        }
        for (String name : uidPackages) if (BridgeContract.SYSTEM_UI.equals(name)) return true;
        return false;
    }
}
