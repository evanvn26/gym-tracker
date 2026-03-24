// GymSessionEntity.java
package com.group15.gymtracker.database.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "GymSessions")
public class GymSessionEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "verification_id")
    public int verificationId;

    @ColumnInfo(name = "session_date")
    public String sessionDate;

    @ColumnInfo(name = "check_in_time")
    public Long checkInTime;

    @ColumnInfo(name = "check_out_time")
    public Long checkOutTime;

    @Ignore
    public GymSessionEntity(String sessionDate, long checkInTime) {
        this.sessionDate = sessionDate;
        this.checkInTime = checkInTime;
        this.checkOutTime = null;
    }

    public GymSessionEntity() {
    }

    public int getDurationMinutes() {
        if (checkOutTime == null) {
            return 0;
        }
        return (int) ((checkOutTime - checkInTime) / (1000 * 60));
    }
}