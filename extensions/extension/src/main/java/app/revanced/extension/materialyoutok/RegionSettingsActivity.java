package app.revanced.extension.materialyoutok;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class RegionSettingsActivity extends Activity {
    private static final int ACCENT = 0xFF8E7CC3;
    private static final int CARD = 0xFF1F1F1F;
    private SharedPreferences prefs;
    private LinearLayout rows;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences(RegionSpoof.PREFS, MODE_PRIVATE);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF000000);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(56), dp(16), dp(32));
        scroll.addView(root);

        root.addView(label("Регион", 30, 0xFFFFFFFF));
        root.addView(label("Страна, для которой TikTok подбирает ленту", 15, 0xFF9E9E9E));

        LinearLayout toggleCard = card();
        toggleCard.setOrientation(LinearLayout.HORIZONTAL);
        toggleCard.setGravity(Gravity.CENTER_VERTICAL);
        TextView toggleText = label("Менять регион", 20, 0xFFFFFFFF);
        toggleText.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        Switch sw = new Switch(this);
        sw.setChecked(prefs.getBoolean(RegionSpoof.KEY_ENABLED, true));
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean on) {
                prefs.edit().putBoolean(RegionSpoof.KEY_ENABLED, on).apply();
                restartHint();
            }
        });
        toggleCard.addView(toggleText);
        toggleCard.addView(sw);
        root.addView(toggleCard);

        root.addView(label("Страна", 16, ACCENT));
        rows = card();
        rows.setOrientation(LinearLayout.VERTICAL);
        root.addView(rows);
        render();
        setContentView(scroll);
    }

    private void render() {
        rows.removeAllViews();
        String selected = prefs.getString(RegionSpoof.KEY_ISO, "by");
        for (final Regions.Region r : Regions.ALL) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(8), dp(12), dp(8), dp(12));

            LinearLayout texts = new LinearLayout(this);
            texts.setOrientation(LinearLayout.VERTICAL);
            texts.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
            texts.addView(label(r.flag() + "  " + r.name, 19, 0xFFFFFFFF));
            texts.addView(label(r.operator + "  ·  " + r.mccmnc + "  ·  " + r.iso.toUpperCase(), 14, 0xFF9E9E9E));
            row.addView(texts);

            if (r.iso.equalsIgnoreCase(selected)) {
                row.addView(label("✓", 22, ACCENT));
            }
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    prefs.edit().putString(RegionSpoof.KEY_ISO, r.iso).apply();
                    render();
                    restartHint();
                }
            });
            rows.addView(row);
        }
    }

    private void restartHint() {
        Toast.makeText(this, "Перезапусти TikTok, чтобы применить", Toast.LENGTH_SHORT).show();
    }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(dp(24));
        l.setBackground(bg);
        l.setPadding(dp(16), dp(8), dp(16), dp(8));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(12), 0, dp(12));
        l.setLayoutParams(p);
        return l;
    }

    private TextView label(String text, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setPadding(0, dp(4), 0, dp(4));
        return t;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
