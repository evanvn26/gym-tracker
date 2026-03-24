// Constants.java (bonus - useful for your team)
package com.group15.gymtracker.utils;

public class Constants {

    // Location verification constants
    public static final double DEFAULT_GYM_RADIUS_METERS = 50.0;
    public static final float REQUIRED_GPS_ACCURACY_METERS = 50.0f;

    // Streak tracking constants
    public static final int FREEZE_TOKEN_STREAK_INTERVAL = 5; // Award token every 5 days

    // Date format
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    // Prevent instantiation
    private Constants() {
        throw new AssertionError("Cannot instantiate Constants class");
    }
}