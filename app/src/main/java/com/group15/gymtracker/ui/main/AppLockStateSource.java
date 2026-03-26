package com.group15.gymtracker.ui.main;

import android.content.Context;

import com.group15.gymtracker.domain.AppLocker;

public final class AppLockStateSource implements LockStateSource {

    private final Context context;
    private final AppLocker locker;

    public AppLockStateSource(Context context) {
        this.context = context.getApplicationContext();
        this.locker = new AppLocker(this.context);
    }

    @Override
    public boolean isAccessibilityEnabled() {
        return AccessibilityServiceStatusHelper.isAppBlockServiceEnabled(context);
    }

    @Override
    public boolean isLocked() {
        return locker.isLocked();
    }

    @Override
    public String getReason() {
        return locker.getReason();
    }

    @Override
    public long getUnlockAtMillis() {
        return locker.getUnlockAtMillis();
    }
}
