package com.sightunlock;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppPickerActivity extends Activity {

    private SrsState srs;
    private final Set<String> selected = new HashSet<>();

    private static class Entry {
        final String pkg;
        final String label;
        final Drawable icon;
        Entry(String p, String l, Drawable i) { pkg = p; label = l; icon = i; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        srs = new SrsState(this);
        selected.addAll(srs.getGuardedApps());

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(28), dp(20), dp(28));
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("Guarded apps");
        title.setTextColor(Color.BLACK);
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("A sight reading challenge appears the next time you open any selected app (with a 60-second cooldown so navigating around the app doesn't keep retriggering).");
        sub.setTextColor(Color.parseColor("#555555"));
        sub.setTextSize(13);
        sub.setPadding(0, dp(6), 0, dp(16));
        root.addView(sub);

        for (Entry e : loadLaunchableApps()) {
            root.addView(buildRow(e));
        }

        setContentView(scroll);
    }

    private View buildRow(final Entry e) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));

        ImageView icon = new ImageView(this);
        icon.setImageDrawable(e.icon);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(36), dp(36));
        iconLp.setMargins(0, 0, dp(12), 0);
        row.addView(icon, iconLp);

        TextView name = new TextView(this);
        name.setText(e.label);
        name.setTextColor(Color.BLACK);
        name.setTextSize(15);
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(name, nameLp);

        final CheckBox cb = new CheckBox(this);
        cb.setChecked(selected.contains(e.pkg));
        cb.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton button, boolean isChecked) {
                if (isChecked) selected.add(e.pkg); else selected.remove(e.pkg);
                srs.setGuardedApps(selected);
            }
        });
        row.addView(cb);

        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { cb.toggle(); }
        });
        return row;
    }

    private List<Entry> loadLaunchableApps() {
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN);
        main.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(main, 0);
        List<Entry> entries = new ArrayList<>();
        String self = getPackageName();
        Set<String> seenPkgs = new HashSet<>();
        for (ResolveInfo r : apps) {
            String pkg = r.activityInfo.packageName;
            if (pkg.equals(self) || !seenPkgs.add(pkg)) continue;
            CharSequence label = r.loadLabel(pm);
            Drawable icon = r.loadIcon(pm);
            entries.add(new Entry(pkg, label == null ? pkg : label.toString(), icon));
        }
        Collections.sort(entries, new Comparator<Entry>() {
            @Override public int compare(Entry a, Entry b) {
                return a.label.compareToIgnoreCase(b.label);
            }
        });
        return entries;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
