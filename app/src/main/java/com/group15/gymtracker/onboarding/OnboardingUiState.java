package com.group15.gymtracker.onboarding;

import java.util.LinkedHashSet;
import java.util.Set;

public final class OnboardingUiState {

    private final Set<String> selectedDays = new LinkedHashSet<>();
    private float sessionHours = 1f;
    private final Set<String> blockedPackages = new LinkedHashSet<>();
    private SelectedGymCoordinates selectedGymCoordinates;
    private boolean locationConfirmed;

    public Set<String> getSelectedDays() {
        return selectedDays;
    }

    public void setSelectedDays(Set<String> days) {
        selectedDays.clear();
        if (days != null) {
            selectedDays.addAll(days);
        }
    }

    public float getSessionHours() {
        return sessionHours;
    }

    public void setSessionHours(float sessionHours) {
        this.sessionHours = sessionHours;
    }

    public Set<String> getBlockedPackages() {
        return blockedPackages;
    }

    public void setBlockedPackages(Set<String> packages) {
        blockedPackages.clear();
        if (packages != null) {
            blockedPackages.addAll(packages);
        }
    }

    public SelectedGymCoordinates getSelectedGymCoordinates() {
        return selectedGymCoordinates;
    }

    public void setSelectedGymCoordinates(SelectedGymCoordinates selectedGymCoordinates) {
        this.selectedGymCoordinates = selectedGymCoordinates;
    }

    public boolean isLocationConfirmed() {
        return locationConfirmed;
    }

    public void setLocationConfirmed(boolean locationConfirmed) {
        this.locationConfirmed = locationConfirmed;
    }
}
