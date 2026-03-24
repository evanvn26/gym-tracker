// UserInfoEntity.java
package com.group15.gymtracker.database.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "UserInfo")
public class UserInfoEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "total_gym_hours", defaultValue = "0")
    public float totalGymHours;

    @ColumnInfo(name = "current_streak", defaultValue = "0")
    public int currentStreak;

    @ColumnInfo(name = "longest_streak", defaultValue = "0")
    public int longestStreak;

    @ColumnInfo(name = "freeze_tokens", defaultValue = "0")
    public int freezeTokens;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "blocked_apps")
    public String blockedApps;

    @ColumnInfo(name = "gym_latitude")
    public double gymLatitude;

    @ColumnInfo(name = "gym_longitude")
    public double gymLongitude;

    @ColumnInfo(name = "gym_radius_meters", defaultValue = "100")
    public int gymRadiusMeters;

    public UserInfoEntity() {
        this.totalGymHours = 0;
        this.currentStreak = 0;
        this.longestStreak = 0;
        this.freezeTokens = 0;
        this.createdAt = System.currentTimeMillis();
        this.blockedApps = "[]";
        this.gymRadiusMeters = 100;
    }
}