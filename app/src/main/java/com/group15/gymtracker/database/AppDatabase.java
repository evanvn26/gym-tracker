package com.group15.gymtracker.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.group15.gymtracker.database.dao.DailyTargetDao;
import com.group15.gymtracker.database.entities.DailyTargetEntity;

@Database(entities = {DailyTargetEntity.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract DailyTargetDao dailyTargetDao();
}