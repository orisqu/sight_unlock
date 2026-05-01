package com.sightunlock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView statsView;
    private Switch trebleSwitch;
    private Switch bassSwitch;
    private TextView accessibilityStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStats();
        refreshAccessibilityStatus();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(28), dp(20), dp(28));
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("Sight Unlock");
        title.setTextSize(28);
        title.setTextColor(Color.BLACK);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("A treble or bass clef sight reading challenge that runs at unlock and (optionally) before guarded apps open.");
        sub.setTextColor(Color.parseColor("#555555"));
        sub.setTextSize(14);
        sub.setPadding(0, dp(4), 0, dp(20));
        root.addView(sub);

        statsView = new TextView(this);
        statsView.setTextSize(15);
        statsView.setTextColor(Color.parseColor("#222222"));
        statsView.setPadding(dp(12), dp(12), dp(12), dp(12));
        statsView.setBackgroundColor(Color.parseColor("#F2F2F2"));
        root.addView(statsView);

        addSectionHeader(root, "Clefs");
        final SrsState srs = new SrsState(this);
        trebleSwitch = addSwitch(root, "Treble clef", srs.isTrebleEnabled(),
                new android.widget.CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(android.widget.CompoundButton button, boolean isChecked) {
                        srs.setTrebleEnabled(isChecked);
                        ensureAtLeastOneClef(srs, true);
                        refreshStats();
                    }
                });
        bassSwitch = addSwitch(root, "Bass clef", srs.isBassEnabled(),
                new android.widget.CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(android.widget.CompoundButton button, boolean isChecked) {
                        srs.setBassEnabled(isChecked);
                        ensureAtLeastOneClef(srs, false);
                        refreshStats();
                    }
                });

        addSectionHeader(root, "Permissions");
        addBigButton(root, "Grant overlay permission", new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && !Settings.canDrawOverlays(MainActivity.this)) {
                    Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivity(i);
                }
            }
        });
        addBigButton(root, "Disable battery optimisation", new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                        Intent i = new Intent(
                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:" + getPackageName()));
                        startActivity(i);
                    }
                }
            }
        });

        addSectionHeader(root, "Lock-screen service");
        addBigButton(root, "Start lock service", new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent svc = new Intent(MainActivity.this, LockService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(svc);
                } else {
                    startService(svc);
                }
            }
        });
        addBigButton(root, "Stop lock service", new View.OnClickListener() {
            @Override public void onClick(View v) {
                stopService(new Intent(MainActivity.this, LockService.class));
            }
        });

        addSectionHeader(root, "Guard specific apps");
        accessibilityStatus = new TextView(this);
        accessibilityStatus.setTextSize(13);
        accessibilityStatus.setTextColor(Color.parseColor("#555555"));
        accessibilityStatus.setPadding(0, 0, 0, dp(8));
        root.addView(accessibilityStatus);
        addBigButton(root, "Enable accessibility service", new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });
        addBigButton(root, "Pick guarded apps", new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AppPickerActivity.class));
            }
        });

        addSectionHeader(root, "Practice");
        addBigButton(root, "Open challenge now", new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, LockActivity.class));
            }
        });
        addBigButton(root, "Reset SRS progress", new View.OnClickListener() {
            @Override public void onClick(View v) {
                new SrsState(MainActivity.this).reset();
                refreshStats();
                trebleSwitch.setChecked(true);
                bassSwitch.setChecked(false);
            }
        });

        TextView footer = new TextView(this);
        footer.setText("The challenge appears whenever the screen is unlocked, and (if the accessibility service is on) whenever a guarded app is opened.");
        footer.setTextColor(Color.parseColor("#888888"));
        footer.setTextSize(12);
        footer.setPadding(0, dp(20), 0, dp(8));
        root.addView(footer);

        setContentView(scroll);
    }

    private Switch addSwitch(ViewGroup parent, String label, boolean checked,
                             android.widget.CompoundButton.OnCheckedChangeListener l) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextColor(Color.BLACK);
        t.setTextSize(16);
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(t, labelLp);
        Switch s = new Switch(this);
        s.setChecked(checked);
        s.setOnCheckedChangeListener(l);
        row.addView(s);
        parent.addView(row);
        return s;
    }

    private void ensureAtLeastOneClef(SrsState srs, boolean preferTreble) {
        if (!srs.isTrebleEnabled() && !srs.isBassEnabled()) {
            if (preferTreble) {
                srs.setBassEnabled(true);
                bassSwitch.setChecked(true);
            } else {
                srs.setTrebleEnabled(true);
                trebleSwitch.setChecked(true);
            }
        }
    }

    private void addSectionHeader(ViewGroup parent, String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(Color.parseColor("#1A1A2E"));
        t.setTextSize(16);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(20), 0, dp(8));
        parent.addView(t);
    }

    private void addBigButton(ViewGroup parent, String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        lp.setMargins(0, 0, 0, dp(8));
        parent.addView(b, lp);
    }

    private void refreshStats() {
        SrsState s = new SrsState(this);
        int total = s.totalReviews();
        int correct = s.correctReviews();
        int pct = total == 0 ? 0 : (int) Math.round(100.0 * correct / total);
        StringBuilder sb = new StringBuilder();
        sb.append("Notes mastered: ").append(s.mastered())
                .append(" / ").append(s.totalNotes()).append('\n');
        sb.append("Total reviews: ").append(total)
                .append("    Accuracy: ").append(pct).append("%\n");
        sb.append("Current streak: ").append(s.streak());
        statsView.setText(sb.toString());
    }

    private void refreshAccessibilityStatus() {
        if (accessibilityStatus == null) return;
        boolean on = isAccessibilityServiceEnabled();
        int guardedCount = new SrsState(this).getGuardedApps().size();
        if (on) {
            accessibilityStatus.setText("Service: enabled  ·  Guarded apps: " + guardedCount);
            accessibilityStatus.setTextColor(Color.parseColor("#1B873B"));
        } else {
            accessibilityStatus.setText("Service: not yet enabled. Tap below, then turn on \"Sight Unlock\" in the system Accessibility list.");
            accessibilityStatus.setTextColor(Color.parseColor("#B00020"));
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        String target = getPackageName() + "/" + AppGuardService.class.getName();
        for (String s : enabled.split(":")) {
            if (s.equalsIgnoreCase(target)) return true;
        }
        return false;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
