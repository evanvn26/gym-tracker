package com.group15.gymtracker.onboarding;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class OnboardingDefaults {

    public static final List<String> WEEKDAYS = Collections.unmodifiableList(Arrays.asList(
            "MONDAY",
            "TUESDAY",
            "WEDNESDAY",
            "THURSDAY",
            "FRIDAY",
            "SATURDAY",
            "SUNDAY"
    ));

    public static final List<CuratedBlockedApp> CURATED_BLOCKED_APPS = Collections.unmodifiableList(
            Arrays.asList(
                    new CuratedBlockedApp(
                            "Instagram",
                            "com.instagram.android"
                    ),
                    new CuratedBlockedApp(
                            "TikTok",
                            "com.zhiliaoapp.musically"
                    ),
                    new CuratedBlockedApp(
                            "YouTube",
                            "com.google.android.youtube"
                    ),
                    new CuratedBlockedApp(
                            "X",
                            "com.twitter.android"
                    ),
                    new CuratedBlockedApp(
                            "Reddit",
                            "com.reddit.frontpage"
                    )
            )
    );

    private OnboardingDefaults() {
    }
}
