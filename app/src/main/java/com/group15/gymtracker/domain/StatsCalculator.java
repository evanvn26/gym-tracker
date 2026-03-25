package com.group15.gymtracker.domain;

import com.group15.gymtracker.database.dao.DailyTargetDao;
import com.group15.gymtracker.database.dao.GymSessionDao;
import com.group15.gymtracker.database.dao.UserInfoDao;
import com.group15.gymtracker.database.entities.DailyTargetEntity;
import com.group15.gymtracker.database.entities.GymSessionEntity;
import com.group15.gymtracker.database.entities.UserInfoEntity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatsCalculator {

    private final GymSessionDao gymSessionDao;
    private final DailyTargetDao dailyTargetDao;
    private final UserInfoDao userInfoDao;
    private final long nowMillis;

    public StatsCalculator(
            GymSessionDao gymSessionDao,
            DailyTargetDao dailyTargetDao,
            UserInfoDao userInfoDao
    ) {
        this(gymSessionDao, dailyTargetDao, userInfoDao, System.currentTimeMillis());
    }

    public StatsCalculator(
            GymSessionDao gymSessionDao,
            DailyTargetDao dailyTargetDao,
            UserInfoDao userInfoDao,
            long nowMillis
    ) {
        this.gymSessionDao = gymSessionDao;
        this.dailyTargetDao = dailyTargetDao;
        this.userInfoDao = userInfoDao;
        this.nowMillis = nowMillis;
    }

    public StatsSummary calculateCurrentMonthSummary() {
        UserInfoEntity userInfo = UserInfoStore.getOrCreate(userInfoDao);
        Calendar monthStart = buildMonthBoundary(true);
        Calendar monthEnd = buildMonthBoundary(false);

        List<GymSessionEntity> completedSessions = gymSessionDao.getCompletedSessionsBetween(
                formatDate(monthStart),
                formatDate(monthEnd)
        );

        Map<String, Integer> completedMinutesByDate = new HashMap<>();
        int completedSessionCount = 0;
        int totalCompletedMinutes = 0;

        for (GymSessionEntity session : completedSessions) {
            int durationMinutes = session.getDurationMinutes();
            if (durationMinutes <= 0 || session.sessionDate == null) {
                continue;
            }

            totalCompletedMinutes += durationMinutes;
            completedSessionCount += 1;
            completedMinutesByDate.put(
                    session.sessionDate,
                    completedMinutesByDate.getOrDefault(session.sessionDate, 0) + durationMinutes
            );
        }

        Map<String, Integer> targetMinutesByDay = new HashMap<>();
        for (DailyTargetEntity target : dailyTargetDao.getAllTargets()) {
            targetMinutesByDay.put(target.dayOfWeek, target.targetMinutes);
        }

        int hitTargetDaysThisMonth = 0;
        int targetDaysThisMonth = 0;
        Calendar cursor = (Calendar) monthStart.clone();

        while (!cursor.after(monthEnd)) {
            String dayKey = new SimpleDateFormat("EEEE", Locale.ENGLISH)
                    .format(cursor.getTime())
                    .toUpperCase(Locale.ENGLISH);
            int targetMinutes = targetMinutesByDay.getOrDefault(dayKey, 0);

            if (targetMinutes > 0) {
                targetDaysThisMonth += 1;
                String dateKey = formatDate(cursor);
                if (completedMinutesByDate.getOrDefault(dateKey, 0) >= targetMinutes) {
                    hitTargetDaysThisMonth += 1;
                }
            }

            cursor.add(Calendar.DAY_OF_YEAR, 1);
        }

        int averageSessionMinutes = completedSessionCount == 0
                ? 0
                : Math.round((float) totalCompletedMinutes / completedSessionCount);

        return new StatsSummary(
                userInfo.currentStreak,
                userInfo.longestStreak,
                hitTargetDaysThisMonth,
                targetDaysThisMonth,
                userInfo.totalGymHours,
                averageSessionMinutes
        );
    }

    private Calendar buildMonthBoundary(boolean startOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(nowMillis);
        if (startOfMonth) {
            calendar.set(Calendar.DAY_OF_MONTH, 1);
        } else {
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        }
        return calendar;
    }

    private String formatDate(Calendar calendar) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(calendar.getTime());
    }
}
