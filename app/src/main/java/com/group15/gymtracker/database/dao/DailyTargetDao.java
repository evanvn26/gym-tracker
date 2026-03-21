package com.group15.gymtracker.database.dao;

import androidx.room.Dao;
import androidx.room.Query;

import com.group15.gymtracker.database.entities.DailyTargetEntity;

@Dao
public interface DailyTargetDao {

    @Query("SELECT * FROM DailyTargets WHERE day = :day LIMIT 1")
    DailyTargetEntity getTargetForDay(String day);
}