package com.group15.gymtracker.ui.main;

import com.group15.gymtracker.database.dao.DailyTargetDao;
import com.group15.gymtracker.database.dao.GymSessionDao;
import com.group15.gymtracker.database.dao.UserInfoDao;
import com.group15.gymtracker.database.entities.DailyTargetEntity;
import com.group15.gymtracker.database.entities.GymSessionEntity;
import com.group15.gymtracker.domain.StatsCalculator;
import com.group15.gymtracker.domain.StatsSummary;
import com.group15.gymtracker.domain.UserInfoStore;
import com.group15.gymtracker.domain.dashboard.DashboardActiveSession;
import com.group15.gymtracker.domain.dashboard.DashboardChartBar;
import com.group15.gymtracker.domain.dashboard.DashboardLockStatus;
import com.group15.gymtracker.domain.dashboard.DashboardMonthStatus;
import com.group15.gymtracker.domain.dashboard.DashboardRecentSession;
import com.group15.gymtracker.domain.dashboard.DashboardSnapshot;
import com.group15.gymtracker.domain.dashboard.WeeklyGoalProgress;
import com.group15.gymtracker.onboarding.OnboardingUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class MainDashboardRepository {

    public enum FreezeTokenUnlockResult {
        SUCCESS,
        NOT_LOCKED,
        NO_TOKENS
    }

    private final GymSessionDao gymSessionDao;
    private final DailyTargetDao dailyTargetDao;
    private final UserInfoDao userInfoDao;
    private final LockStateSource lockStateSource;
    private final long nowMillis;

    public MainDashboardRepository(
            GymSessionDao gymSessionDao,
            DailyTargetDao dailyTargetDao,
            UserInfoDao userInfoDao,
            LockStateSource lockStateSource
    ) {
        this(gymSessionDao, dailyTargetDao, userInfoDao, lockStateSource, System.currentTimeMillis());
    }

    public MainDashboardRepository(
            GymSessionDao gymSessionDao,
            DailyTargetDao dailyTargetDao,
            UserInfoDao userInfoDao,
            LockStateSource lockStateSource,
            long nowMillis
    ) {
        this.gymSessionDao = gymSessionDao;
        this.dailyTargetDao = dailyTargetDao;
        this.userInfoDao = userInfoDao;
        this.lockStateSource = lockStateSource;
        this.nowMillis = nowMillis;
    }

    public DashboardSnapshot load() {
        UserInfoStore.getOrCreate(userInfoDao);
        GymSessionEntity activeSessionEntity = gymSessionDao.getActiveSession();
        DashboardActiveSession activeSession = activeSessionEntity == null || activeSessionEntity.checkInTime == null
                ? null
                : new DashboardActiveSession(
                        activeSessionEntity.verificationId,
                        activeSessionEntity.sessionDate,
                        activeSessionEntity.checkInTime
                );

        Calendar now = Calendar.getInstance(Locale.getDefault());
        now.setTimeInMillis(nowMillis);

        int todayTargetMinutes = findTargetMinutes(dayKey(now));
        int todayCompletedMinutes = gymSessionDao.getTotalCompletedMinutesForDate(formatDate(now))
                + activeMinutesForDate(activeSessionEntity, formatDate(now));

        StatsCalculator statsCalculator = new StatsCalculator(gymSessionDao, dailyTargetDao, userInfoDao, nowMillis);
        StatsSummary statsSummary = statsCalculator.calculateCurrentMonthSummary();
        List<DashboardChartBar> last7DaysSeries = includeActiveMinutes(
                statsCalculator.calculateLast7DaysSeries(),
                activeSessionEntity
        );
        List<DashboardMonthStatus> monthlyTargetSeries = includeActiveMonthProgress(
                statsCalculator.calculateMonthTargetSeries(),
                activeSessionEntity,
                todayTargetMinutes
        );

        Set<String> blockedPackages = OnboardingUtils.deserializeBlockedPackages(userInfoDao.getBlockedApps());
        boolean accessibilityEnabled = lockStateSource.isAccessibilityEnabled();

        return new DashboardSnapshot(
                activeSession,
                todayTargetMinutes,
                todayCompletedMinutes,
                buildWeeklyGoalProgress(activeSessionEntity, now),
                statsSummary,
                last7DaysSeries,
                monthlyTargetSeries,
                buildLockStatus(accessibilityEnabled, blockedPackages.size()),
                buildRecentSessions(activeSessionEntity),
                blockedPackages.size(),
                accessibilityEnabled,
                userInfoDao.getFreezeTokens()
        );
    }

    public FreezeTokenUnlockResult useFreezeTokenToUnlock() {
        if (!lockStateSource.isLocked()) {
            return FreezeTokenUnlockResult.NOT_LOCKED;
        }

        int freezeTokens = UserInfoStore.getOrCreate(userInfoDao).freezeTokens;
        if (freezeTokens <= 0) {
            return FreezeTokenUnlockResult.NO_TOKENS;
        }

        userInfoDao.updateFreezeTokens(freezeTokens - 1);
        lockStateSource.unlockApps();
        return FreezeTokenUnlockResult.SUCCESS;
    }

    private WeeklyGoalProgress buildWeeklyGoalProgress(GymSessionEntity activeSessionEntity, Calendar now) {
        Calendar weekStart = (Calendar) now.clone();
        int distanceFromMonday = (weekStart.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        weekStart.add(Calendar.DAY_OF_YEAR, -distanceFromMonday);

        int scheduledDays = 0;
        int hitDays = 0;
        for (int index = 0; index < 7; index++) {
            Calendar day = (Calendar) weekStart.clone();
            day.add(Calendar.DAY_OF_YEAR, index);
            int targetMinutes = findTargetMinutes(dayKey(day));
            if (targetMinutes <= 0) {
                continue;
            }
            scheduledDays += 1;
            if (day.after(now)) {
                continue;
            }
            String dateKey = formatDate(day);
            int completedMinutes = gymSessionDao.getTotalCompletedMinutesForDate(dateKey)
                    + activeMinutesForDate(activeSessionEntity, dateKey);
            if (completedMinutes >= targetMinutes) {
                hitDays += 1;
            }
        }
        return new WeeklyGoalProgress(hitDays, scheduledDays);
    }

    private DashboardLockStatus buildLockStatus(boolean accessibilityEnabled, int blockedAppCount) {
        if (!accessibilityEnabled) {
            return new DashboardLockStatus(
                    DashboardLockStatus.State.UNAVAILABLE,
                    "App blocking is unavailable until Accessibility is enabled.",
                    "",
                    0L
            );
        }
        if (lockStateSource.isLocked()) {
            return new DashboardLockStatus(
                    DashboardLockStatus.State.LOCKED,
                    "Blocked apps are locked right now.",
                    lockStateSource.getReason(),
                    lockStateSource.getUnlockAtMillis()
            );
        }
        String summary = blockedAppCount == 0
                ? "App blocking is ready, but no blocked apps are configured."
                : "App blocking is armed and ready for the next missed gym day.";
        return new DashboardLockStatus(
                DashboardLockStatus.State.READY,
                summary,
                "",
                0L
        );
    }

    private List<DashboardRecentSession> buildRecentSessions(GymSessionEntity activeSessionEntity) {
        List<GymSessionEntity> sessions = new ArrayList<>(gymSessionDao.getAllSessions());
        sessions.sort(Comparator
                .comparing((GymSessionEntity session) -> session.sessionDate == null ? "" : session.sessionDate)
                .thenComparing(session -> session.checkInTime == null ? 0L : session.checkInTime)
                .reversed());

        List<DashboardRecentSession> rows = new ArrayList<>();
        if (activeSessionEntity != null && activeSessionEntity.checkInTime != null) {
            rows.add(new DashboardRecentSession(
                    activeSessionEntity.verificationId,
                    activeSessionEntity.sessionDate,
                    activeSessionEntity.checkInTime,
                    activeSessionEntity.checkOutTime,
                    elapsedMinutes(activeSessionEntity.checkInTime),
                    true
            ));
        }

        for (GymSessionEntity session : sessions) {
            if (rows.size() >= 5) {
                break;
            }
            if (session.checkOutTime == null) {
                continue;
            }
            rows.add(new DashboardRecentSession(
                    session.verificationId,
                    session.sessionDate,
                    session.checkInTime,
                    session.checkOutTime,
                    session.getDurationMinutes(),
                    false
            ));
        }
        return rows;
    }

    private List<DashboardChartBar> includeActiveMinutes(
            List<DashboardChartBar> original,
            GymSessionEntity activeSessionEntity
    ) {
        if (activeSessionEntity == null || activeSessionEntity.checkInTime == null || activeSessionEntity.sessionDate == null) {
            return original;
        }
        Calendar now = Calendar.getInstance(Locale.getDefault());
        now.setTimeInMillis(nowMillis);
        String todayDate = formatDate(now);
        if (!todayDate.equals(activeSessionEntity.sessionDate) || original.isEmpty()) {
            return original;
        }

        List<DashboardChartBar> updated = new ArrayList<>(original);
        DashboardChartBar lastBar = updated.get(updated.size() - 1);
        int completedMinutes = lastBar.completedMinutes() + elapsedMinutes(activeSessionEntity.checkInTime);
        updated.set(updated.size() - 1, new DashboardChartBar(
                lastBar.label(),
                completedMinutes,
                lastBar.targetMinutes(),
                lastBar.targetMinutes() > 0 && completedMinutes >= lastBar.targetMinutes()
        ));
        return updated;
    }

    private List<DashboardMonthStatus> includeActiveMonthProgress(
            List<DashboardMonthStatus> original,
            GymSessionEntity activeSessionEntity,
            int todayTargetMinutes
    ) {
        if (activeSessionEntity == null || activeSessionEntity.checkInTime == null || activeSessionEntity.sessionDate == null) {
            return original;
        }
        Calendar now = Calendar.getInstance(Locale.getDefault());
        now.setTimeInMillis(nowMillis);
        String todayDate = formatDate(now);
        if (!todayDate.equals(activeSessionEntity.sessionDate) || original.isEmpty() || todayTargetMinutes <= 0) {
            return original;
        }

        List<DashboardMonthStatus> updated = new ArrayList<>(original);
        DashboardMonthStatus current = updated.get(updated.size() - 1);
        int completedMinutes = current.completedMinutes() + elapsedMinutes(activeSessionEntity.checkInTime);
        DashboardMonthStatus.Status status = completedMinutes >= current.targetMinutes()
                ? DashboardMonthStatus.Status.HIT
                : DashboardMonthStatus.Status.PENDING;
        updated.set(updated.size() - 1, new DashboardMonthStatus(
                current.label(),
                current.targetMinutes(),
                completedMinutes,
                status
        ));
        return updated;
    }

    private int findTargetMinutes(String dayKey) {
        DailyTargetEntity target = dailyTargetDao.getTargetForDay(dayKey);
        return target == null ? 0 : target.targetMinutes;
    }

    private int activeMinutesForDate(GymSessionEntity activeSessionEntity, String dateKey) {
        if (activeSessionEntity == null || activeSessionEntity.checkInTime == null) {
            return 0;
        }
        if (activeSessionEntity.sessionDate == null || !activeSessionEntity.sessionDate.equals(dateKey)) {
            return 0;
        }
        return elapsedMinutes(activeSessionEntity.checkInTime);
    }

    private int elapsedMinutes(long checkInTimeMillis) {
        return (int) Math.max(0L, (nowMillis - checkInTimeMillis) / 60000L);
    }

    private String formatDate(Calendar calendar) {
        return String.format(
                Locale.ENGLISH,
                "%04d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
        );
    }

    private String dayKey(Calendar calendar) {
        switch (calendar.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:
                return "MONDAY";
            case Calendar.TUESDAY:
                return "TUESDAY";
            case Calendar.WEDNESDAY:
                return "WEDNESDAY";
            case Calendar.THURSDAY:
                return "THURSDAY";
            case Calendar.FRIDAY:
                return "FRIDAY";
            case Calendar.SATURDAY:
                return "SATURDAY";
            default:
                return "SUNDAY";
        }
    }
}
