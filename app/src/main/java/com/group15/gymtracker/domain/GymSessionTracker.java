package com.group15.gymtracker.domain;

import com.group15.gymtracker.database.dao.GymSessionDao;
import com.group15.gymtracker.database.dao.UserInfoDao;
import com.group15.gymtracker.database.entities.GymSessionEntity;

public class GymSessionTracker {

    private static final float MILLIS_PER_HOUR = 1000f * 60f * 60f;

    private final GymSessionDao gymSessionDao;
    private final UserInfoDao userInfoDao;

    public GymSessionTracker(GymSessionDao gymSessionDao, UserInfoDao userInfoDao) {
        this.gymSessionDao = gymSessionDao;
        this.userInfoDao = userInfoDao;
    }

    public boolean checkOutActiveSession(long checkOutTimeMillis) {
        GymSessionEntity activeSession = gymSessionDao.getActiveSession();
        if (activeSession == null || activeSession.checkInTime == null) {
            return false;
        }

        long durationMillis = Math.max(0L, checkOutTimeMillis - activeSession.checkInTime);
        gymSessionDao.updateCheckOutTime(activeSession.verificationId, checkOutTimeMillis);

        UserInfoStore.getOrCreate(userInfoDao);
        userInfoDao.addGymHours(durationMillis / MILLIS_PER_HOUR);
        return true;
    }
}
