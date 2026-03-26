package com.group15.gymtracker.domain.dashboard;

public record DashboardChartBar(
        String label,
        int completedMinutes,
        int targetMinutes,
        boolean targetMet
) {
}
