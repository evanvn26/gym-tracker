package com.group15.gymtracker.domain;

public record StatsSummary(
        int currentStreak,
        int longestStreak,
        int hitTargetDaysThisMonth,
        int targetDaysThisMonth,
        float totalGymHours,
        int averageSessionMinutes
) {
}
