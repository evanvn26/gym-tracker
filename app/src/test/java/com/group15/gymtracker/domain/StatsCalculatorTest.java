package com.group15.gymtracker.domain;

// Generated with Codex assistance to speed up test coverage work.

import static org.junit.Assert.assertEquals;

import com.group15.gymtracker.database.dao.DailyTargetDao;
import com.group15.gymtracker.database.dao.GymSessionDao;
import com.group15.gymtracker.database.dao.UserInfoDao;
import com.group15.gymtracker.database.entities.DailyTargetEntity;
import com.group15.gymtracker.database.entities.GymSessionEntity;
import com.group15.gymtracker.database.entities.UserInfoEntity;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class StatsCalculatorTest {

    @Test
    public void calculateCurrentMonthSummary_returnsDefaultsWhenNoDataExists() {
        InMemoryGymSessionDao gymSessionDao = new InMemoryGymSessionDao();
        InMemoryDailyTargetDao dailyTargetDao = new InMemoryDailyTargetDao();
        InMemoryUserInfoDao userInfoDao = new InMemoryUserInfoDao();

        StatsSummary summary = new StatsCalculator(
                gymSessionDao,
                dailyTargetDao,
                userInfoDao,
                buildUtcMillis(2026, Calendar.MARCH, 15)
        ).calculateCurrentMonthSummary();

        assertEquals(0, summary.currentStreak());
        assertEquals(0, summary.longestStreak());
        assertEquals(0, summary.hitTargetDaysThisMonth());
        assertEquals(0, summary.targetDaysThisMonth());
        assertEquals(0f, summary.totalGymHours(), 0.0001f);
        assertEquals(0, summary.averageSessionMinutes());
    }

    @Test
    public void calculateCurrentMonthSummary_usesWholeMonthTargetsAndAggregatesSessionsPerDay() {
        InMemoryGymSessionDao gymSessionDao = new InMemoryGymSessionDao();
        InMemoryDailyTargetDao dailyTargetDao = new InMemoryDailyTargetDao();
        InMemoryUserInfoDao userInfoDao = new InMemoryUserInfoDao();

        UserInfoEntity userInfo = new UserInfoEntity();
        userInfo.currentStreak = 4;
        userInfo.longestStreak = 9;
        userInfo.totalGymHours = 12.5f;
        userInfoDao.insert(userInfo);

        DailyTargetEntity mondayTarget = new DailyTargetEntity();
        mondayTarget.dayOfWeek = "MONDAY";
        mondayTarget.targetMinutes = 60;
        dailyTargetDao.insert(mondayTarget);

        gymSessionDao.insert(completedSession(1, "2026-03-02", 30));
        gymSessionDao.insert(completedSession(2, "2026-03-02", 30));
        gymSessionDao.insert(completedSession(3, "2026-03-09", 45));
        gymSessionDao.insert(openSession(4, "2026-03-16", 20));

        StatsSummary summary = new StatsCalculator(
                gymSessionDao,
                dailyTargetDao,
                userInfoDao,
                buildUtcMillis(2026, Calendar.MARCH, 15)
        ).calculateCurrentMonthSummary();

        assertEquals(4, summary.currentStreak());
        assertEquals(9, summary.longestStreak());
        assertEquals(1, summary.hitTargetDaysThisMonth());
        assertEquals(countWeekdaysInMonth(2026, Calendar.MARCH, Calendar.MONDAY), summary.targetDaysThisMonth());
        assertEquals(12.5f, summary.totalGymHours(), 0.0001f);
        assertEquals(35, summary.averageSessionMinutes());
    }

    private static GymSessionEntity completedSession(int id, String date, int durationMinutes) {
        GymSessionEntity session = new GymSessionEntity();
        session.verificationId = id;
        session.sessionDate = date;
        session.checkInTime = 0L;
        session.checkOutTime = durationMinutes * 60L * 1000L;
        return session;
    }

    private static GymSessionEntity openSession(int id, String date, int elapsedMinutes) {
        GymSessionEntity session = new GymSessionEntity();
        session.verificationId = id;
        session.sessionDate = date;
        session.checkInTime = 0L;
        session.checkOutTime = null;
        return session;
    }

    private static long buildUtcMillis(int year, int month, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance(Locale.UK);
        calendar.clear();
        calendar.set(year, month, dayOfMonth, 12, 0, 0);
        return calendar.getTimeInMillis();
    }

    private static int countWeekdaysInMonth(int year, int month, int weekday) {
        Calendar calendar = Calendar.getInstance(Locale.UK);
        calendar.clear();
        calendar.set(year, month, 1, 12, 0, 0);

        int count = 0;
        int lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int day = 1; day <= lastDay; day++) {
            calendar.set(Calendar.DAY_OF_MONTH, day);
            if (calendar.get(Calendar.DAY_OF_WEEK) == weekday) {
                count += 1;
            }
        }
        return count;
    }

    private static class InMemoryGymSessionDao implements GymSessionDao {
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
            List<GymSessionEntity> completedSessions = new ArrayList<>();
            for (GymSessionEntity session : sessions) {
                if (session.checkOutTime == null || session.sessionDate == null) {
                    continue;
                }
                if (session.sessionDate.compareTo(startDate) < 0 || session.sessionDate.compareTo(endDate) > 0) {
                    continue;
                }
                completedSessions.add(session);
            }
            return completedSessions;
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

    private static class InMemoryDailyTargetDao implements DailyTargetDao {
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
            List<DailyTargetEntity> gymDays = new ArrayList<>();
            for (DailyTargetEntity target : targets) {
                if (target.targetMinutes > 0) {
                    gymDays.add(target);
                }
            }
            return gymDays;
        }
    }

    private static class InMemoryUserInfoDao implements UserInfoDao {
        private UserInfoEntity userInfo;

        @Override
        public void insert(UserInfoEntity userInfo) {
            userInfo.id = 1;
            this.userInfo = userInfo;
        }

        @Override
        public void update(UserInfoEntity userInfo) {
            this.userInfo = userInfo;
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
            return userInfo == null ? "[]" : userInfo.blockedApps;
        }

        @Override
        public int getFreezeTokens() {
            return userInfo == null ? 0 : userInfo.freezeTokens;
        }

        @Override
        public int getCurrentStreak() {
            return userInfo == null ? 0 : userInfo.currentStreak;
        }
    }
}
