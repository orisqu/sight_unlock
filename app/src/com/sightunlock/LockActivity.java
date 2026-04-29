package com.sightunlock;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class LockActivity extends Activity {

    private SrsState srs;
    private StaffView staff;
    private TextView feedback;
    private TextView stats;
    private LinearLayout buttonRow;
    private int currentIndex;
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
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        root.setPadding(dp(16), dp(24), dp(16), dp(24));

        stats = new TextView(this);
        stats.setTextColor(Color.parseColor("#666666"));
        stats.setTextSize(13);
        stats.setGravity(Gravity.CENTER);
        root.addView(stats, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView prompt = new TextView(this);
        prompt.setText("Name this note");
        prompt.setTextColor(Color.BLACK);
        prompt.setTextSize(22);
        prompt.setTypeface(Typeface.DEFAULT_BOLD);
        prompt.setGravity(Gravity.CENTER);
        prompt.setPadding(0, dp(8), 0, dp(8));
        root.addView(prompt, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        FrameLayout staffWrap = new FrameLayout(this);
        staffWrap.setBackgroundColor(Color.WHITE);
        staff = new StaffView(this);
        staffWrap.addView(staff, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout.LayoutParams staffLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(staffWrap, staffLp);

        feedback = new TextView(this);
        feedback.setTextSize(20);
        feedback.setTypeface(Typeface.DEFAULT_BOLD);
        feedback.setGravity(Gravity.CENTER);
        feedback.setPadding(0, dp(8), 0, dp(8));
        feedback.setMinHeight(dp(40));
        root.addView(feedback, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setGravity(Gravity.CENTER);
        char[] letters = {'A', 'B', 'C', 'D', 'E', 'F', 'G'};
        for (final char letter : letters) {
            Button b = new Button(this);
            b.setText(String.valueOf(letter));
            b.setTextSize(20);
            b.setTypeface(Typeface.DEFAULT_BOLD);
            b.setAllCaps(false);
            LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                    0, dp(64), 1f);
            bLp.setMargins(dp(3), 0, dp(3), 0);
            buttonRow.addView(b, bLp);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onAnswer(letter);
                }
            });
        }
        root.addView(buttonRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);
    }

    private void nextQuestion() {
        currentIndex = srs.pickNextIndex();
        staff.setNotePosition(SrsState.POSITIONS[currentIndex]);
        feedback.setText("");
        awaitingAnswer = true;
        updateStats();
        for (int i = 0; i < buttonRow.getChildCount(); i++) {
            buttonRow.getChildAt(i).setEnabled(true);
        }
    }

    private void updateStats() {
        int total = srs.totalReviews();
        int correct = srs.correctReviews();
        int pct = total == 0 ? 0 : (int) Math.round(100.0 * correct / total);
        stats.setText("Mastered " + srs.mastered() + "/" + SrsState.POSITIONS.length
                + "   Streak " + srs.streak()
                + "   Reviews " + total + " (" + pct + "%)");
    }

    private void onAnswer(char letter) {
        if (!awaitingAnswer) return;
        char correct = SrsState.LETTERS[currentIndex];
        boolean isRight = (letter == correct);
        srs.recordAnswer(currentIndex, isRight);
        awaitingAnswer = false;
        for (int i = 0; i < buttonRow.getChildCount(); i++) {
            buttonRow.getChildAt(i).setEnabled(false);
        }
        if (isRight) {
            feedback.setTextColor(Color.parseColor("#1B873B"));
            feedback.setText(correct + " — correct");
            staff.postDelayed(new Runnable() {
                @Override
                public void run() {
                    finishAndUnlock();
                }
            }, 350);
        } else {
            vibrate();
            feedback.setTextColor(Color.parseColor("#B00020"));
            feedback.setText("That was " + correct + ". Try the next one.");
            staff.postDelayed(new Runnable() {
                @Override
                public void run() {
                    nextQuestion();
                }
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
