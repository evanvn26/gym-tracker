// Generated with Codex to create tests quickly for the new features we implemented.
package com.group15.gymtracker.domain;

import static org.junit.Assert.assertEquals;

import com.group15.gymtracker.database.dao.GymSessionDao;
import com.group15.gymtracker.database.dao.UserInfoDao;
import com.group15.gymtracker.database.entities.GymSessionEntity;
import com.group15.gymtracker.database.entities.UserInfoEntity;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class LocationVerifierTest {

    @Test
    public void evaluateCheckIn_returnsAlreadyCheckedInWhenSessionExists() {
        LocationVerifier verifier = new LocationVerifier(new StubGymSessionDao(), new StubUserInfoDao());

        assertEquals(
                CheckInStatus.ALREADY_CHECKED_IN,
                verifier.evaluateCheckIn(new UserInfoEntity(), true, 10f, 51.5, -0.12, 51.5, -0.12)
        );
    }

    @Test
    public void evaluateCheckIn_returnsNotConfiguredWhenUserInfoMissing() {
        LocationVerifier verifier = new LocationVerifier(new StubGymSessionDao(), new StubUserInfoDao());

        assertEquals(
                CheckInStatus.NOT_CONFIGURED,
                verifier.evaluateCheckIn(null, false, 10f, 51.5, -0.12, 51.5, -0.12)
        );
    }

    @Test
    public void evaluateCheckIn_returnsPoorAccuracyWhenAccuracyIsTooLow() {
        LocationVerifier verifier = new LocationVerifier(new StubGymSessionDao(), new StubUserInfoDao());
        UserInfoEntity userInfo = configuredUserInfo();

        assertEquals(
                CheckInStatus.POOR_ACCURACY,
                verifier.evaluateCheckIn(userInfo, false, 75f, 51.5, -0.12, 51.5, -0.12)
        );
    }

    @Test
    public void evaluateCheckIn_returnsOutsideGymWhenLocationIsTooFar() {
        LocationVerifier verifier = new LocationVerifier(new StubGymSessionDao(), new StubUserInfoDao());
        UserInfoEntity userInfo = configuredUserInfo();

        assertEquals(
                CheckInStatus.OUTSIDE_GYM,
                verifier.evaluateCheckIn(userInfo, false, 10f, 51.51, -0.13, 51.5, -0.12)
        );
    }

    @Test
    public void evaluateCheckIn_returnsSuccessWhenLocationIsInsideGymRadius() {
        LocationVerifier verifier = new LocationVerifier(new StubGymSessionDao(), new StubUserInfoDao());
        UserInfoEntity userInfo = configuredUserInfo();

        assertEquals(
                CheckInStatus.SUCCESS,
                verifier.evaluateCheckIn(userInfo, false, 10f, 51.5002, -0.1201, 51.5, -0.12)
        );
    }

    private static UserInfoEntity configuredUserInfo() {
        UserInfoEntity userInfo = new UserInfoEntity();
        userInfo.gymLatitude = 51.5;
        userInfo.gymLongitude = -0.12;
        userInfo.gymRadiusMeters = UserInfoEntity.DEFAULT_GYM_RADIUS_METERS;
        return userInfo;
    }

    private static final class StubGymSessionDao implements GymSessionDao {
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
        }

        @Override
        public GymSessionEntity getActiveSession() {
            return null;
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

    private static final class StubUserInfoDao implements UserInfoDao {
        @Override
        public void insert(UserInfoEntity userInfo) {
        }

        @Override
        public void update(UserInfoEntity userInfo) {
        }

        @Override
        public UserInfoEntity getUserInfo() {
            return null;
        }

        @Override
        public void updateCurrentStreak(int streak) {
        }

        @Override
        public void updateLongestStreak(int streak) {
        }

        @Override
        public void updateFreezeTokens(int tokens) {
        }

        @Override
        public void addGymHours(float hours) {
        }

        @Override
        public void setTotalGymHours(float hours) {
        }

        @Override
        public void updateBlockedApps(String appsJson) {
        }

        @Override
        public void updateGymLocation(double lat, double lng) {
        }

        @Override
        public void updateGymRadius(int radiusMeters) {
        }

        @Override
        public String getBlockedApps() {
            return "[]";
        }

        @Override
        public int getFreezeTokens() {
            return 0;
        }

        @Override
        public int getCurrentStreak() {
            return 0;
        }
    }
}
