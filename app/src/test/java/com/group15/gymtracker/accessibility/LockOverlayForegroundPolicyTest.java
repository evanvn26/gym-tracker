package com.group15.gymtracker.accessibility;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Set;

public class LockOverlayForegroundPolicyTest {

    private static final String APP_PACKAGE = "com.group15.gymtracker";
    private static final String LAUNCHER_PACKAGE = "com.android.launcher";
    private static final String BLOCKED_PACKAGE = "com.google.android.youtube";

    @Test
    public void blockedPackageWhileLocked_returnsShow() {
        LockOverlayForegroundPolicy.Decision decision = decide(BLOCKED_PACKAGE, true, null);

        assertEquals(LockOverlayForegroundPolicy.Action.SHOW, decision.action());
        assertEquals(BLOCKED_PACKAGE, decision.blockedPackage());
    }

    @Test
    public void repeatedBlockedPackage_returnsUpdate() {
        LockOverlayForegroundPolicy.Decision decision = decide(BLOCKED_PACKAGE, true, BLOCKED_PACKAGE);

        assertEquals(LockOverlayForegroundPolicy.Action.UPDATE, decision.action());
        assertEquals(BLOCKED_PACKAGE, decision.blockedPackage());
    }

    @Test
    public void transitionToGymTracker_returnsHide() {
        LockOverlayForegroundPolicy.Decision decision = decide(APP_PACKAGE, true, BLOCKED_PACKAGE);

        assertEquals(LockOverlayForegroundPolicy.Action.HIDE, decision.action());
    }

    @Test
    public void transitionToLauncherOrSystemUi_returnsHide() {
        LockOverlayForegroundPolicy.Decision launcherDecision = decide(LAUNCHER_PACKAGE, true, BLOCKED_PACKAGE);
        LockOverlayForegroundPolicy.Decision systemUiDecision = decide(
                LockOverlayForegroundPolicy.SYSTEM_UI_PACKAGE,
                true,
                BLOCKED_PACKAGE
        );

        assertEquals(LockOverlayForegroundPolicy.Action.HIDE, launcherDecision.action());
        assertEquals(LockOverlayForegroundPolicy.Action.HIDE, systemUiDecision.action());
    }

    @Test
    public void unlockedLock_hidesOrDoesNothing() {
        LockOverlayForegroundPolicy.Decision hiddenDecision = decide(BLOCKED_PACKAGE, false, BLOCKED_PACKAGE);
        LockOverlayForegroundPolicy.Decision idleDecision = decide(BLOCKED_PACKAGE, false, null);

        assertEquals(LockOverlayForegroundPolicy.Action.HIDE, hiddenDecision.action());
        assertEquals(LockOverlayForegroundPolicy.Action.NONE, idleDecision.action());
    }

    private LockOverlayForegroundPolicy.Decision decide(
            String foregroundPackage,
            boolean lockActive,
            String coveredPackage
    ) {
        return LockOverlayForegroundPolicy.decide(
                foregroundPackage,
                APP_PACKAGE,
                LAUNCHER_PACKAGE,
                lockActive,
                Set.of(BLOCKED_PACKAGE),
                coveredPackage
        );
    }
}
