package com.sightunlock;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class SrsState {

    public enum Clef {
        TREBLE("treble"),
        BASS("bass");

        public final String prefKey;
        Clef(String pk) { prefKey = pk; }
    }

    // Position 0 = bottom staff line. Each position is half a staff space.
    // Range −4..12 covers 17 notes: 2 ledgers below to 2 ledgers above the staff.
    public static final int[] POSITIONS = {
            -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12
    };

    // Treble: bottom line = E4, so −4 = A3 (2nd ledger below).
    public static final char[] TREBLE_LETTERS = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G',
            'A', 'B', 'C', 'D', 'E', 'F', 'G',
            'A', 'B', 'C'
    };
    // Bass: bottom line = G2, so −4 = C2 (2nd ledger below).
    public static final char[] BASS_LETTERS = {
            'C', 'D', 'E', 'F', 'G', 'A', 'B',
            'C', 'D', 'E', 'F', 'G', 'A', 'B',
            'C', 'D', 'E'
    };

    public static final int MAX_BOX = 5;

    public static char letterFor(Clef c, int position) {
        char[] arr = c == Clef.TREBLE ? TREBLE_LETTERS : BASS_LETTERS;
        // POSITIONS is contiguous from −4 to 12, so index = position + 4.
        int idx = position + 4;
        if (idx < 0 || idx >= arr.length) return '?';
        return arr[idx];
    }

    public static class Pick {
        public final Clef clef;
        public final int position;
        public Pick(Clef c, int p) { clef = c; position = p; }
        public char letter() { return letterFor(clef, position); }
    }

    private static final String PREFS = "srs";
    private static final String KEY_TREBLE_ENABLED = "treble_enabled";
    private static final String KEY_BASS_ENABLED = "bass_enabled";
    private static final String KEY_GUARDED_APPS = "guarded_apps";
    private static final String KEY_TOTAL = "total";
    private static final String KEY_CORRECT = "correct";
    private static final String KEY_STREAK = "streak";
    private static final String KEY_MIGRATED = "migrated_v2";

    private final SharedPreferences sp;
    private final Random random = new Random();

    public SrsState(Context ctx) {
        sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        migrateLegacyKeys();
    }

    private void migrateLegacyKeys() {
        if (sp.getBoolean(KEY_MIGRATED, false)) return;
        // v1 used numeric indices into a 15-note treble-only POSITIONS array
        // {−2..12}. Rewrite as position-keyed treble entries.
        int[] LEGACY = { -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };
        SharedPreferences.Editor e = sp.edit();
        for (int i = 0; i < LEGACY.length; i++) {
            int box = sp.getInt("box_" + i, 0);
            int seen = sp.getInt("seen_" + i, 0);
            if (box > 0) e.putInt(boxKey(Clef.TREBLE, LEGACY[i]), box);
            if (seen > 0) e.putInt(seenKey(Clef.TREBLE, LEGACY[i]), seen);
            e.remove("box_" + i);
            e.remove("seen_" + i);
        }
        e.putBoolean(KEY_MIGRATED, true);
        e.apply();
    }

    private static String boxKey(Clef c, int position) {
        return c.prefKey + "_box_" + position;
    }
    private static String seenKey(Clef c, int position) {
        return c.prefKey + "_seen_" + position;
    }

    public int getBox(Clef c, int position) {
        return sp.getInt(boxKey(c, position), 1);
    }
    public int seenCount(Clef c, int position) {
        return sp.getInt(seenKey(c, position), 0);
    }

    public boolean isTrebleEnabled() { return sp.getBoolean(KEY_TREBLE_ENABLED, true); }
    public boolean isBassEnabled() { return sp.getBoolean(KEY_BASS_ENABLED, false); }
    public void setTrebleEnabled(boolean v) {
        sp.edit().putBoolean(KEY_TREBLE_ENABLED, v).apply();
    }
    public void setBassEnabled(boolean v) {
        sp.edit().putBoolean(KEY_BASS_ENABLED, v).apply();
    }

    public Set<String> getGuardedApps() {
        Set<String> raw = sp.getStringSet(KEY_GUARDED_APPS, Collections.<String>emptySet());
        return new HashSet<>(raw);
    }
    public void setGuardedApps(Set<String> apps) {
        sp.edit().putStringSet(KEY_GUARDED_APPS, new HashSet<>(apps)).apply();
    }

    public int totalReviews() { return sp.getInt(KEY_TOTAL, 0); }
    public int correctReviews() { return sp.getInt(KEY_CORRECT, 0); }
    public int streak() { return sp.getInt(KEY_STREAK, 0); }

    public int mastered() {
        int n = 0;
        for (Clef c : enabledClefs()) {
            for (int p : POSITIONS) {
                if (getBox(c, p) >= MAX_BOX) n++;
            }
        }
        return n;
    }

    public int totalNotes() {
        return enabledClefs().size() * POSITIONS.length;
    }

    private List<Clef> enabledClefs() {
        List<Clef> out = new ArrayList<>();
        if (isTrebleEnabled()) out.add(Clef.TREBLE);
        if (isBassEnabled()) out.add(Clef.BASS);
        return out;
    }

    public Pick pickNext() {
        List<Clef> clefs = enabledClefs();
        if (clefs.isEmpty()) {
            // Settings have both clefs off — fall back to treble so the user
            // never sees a broken empty quiz.
            clefs = Collections.singletonList(Clef.TREBLE);
        }

        // Phase 1: any unseen note across enabled clefs.
        List<Pick> unseen = new ArrayList<>();
        for (Clef c : clefs) {
            for (int p : POSITIONS) {
                if (seenCount(c, p) == 0) unseen.add(new Pick(c, p));
            }
        }
        if (!unseen.isEmpty()) {
            return unseen.get(random.nextInt(unseen.size()));
        }

        // Phase 2: weighted by Leitner box; mastered notes have a 0.18 floor
        // so they keep showing up.
        List<Pick> picks = new ArrayList<>();
        double[] weights = new double[clefs.size() * POSITIONS.length];
        double total = 0;
        int i = 0;
        for (Clef c : clefs) {
            for (int p : POSITIONS) {
                int box = getBox(c, p);
                double w = Math.max(0.18, Math.pow(0.55, box - 1));
                picks.add(new Pick(c, p));
                weights[i++] = w;
                total += w;
            }
        }
        double r = random.nextDouble() * total;
        double acc = 0;
        for (int k = 0; k < picks.size(); k++) {
            acc += weights[k];
            if (r < acc) return picks.get(k);
        }
        return picks.get(picks.size() - 1);
    }

    public void recordAnswer(Clef c, int position, boolean correct) {
        SharedPreferences.Editor e = sp.edit();
        int box = getBox(c, position);
        if (correct) {
            box = Math.min(MAX_BOX, box + 1);
            e.putInt(KEY_CORRECT, correctReviews() + 1);
            e.putInt(KEY_STREAK, streak() + 1);
        } else {
            box = 1;
            e.putInt(KEY_STREAK, 0);
        }
        e.putInt(boxKey(c, position), box);
        e.putInt(seenKey(c, position), seenCount(c, position) + 1);
        e.putInt(KEY_TOTAL, totalReviews() + 1);
        e.apply();
    }

    public void reset() {
        sp.edit().clear().apply();
    }
}
