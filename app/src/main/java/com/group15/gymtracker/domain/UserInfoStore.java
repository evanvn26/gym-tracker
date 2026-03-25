package com.group15.gymtracker.domain;

import com.group15.gymtracker.database.dao.UserInfoDao;
import com.group15.gymtracker.database.entities.UserInfoEntity;

public final class UserInfoStore {

    private UserInfoStore() {
    }

    public static UserInfoEntity getOrCreate(UserInfoDao userInfoDao) {
        UserInfoEntity userInfo = userInfoDao.getUserInfo();
        if (userInfo != null) {
            return userInfo;
        }

        userInfoDao.insert(new UserInfoEntity());
        userInfo = userInfoDao.getUserInfo();
        return userInfo == null ? new UserInfoEntity() : userInfo;
    }
}
