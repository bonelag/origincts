package io.github.origincts;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.widget.LinearLayout;
import java.nio.charset.StandardCharsets;

public final class AboutActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        String version;
        try { version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName; }
        catch (Exception ignored) { version = "Unknown"; }
        SettingsUi ui = new SettingsUi(this, "About OriginCTS", "");
        LinearLayout info = ui.card();
        ui.row(info, "OriginCTS", "Version " + version);
        ui.divider(info);
        ui.row(info, "Built for OriginOS 5 and 6", "Android 15 / 16 · Current device: Android " + Build.VERSION.RELEASE);
        ui.divider(info);
        ui.row(info, "Features", "Circle to Search, Scroll and translate, and Gemini (Power & Voice)");
        ui.section("Setup");
        LinearLayout setup = ui.card();
        ui.row(setup, "LSPosed", "Select System Framework, System UI, UpSlide and Google App.");
        ui.divider(setup);
        ui.row(setup, "Gemini", "Choose Google as your default digital assistant, then select Gemini in Google App. Keep holding Power to open the power menu.");
        ui.divider(setup);
        ui.row(setup, "Changing settings", "Shortcuts use the new setting on the next gesture. Reopen Circle to Search after changing Scroll and translate.");
        ui.divider(setup);
        ui.row(setup, "Disabling OriginCTS", "The main switch turns off shortcuts, manual buttons and translation overrides. To remove framework hooks too, disable the module in LSPosed and restart using your usual procedure.");
        ui.section("License");
        LinearLayout credits = ui.card();
        ui.row(credits, "Apache License 2.0", "OriginCTS source code");
        ui.button(credits, "Third-party notices", false, view -> {
            try (java.io.InputStream stream = getAssets().open("third-party-notices.txt")) {
                String notices = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                new AlertDialog.Builder(this).setTitle("Third-party notices").setMessage(notices)
                        .setPositiveButton("Close", null).show();
            } catch (Exception failure) {
                new AlertDialog.Builder(this).setMessage("Could not read notices.").setPositiveButton("Close", null).show();
            }
        });
        ui.button(ui.card(), "Back to settings", false, view -> finish());
    }
}
