import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
class VerifyApk {
    public static void main(String[] args) throws Exception {
        try (ZipFile apk = new ZipFile(args[0])) {
            ZipEntry dexkit = apk.getEntry("lib/arm64-v8a/libdexkit.so");
            if (dexkit == null || dexkit.getMethod() != ZipEntry.STORED) {
                throw new AssertionError("DexKit must be stored uncompressed for LSPosed APK-path loading");
            }
            if (apk.getEntry("assets/xposed_init") == null) throw new AssertionError("Missing Xposed entry");
            System.out.println("PASS: DexKit uncompressed; Xposed entry present");
        }
    }
}
