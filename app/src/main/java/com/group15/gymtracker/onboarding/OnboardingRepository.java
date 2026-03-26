package com.group15.gymtracker.onboarding;

import com.group15.gymtracker.database.dao.DailyTargetDao;
import com.group15.gymtracker.database.dao.UserInfoDao;
import com.group15.gymtracker.database.entities.DailyTargetEntity;
import com.group15.gymtracker.database.entities.UserInfoEntity;
import com.group15.gymtracker.domain.UserInfoStore;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class OnboardingRepository {

    private final DailyTargetDao dailyTargetDao;
    private final UserInfoDao userInfoDao;
    private final OnboardingFlagStore flagStore;

    public OnboardingRepository(
            DailyTargetDao dailyTargetDao,
            UserInfoDao userInfoDao,
            OnboardingFlagStore flagStore
    ) {
        this.dailyTargetDao = dailyTargetDao;
        this.userInfoDao = userInfoDao;
        this.flagStore = flagStore;
    }

    public void persist(OnboardingUiState uiState) {
        UserInfoEntity userInfo = UserInfoStore.getOrCreate(userInfoDao);

        userInfo.blockedApps = OnboardingUtils.serializeBlockedPackages(uiState.getBlockedPackages());
        userInfo.gymRadiusMeters = UserInfoEntity.DEFAULT_GYM_RADIUS_METERS;
        SelectedGymCoordinates coordinates = uiState.getSelectedGymCoordinates();
        if (coordinates != null) {
            userInfo.gymLatitude = coordinates.latitude();
            userInfo.gymLongitude = coordinates.longitude();
        }
        userInfoDao.update(userInfo);

        Map<String, Integer> targetMinutesByDay = OnboardingUtils.buildDailyTargetMinutes(
                uiState.getSelectedDays(),
                uiState.getSessionHours()
        );
        for (Map.Entry<String, Integer> entry : targetMinutesByDay.entrySet()) {
            String day = entry.getKey();
            Integer targetMinutes = entry.getValue();
            DailyTargetEntity existing = dailyTargetDao.getTargetForDay(day);
            if (existing == null) {
                DailyTargetEntity newTarget = new DailyTargetEntity();
                newTarget.dayOfWeek = day;
                newTarget.targetMinutes = targetMinutes;
                dailyTargetDao.insert(newTarget);
            } else {
                dailyTargetDao.updateTargetMinutes(day, targetMinutes);
            }
        }

        flagStore.markComplete();
    }

    public OnboardingUiState loadExistingState() {
        OnboardingUiState state = new OnboardingUiState();

        UserInfoEntity userInfo = UserInfoStore.getOrCreate(userInfoDao);
        state.setBlockedPackages(OnboardingUtils.deserializeBlockedPackages(userInfo.blockedApps));
        state.setSelectedGymCoordinates(new SelectedGymCoordinates(userInfo.gymLatitude, userInfo.gymLongitude));
        state.setLocationConfirmed(true);

        List<DailyTargetEntity> targets = dailyTargetDao.getAllTargets();
        LinkedHashSet<String> selectedDays = new LinkedHashSet<>();
        float sessionHours = 1f;
        boolean hasPositiveTarget = false;

        for (DailyTargetEntity target : targets) {
            if (target.targetMinutes <= 0) {
                continue;
            }
            hasPositiveTarget = true;
            selectedDays.add(target.dayOfWeek);
            sessionHours = target.targetMinutes / 60f;
        }

        state.setSelectedDays(selectedDays);
        state.setSessionHours(hasPositiveTarget ? sessionHours : 1f);
        return state;
    }
}
