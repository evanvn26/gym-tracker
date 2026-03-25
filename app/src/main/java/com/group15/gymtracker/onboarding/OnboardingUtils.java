package com.group15.gymtracker.onboarding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class OnboardingUtils {

    private OnboardingUtils() {
    }

    public static int sessionHoursToMinutes(float sessionHours) {
        return (int) (sessionHours * 60f);
    }

    public static Map<String, Integer> buildDailyTargetMinutes(
            Set<String> selectedDays,
            float sessionHours
    ) {
        int targetMinutes = sessionHoursToMinutes(sessionHours);
        Map<String, Integer> targetMinutesByDay = new LinkedHashMap<>();
        for (String day : OnboardingDefaults.WEEKDAYS) {
            targetMinutesByDay.put(day, selectedDays.contains(day) ? targetMinutes : 0);
        }
        return targetMinutesByDay;
    }

    public static String serializeBlockedPackages(Set<String> blockedPackages) {
        if (blockedPackages.isEmpty()) {
            return "[]";
        }

        ArrayList<String> sortedPackages = new ArrayList<>(blockedPackages);
        Collections.sort(sortedPackages);

        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < sortedPackages.size(); i++) {
            if (i > 0) {
                builder.append(",");
            }
            String packageName = sortedPackages.get(i)
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"");
            builder.append("\"").append(packageName).append("\"");
        }
        builder.append("]");
        return builder.toString();
    }

    public static Set<String> deserializeBlockedPackages(String rawBlockedApps) {
        LinkedHashSet<String> blockedPackages = new LinkedHashSet<>();
        if (rawBlockedApps == null) {
            return blockedPackages;
        }

        String normalized = rawBlockedApps.trim();
        if (normalized.isEmpty()) {
            return blockedPackages;
        }
        if (normalized.length() < 2
                || normalized.charAt(0) != '['
                || normalized.charAt(normalized.length() - 1) != ']') {
            return blockedPackages;
        }

        int index = skipWhitespace(normalized, 1);
        int endIndex = normalized.length() - 1;
        if (index == endIndex) {
            return blockedPackages;
        }

        while (index < endIndex) {
            if (normalized.charAt(index) != '"') {
                return new LinkedHashSet<>();
            }
            index++;

            StringBuilder packageNameBuilder = new StringBuilder();
            boolean closedString = false;
            while (index < endIndex) {
                char current = normalized.charAt(index++);
                if (current == '\\') {
                    if (index >= endIndex) {
                        return new LinkedHashSet<>();
                    }
                    char escaped = normalized.charAt(index++);
                    if (escaped != '\\' && escaped != '"') {
                        return new LinkedHashSet<>();
                    }
                    packageNameBuilder.append(escaped);
                } else if (current == '"') {
                    closedString = true;
                    break;
                } else {
                    packageNameBuilder.append(current);
                }
            }

            if (!closedString) {
                return new LinkedHashSet<>();
            }

            blockedPackages.add(packageNameBuilder.toString());
            index = skipWhitespace(normalized, index);
            if (index == endIndex) {
                return blockedPackages;
            }

            if (normalized.charAt(index) != ',') {
                return new LinkedHashSet<>();
            }

            index = skipWhitespace(normalized, index + 1);
            if (index == endIndex) {
                return new LinkedHashSet<>();
            }
        }

        return blockedPackages;
    }

    public static boolean isLocationStepComplete(OnboardingUiState uiState) {
        return uiState.getSelectedGymCoordinates() != null && uiState.isLocationConfirmed();
    }

    public static CoordinateValidationError validateGymCoordinates(String latitudeText, String longitudeText) {
        String normalizedLatitude = latitudeText == null ? "" : latitudeText.trim();
        String normalizedLongitude = longitudeText == null ? "" : longitudeText.trim();

        if (normalizedLatitude.isEmpty() || normalizedLongitude.isEmpty()) {
            return CoordinateValidationError.MISSING_VALUE;
        }

        Double latitude = tryParseDecimal(normalizedLatitude);
        Double longitude = tryParseDecimal(normalizedLongitude);
        if (latitude == null || longitude == null) {
            return CoordinateValidationError.INVALID_DECIMAL;
        }
        if (latitude < -90d || latitude > 90d) {
            return CoordinateValidationError.LATITUDE_OUT_OF_RANGE;
        }
        if (longitude < -180d || longitude > 180d) {
            return CoordinateValidationError.LONGITUDE_OUT_OF_RANGE;
        }
        return CoordinateValidationError.NONE;
    }

    public static SelectedGymCoordinates parseGymCoordinates(String latitudeText, String longitudeText) {
        if (validateGymCoordinates(latitudeText, longitudeText) != CoordinateValidationError.NONE) {
            return null;
        }

        double latitude = Double.parseDouble(latitudeText.trim());
        double longitude = Double.parseDouble(longitudeText.trim());
        return new SelectedGymCoordinates(latitude, longitude);
    }

    private static Double tryParseDecimal(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static int skipWhitespace(String value, int index) {
        while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
            index++;
        }
        return index;
    }
}
