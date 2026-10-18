package io.github.origincts;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import static org.junit.Assert.*;

public class RootlessLauncherTest {
    private static String read(Path file) throws Exception { return new String(Files.readAllBytes(file), java.nio.charset.StandardCharsets.UTF_8); }
    @Test public void launcherNeverSpawnsAShell() throws Exception {
        for (String name : new String[]{"LaunchActivity", "NativeLauncher"}) {
            String code = read(Path.of("src/main/java/io/github/origincts/" + name + ".java"));
            assertFalse(name, code.contains("ProcessBuilder"));
            assertFalse(name, code.contains("Runtime.getRuntime"));
            assertFalse(name, code.contains("app_process"));
        }
    }
    @Test public void bridgeRequiresSignatureProtectedPermission() throws Exception {
        String manifest = read(Path.of("src/main/AndroidManifest.xml"));
        assertTrue(manifest.contains("android:protectionLevel=\"signature\""));
        String bridge = read(Path.of("src/main/java/io/github/origincts/SystemUiBridge.java"));
        assertTrue(bridge.contains("BridgeContract.PERMISSION"));
        assertTrue(bridge.contains("pm.checkPermission"));
        assertTrue(bridge.contains("isKeyguardLocked"));
    }
}
