package com.group15.gymtracker.domain.dashboard;

public record DashboardMonthStatus(
        String label,
        int targetMinutes,
        int completedMinutes,
        Status status
) {
    public enum Status {
        HIT,
        MISSED,
        PENDING
    }
}
