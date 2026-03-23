package com.group15.gymtracker.accessibility;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;

import com.group15.gymtracker.R;
import com.group15.gymtracker.domain.AppLocker;

import java.text.DateFormat;
import java.util.Date;

public class LockOverlayActivity extends ComponentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_overlay);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                moveTaskToBack(true);
            }
        });

        TextView titleText = findViewById(R.id.lockTitleText);
        TextView reasonText = findViewById(R.id.lockReasonText);
        TextView packageText = findViewById(R.id.lockedPackageText);
        TextView unlockText = findViewById(R.id.unlockTimeText);

        String blockedPackage = getIntent().getStringExtra("blocked_package");

        AppLocker locker = new AppLocker(this);

        titleText.setText("This app is locked");
        reasonText.setText("Reason: " + locker.getReason());
        packageText.setText("Blocked app: " + (blockedPackage == null ? "" : blockedPackage));

        long unlockAt = locker.getUnlockAtMillis();
        if (unlockAt > 0) {
            String formatted = DateFormat.getDateTimeInstance().format(new Date(unlockAt));
            unlockText.setText("Unlocks at: " + formatted);
        } else {
            unlockText.setText("");
        }
    }
}
