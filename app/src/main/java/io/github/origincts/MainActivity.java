package io.github.origincts;

import android.app.Activity;
import android.content.Intent;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MainActivity extends Activity {
    private final Map<Integer, Switch> switches = new LinkedHashMap<>();
    private int state;
    private boolean rendering;
    private Switch hideIcon;
    private Button ctsButton, geminiButton;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        state = SettingsStore.read(this);
        SettingsUi ui = new SettingsUi(this, "OriginCTS", "");
        LinearLayout main = ui.card();
        addSwitch(ui, main, FeatureState.MASTER, "Use OriginCTS", "Shortcuts and translation");
        ui.section("Shortcuts");
        LinearLayout shortcuts = ui.card();
        addSwitch(ui, shortcuts, FeatureState.CTS, "Circle to Search", "Hold Home or the centre of the navigation bar");
        ui.divider(shortcuts);
        addSwitch(ui, shortcuts, FeatureState.GEMINI, "Gemini", "Hold the power button for 0.5 seconds");
        ui.section("Translation");
        LinearLayout translation = ui.card();
        addSwitch(ui, translation, FeatureState.TRANSLATE, "Scroll and translate", "Translate while scrolling in Circle to Search");

        ui.section("Open");
        LinearLayout actions = ui.card();
        ctsButton = ui.button(actions, "Circle to Search", false, view -> launch(false));
        geminiButton = ui.button(actions, "Gemini", false, view -> launch(true));
        ui.note("These buttons work even when the shortcuts are off.");

        ui.section("App");
        LinearLayout app = ui.card();
        String iconHelp = "Open hidden settings from LSPosed → OriginCTS → Module settings.";
        LinearLayout iconRow = ui.row(app, "Hide app icon", iconHelp);
        hideIcon = new Switch(this);
        hideIcon.setShowText(false);
        hideIcon.setContentDescription("Hide app icon. " + iconHelp);
        hideIcon.setMinHeight(ui.dp(48));
        hideIcon.setMinWidth(ui.dp(52));
        iconRow.addView(hideIcon);
        iconRow.setOnClickListener(view -> hideIcon.toggle());
        hideIcon.setOnCheckedChangeListener((button, hidden) -> {
            if (rendering) return;
            try {
                int requested = hidden ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                        : PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
                getPackageManager().setComponentEnabledSetting(
                        launcher(), requested, PackageManager.DONT_KILL_APP);
                if (getPackageManager().getComponentEnabledSetting(launcher()) != requested) {
                    throw new IllegalStateException("Launcher component state did not change.");
                }
                Toast.makeText(this, hidden
                        ? "Icon hidden. Refresh the launcher if it stays visible."
                        : "App icon shown.", Toast.LENGTH_LONG).show();
            } catch (RuntimeException failure) {
                Toast.makeText(this, "Could not change app icon visibility.", Toast.LENGTH_LONG).show();
            }
            render();
        });
        LinearLayout about = ui.card();
        ui.button(about, "About OriginCTS", false, view -> startActivity(new Intent(this, AboutActivity.class)));

        render();
    }
    private void addSwitch(SettingsUi ui, LinearLayout parent, int feature, String title, String detail) {
        LinearLayout row = ui.row(parent, title, detail);
        Switch control = new Switch(this);
        control.setShowText(false);
        control.setContentDescription(title + ". " + detail);
        control.setMinHeight(ui.dp(48)); control.setMinWidth(ui.dp(52));
        row.addView(control);
        switches.put(feature, control);
        row.setOnClickListener(view -> { if (control.isEnabled()) control.toggle(); });
        control.setOnCheckedChangeListener((button, checked) -> {
            if (rendering) return;
            int next = FeatureState.set(state, feature, checked);
            if (SettingsStore.write(this, next)) state = next;
            else Toast.makeText(this, "Could not save settings. Try again.", Toast.LENGTH_LONG).show();
            render();
        });
    }
    private ComponentName launcher() {
        return new ComponentName(this, getPackageName() + ".Launcher");
    }
    private void render() {
        rendering = true;
        if (hideIcon != null) hideIcon.setChecked(getPackageManager().getComponentEnabledSetting(launcher())
                == PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
        boolean enabled = FeatureState.enabled(state, FeatureState.MASTER);
        for (Map.Entry<Integer, Switch> entry : switches.entrySet()) {
            entry.getValue().setChecked((state & entry.getKey()) != 0);
            entry.getValue().setEnabled(entry.getKey() == FeatureState.MASTER || enabled);
        }
        if (ctsButton != null) {
            ctsButton.setEnabled(enabled); ctsButton.setAlpha(enabled ? 1 : .4f);
            geminiButton.setEnabled(enabled); geminiButton.setAlpha(enabled ? 1 : .4f);
        }
        rendering = false;
    }
    private void launch(boolean gemini) {
        startActivity(new Intent(this, LaunchActivity.class).putExtra("gemini", gemini)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK));
        moveTaskToBack(true);
    }
    @Override protected void onResume() {
        super.onResume(); state = SettingsStore.read(this); render();
    }
}
