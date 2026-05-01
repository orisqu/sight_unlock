package com.sightunlock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ScreenReceiver extends BroadcastReceiver {

    private long lastLaunchAtMs = 0L;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;
        if (Intent.ACTION_USER_PRESENT.equals(action)) {
            long now = System.currentTimeMillis();
            if (now - lastLaunchAtMs < 800) return;
            lastLaunchAtMs = now;
            Intent i = new Intent(context, LockActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_NO_ANIMATION
                    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            try {
                context.startActivity(i);
            } catch (Exception ignored) {
            }
        }
    }
}
