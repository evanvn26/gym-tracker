package com.group15.gymtracker.ui.main;

public interface LockStateSource {
    boolean isAccessibilityEnabled();
    boolean isLocked();
    String getReason();
    long getUnlockAtMillis();
}
