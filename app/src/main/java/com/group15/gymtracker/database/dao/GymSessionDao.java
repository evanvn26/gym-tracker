// GymSessionDao.java
package com.group15.gymtracker.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.group15.gymtracker.database.entities.GymSessionEntity;

import java.util.List;

@Dao
public interface GymSessionDao {

    @Insert
    long insert(GymSessionEntity session);

    @Update
    void update(GymSessionEntity session);

    @Query("SELECT * FROM GymSessions WHERE session_date = :date")
    GymSessionEntity getSessionByDate(String date);

    @Query("SELECT * FROM GymSessions ORDER BY session_date DESC")
    List<GymSessionEntity> getAllSessions();

    @Query("SELECT * FROM GymSessions ORDER BY session_date DESC LIMIT :limit")
    List<GymSessionEntity> getRecentSessions(int limit);

    @Query("SELECT * FROM GymSessions WHERE session_date BETWEEN :startDate AND :endDate AND check_out_time IS NOT NULL ORDER BY session_date ASC, check_in_time ASC")
    List<GymSessionEntity> getCompletedSessionsBetween(String startDate, String endDate);

    @Query("UPDATE GymSessions SET check_out_time = :checkOutTime WHERE verification_id = :id")
    void updateCheckOutTime(int id, long checkOutTime);

    @Query("SELECT * FROM GymSessions WHERE check_out_time IS NULL LIMIT 1")
    GymSessionEntity getActiveSession();

    @Query("DELETE FROM GymSessions WHERE verification_id = :id")
    void deleteSession(int id);

    @Query("SELECT COUNT(*) FROM GymSessions WHERE session_date BETWEEN :startDate AND :endDate")
    int getSessionCountInRange(String startDate, String endDate);
}
