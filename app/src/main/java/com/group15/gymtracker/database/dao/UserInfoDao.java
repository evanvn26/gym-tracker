// UserInfoDao.java
package com.group15.gymtracker.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.group15.gymtracker.database.entities.UserInfoEntity;

@Dao
public interface UserInfoDao {

    @Insert
    void insert(UserInfoEntity userInfo);

    @Update
    void update(UserInfoEntity userInfo);

    @Query("SELECT * FROM UserInfo LIMIT 1")
    UserInfoEntity getUserInfo();

    @Query("UPDATE UserInfo SET current_streak = :streak WHERE id = 1")
    void updateCurrentStreak(int streak);

    @Query("UPDATE UserInfo SET longest_streak = :streak WHERE id = 1")
    void updateLongestStreak(int streak);

    @Query("UPDATE UserInfo SET freeze_tokens = :tokens WHERE id = 1")
    void updateFreezeTokens(int tokens);

    @Query("UPDATE UserInfo SET total_gym_hours = total_gym_hours + :hours WHERE id = 1")
    void addGymHours(float hours);

    @Query("UPDATE UserInfo SET blocked_apps = :appsJson WHERE id = 1")
    void updateBlockedApps(String appsJson);

    @Query("UPDATE UserInfo SET gym_latitude = :lat, gym_longitude = :lng WHERE id = 1")
    void updateGymLocation(double lat, double lng);

    @Query("SELECT blocked_apps FROM UserInfo LIMIT 1")
    String getBlockedApps();

    @Query("SELECT freeze_tokens FROM UserInfo LIMIT 1")
    int getFreezeTokens();

    @Query("SELECT current_streak FROM UserInfo LIMIT 1")
    int getCurrentStreak();
}