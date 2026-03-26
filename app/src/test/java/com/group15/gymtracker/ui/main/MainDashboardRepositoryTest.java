package com.group15.gymtracker.ui.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.group15.gymtracker.database.dao.DailyTargetDao;
import com.group15.gymtracker.database.dao.GymSessionDao;
import com.group15.gymtracker.database.dao.UserInfoDao;
import com.group15.gymtracker.database.entities.DailyTargetEntity;
import com.group15.gymtracker.database.entities.GymSessionEntity;
import com.group15.gymtracker.database.entities.UserInfoEntity;
import com.group15.gymtracker.domain.dashboard.DashboardLockStatus;
import com.group15.gymtracker.domain.dashboard.DashboardMonthStatus;
import com.group15.gymtracker.domain.dashboard.DashboardSnapshot;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainDashboardRepositoryTest {

    @Test
    public void load_returnsRestDaySnapshotWhenTodayHasNoTarget() {
        InMemoryGymSessionDao gymSessionDao = new InMemoryGymSessionDao();
        InMemoryDailyTargetDao dailyTargetDao = new InMemoryDailyTargetDao();
        InMemoryUserInfoDao userInfoDao = new InMemoryUserInfoDao();
        userInfoDao.userInfo.blockedApps = "[]";
        insertTarget(dailyTargetDao, "MONDAY", 60);

        DashboardSnapshot snapshot = new MainDashboardRepository(
                gymSessionDao,
                dailyTargetDao,
                userInfoDao,
                new FakeLockStateSource(false, false, "", 0L),
                buildMillis(2026, Calendar.MARCH, 26, 12, 0)
        ).load();

        assertNull(snapshot.activeSession());
        assertEquals(0, snapshot.todayTargetMinutes());
        assertEquals(0, snapshot.todayCompletedMinutes());
        assertEquals(DashboardLockStatus.State.UNAVAILABLE, snapshot.lockStatus().state());
    }

    @Test
    public void load_includesActiveSessionMinutesAndRecentHistory() {
        InMemoryGymSessionDao gymSessionDao = new InMemoryGymSessionDao();
        InMemoryDailyTargetDao dailyTargetDao = new InMemoryDailyTargetDao();
        InMemoryUserInfoDao userInfoDao = new InMemoryUserInfoDao();
        userInfoDao.userInfo.blockedApps = "[\"com.instagram.android\"]";
        userInfoDao.userInfo.currentStreak = 3;
        userInfoDao.userInfo.longestStreak = 5;
        userInfoDao.userInfo.freezeTokens = 2;
        userInfoDao.userInfo.totalGymHours = 18.5f;

        insertTarget(dailyTargetDao, "THURSDAY", 60);
        insertTarget(dailyTargetDao, "MONDAY", 60);
        gymSessionDao.insert(completedSession(1, "2026-03-23", 60, buildMillis(2026, Calendar.MARCH, 23, 7, 0)));
        gymSessionDao.insert(completedSession(2, "2026-03-26", 15, buildMillis(2026, Calendar.MARCH, 26, 7, 0)));
        gymSessionDao.insert(activeSession(3, "2026-03-26", buildMillis(2026, Calendar.MARCH, 26, 11, 15)));

        DashboardSnapshot snapshot = new MainDashboardRepository(
                gymSessionDao,
                dailyTargetDao,
                userInfoDao,
                new FakeLockStateSource(true, false, "", 0L),
                buildMillis(2026, Calendar.MARCH, 26, 12, 0)
        ).load();

        assertNotNull(snapshot.activeSession());
        assertEquals(60, snapshot.todayTargetMinutes());
        assertEquals(60, snapshot.todayCompletedMinutes());
        assertEquals(2, snapshot.weeklyGoalProgress().scheduledDays());
        assertEquals(2, snapshot.weeklyGoalProgress().hitDays());
        assertEquals(2, snapshot.freezeTokens());
        assertEquals(3, snapshot.recentSessions().size());
        assertEquals(true, snapshot.recentSessions().get(0).active());
        assertEquals(60, snapshot.last7DaysSeries().get(snapshot.last7DaysSeries().size() - 1).completedMinutes());
        assertEquals(DashboardMonthStatus.Status.HIT, snapshot.monthlyTargetSeries().get(snapshot.monthlyTargetSeries().size() - 1).status());
        assertEquals(DashboardLockStatus.State.READY, snapshot.lockStatus().state());
    }

    @Test
    public void load_buildsMonthSeriesTruncatesHistoryAndReflectsLockedState() {
        InMemoryGymSessionDao gymSessionDao = new InMemoryGymSessionDao();
        InMemoryDailyTargetDao dailyTargetDao = new InMemoryDailyTargetDao();
        InMemoryUserInfoDao userInfoDao = new InMemoryUserInfoDao();
        userInfoDao.userInfo.blockedApps = "[\"com.instagram.android\",\"com.reddit.frontpage\"]";

        insertTarget(dailyTargetDao, "MONDAY", 60);
        insertTarget(dailyTargetDao, "THURSDAY", 60);

        gymSessionDao.insert(completedSession(1, "2026-03-02", 60, buildMillis(2026, Calendar.MARCH, 2, 7, 0)));
        gymSessionDao.insert(completedSession(2, "2026-03-05", 30, buildMillis(2026, Calendar.MARCH, 5, 7, 0)));
        gymSessionDao.insert(completedSession(3, "2026-03-09", 60, buildMillis(2026, Calendar.MARCH, 9, 7, 0)));
        gymSessionDao.insert(completedSession(4, "2026-03-12", 60, buildMillis(2026, Calendar.MARCH, 12, 7, 0)));
        gymSessionDao.insert(completedSession(5, "2026-03-16", 30, buildMillis(2026, Calendar.MARCH, 16, 7, 0)));
        gymSessionDao.insert(completedSession(6, "2026-03-19", 60, buildMillis(2026, Calendar.MARCH, 19, 7, 0)));
        gymSessionDao.insert(completedSession(7, "2026-03-23", 60, buildMillis(2026, Calendar.MARCH, 23, 7, 0)));
        gymSessionDao.insert(completedSession(8, "2026-03-24", 50, buildMillis(2026, Calendar.MARCH, 24, 7, 0)));
        gymSessionDao.insert(completedSession(9, "2026-03-25", 55, buildMillis(2026, Calendar.MARCH, 25, 7, 0)));

        DashboardSnapshot snapshot = new MainDashboardRepository(
                gymSessionDao,
                dailyTargetDao,
                userInfoDao,
                new FakeLockStateSource(true, true, "Missed gym day", 99L),
                buildMillis(2026, Calendar.MARCH, 26, 12, 0)
        ).load();

        assertEquals(DashboardLockStatus.State.LOCKED, snapshot.lockStatus().state());
        assertEquals("Missed gym day", snapshot.lockStatus().reason());
        assertEquals(99L, snapshot.lockStatus().unlockAtMillis());
        assertEquals(5, snapshot.recentSessions().size());
        assertEquals(8, snapshot.monthlyTargetSeries().size());
        assertEquals(DashboardMonthStatus.Status.HIT, snapshot.monthlyTargetSeries().get(0).status());
        assertEquals(DashboardMonthStatus.Status.MISSED, snapshot.monthlyTargetSeries().get(1).status());
        assertEquals(DashboardMonthStatus.Status.PENDING, snapshot.monthlyTargetSeries().get(snapshot.monthlyTargetSeries().size() - 1).status());
        assertEquals(2, snapshot.blockedAppCount());
    }

    @Test
    public void useFreezeTokenToUnlock_unlocksAppsAndConsumesToken() {
        InMemoryGymSessionDao gymSessionDao = new InMemoryGymSessionDao();
        InMemoryDailyTargetDao dailyTargetDao = new InMemoryDailyTargetDao();
        InMemoryUserInfoDao userInfoDao = new InMemoryUserInfoDao();
        userInfoDao.userInfo.freezeTokens = 2;
        FakeLockStateSource lockStateSource = new FakeLockStateSource(true, true, "Missed gym day", 99L);

        MainDashboardRepository.FreezeTokenUnlockResult result = new MainDashboardRepository(
                gymSessionDao,
                dailyTargetDao,
                userInfoDao,
                lockStateSource,
                buildMillis(2026, Calendar.MARCH, 26, 12, 0)
        ).useFreezeTokenToUnlock();

        assertEquals(MainDashboardRepository.FreezeTokenUnlockResult.SUCCESS, result);
        assertEquals(1, userInfoDao.userInfo.freezeTokens);
        assertTrue(lockStateSource.unlockCalled);
        assertFalse(lockStateSource.locked);
    }

    @Test
    public void useFreezeTokenToUnlock_returnsNoTokensWhenNoneAvailable() {
        InMemoryGymSessionDao gymSessionDao = new InMemoryGymSessionDao();
        InMemoryDailyTargetDao dailyTargetDao = new InMemoryDailyTargetDao();
        InMemoryUserInfoDao userInfoDao = new InMemoryUserInfoDao();
        userInfoDao.userInfo.freezeTokens = 0;
        FakeLockStateSource lockStateSource = new FakeLockStateSource(true, true, "Missed gym day", 99L);

        MainDashboardRepository.FreezeTokenUnlockResult result = new MainDashboardRepository(
                gymSessionDao,
                dailyTargetDao,
                userInfoDao,
                lockStateSource,
                buildMillis(2026, Calendar.MARCH, 26, 12, 0)
        ).useFreezeTokenToUnlock();

        assertEquals(MainDashboardRepository.FreezeTokenUnlockResult.NO_TOKENS, result);
        assertEquals(0, userInfoDao.userInfo.freezeTokens);
        assertFalse(lockStateSource.unlockCalled);
        assertTrue(lockStateSource.locked);
    }

    @Test
    public void useFreezeTokenToUnlock_returnsNotLockedWhenBlockingInactive() {
        InMemoryGymSessionDao gymSessionDao = new InMemoryGymSessionDao();
        InMemoryDailyTargetDao dailyTargetDao = new InMemoryDailyTargetDao();
        InMemoryUserInfoDao userInfoDao = new InMemoryUserInfoDao();
        userInfoDao.userInfo.freezeTokens = 2;
        FakeLockStateSource lockStateSource = new FakeLockStateSource(true, false, "", 0L);

        MainDashboardRepository.FreezeTokenUnlockResult result = new MainDashboardRepository(
                gymSessionDao,
                dailyTargetDao,
                userInfoDao,
                lockStateSource,
                buildMillis(2026, Calendar.MARCH, 26, 12, 0)
        ).useFreezeTokenToUnlock();

        assertEquals(MainDashboardRepository.FreezeTokenUnlockResult.NOT_LOCKED, result);
        assertEquals(2, userInfoDao.userInfo.freezeTokens);
        assertFalse(lockStateSource.unlockCalled);
    }

    private static long buildMillis(int year, int month, int day, int hour, int minute) {
        Calendar calendar = Calendar.getInstance(Locale.UK);
        calendar.clear();
        calendar.set(year, month, day, hour, minute, 0);
        return calendar.getTimeInMillis();
    }

    private static void insertTarget(InMemoryDailyTargetDao dao, String day, int minutes) {
        DailyTargetEntity target = new DailyTargetEntity();
        target.dayOfWeek = day;
        target.targetMinutes = minutes;
        dao.insert(target);
    }

    private static GymSessionEntity completedSession(int id, String date, int minutes, long checkInTime) {
        GymSessionEntity session = new GymSessionEntity();
        session.verificationId = id;
        session.sessionDate = date;
        session.checkInTime = checkInTime;
        session.checkOutTime = checkInTime + (minutes * 60_000L);
        return session;
    }

    private static GymSessionEntity activeSession(int id, String date, long checkInTime) {
        GymSessionEntity session = new GymSessionEntity();
        session.verificationId = id;
        session.sessionDate = date;
        session.checkInTime = checkInTime;
        session.checkOutTime = null;
        return session;
    }

    private static final class FakeLockStateSource implements LockStateSource {
        private final boolean accessibilityEnabled;
        private boolean locked;
        private final String reason;
        private final long unlockAtMillis;
        private boolean unlockCalled;

        private FakeLockStateSource(boolean accessibilityEnabled, boolean locked, String reason, long unlockAtMillis) {
            this.accessibilityEnabled = accessibilityEnabled;
            this.locked = locked;
            this.reason = reason;
            this.unlockAtMillis = unlockAtMillis;
        }

        @Override
        public boolean isAccessibilityEnabled() {
            return accessibilityEnabled;
        }

        @Override
        public boolean isLocked() {
            return locked;
        }

        @Override
        public String getReason() {
            return reason;
        }

        @Override
        public long getUnlockAtMillis() {
            return unlockAtMillis;
        }

        @Override
        public void unlockApps() {
            unlockCalled = true;
            locked = false;
        }
    }

    private static final class InMemoryGymSessionDao implements GymSessionDao {
        private final List<GymSessionEntity> sessions = new ArrayList<>();
        private GymSessionEntity activeSession;

        @Override
        public long insert(GymSessionEntity session) {
            sessions.add(session);
            if (session.checkOutTime == null) {
                activeSession = session;
            }
            return session.verificationId;
        }

        @Override
        public void update(GymSessionEntity session) {
        }

        @Override
        public GymSessionEntity getSessionByDate(String date) {
            for (GymSessionEntity session : sessions) {
                if (date.equals(session.sessionDate)) {
                    return session;
                }
            }
            return null;
        }

        @Override
        public List<GymSessionEntity> getAllSessions() {
            return new ArrayList<>(sessions);
        }

        @Override
        public List<GymSessionEntity> getRecentSessions(int limit) {
            return new ArrayList<>(sessions.subList(0, Math.min(limit, sessions.size())));
        }

        @Override
        public List<GymSessionEntity> getCompletedSessionsBetween(String startDate, String endDate) {
            List<GymSessionEntity> matches = new ArrayList<>();
            for (GymSessionEntity session : sessions) {
                if (session.checkOutTime == null || session.sessionDate == null) {
                    continue;
                }
                if (session.sessionDate.compareTo(startDate) < 0 || session.sessionDate.compareTo(endDate) > 0) {
                    continue;
                }
                matches.add(session);
            }
            return matches;
        }

        @Override
        public void updateCheckOutTime(int id, long checkOutTime) {
            if (activeSession != null && activeSession.verificationId == id) {
                activeSession.checkOutTime = checkOutTime;
            }
        }

        @Override
        public GymSessionEntity getActiveSession() {
            return activeSession;
        }

        @Override
        public void deleteSession(int id) {
        }

        @Override
        public int getSessionCountInRange(String startDate, String endDate) {
            return getCompletedSessionsBetween(startDate, endDate).size();
        }

        @Override
        public int getTotalCompletedMinutesForDate(String date) {
            int total = 0;
            for (GymSessionEntity session : sessions) {
                if (session.checkOutTime == null || !date.equals(session.sessionDate)) {
                    continue;
                }
                total += session.getDurationMinutes();
            }
            return total;
        }
    }

    private static final class InMemoryDailyTargetDao implements DailyTargetDao {
        private final List<DailyTargetEntity> targets = new ArrayList<>();

        @Override
        public void insert(DailyTargetEntity dailyTarget) {
            targets.add(dailyTarget);
        }

        @Override
        public void update(DailyTargetEntity dailyTarget) {
        }

        @Override
        public List<DailyTargetEntity> getAllTargets() {
            return new ArrayList<>(targets);
        }

        @Override
        public DailyTargetEntity getTargetForDay(String day) {
            for (DailyTargetEntity target : targets) {
                if (day.equals(target.dayOfWeek)) {
                    return target;
                }
            }
            return null;
        }

        @Override
        public void updateTargetMinutes(String day, int minutes) {
        }

        @Override
        public int getTargetMinutes(String day) {
            DailyTargetEntity target = getTargetForDay(day);
            return target == null ? 0 : target.targetMinutes;
        }

        @Override
        public List<DailyTargetEntity> getGymDays() {
            return getAllTargets();
        }
    }

    private static final class InMemoryUserInfoDao implements UserInfoDao {
        private final UserInfoEntity userInfo = new UserInfoEntity();

        @Override
        public void insert(UserInfoEntity userInfo) {
        }

        @Override
        public void update(UserInfoEntity userInfo) {
        }

        @Override
        public UserInfoEntity getUserInfo() {
            return userInfo;
        }

        @Override
        public void updateCurrentStreak(int streak) {
            userInfo.currentStreak = streak;
        }

        @Override
        public void updateLongestStreak(int streak) {
            userInfo.longestStreak = streak;
        }

        @Override
        public void updateFreezeTokens(int tokens) {
            userInfo.freezeTokens = tokens;
        }

        @Override
        public void addGymHours(float hours) {
            userInfo.totalGymHours += hours;
        }

        @Override
        public void setTotalGymHours(float hours) {
            userInfo.totalGymHours = hours;
        }

        @Override
        public void updateBlockedApps(String appsJson) {
            userInfo.blockedApps = appsJson;
        }

        @Override
        public void updateGymLocation(double lat, double lng) {
            userInfo.gymLatitude = lat;
            userInfo.gymLongitude = lng;
        }

        @Override
        public void updateGymRadius(int radiusMeters) {
            userInfo.gymRadiusMeters = radiusMeters;
        }

        @Override
        public String getBlockedApps() {
            return userInfo.blockedApps;
        }

        @Override
        public int getFreezeTokens() {
            return userInfo.freezeTokens;
        }

        @Override
        public int getCurrentStreak() {
            return userInfo.currentStreak;
        }
    }
}
