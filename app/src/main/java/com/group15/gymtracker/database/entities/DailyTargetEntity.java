package com.group15.gymtracker.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "DailyTargets")
public class DailyTargetEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String day;
    public int targetMinutes;
}