package com.sightunlock;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class LockActivity extends Activity {

    private static final int BG = 0xFF2A2F3D;
    private static final int INK = 0xFFE8DDC5;
    private static final int INK_DIM = 0xFF8A8170;
    private static final int OK_INK = 0xFF9BC58A;
    private static final int ERR_INK = 0xFFD08C8C;
    private static final long IDLE_TIMEOUT_MS = 30_000L;

    private SrsState srs;
    private final Handler idleHandler = new Handler(Looper.getMainLooper());
    private final Runnable idleRunnable = new Runnable() {
        @Override public void run() { finish(); }
    };
    private StaffView staff;
    private TextView feedback;
    private TextView stats;
    private PianoView piano;
    private SrsState.Pick current;
    private boolean awaitingAnswer = true;

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

        srs = new SrsState(this);
        buildUi();
        nextQuestion();
        resetIdleTimer();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        resetIdleTimer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        idleHandler.removeCallbacks(idleRunnable);
    }

    private void resetIdleTimer() {
        idleHandler.removeCallbacks(idleRunnable);
        idleHandler.postDelayed(idleRunnable, IDLE_TIMEOUT_MS);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(16), dp(24), dp(16), dp(16));

        stats = new TextView(this);
        stats.setTextColor(INK_DIM);
        stats.setTextSize(12);
        stats.setLetterSpacing(0.04f);
        stats.setGravity(Gravity.CENTER);
        root.addView(stats, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView prompt = new TextView(this);
        prompt.setText("Name this note");
        prompt.setTextColor(INK);
        prompt.setTextSize(20);
        prompt.setTypeface(Typeface.create(Typeface.SERIF, Typeface.NORMAL));
        prompt.setGravity(Gravity.CENTER);
        prompt.setPadding(0, dp(10), 0, dp(6));
        root.addView(prompt, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        FrameLayout staffWrap = new FrameLayout(this);
        staffWrap.setBackgroundColor(BG);
        staff = new StaffView(this);
        staffWrap.addView(staff, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout.LayoutParams staffLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(staffWrap, staffLp);

        feedback = new TextView(this);
        feedback.setTextSize(18);
        feedback.setTypeface(Typeface.create(Typeface.SERIF, Typeface.NORMAL));
        feedback.setGravity(Gravity.CENTER);
        feedback.setPadding(0, dp(8), 0, dp(8));
        feedback.setMinHeight(dp(40));
        root.addView(feedback, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        piano = new PianoView(this);
        piano.setOnLetterClick(new PianoView.OnLetterClick() {
            @Override public void onLetter(char letter) { onAnswer(letter); }
        });
        LinearLayout.LayoutParams pianoLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(150));
        root.addView(piano, pianoLp);

        setContentView(root);
    }

    private void nextQuestion() {
        current = srs.pickNext();
        staff.setClef(current.clef);
        staff.setNotePosition(current.position);
        feedback.setText("");
        awaitingAnswer = true;
        updateStats();
        piano.setInputEnabled(true);
    }

    private void updateStats() {
        int total = srs.totalReviews();
        int correct = srs.correctReviews();
        int pct = total == 0 ? 0 : (int) Math.round(100.0 * correct / total);
        stats.setText("Mastered " + srs.mastered() + "/" + srs.totalNotes()
                + "   Streak " + srs.streak()
                + "   Reviews " + total + " (" + pct + "%)");
    }

    private void onAnswer(char letter) {
        if (!awaitingAnswer) return;
        char correct = current.letter();
        boolean isRight = (letter == correct);
        srs.recordAnswer(current.clef, current.position, isRight);
        awaitingAnswer = false;
        piano.setInputEnabled(false);
        if (isRight) {
            feedback.setTextColor(OK_INK);
            feedback.setText(correct + " — correct");
            staff.postDelayed(new Runnable() {
                @Override public void run() { finishAndUnlock(); }
            }, 350);
        } else {
            vibrate();
            feedback.setTextColor(ERR_INK);
            feedback.setText("That was " + correct + ". Try the next one.");
            staff.postDelayed(new Runnable() {
                @Override public void run() { nextQuestion(); }
            }, 1100);
        }
    }

    private void vibrate() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(120);
        }
    }

    private void finishAndUnlock() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null && !km.isKeyguardSecure()) {
                km.requestDismissKeyguard(this, null);
            }
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }
        finish();
        overridePendingTransition(0, android.R.anim.fade_out);
    }

    @Override
    public void onBackPressed() {
        // Back doesn't dismiss; only a correct answer does.
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
