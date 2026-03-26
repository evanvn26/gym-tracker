package com.group15.gymtracker.domain;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AppLocker {

    private static final String PREFS_NAME = "app_lock_prefs";
    private static final String KEY_IS_LOCKED = "is_locked";
    private static final String KEY_BLOCKED_PACKAGES = "blocked_packages";
    private static final String KEY_REASON = "reason";
    private static final String KEY_UNLOCK_AT = "unlock_at";

    private final SharedPreferences prefs;

    public AppLocker(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void lockApps(Set<String> blockedPackages, String reason, long unlockAtMillis) {
        prefs.edit()
                .putBoolean(KEY_IS_LOCKED, true)
                .putStringSet(KEY_BLOCKED_PACKAGES, new HashSet<>(blockedPackages))
                .putString(KEY_REASON, reason)
                .putLong(KEY_UNLOCK_AT, unlockAtMillis)
                .apply();
    }

    public void unlockApps() {
        prefs.edit()
                .putBoolean(KEY_IS_LOCKED, false)
                .putString(KEY_REASON, "")
                .putLong(KEY_UNLOCK_AT, 0L)
                .apply();
    }

    public boolean isLocked() {
        refreshExpiredLockIfNeeded();
        return prefs.getBoolean(KEY_IS_LOCKED, false);
    }

    public boolean shouldBlock(String packageName) {
        if (!isLocked()) return false;
        Set<String> blocked = getBlockedPackages();
        return blocked.contains(packageName);
    }

    public Set<String> getBlockedPackages() {
        refreshExpiredLockIfNeeded();
        return new HashSet<>(prefs.getStringSet(KEY_BLOCKED_PACKAGES, Collections.emptySet()));
    }

    public String getReason() {
        refreshExpiredLockIfNeeded();
        return prefs.getString(KEY_REASON, "");
    }

    public long getUnlockAtMillis() {
        refreshExpiredLockIfNeeded();
        return prefs.getLong(KEY_UNLOCK_AT, 0L);
    }

    private void refreshExpiredLockIfNeeded() {
        if (!prefs.getBoolean(KEY_IS_LOCKED, false)) {
            return;
        }

        long unlockAt = prefs.getLong(KEY_UNLOCK_AT, 0L);
        if (unlockAt > 0 && System.currentTimeMillis() >= unlockAt) {
            unlockApps();
        }
    }
}
