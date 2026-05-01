package com.sightunlock;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** A C–B octave of a piano keyboard. White keys are tappable answers; black
 *  keys are inert and present only for visual orientation. */
public class PianoView extends ViewGroup {

    public interface OnLetterClick {
        void onLetter(char letter);
    }

    // Piano-standard order, left to right.
    private static final char[] WHITE_LETTERS = {'C', 'D', 'E', 'F', 'G', 'A', 'B'};
    // Indices (into WHITE_LETTERS) of the white key that each black key sits
    // immediately after. There is no black key after E (idx 2) or B (idx 6).
    private static final int[] BLACK_AFTER = {0, 1, 3, 4, 5};

    private static final int WHITE_BG = 0xFFE8DDC5;
    private static final int WHITE_BG_DOWN = 0xFFB8AE99;
    private static final int WHITE_INK = 0xFF20242F;
    private static final int BLACK_BG = 0xFF1A1E29;
    private static final int BORDER = 0xFF20242F;

    private final List<TextView> whiteKeys = new ArrayList<>();
    private final List<View> blackKeys = new ArrayList<>();
    private OnLetterClick listener;
    private boolean enabledForInput = true;

    public PianoView(Context c) { super(c); init(); }
    public PianoView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        setClipChildren(false);
        for (final char letter : WHITE_LETTERS) {
            TextView w = new TextView(getContext());
            w.setText(String.valueOf(letter));
            w.setTextColor(WHITE_INK);
            w.setTextSize(20);
            w.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
            w.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
            w.setPadding(0, 0, 0, dp(10));
            w.setBackground(makeKeyBg(WHITE_BG));
            w.setClickable(true);
            w.setOnClickListener(new OnClickListener() {
                @Override public void onClick(View v) {
                    if (!enabledForInput) return;
                    flash(v, WHITE_BG_DOWN, WHITE_BG);
                    if (listener != null) listener.onLetter(letter);
                }
            });
            whiteKeys.add(w);
            addView(w);
        }
        for (int i = 0; i < BLACK_AFTER.length; i++) {
            View b = new View(getContext());
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(BLACK_BG);
            bg.setCornerRadii(new float[]{0, 0, 0, 0, dp(4), dp(4), dp(4), dp(4)});
            b.setBackground(bg);
            blackKeys.add(b);
            addView(b);
        }
    }

    public void setOnLetterClick(OnLetterClick l) { listener = l; }

    public void setInputEnabled(boolean b) {
        enabledForInput = b;
        for (TextView w : whiteKeys) w.setAlpha(b ? 1f : 0.6f);
    }

    private GradientDrawable makeKeyBg(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setStroke(Math.max(1, dp(1)), BORDER);
        g.setCornerRadii(new float[]{0, 0, 0, 0, dp(6), dp(6), dp(6), dp(6)});
        return g;
    }

    private void flash(final View v, int down, final int up) {
        v.setBackground(makeKeyBg(down));
        v.postDelayed(new Runnable() {
            @Override public void run() { v.setBackground(makeKeyBg(up)); }
        }, 90);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        if (h == 0) h = dp(140);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int w = r - l;
        int h = b - t;
        float whiteW = w / (float) WHITE_LETTERS.length;
        for (int i = 0; i < WHITE_LETTERS.length; i++) {
            int xL = Math.round(i * whiteW);
            int xR = Math.round((i + 1) * whiteW);
            whiteKeys.get(i).layout(xL, 0, xR, h);
        }
        // Black keys are 0.6× the width of a white and 0.6× its height,
        // centered on the boundary between two whites.
        float blackW = whiteW * 0.6f;
        int blackH = Math.round(h * 0.6f);
        for (int i = 0; i < BLACK_AFTER.length; i++) {
            float center = (BLACK_AFTER[i] + 1) * whiteW;
            int xL = Math.round(center - blackW / 2f);
            int xR = Math.round(center + blackW / 2f);
            blackKeys.get(i).layout(xL, 0, xR, blackH);
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
