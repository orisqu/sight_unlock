package com.sightunlock;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Listens to window-state changes; when the foreground package matches one of
 * the user-selected guarded apps and isn't on cooldown, it launches the quiz
 * over the app.
 */
public class AppGuardService extends AccessibilityService {

    /** Per-package cooldown so the quiz only fires the first time the app is
     *  brought to the foreground in any short window — not on every internal
     *  activity transition or after each answered challenge. */
    private static final long COOLDOWN_MS = 60_000L;

    private final Map<String, Long> lastTriggered = new HashMap<>();
    private String lastForegroundPackage = "";

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 50;
        setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence pkg = event.getPackageName();
        if (TextUtils.isEmpty(pkg)) return;
        String pkgStr = pkg.toString();

        // A WINDOW_STATE_CHANGED for the SAME package — e.g. a dialog opening
        // inside YouTube — should not retrigger.
        if (pkgStr.equals(lastForegroundPackage)) return;
        String prev = lastForegroundPackage;
        lastForegroundPackage = pkgStr;

        if (pkgStr.equals(getPackageName())) return;
        if (isSystemSurface(pkgStr)) return;

        Set<String> guarded = new SrsState(this).getGuardedApps();
        if (!guarded.contains(pkgStr)) return;

        long now = System.currentTimeMillis();
        Long last = lastTriggered.get(pkgStr);
        if (last != null && (now - last) < COOLDOWN_MS) return;
        lastTriggered.put(pkgStr, now);

        // Don't bother announcing the previous package; we only need it for
        // de-duplication above.
        if (prev == null) prev = "";

        Intent i = new Intent(this, LockActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_NO_ANIMATION
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        try {
            startActivity(i);
        } catch (Exception ignored) {
        }
    }

    private boolean isSystemSurface(String pkg) {
        return pkg.startsWith("com.android.systemui")
                || pkg.equals("android")
                || pkg.startsWith("com.google.android.inputmethod")
                || pkg.startsWith("com.android.inputmethod");
    }

    @Override
    public void onInterrupt() {}
}
