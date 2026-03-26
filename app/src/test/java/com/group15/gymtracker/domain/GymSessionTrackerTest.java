package com.group15.gymtracker.domain;

// Generated with Codex assistance to speed up test coverage work.

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.group15.gymtracker.database.dao.GymSessionDao;
import com.group15.gymtracker.database.dao.UserInfoDao;
import com.group15.gymtracker.database.entities.GymSessionEntity;
import com.group15.gymtracker.database.entities.UserInfoEntity;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class GymSessionTrackerTest {

    @Test
    public void checkOutActiveSession_updatesCheckoutAndAddsHours() {
        TrackingGymSessionDao gymSessionDao = new TrackingGymSessionDao();
        TrackingUserInfoDao userInfoDao = new TrackingUserInfoDao();

        GymSessionEntity session = new GymSessionEntity();
        session.verificationId = 7;
        session.sessionDate = "2026-03-25";
        session.checkInTime = 1_000L;
        session.checkOutTime = null;
        gymSessionDao.activeSession = session;

        boolean checkedOut = new GymSessionTracker(gymSessionDao, userInfoDao)
                .checkOutActiveSession(9_001_000L);

        assertTrue(checkedOut);
        assertEquals(7, gymSessionDao.updatedSessionId);
        assertEquals(9_001_000L, gymSessionDao.updatedCheckOutTime);
        assertEquals(2.5f, userInfoDao.userInfo.totalGymHours, 0.0001f);
    }

    @Test
    public void checkOutActiveSession_returnsFalseWhenNoActiveSessionExists() {
        TrackingGymSessionDao gymSessionDao = new TrackingGymSessionDao();
        TrackingUserInfoDao userInfoDao = new TrackingUserInfoDao();

        boolean checkedOut = new GymSessionTracker(gymSessionDao, userInfoDao)
                .checkOutActiveSession(9_001_000L);

        assertFalse(checkedOut);
        assertEquals(-1, gymSessionDao.updatedSessionId);
        assertEquals(0f, userInfoDao.userInfo.totalGymHours, 0.0001f);
    }

    private static class TrackingGymSessionDao implements GymSessionDao {
        private GymSessionEntity activeSession;
        private int updatedSessionId = -1;
        private long updatedCheckOutTime = -1L;

        @Override
        public long insert(GymSessionEntity session) {
            return 0;
        }

        @Override
        public void update(GymSessionEntity session) {
        }

        @Override
        public GymSessionEntity getSessionByDate(String date) {
            return null;
        }

        @Override
        public List<GymSessionEntity> getAllSessions() {
            return new ArrayList<>();
        }

        @Override
        public List<GymSessionEntity> getRecentSessions(int limit) {
            return new ArrayList<>();
        }

        @Override
        public List<GymSessionEntity> getCompletedSessionsBetween(String startDate, String endDate) {
            return new ArrayList<>();
        }

        @Override
        public void updateCheckOutTime(int id, long checkOutTime) {
            updatedSessionId = id;
            updatedCheckOutTime = checkOutTime;
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
            return 0;
        }

        @Override
        public int getTotalCompletedMinutesForDate(String date) {
            return 0;
        }
    }

    private static class TrackingUserInfoDao implements UserInfoDao {
        private UserInfoEntity userInfo = new UserInfoEntity();

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
