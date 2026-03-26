package com.group15.gymtracker.domain.dashboard;

import com.group15.gymtracker.domain.StatsSummary;

import java.util.List;

public record DashboardSnapshot(
        DashboardActiveSession activeSession,
        int todayTargetMinutes,
        int todayCompletedMinutes,
        WeeklyGoalProgress weeklyGoalProgress,
        StatsSummary statsSummary,
        List<DashboardChartBar> last7DaysSeries,
        List<DashboardMonthStatus> monthlyTargetSeries,
        DashboardLockStatus lockStatus,
        List<DashboardRecentSession> recentSessions,
        int blockedAppCount,
        boolean accessibilityEnabled,
        int freezeTokens
) {
    public DashboardSnapshot {
        last7DaysSeries = List.copyOf(last7DaysSeries);
        monthlyTargetSeries = List.copyOf(monthlyTargetSeries);
        recentSessions = List.copyOf(recentSessions);
    }
}
