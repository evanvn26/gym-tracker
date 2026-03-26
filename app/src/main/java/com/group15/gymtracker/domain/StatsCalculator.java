package com.group15.gymtracker.domain;

import com.group15.gymtracker.database.dao.DailyTargetDao;
import com.group15.gymtracker.database.dao.GymSessionDao;
import com.group15.gymtracker.database.dao.UserInfoDao;
import com.group15.gymtracker.database.entities.DailyTargetEntity;
import com.group15.gymtracker.database.entities.GymSessionEntity;
import com.group15.gymtracker.database.entities.UserInfoEntity;
import com.group15.gymtracker.domain.dashboard.DashboardChartBar;
import com.group15.gymtracker.domain.dashboard.DashboardMonthStatus;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

        List<GymSessionEntity> completedSessions = getCompletedSessionsBetween(monthStart, monthEnd);
        Map<String, Integer> completedMinutesByDate = buildCompletedMinutesByDate(completedSessions);
        int completedSessionCount = countCompletedSessions(completedSessions);
        int totalCompletedMinutes = countTotalCompletedMinutes(completedSessions);
        Map<String, Integer> targetMinutesByDay = buildTargetMinutesByDay();

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

    public List<DashboardChartBar> calculateLast7DaysSeries() {
        Calendar end = Calendar.getInstance(Locale.getDefault());
        end.setTimeInMillis(nowMillis);
        Calendar start = (Calendar) end.clone();
        start.add(Calendar.DAY_OF_YEAR, -6);

        Map<String, Integer> completedMinutesByDate = buildCompletedMinutesByDate(getCompletedSessionsBetween(start, end));
        Map<String, Integer> targetMinutesByDay = buildTargetMinutesByDay();
        List<DashboardChartBar> series = new ArrayList<>();
        Calendar cursor = (Calendar) start.clone();

        while (!cursor.after(end)) {
            String dateKey = formatDate(cursor);
            int completedMinutes = completedMinutesByDate.getOrDefault(dateKey, 0);
            int targetMinutes = targetMinutesByDay.getOrDefault(dayKey(cursor), 0);
            series.add(new DashboardChartBar(
                    buildShortDayLabel(cursor),
                    completedMinutes,
                    targetMinutes,
                    targetMinutes > 0 && completedMinutes >= targetMinutes
            ));
            cursor.add(Calendar.DAY_OF_YEAR, 1);
        }

        return series;
    }

    public List<DashboardMonthStatus> calculateMonthTargetSeries() {
        Calendar monthStart = buildMonthBoundary(true);
        Calendar today = Calendar.getInstance(Locale.getDefault());
        today.setTimeInMillis(nowMillis);

        Map<String, Integer> completedMinutesByDate = buildCompletedMinutesByDate(getCompletedSessionsBetween(monthStart, today));
        Map<String, Integer> targetMinutesByDay = buildTargetMinutesByDay();
        List<DashboardMonthStatus> series = new ArrayList<>();
        Calendar cursor = (Calendar) monthStart.clone();

        while (!cursor.after(today)) {
            int targetMinutes = targetMinutesByDay.getOrDefault(dayKey(cursor), 0);
            if (targetMinutes > 0) {
                String dateKey = formatDate(cursor);
                int completedMinutes = completedMinutesByDate.getOrDefault(dateKey, 0);
                DashboardMonthStatus.Status status;
                if (completedMinutes >= targetMinutes) {
                    status = DashboardMonthStatus.Status.HIT;
                } else if (isSameDay(cursor, today)) {
                    status = DashboardMonthStatus.Status.PENDING;
                } else {
                    status = DashboardMonthStatus.Status.MISSED;
                }
                series.add(new DashboardMonthStatus(
                        String.valueOf(cursor.get(Calendar.DAY_OF_MONTH)),
                        targetMinutes,
                        completedMinutes,
                        status
                ));
            }
            cursor.add(Calendar.DAY_OF_YEAR, 1);
        }

        return series;
    }

    private Calendar buildMonthBoundary(boolean startOfMonth) {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
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

    private List<GymSessionEntity> getCompletedSessionsBetween(Calendar start, Calendar end) {
        return gymSessionDao.getCompletedSessionsBetween(formatDate(start), formatDate(end));
    }

    private Map<String, Integer> buildCompletedMinutesByDate(List<GymSessionEntity> completedSessions) {
        Map<String, Integer> completedMinutesByDate = new HashMap<>();
        for (GymSessionEntity session : completedSessions) {
            int durationMinutes = session.getDurationMinutes();
            if (durationMinutes <= 0 || session.sessionDate == null) {
                continue;
            }
            completedMinutesByDate.put(
                    session.sessionDate,
                    completedMinutesByDate.getOrDefault(session.sessionDate, 0) + durationMinutes
            );
        }
        return completedMinutesByDate;
    }

    private Map<String, Integer> buildTargetMinutesByDay() {
        Map<String, Integer> targetMinutesByDay = new HashMap<>();
        for (DailyTargetEntity target : dailyTargetDao.getAllTargets()) {
            targetMinutesByDay.put(target.dayOfWeek, target.targetMinutes);
        }
        return targetMinutesByDay;
    }

    private int countCompletedSessions(List<GymSessionEntity> completedSessions) {
        int completedSessionCount = 0;
        for (GymSessionEntity session : completedSessions) {
            if (session.getDurationMinutes() > 0 && session.sessionDate != null) {
                completedSessionCount += 1;
            }
        }
        return completedSessionCount;
    }

    private int countTotalCompletedMinutes(List<GymSessionEntity> completedSessions) {
        int totalCompletedMinutes = 0;
        for (GymSessionEntity session : completedSessions) {
            int durationMinutes = session.getDurationMinutes();
            if (durationMinutes > 0 && session.sessionDate != null) {
                totalCompletedMinutes += durationMinutes;
            }
        }
        return totalCompletedMinutes;
    }

    private String dayKey(Calendar calendar) {
        return new SimpleDateFormat("EEEE", Locale.ENGLISH)
                .format(calendar.getTime())
                .toUpperCase(Locale.ENGLISH);
    }

    private boolean isSameDay(Calendar first, Calendar second) {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);
    }

    private String buildShortDayLabel(Calendar calendar) {
        String[] weekdays = new DateFormatSymbols(Locale.getDefault()).getShortWeekdays();
        String label = weekdays[calendar.get(Calendar.DAY_OF_WEEK)];
        if (label == null || label.isEmpty()) {
            return "";
        }
        return label.substring(0, Math.min(3, label.length()));
    }
}
