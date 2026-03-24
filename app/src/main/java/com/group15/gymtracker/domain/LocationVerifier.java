// LocationVerifier.java
package com.group15.gymtracker.domain;

import android.location.Location;

import com.group15.gymtracker.database.dao.GymSessionDao;
import com.group15.gymtracker.database.dao.UserInfoDao;
import com.group15.gymtracker.database.entities.GymSessionEntity;
import com.group15.gymtracker.database.entities.UserInfoEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LocationVerifier {

    private final GymSessionDao gymSessionDao;
    private final UserInfoDao userInfoDao;

    // Default threshold values
    private static final double DEFAULT_RADIUS_METERS = 50.0;
    private static final float REQUIRED_GPS_ACCURACY_METERS = 50.0f;

    public LocationVerifier(GymSessionDao gymSessionDao, UserInfoDao userInfoDao) {
        this.gymSessionDao = gymSessionDao;
        this.userInfoDao = userInfoDao;
    }

    /**
     * Verifies if the user is at their gym location and creates a check-in session
     * @param currentLocation The user's current GPS location
     * @return true if check-in successful, false otherwise
     */
    public boolean verifyAndCheckIn(Location currentLocation) {
        // Get gym location from database
        UserInfoEntity userInfo = userInfoDao.getUserInfo();
        if (userInfo == null) {
            return false; // No user info configured
        }

        double gymLat = userInfo.gymLatitude;
        double gymLng = userInfo.gymLongitude;
        int gymRadius = userInfo.gymRadiusMeters;

        // Check GPS accuracy
        float accuracy = currentLocation.getAccuracy();
        if (accuracy > REQUIRED_GPS_ACCURACY_METERS) {
            return false; // GPS accuracy too poor
        }

        // Calculate distance to gym using Haversine formula
        double distance = calculateDistance(
                currentLocation.getLatitude(),
                currentLocation.getLongitude(),
                gymLat,
                gymLng
        );

        // Check if within acceptable radius
        if (distance <= gymRadius) {
            // Create check-in session
            String todayDate = getTodayDate();
            long checkInTime = System.currentTimeMillis();

            GymSessionEntity session = new GymSessionEntity(todayDate, checkInTime);
            gymSessionDao.insert(session);

            return true; // Check-in successful
        }

        return false; // Not at gym location
    }

    /**
     * Calculates distance between two GPS coordinates using Haversine formula
     * @param lat1 Latitude of first point
     * @param lon1 Longitude of first point
     * @param lat2 Latitude of second point
     * @param lon2 Longitude of second point
     * @return Distance in meters
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS_METERS = 6371000; // Earth's radius in meters

        // Convert degrees to radians
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLatRad = Math.toRadians(lat2 - lat1);
        double deltaLonRad = Math.toRadians(lon2 - lon1);

        // Haversine formula
        double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(deltaLonRad / 2) * Math.sin(deltaLonRad / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c; // Distance in meters
    }

    /**
     * Gets today's date in YYYY-MM-DD format
     */
    private String getTodayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(new Date());
    }

    /**
     * Checks out from active session
     * @return true if checkout successful, false if no active session
     */
    public boolean checkOut() {
        GymSessionEntity activeSession = gymSessionDao.getActiveSession();
        if (activeSession != null) {
            long checkOutTime = System.currentTimeMillis();
            gymSessionDao.updateCheckOutTime(activeSession.verificationId, checkOutTime);
            return true;
        }
        return false; // No active session to check out from
    }
}