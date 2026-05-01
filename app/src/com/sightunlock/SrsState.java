package com.sightunlock;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SrsState {

    public static final int[] POSITIONS = {
            -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12
    };
    public static final char[] LETTERS = {
            'C', 'D', 'E', 'F', 'G', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'A', 'B', 'C'
    };
    public static final int MAX_BOX = 5;

    private static final String PREFS = "srs";
    private static final String KEY_BOX_PREFIX = "box_";
    private static final String KEY_SEEN_PREFIX = "seen_";
    private static final String KEY_TOTAL = "total";
    private static final String KEY_CORRECT = "correct";
    private static final String KEY_STREAK = "streak";

    private final SharedPreferences sp;
    private final Random random = new Random();

    public SrsState(Context ctx) {
        sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public int getBox(int index) {
        return sp.getInt(KEY_BOX_PREFIX + index, 1);
    }

    public int seenCount(int index) {
        return sp.getInt(KEY_SEEN_PREFIX + index, 0);
    }

    public int totalReviews() { return sp.getInt(KEY_TOTAL, 0); }
    public int correctReviews() { return sp.getInt(KEY_CORRECT, 0); }
    public int streak() { return sp.getInt(KEY_STREAK, 0); }

    public int mastered() {
        int n = 0;
        for (int i = 0; i < POSITIONS.length; i++) {
            if (getBox(i) >= MAX_BOX) n++;
        }
        return n;
    }

    public int pickNextIndex() {
        // Phase 1: guarantee every note appears in the first N reviews.
        // Pick uniformly at random from any note that has never been shown.
        List<Integer> unseen = new ArrayList<>();
        for (int i = 0; i < POSITIONS.length; i++) {
            if (seenCount(i) == 0) unseen.add(i);
        }
        if (!unseen.isEmpty()) {
            return unseen.get(random.nextInt(unseen.size()));
        }

        // Phase 2: prefer notes the user has been getting wrong, but keep
        // mastered notes in rotation so they don't decay silently.
        double[] weights = new double[POSITIONS.length];
        double total = 0;
        for (int i = 0; i < POSITIONS.length; i++) {
            int box = getBox(i);
            // Floor of 0.18 means even fully-mastered notes appear ~1 time
            // per ~6 reviews of an unmastered note.
            double w = Math.max(0.18, Math.pow(0.55, box - 1));
            weights[i] = w;
            total += w;
        }
        double r = random.nextDouble() * total;
        double acc = 0;
        for (int i = 0; i < POSITIONS.length; i++) {
            acc += weights[i];
            if (r < acc) return i;
        }
        return POSITIONS.length - 1;
    }

    public void recordAnswer(int index, boolean correct) {
        SharedPreferences.Editor e = sp.edit();
        int box = getBox(index);
        if (correct) {
            box = Math.min(MAX_BOX, box + 1);
            e.putInt(KEY_CORRECT, correctReviews() + 1);
            e.putInt(KEY_STREAK, streak() + 1);
        } else {
            box = 1;
            e.putInt(KEY_STREAK, 0);
        }
        e.putInt(KEY_BOX_PREFIX + index, box);
        e.putInt(KEY_SEEN_PREFIX + index, seenCount(index) + 1);
        e.putInt(KEY_TOTAL, totalReviews() + 1);
        e.apply();
    }

    public void reset() {
        sp.edit().clear().apply();
    }
}
