package com.group15.gymtracker.domain.dashboard;

public record DashboardLockStatus(
        State state,
        String summary,
        String reason,
        long unlockAtMillis
) {
    public enum State {
        UNAVAILABLE,
        READY,
        LOCKED
    }
}
