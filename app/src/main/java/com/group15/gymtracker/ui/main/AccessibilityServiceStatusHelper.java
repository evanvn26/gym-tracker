package com.group15.gymtracker.ui.main;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.view.accessibility.AccessibilityManager;

import com.group15.gymtracker.accessibility.AppBlockAccessibilityService;

import java.util.List;

public final class AccessibilityServiceStatusHelper {

    private AccessibilityServiceStatusHelper() {
    }

    public static boolean isAppBlockServiceEnabled(Context context) {
        AccessibilityManager manager = context.getSystemService(AccessibilityManager.class);
        if (manager == null) {
            return false;
        }

        List<AccessibilityServiceInfo> enabledServices =
                manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo serviceInfo : enabledServices) {
            if (serviceInfo.getResolveInfo() == null || serviceInfo.getResolveInfo().serviceInfo == null) {
                continue;
            }
            String packageName = serviceInfo.getResolveInfo().serviceInfo.packageName;
            String className = serviceInfo.getResolveInfo().serviceInfo.name;
            if (context.getPackageName().equals(packageName)
                    && AppBlockAccessibilityService.class.getName().equals(className)) {
                return true;
            }
        }
        return false;
    }
}
