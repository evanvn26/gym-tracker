package com.group15.gymtracker.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.accessibility.AccessibilityEvent;

import com.group15.gymtracker.domain.AppLocker;

public class AppBlockAccessibilityService extends AccessibilityService {

    private LockOverlayController overlayController;
    private String defaultLauncherPackage;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        overlayController = new LockOverlayController(this);
        defaultLauncherPackage = getDefaultLauncherPackage();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;

        if (overlayController == null) {
            overlayController = new LockOverlayController(this);
        }
        if (defaultLauncherPackage == null) {
            defaultLauncherPackage = getDefaultLauncherPackage();
        }

        String packageName = event.getPackageName() == null ? null : event.getPackageName().toString();
        AppLocker locker = new AppLocker(this);
        LockOverlayForegroundPolicy.Decision decision = LockOverlayForegroundPolicy.decide(
                packageName,
                getPackageName(),
                defaultLauncherPackage,
                locker.isLocked(),
                locker.getBlockedPackages(),
                overlayController.getCoveredPackage()
        );

        switch (decision.action()) {
            case SHOW:
                overlayController.show(locker, decision.blockedPackage());
                break;
            case UPDATE:
                overlayController.update(locker, decision.blockedPackage());
                break;
            case HIDE:
                overlayController.hide();
                break;
            case NONE:
                break;
        }
    }

    private String getDefaultLauncherPackage() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo info = getPackageManager().resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);
        return info != null ? info.activityInfo.packageName : null;
    }

    @Override
    public void onInterrupt() {
        clearOverlay();
    }

    @Override
    public void onDestroy() {
        clearOverlay();
        super.onDestroy();
    }

    private void clearOverlay() {
        if (overlayController != null) {
            overlayController.destroy();
        }
    }
}
