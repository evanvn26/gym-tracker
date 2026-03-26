package com.group15.gymtracker.accessibility;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.group15.gymtracker.R;
import com.group15.gymtracker.domain.AppLocker;

import java.text.DateFormat;
import java.util.Date;

final class LockOverlayController {

    private final Context context;
    private final WindowManager windowManager;
    private final LayoutInflater inflater;
    private final PackageManager packageManager;

    private View overlayView;
    private TextView titleText;
    private TextView reasonText;
    private TextView packageText;
    private TextView unlockText;
    private String coveredPackage;

    LockOverlayController(Context context) {
        this.context = context;
        this.windowManager = context.getSystemService(WindowManager.class);
        this.inflater = LayoutInflater.from(context);
        this.packageManager = context.getPackageManager();
    }

    String getCoveredPackage() {
        return isAttached() ? coveredPackage : null;
    }

    void show(AppLocker locker, String blockedPackage) {
        ensureView();
        bind(locker, blockedPackage);
        if (!isAttached()) {
            windowManager.addView(overlayView, createLayoutParams());
        }
        coveredPackage = blockedPackage;
    }

    void update(AppLocker locker, String blockedPackage) {
        if (!isAttached()) {
            show(locker, blockedPackage);
            return;
        }

        bind(locker, blockedPackage);
        coveredPackage = blockedPackage;
    }

    void hide() {
        if (isAttached()) {
            windowManager.removeViewImmediate(overlayView);
        }
        coveredPackage = null;
    }

    void destroy() {
        hide();
        overlayView = null;
        titleText = null;
        reasonText = null;
        packageText = null;
        unlockText = null;
    }

    private void ensureView() {
        if (overlayView != null) {
            return;
        }

        overlayView = inflater.inflate(R.layout.view_lock_overlay, null, false);
        titleText = overlayView.findViewById(R.id.lockTitleText);
        reasonText = overlayView.findViewById(R.id.lockReasonText);
        packageText = overlayView.findViewById(R.id.lockedPackageText);
        unlockText = overlayView.findViewById(R.id.unlockTimeText);
    }

    private void bind(AppLocker locker, String blockedPackage) {
        titleText.setText(R.string.lock_overlay_title);
        reasonText.setText(context.getString(R.string.lock_overlay_reason, locker.getReason()));
        packageText.setText(context.getString(
                R.string.lock_overlay_blocked_app,
                resolveAppLabel(blockedPackage)
        ));

        long unlockAt = locker.getUnlockAtMillis();
        if (unlockAt > 0) {
            String formatted = DateFormat.getDateTimeInstance().format(new Date(unlockAt));
            unlockText.setVisibility(View.VISIBLE);
            unlockText.setText(context.getString(R.string.lock_overlay_unlocks, formatted));
        } else {
            unlockText.setVisibility(View.GONE);
            unlockText.setText("");
        }
    }

    private String resolveAppLabel(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return "";
        }

        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            CharSequence label = packageManager.getApplicationLabel(applicationInfo);
            if (label != null && !label.toString().isBlank()) {
                return label.toString();
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        return packageName;
    }

    private boolean isAttached() {
        return overlayView != null && overlayView.getParent() != null;
    }

    private WindowManager.LayoutParams createLayoutParams() {
        return new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
    }
}
