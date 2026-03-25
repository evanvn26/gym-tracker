package com.group15.gymtracker.onboarding;

import android.content.Context;
import android.content.SharedPreferences;

public final class SharedPreferencesOnboardingFlagStore implements OnboardingFlagStore {

    private static final String PREFS_NAME = "onboarding_prefs";
    private static final String KEY_ONBOARDING_COMPLETE = "onboarding_complete";

    private final SharedPreferences prefs;

    public SharedPreferencesOnboardingFlagStore(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public boolean isComplete() {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false);
    }

    @Override
    public void markComplete() {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, true).apply();
    }
}
