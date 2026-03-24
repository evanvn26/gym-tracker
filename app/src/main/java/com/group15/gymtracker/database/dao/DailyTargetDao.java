// DailyTargetDao.java
package com.group15.gymtracker.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.group15.gymtracker.database.entities.DailyTargetEntity;

import java.util.List;

@Dao
public interface DailyTargetDao {

    @Insert
    void insert(DailyTargetEntity dailyTarget);

    @Update
    void update(DailyTargetEntity dailyTarget);

    @Query("SELECT * FROM DailyTargets")
    List<DailyTargetEntity> getAllTargets();

    @Query("SELECT * FROM DailyTargets WHERE day_of_week = :day")
    DailyTargetEntity getTargetForDay(String day);

    @Query("UPDATE DailyTargets SET target_minutes = :minutes WHERE day_of_week = :day")
    void updateTargetMinutes(String day, int minutes);

    @Query("SELECT target_minutes FROM DailyTargets WHERE day_of_week = :day")
    int getTargetMinutes(String day);

    @Query("SELECT * FROM DailyTargets WHERE target_minutes > 0")
    List<DailyTargetEntity> getGymDays();
}