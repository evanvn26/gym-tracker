package com.group15.gymtracker.domain.dashboard;

public record DashboardActiveSession(
        int verificationId,
        String sessionDate,
        long checkInTimeMillis
) {
}
