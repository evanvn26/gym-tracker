// Generated with Codex to create tests quickly for the new features we implemented.
package com.group15.gymtracker.onboarding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.group15.gymtracker.database.dao.DailyTargetDao;
import com.group15.gymtracker.database.dao.UserInfoDao;
import com.group15.gymtracker.database.entities.DailyTargetEntity;
import com.group15.gymtracker.database.entities.UserInfoEntity;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class OnboardingRepositoryTest {

    @Test
    public void buildDailyTargetMinutes_appliesMinutesToSelectedDaysOnly() {
        Map<String, Integer> targets = OnboardingUtils.buildDailyTargetMinutes(
                Set.of("MONDAY", "WEDNESDAY"),
                1.5f
        );

        assertEquals(7, targets.size());
        assertEquals(Integer.valueOf(90), targets.get("MONDAY"));
        assertEquals(Integer.valueOf(90), targets.get("WEDNESDAY"));
        assertEquals(Integer.valueOf(0), targets.get("FRIDAY"));
    }

    @Test
    public void serializeBlockedPackages_returnsStableJson() {
        String json = OnboardingUtils.serializeBlockedPackages(
                Set.of("com.google.android.youtube", "com.instagram.android")
        );

        assertEquals("[\"com.google.android.youtube\",\"com.instagram.android\"]", json);
    }

    @Test
    public void deserializeBlockedPackages_roundTripsSerializedPackageList() {
        String json = OnboardingUtils.serializeBlockedPackages(
                Set.of("com.google.android.youtube", "com.instagram.android")
        );

        assertEquals(
                Set.of("com.google.android.youtube", "com.instagram.android"),
                OnboardingUtils.deserializeBlockedPackages(json)
        );
    }

    @Test
    public void deserializeBlockedPackages_returnsEmptySetForBlankOrMalformedValues() {
        assertEquals(Set.of(), OnboardingUtils.deserializeBlockedPackages(null));
        assertEquals(Set.of(), OnboardingUtils.deserializeBlockedPackages(""));
        assertEquals(Set.of(), OnboardingUtils.deserializeBlockedPackages("   "));
        assertEquals(Set.of(), OnboardingUtils.deserializeBlockedPackages("[]"));
        assertEquals(Set.of(), OnboardingUtils.deserializeBlockedPackages("["));
        assertEquals(Set.of(), OnboardingUtils.deserializeBlockedPackages("[\"com.instagram.android\",]"));
        assertEquals(Set.of(), OnboardingUtils.deserializeBlockedPackages("[com.instagram.android]"));
    }

    @Test
    public void persist_createsUserInfoTargetsAndMarksOnboardingComplete() {
        FakeDailyTargetDao dailyTargetDao = new FakeDailyTargetDao();
        FakeUserInfoDao userInfoDao = new FakeUserInfoDao();
        InMemoryFlagStore flagStore = new InMemoryFlagStore();
        OnboardingRepository repository = new OnboardingRepository(dailyTargetDao, userInfoDao, flagStore);

        OnboardingUiState uiState = new OnboardingUiState();
        uiState.setSelectedDays(Set.of("MONDAY", "THURSDAY"));
        uiState.setSessionHours(2f);
        uiState.setBlockedPackages(Set.of("com.google.android.youtube"));
        uiState.setSelectedGymCoordinates(new SelectedGymCoordinates(51.5, -0.1));
        uiState.setLocationConfirmed(true);

        repository.persist(uiState);

        assertTrue(flagStore.complete);
        assertEquals("[\"com.google.android.youtube\"]", userInfoDao.storedUserInfo.blockedApps);
        assertEquals(51.5, userInfoDao.storedUserInfo.gymLatitude, 0.0);
        assertEquals(-0.1, userInfoDao.storedUserInfo.gymLongitude, 0.0);
        assertEquals(120, dailyTargetDao.targets.get("MONDAY").targetMinutes);
        assertEquals(120, dailyTargetDao.targets.get("THURSDAY").targetMinutes);
        assertEquals(0, dailyTargetDao.targets.get("SUNDAY").targetMinutes);
    }

    @Test
    public void isLocationStepComplete_requiresSelectedCoordinatesOnly() {
        OnboardingUiState uiState = new OnboardingUiState();

        assertTrue(!OnboardingUtils.isLocationStepComplete(uiState));

        uiState.setSelectedGymCoordinates(new SelectedGymCoordinates(51.5, -0.1));
        assertTrue(OnboardingUtils.isLocationStepComplete(uiState));
    }

    @Test
    public void validateGymCoordinates_rejectsMissingAndInvalidValues() {
        assertEquals(
                CoordinateValidationError.MISSING_VALUE,
                OnboardingUtils.validateGymCoordinates("", "-0.1")
        );
        assertEquals(
                CoordinateValidationError.INVALID_DECIMAL,
                OnboardingUtils.validateGymCoordinates("north", "-0.1")
        );
        assertEquals(
                CoordinateValidationError.LATITUDE_OUT_OF_RANGE,
                OnboardingUtils.validateGymCoordinates("91", "-0.1")
        );
        assertEquals(
                CoordinateValidationError.LONGITUDE_OUT_OF_RANGE,
                OnboardingUtils.validateGymCoordinates("51.5", "-181")
        );
    }

    @Test
    public void parseGymCoordinates_returnsCoordinatesForValidDecimalInputs() {
        SelectedGymCoordinates coordinates = OnboardingUtils.parseGymCoordinates("51.5007", "-0.1246");

        assertEquals(51.5007, coordinates.latitude(), 0.0);
        assertEquals(-0.1246, coordinates.longitude(), 0.0);
    }

    private static final class InMemoryFlagStore implements OnboardingFlagStore {
        private boolean complete;

        @Override
        public boolean isComplete() {
            return complete;
        }

        @Override
        public void markComplete() {
            complete = true;
        }
    }

    private static final class FakeDailyTargetDao implements DailyTargetDao {
        private final Map<String, DailyTargetEntity> targets = new LinkedHashMap<>();

        @Override
        public void insert(DailyTargetEntity dailyTarget) {
            targets.put(dailyTarget.dayOfWeek, dailyTarget);
        }

        @Override
        public void update(DailyTargetEntity dailyTarget) {
            targets.put(dailyTarget.dayOfWeek, dailyTarget);
        }

        @Override
        public java.util.List<DailyTargetEntity> getAllTargets() {
            return java.util.List.copyOf(targets.values());
        }

        @Override
        public DailyTargetEntity getTargetForDay(String day) {
            return targets.get(day);
        }

        @Override
        public void updateTargetMinutes(String day, int minutes) {
            DailyTargetEntity target = targets.get(day);
            if (target == null) {
                target = new DailyTargetEntity();
                target.dayOfWeek = day;
            }
            target.targetMinutes = minutes;
            targets.put(day, target);
        }

        @Override
        public int getTargetMinutes(String day) {
            DailyTargetEntity target = targets.get(day);
            return target == null ? 0 : target.targetMinutes;
        }

        @Override
        public java.util.List<DailyTargetEntity> getGymDays() {
            java.util.ArrayList<DailyTargetEntity> gymDays = new java.util.ArrayList<>();
            for (DailyTargetEntity entity : targets.values()) {
                if (entity.targetMinutes > 0) {
                    gymDays.add(entity);
                }
            }
            return gymDays;
        }
    }

    private static final class FakeUserInfoDao implements UserInfoDao {
        private UserInfoEntity storedUserInfo;

        @Override
        public void insert(UserInfoEntity userInfo) {
            userInfo.id = 1;
            storedUserInfo = userInfo;
        }

        @Override
        public void update(UserInfoEntity userInfo) {
            storedUserInfo = userInfo;
        }

        @Override
        public UserInfoEntity getUserInfo() {
            return storedUserInfo;
        }

        @Override
        public void updateCurrentStreak(int streak) {
            if (storedUserInfo != null) {
                storedUserInfo.currentStreak = streak;
            }
        }

        @Override
        public void updateLongestStreak(int streak) {
            if (storedUserInfo != null) {
                storedUserInfo.longestStreak = streak;
            }
        }

        @Override
        public void updateFreezeTokens(int tokens) {
            if (storedUserInfo != null) {
                storedUserInfo.freezeTokens = tokens;
            }
        }

        @Override
        public void addGymHours(float hours) {
            if (storedUserInfo != null) {
                storedUserInfo.totalGymHours += hours;
            }
        }

        @Override
        public void setTotalGymHours(float hours) {
            if (storedUserInfo != null) {
                storedUserInfo.totalGymHours = hours;
            }
        }

        @Override
        public void updateBlockedApps(String appsJson) {
            if (storedUserInfo != null) {
                storedUserInfo.blockedApps = appsJson;
            }
        }

        @Override
        public void updateGymLocation(double lat, double lng) {
            if (storedUserInfo != null) {
                storedUserInfo.gymLatitude = lat;
                storedUserInfo.gymLongitude = lng;
            }
        }

        @Override
        public void updateGymRadius(int radiusMeters) {
            if (storedUserInfo != null) {
                storedUserInfo.gymRadiusMeters = radiusMeters;
            }
        }

        @Override
        public String getBlockedApps() {
            return storedUserInfo == null ? "[]" : storedUserInfo.blockedApps;
        }

        @Override
        public int getFreezeTokens() {
            return storedUserInfo == null ? 0 : storedUserInfo.freezeTokens;
        }

        @Override
        public int getCurrentStreak() {
            return storedUserInfo == null ? 0 : storedUserInfo.currentStreak;
        }
    }
}
