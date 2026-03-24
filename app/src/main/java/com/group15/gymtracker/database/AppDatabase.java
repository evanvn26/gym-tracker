// AppDatabase.java
package com.group15.gymtracker.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.group15.gymtracker.database.dao.DailyTargetDao;
import com.group15.gymtracker.database.dao.GymSessionDao;
import com.group15.gymtracker.database.dao.UserInfoDao;
import com.group15.gymtracker.database.entities.DailyTargetEntity;
import com.group15.gymtracker.database.entities.GymSessionEntity;
import com.group15.gymtracker.database.entities.UserInfoEntity;

@Database(
        entities = {
                UserInfoEntity.class,
                DailyTargetEntity.class,
                GymSessionEntity.class
        },
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    // Singleton instance
    private static volatile AppDatabase INSTANCE;

    // DAO accessors
    public abstract UserInfoDao userInfoDao();
    public abstract DailyTargetDao dailyTargetDao();
    public abstract GymSessionDao gymSessionDao();

    // Singleton pattern to prevent multiple database instances
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "gym_tracker_database"
                            )
                            .fallbackToDestructiveMigration() // For development - wipes data on schema changes
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    // Optional: Method to destroy instance (useful for testing)
    public static void destroyInstance() {
        INSTANCE = null;
    }
}