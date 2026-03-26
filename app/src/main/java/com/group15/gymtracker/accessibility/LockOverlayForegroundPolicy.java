package com.group15.gymtracker.accessibility;

import java.util.Set;

final class LockOverlayForegroundPolicy {

    static final String SYSTEM_UI_PACKAGE = "com.android.systemui";

    private LockOverlayForegroundPolicy() {
    }

    static Decision decide(
            String foregroundPackage,
            String appPackage,
            String launcherPackage,
            boolean lockActive,
            Set<String> blockedPackages,
            String coveredPackage
    ) {
        if (foregroundPackage == null || foregroundPackage.isBlank()) {
            return coveredPackage == null ? Decision.none() : Decision.hide();
        }

        if (foregroundPackage.equals(appPackage)
                || foregroundPackage.equals(SYSTEM_UI_PACKAGE)
                || (launcherPackage != null && foregroundPackage.equals(launcherPackage))) {
            return coveredPackage == null ? Decision.none() : Decision.hide();
        }

        if (!lockActive || !blockedPackages.contains(foregroundPackage)) {
            return coveredPackage == null ? Decision.none() : Decision.hide();
        }

        if (foregroundPackage.equals(coveredPackage)) {
            return Decision.update(foregroundPackage);
        }

        return Decision.show(foregroundPackage);
    }

    enum Action {
        SHOW,
        UPDATE,
        HIDE,
        NONE
    }

    record Decision(Action action, String blockedPackage) {

        static Decision show(String blockedPackage) {
            return new Decision(Action.SHOW, blockedPackage);
        }

        static Decision update(String blockedPackage) {
            return new Decision(Action.UPDATE, blockedPackage);
        }

        static Decision hide() {
            return new Decision(Action.HIDE, null);
        }

        static Decision none() {
            return new Decision(Action.NONE, null);
        }
    }
}
