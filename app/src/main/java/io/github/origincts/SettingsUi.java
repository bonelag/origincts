package io.github.origincts;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Small Material-style screen primitives using the platform's accessible controls. */
final class SettingsUi {
    final Activity activity;
    final LinearLayout content;
    SettingsUi(Activity activity, String title, String subtitle) {
        this.activity = activity;
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(color(R.color.page));
        content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(24));
        scroll.addView(content);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        TextView heading = text(title, 24, R.color.ink);
        heading.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        heading.setPadding(dp(4), dp(8), 0, dp(24));
        heading.setAccessibilityHeading(true);
        content.addView(heading);
        if (!subtitle.isEmpty()) {
        TextView description = text(subtitle, 14, R.color.muted);
        description.setPadding(0, dp(8), 0, dp(28));
        content.addView(description);
        }
        activity.setContentView(scroll);
        scroll.requestApplyInsets();
    }
    int dp(float value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }
    int color(int id) { return activity.getColor(id); }
    TextView text(String value, int size, int color) {
        TextView view = new TextView(activity);
        view.setText(value); view.setTextSize(size); view.setTextColor(color(color));
        return view;
    }
    void section(String label) {
        TextView view = text(label, 13, R.color.accent);
        view.setTypeface(null, Typeface.BOLD);
        view.setPadding(dp(4), dp(12), dp(4), dp(8));
        view.setAccessibilityHeading(true);
        content.addView(view);
    }
    LinearLayout card() {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), 0, dp(16), 0);
        card.setBackground(shape(R.color.surface, 12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(8);
        content.addView(card, params);
        return card;
    }
    GradientDrawable shape(int color, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color(color)); shape.setCornerRadius(dp(radius));
        return shape;
    }
    void divider(LinearLayout parent) {
        View line = new View(activity);
        line.setBackgroundColor(color(R.color.outline));
        parent.addView(line, new LinearLayout.LayoutParams(-1, dp(1)));
    }
    Button button(LinearLayout parent, String label, boolean primary, View.OnClickListener click) {
        Button button = new Button(activity);
        button.setText(label); button.setAllCaps(false); button.setTextSize(16);
        button.setMinHeight(dp(52)); button.setPadding(0, dp(12), 0, dp(12));
        button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        button.setTextColor(color(primary ? R.color.on_accent : R.color.accent));
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(color(R.color.ripple)),
                shape(primary ? R.color.accent : R.color.surface, 8), null));
        button.setOnClickListener(click);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = 0; params.bottomMargin = 0;
        parent.addView(button, params);
        return button;
    }
    LinearLayout row(LinearLayout parent, String title, String summary) {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(14), 0, dp(14));
        row.setMinimumHeight(dp(72));
        LinearLayout labels = new LinearLayout(activity);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text(title, 17, R.color.ink));
        TextView detail = text(summary, 13, R.color.muted);
        detail.setPadding(0, dp(4), dp(12), 0);
        labels.addView(detail);
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        parent.addView(row);
        return row;
    }
    void note(String message) {
        TextView note = text(message, 13, R.color.muted);
        note.setPadding(dp(4), dp(6), dp(4), dp(8));
        content.addView(note);
    }
}
