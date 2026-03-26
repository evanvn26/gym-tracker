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
        return verifyAndCheckInDetailed(currentLocation) == CheckInStatus.SUCCESS;
    }

    public CheckInStatus verifyAndCheckInDetailed(Location currentLocation) {
        UserInfoEntity userInfo = UserInfoStore.getOrCreate(userInfoDao);
        CheckInStatus status = evaluateCheckIn(
                userInfo,
                gymSessionDao.getActiveSession() != null,
                currentLocation.getAccuracy(),
                currentLocation.getLatitude(),
                currentLocation.getLongitude(),
                userInfo == null ? 0d : userInfo.gymLatitude,
                userInfo == null ? 0d : userInfo.gymLongitude
        );
        if (status == CheckInStatus.SUCCESS) {
            String todayDate = getTodayDate();
            long checkInTime = System.currentTimeMillis();

            GymSessionEntity session = new GymSessionEntity(todayDate, checkInTime);
            gymSessionDao.insert(session);
        }
        return status;
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
        return new GymSessionTracker(gymSessionDao, userInfoDao)
                .checkOutActiveSession(System.currentTimeMillis());
    }

    CheckInStatus evaluateCheckIn(
            UserInfoEntity userInfo,
            boolean hasActiveSession,
            float accuracy,
            double currentLatitude,
            double currentLongitude,
            double gymLatitude,
            double gymLongitude
    ) {
        if (hasActiveSession) {
            return CheckInStatus.ALREADY_CHECKED_IN;
        }
        if (userInfo == null) {
            return CheckInStatus.NOT_CONFIGURED;
        }
        if (accuracy > REQUIRED_GPS_ACCURACY_METERS) {
            return CheckInStatus.POOR_ACCURACY;
        }

        double distance = calculateDistance(
                currentLatitude,
                currentLongitude,
                gymLatitude,
                gymLongitude
        );
        return distance <= userInfo.gymRadiusMeters
                ? CheckInStatus.SUCCESS
                : CheckInStatus.OUTSIDE_GYM;
    }
}
