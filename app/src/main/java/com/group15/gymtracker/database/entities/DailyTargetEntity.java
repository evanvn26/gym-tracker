// DailyTargetEntity.java
package com.group15.gymtracker.database.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "DailyTargets")
public class DailyTargetEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "day_of_week")
    public String dayOfWeek;

    @ColumnInfo(name = "target_minutes", defaultValue = "0")
    public int targetMinutes;

    @Ignore
    public DailyTargetEntity(@NonNull String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
        this.targetMinutes = 0;
    }

    public DailyTargetEntity() {
    }
}