// Generated with Codex to create tests quickly for the new features we implemented.
package com.group15.gymtracker.workers;

import static org.junit.Assert.assertEquals;

import com.group15.gymtracker.database.dao.UserInfoDao;
import com.group15.gymtracker.database.entities.UserInfoEntity;

import org.junit.Test;

import java.util.Set;

public class MidnightCheckWorkerTest {

    @Test
    public void readBlockedPackages_usesPersistedSelectionFromUserInfo() {
        FakeUserInfoDao userInfoDao = new FakeUserInfoDao();
        userInfoDao.blockedApps = "[\"com.instagram.android\",\"com.reddit.frontpage\"]";

        assertEquals(
                Set.of("com.instagram.android", "com.reddit.frontpage"),
                MidnightCheckWorker.readBlockedPackages(userInfoDao)
        );
    }

    @Test
    public void readBlockedPackages_returnsEmptySetWhenStoredValueIsMalformed() {
        FakeUserInfoDao userInfoDao = new FakeUserInfoDao();
        userInfoDao.blockedApps = "[com.instagram.android]";

        assertEquals(Set.of(), MidnightCheckWorker.readBlockedPackages(userInfoDao));
    }

    private static final class FakeUserInfoDao implements UserInfoDao {
        private String blockedApps = "[]";

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
            blockedApps = appsJson;
        }

        @Override
        public void updateGymLocation(double lat, double lng) {
        }

        @Override
        public void updateGymRadius(int radiusMeters) {
        }

        @Override
        public String getBlockedApps() {
            return blockedApps;
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
