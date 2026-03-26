package com.group15.gymtracker.domain.dashboard;

public record DashboardRecentSession(
        int verificationId,
        String sessionDate,
        Long checkInTimeMillis,
        Long checkOutTimeMillis,
        int durationMinutes,
        boolean active
) {
}
