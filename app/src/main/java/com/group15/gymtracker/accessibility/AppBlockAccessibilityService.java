package com.group15.gymtracker.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;

import com.group15.gymtracker.domain.AppLocker;

public class AppBlockAccessibilityService extends AccessibilityService {

    private static final long OVERLAY_COOLDOWN_MS = 800;
    private long lastOverlayLaunchTime = 0L;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        if (event.getPackageName() == null) return;

        String packageName = event.getPackageName().toString();

        if (packageName.equals(getPackageName())) return;
        if (packageName.equals("com.android.systemui")) return;
        if (packageName.equals(getDefaultLauncherPackage())) return;

        AppLocker locker = new AppLocker(this);
        if (!locker.shouldBlock(packageName)) return;

        long now = SystemClock.elapsedRealtime();
        if (now - lastOverlayLaunchTime < OVERLAY_COOLDOWN_MS) {
            return;
        }
        lastOverlayLaunchTime = now;

        performGlobalAction(GLOBAL_ACTION_HOME);

        Intent intent = new Intent(this, LockOverlayActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("blocked_package", packageName);
        startActivity(intent);
    }

    private String getDefaultLauncherPackage() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo info = getPackageManager().resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);
        return info != null ? info.activityInfo.packageName : null;
    }

    @Override
    public void onInterrupt() {
        // no-op
    }
}
