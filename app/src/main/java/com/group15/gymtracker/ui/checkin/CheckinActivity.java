package com.group15.gymtracker.ui.checkin;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.group15.gymtracker.R;
import com.group15.gymtracker.database.AppDatabase;
import com.group15.gymtracker.database.entities.GymSessionEntity;
import com.group15.gymtracker.domain.LocationVerifier;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CheckinActivity extends ComponentActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private TextView statusText;
    private Button checkinButton;
    private Button checkoutButton;

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    performCheckin();
                } else {
                    setStatus("Location permission is required to check in.");
                    setButtonsEnabled(true);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkin);

        statusText = findViewById(R.id.checkinStatusText);
        checkinButton = findViewById(R.id.checkinButton);
        checkoutButton = findViewById(R.id.checkoutButton);

        checkinButton.setOnClickListener(v -> onCheckinClicked());
        checkoutButton.setOnClickListener(v -> onCheckoutClicked());

        loadSessionState();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private void loadSessionState() {
        setStatus("Loading...");
        setButtonsEnabled(false);

        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            GymSessionEntity activeSession = db.gymSessionDao().getActiveSession();
            runOnUiThread(() -> renderState(activeSession != null));
        });
    }

    private void renderState(boolean isCheckedIn) {
        if (isCheckedIn) {
            setStatus("You are currently checked in.");
            checkinButton.setVisibility(View.GONE);
            checkoutButton.setVisibility(View.VISIBLE);
        } else {
            setStatus("You are not checked in.");
            checkinButton.setVisibility(View.VISIBLE);
            checkoutButton.setVisibility(View.GONE);
        }
        setButtonsEnabled(true);
    }

    private void onCheckinClicked() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            performCheckin();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void performCheckin() {
        setStatus("Getting your location...");
        setButtonsEnabled(false);

        executor.execute(() -> {
            Location location = getBestLastKnownLocation();
            if (location == null) {
                runOnUiThread(() -> {
                    setStatus("Could not get location. Make sure GPS is enabled and try again.");
                    setButtonsEnabled(true);
                });
                return;
            }

            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            LocationVerifier verifier = new LocationVerifier(db.gymSessionDao(), db.userInfoDao());
            boolean success = verifier.verifyAndCheckIn(location);

            runOnUiThread(() -> {
                if (success) {
                    renderState(true);
                } else {
                    setStatus("Check-in failed. Make sure you are at the gym and GPS accuracy is good.");
                    setButtonsEnabled(true);
                }
            });
        });
    }

    private void onCheckoutClicked() {
        setStatus("Checking out...");
        setButtonsEnabled(false);

        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            LocationVerifier verifier = new LocationVerifier(db.gymSessionDao(), db.userInfoDao());
            boolean success = verifier.checkOut();

            runOnUiThread(() -> {
                if (success) {
                    renderState(false);
                } else {
                    setStatus("No active session found.");
                    renderState(false);
                }
            });
        });
    }

    private Location getBestLastKnownLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager == null) return null;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        Location gps = null;
        Location network = null;
        try {
            gps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            network = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ignored) {
        }

        // Prefer GPS — it satisfies the accuracy requirement in LocationVerifier
        return gps != null ? gps : network;
    }

    private void setStatus(String message) {
        statusText.setText(message);
    }

    private void setButtonsEnabled(boolean enabled) {
        checkinButton.setEnabled(enabled);
        checkoutButton.setEnabled(enabled);
    }
}
