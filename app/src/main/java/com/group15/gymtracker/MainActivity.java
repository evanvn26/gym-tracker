package com.group15.gymtracker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.core.os.CancellationSignal;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.group15.gymtracker.database.AppDatabase;
import com.group15.gymtracker.domain.CheckInStatus;
import com.group15.gymtracker.domain.LocationVerifier;
import com.group15.gymtracker.domain.dashboard.DashboardActiveSession;
import com.group15.gymtracker.domain.dashboard.DashboardLockStatus;
import com.group15.gymtracker.domain.dashboard.DashboardRecentSession;
import com.group15.gymtracker.domain.dashboard.DashboardSnapshot;
import com.group15.gymtracker.onboarding.OnboardingStep;
import com.group15.gymtracker.onboarding.SharedPreferencesOnboardingFlagStore;
import com.group15.gymtracker.ui.main.AppLockStateSource;
import com.group15.gymtracker.ui.main.MainDashboardRepository;
import com.group15.gymtracker.ui.main.view.MonthlyTargetChartView;
import com.group15.gymtracker.ui.main.view.WeeklyMinutesBarChartView;
import com.group15.gymtracker.ui.onboarding.OnboardingActivity;
import com.group15.gymtracker.workers.MidnightCheckWorker;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Main dashboard logic.
 *
 * Loads and renders the user's current training state, including active session status,
 * goal progress, app blocking state, statistics, and recent session history.
 *
 * Coordinates the main user actions on the home screen:
 * starting a gym session with location verification, checking out of an active session,
 * refreshing dashboard data, opening setup-edit flows, and scheduling the midnight worker
 * that updates streaks and lock state.
 */

public class MainActivity extends ComponentActivity {

    private static final long LOCATION_TIMEOUT_MS = 10_000L;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable elapsedTimerRunnable = new Runnable() {
        @Override
        public void run() {
            DashboardActiveSession activeSession = currentSnapshot == null ? null : currentSnapshot.activeSession();
            if (activeSession != null) {
                sessionElapsedText.setText(getString(
                        R.string.dashboard_elapsed,
                        formatDuration((System.currentTimeMillis() - activeSession.checkInTimeMillis()) / 1000L)
                ));
                handler.postDelayed(this, 1000L);
            }
        }
    };

    private ActivityResultLauncher<String> locationPermissionLauncher;

    private MainDashboardRepository dashboardRepository;
    private AppDatabase database;

    private TextView dashboardLoadingText;
    private TextView heroDateText;
    private TextView heroTargetSummaryText;
    private TextView heroStatusChipText;
    private TextView sessionSummaryText;
    private TextView sessionElapsedText;
    private TextView sessionMessageText;
    private Button sessionPrimaryButton;
    private Button sessionSecondaryButton;
    private TextView todayProgressValueText;
    private ProgressBar todayProgressBar;
    private TextView weeklyProgressText;
    private TextView currentStreakValueText;
    private TextView longestStreakValueText;
    private TextView freezeTokensValueText;
    private TextView blockingStatusChipText;
    private TextView blockingSummaryText;
    private TextView blockingCountText;
    private TextView blockingReasonText;
    private TextView blockingUnlockText;
    private Button useFreezeTokenButton;
    private Button openAccessibilityButton;
    private TextView monthProgressText;
    private TextView totalHoursValueText;
    private TextView averageSessionValueText;
    private WeeklyMinutesBarChartView weeklyBarChartView;
    private MonthlyTargetChartView monthlyTargetChartView;
    private LinearLayout historyContainer;
    private TextView historyEmptyText;
    private Button manageTargetsButton;
    private Button manageBlockedAppsButton;
    private Button manageLocationButton;

    private DashboardSnapshot currentSnapshot;
    private String sessionMessage;
    private boolean sessionMessageIsError;
    private CancellationSignal currentLocationCancellationSignal;
    private Runnable currentLocationTimeoutRunnable;
    private int currentLocationRequestToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!new SharedPreferencesOnboardingFlagStore(this).isComplete()) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }

        scheduleMidnightWorker();
        database = AppDatabase.getInstance(getApplicationContext());
        dashboardRepository = new MainDashboardRepository(
                database.gymSessionDao(),
                database.dailyTargetDao(),
                database.userInfoDao(),
                new AppLockStateSource(getApplicationContext())
        );

        registerPermissionLauncher();
        setContentView(R.layout.activity_main);
        bindViews();
        bindListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboard();
    }

    @Override
    protected void onPause() {
        stopElapsedTimer();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopElapsedTimer();
        cancelCurrentLocationRequest();
        executor.shutdownNow();
        super.onDestroy();
    }

    private void registerPermissionLauncher() {
        locationPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) {
                        performCheckIn();
                    } else {
                        showSessionMessage(getString(R.string.dashboard_permission_required), true);
                        renderSessionCard(currentSnapshot);
                    }
                });
    }

    private void bindViews() {
        dashboardLoadingText = findViewById(R.id.dashboardLoadingText);
        heroDateText = findViewById(R.id.heroDateText);
        heroTargetSummaryText = findViewById(R.id.heroTargetSummaryText);
        heroStatusChipText = findViewById(R.id.heroStatusChipText);
        sessionSummaryText = findViewById(R.id.sessionSummaryText);
        sessionElapsedText = findViewById(R.id.sessionElapsedText);
        sessionMessageText = findViewById(R.id.sessionMessageText);
        sessionPrimaryButton = findViewById(R.id.sessionPrimaryButton);
        sessionSecondaryButton = findViewById(R.id.sessionSecondaryButton);
        todayProgressValueText = findViewById(R.id.todayProgressValueText);
        todayProgressBar = findViewById(R.id.todayProgressBar);
        weeklyProgressText = findViewById(R.id.weeklyProgressText);
        currentStreakValueText = findViewById(R.id.currentStreakValueText);
        longestStreakValueText = findViewById(R.id.longestStreakValueText);
        freezeTokensValueText = findViewById(R.id.freezeTokensValueText);
        blockingStatusChipText = findViewById(R.id.blockingStatusChipText);
        blockingSummaryText = findViewById(R.id.blockingSummaryText);
        blockingCountText = findViewById(R.id.blockingCountText);
        blockingReasonText = findViewById(R.id.blockingReasonText);
        blockingUnlockText = findViewById(R.id.blockingUnlockText);
        useFreezeTokenButton = findViewById(R.id.useFreezeTokenButton);
        openAccessibilityButton = findViewById(R.id.openAccessibilityButton);
        monthProgressText = findViewById(R.id.monthProgressText);
        totalHoursValueText = findViewById(R.id.totalHoursValueText);
        averageSessionValueText = findViewById(R.id.averageSessionValueText);
        weeklyBarChartView = findViewById(R.id.weeklyBarChartView);
        monthlyTargetChartView = findViewById(R.id.monthlyTargetChartView);
        historyContainer = findViewById(R.id.historyContainer);
        historyEmptyText = findViewById(R.id.historyEmptyText);
        manageTargetsButton = findViewById(R.id.manageTargetsButton);
        manageBlockedAppsButton = findViewById(R.id.manageBlockedAppsButton);
        manageLocationButton = findViewById(R.id.manageLocationButton);
    }

    private void bindListeners() {
        sessionPrimaryButton.setOnClickListener(view -> onPrimarySessionAction());
        sessionSecondaryButton.setOnClickListener(view -> onCheckOutClicked());
        useFreezeTokenButton.setOnClickListener(view -> onUseFreezeTokenClicked());
        openAccessibilityButton.setOnClickListener(view ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        );
        manageTargetsButton.setOnClickListener(view -> openOnboardingStep(OnboardingStep.TARGETS));
        manageBlockedAppsButton.setOnClickListener(view -> openOnboardingStep(OnboardingStep.BLOCKED_APPS));
        manageLocationButton.setOnClickListener(view -> openOnboardingStep(OnboardingStep.LOCATION));
    }

    private void loadDashboard() {
        dashboardLoadingText.setVisibility(View.VISIBLE);
        dashboardLoadingText.setText(R.string.dashboard_loading);
        sessionPrimaryButton.setEnabled(false);
        sessionSecondaryButton.setEnabled(false);

        executor.execute(() -> {
            try {
                DashboardSnapshot snapshot = dashboardRepository.load();
                runOnUiThread(() -> renderDashboard(snapshot));
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    dashboardLoadingText.setVisibility(View.VISIBLE);
                    dashboardLoadingText.setText(R.string.dashboard_load_error);
                    sessionPrimaryButton.setEnabled(true);
                    sessionSecondaryButton.setEnabled(true);
                });
            }
        });
    }

    private void renderDashboard(DashboardSnapshot snapshot) {
        currentSnapshot = snapshot;
        dashboardLoadingText.setVisibility(View.GONE);
        renderHero(snapshot);
        renderSessionCard(snapshot);
        renderGoals(snapshot);
        renderBlocking(snapshot);
        renderStats(snapshot);
        renderHistory(snapshot);
    }

    private void renderHero(DashboardSnapshot snapshot) {
        Calendar now = Calendar.getInstance();
        heroDateText.setText(DateFormat.getDateInstance(DateFormat.FULL).format(now.getTime()));

        if (snapshot.todayTargetMinutes() > 0) {
            heroTargetSummaryText.setText(getString(
                    R.string.dashboard_target_summary,
                    snapshot.todayTargetMinutes()
            ));
        } else {
            heroTargetSummaryText.setText(R.string.dashboard_rest_day_summary);
        }

        if (snapshot.activeSession() != null) {
            heroStatusChipText.setText("Checked in");
            applyChipStyle(heroStatusChipText, R.color.dashboard_success_soft, R.color.dashboard_success);
        } else if (snapshot.todayTargetMinutes() == 0) {
            heroStatusChipText.setText("Recovery");
            applyChipStyle(heroStatusChipText, R.color.dashboard_info_soft, R.color.onboarding_accent);
        } else if (snapshot.todayCompletedMinutes() >= snapshot.todayTargetMinutes()) {
            heroStatusChipText.setText("Target hit");
            applyChipStyle(heroStatusChipText, R.color.dashboard_success_soft, R.color.dashboard_success);
        } else {
            heroStatusChipText.setText("Gym day");
            applyChipStyle(heroStatusChipText, R.color.dashboard_warning_soft, R.color.dashboard_warning);
        }
    }

    private void renderSessionCard(DashboardSnapshot snapshot) {
        DashboardActiveSession activeSession = snapshot == null ? null : snapshot.activeSession();
        sessionPrimaryButton.setEnabled(true);
        sessionSecondaryButton.setEnabled(true);
        sessionMessageText.setVisibility(sessionMessage == null ? View.GONE : View.VISIBLE);
        if (sessionMessage != null) {
            sessionMessageText.setText(sessionMessage);
            applyInfoSurface(sessionMessageText, sessionMessageIsError);
        }

        if (activeSession == null) {
            stopElapsedTimer();
            sessionSummaryText.setText(snapshot != null && snapshot.todayTargetMinutes() > 0
                    ? "Start your verified gym session once you are at your saved location."
                    : "You can still log a session today, but there is no scheduled target to hit.");
            sessionElapsedText.setVisibility(View.GONE);
            sessionPrimaryButton.setText(R.string.dashboard_check_in);
            sessionPrimaryButton.setVisibility(View.VISIBLE);
            sessionSecondaryButton.setVisibility(View.GONE);
            return;
        }

        sessionSummaryText.setText(getString(
                R.string.dashboard_checked_in_at,
                DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(activeSession.checkInTimeMillis()))
        ));
        sessionElapsedText.setVisibility(View.VISIBLE);
        sessionPrimaryButton.setText(R.string.dashboard_check_out);
        sessionPrimaryButton.setVisibility(View.GONE);
        sessionSecondaryButton.setVisibility(View.VISIBLE);
        startElapsedTimer();
    }

    private void renderGoals(DashboardSnapshot snapshot) {
        int todayCompleted = snapshot.todayCompletedMinutes();
        int todayTarget = snapshot.todayTargetMinutes();
        if (todayTarget > 0) {
            todayProgressValueText.setText(getString(R.string.dashboard_today_progress, todayCompleted, todayTarget));
            todayProgressBar.setMax(todayTarget);
            todayProgressBar.setProgress(Math.min(todayCompleted, todayTarget));
        } else {
        todayProgressValueText.setText(getString(R.string.dashboard_today_progress_no_target, todayCompleted));
        todayProgressBar.setMax(Math.max(todayCompleted, 1));
        todayProgressBar.setProgress(Math.max(todayCompleted, 0));
        }

        weeklyProgressText.setText(getString(
                R.string.dashboard_week_progress,
                snapshot.weeklyGoalProgress().hitDays(),
                snapshot.weeklyGoalProgress().scheduledDays()
        ));
        currentStreakValueText.setText(String.valueOf(snapshot.statsSummary().currentStreak()));
        longestStreakValueText.setText(String.valueOf(snapshot.statsSummary().longestStreak()));
        freezeTokensValueText.setText(String.valueOf(snapshot.freezeTokens()));
    }

    private void renderBlocking(DashboardSnapshot snapshot) {
        DashboardLockStatus lockStatus = snapshot.lockStatus();
        boolean canUseFreezeToken = lockStatus.state() == DashboardLockStatus.State.LOCKED
                && snapshot.freezeTokens() > 0;
        blockingSummaryText.setText(lockStatus.summary());
        blockingCountText.setText(getString(R.string.dashboard_blocking_count, snapshot.blockedAppCount()));
        blockingReasonText.setVisibility(View.GONE);
        blockingUnlockText.setVisibility(View.GONE);
        useFreezeTokenButton.setEnabled(canUseFreezeToken);

        switch (lockStatus.state()) {
            case LOCKED:
                blockingStatusChipText.setText("Locked now");
                applyChipStyle(blockingStatusChipText, R.color.dashboard_warning_soft, R.color.dashboard_warning);
                if (!lockStatus.reason().isEmpty()) {
                    blockingReasonText.setVisibility(View.VISIBLE);
                    blockingReasonText.setText(getString(R.string.dashboard_lock_reason, lockStatus.reason()));
                }
                if (lockStatus.unlockAtMillis() > 0L) {
                    blockingUnlockText.setVisibility(View.VISIBLE);
                    blockingUnlockText.setText(getString(
                            R.string.dashboard_lock_unlocks,
                            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                    .format(new Date(lockStatus.unlockAtMillis()))
                    ));
                }
                openAccessibilityButton.setVisibility(View.GONE);
                break;
            case READY:
                blockingStatusChipText.setText("Ready");
                applyChipStyle(blockingStatusChipText, R.color.dashboard_success_soft, R.color.dashboard_success);
                openAccessibilityButton.setVisibility(View.GONE);
                break;
            default:
                blockingStatusChipText.setText("Unavailable");
                applyChipStyle(blockingStatusChipText, R.color.dashboard_info_soft, R.color.onboarding_accent);
                openAccessibilityButton.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void renderStats(DashboardSnapshot snapshot) {
        monthProgressText.setText(getString(
                R.string.dashboard_stats_month_progress,
                snapshot.statsSummary().hitTargetDaysThisMonth(),
                snapshot.statsSummary().targetDaysThisMonth()
        ));
        totalHoursValueText.setText(getString(
                R.string.dashboard_total_hours,
                snapshot.statsSummary().totalGymHours()
        ));
        averageSessionValueText.setText(getString(
                R.string.dashboard_average_session,
                snapshot.statsSummary().averageSessionMinutes()
        ));
        weeklyBarChartView.setBars(snapshot.last7DaysSeries());
        monthlyTargetChartView.setStatuses(snapshot.monthlyTargetSeries());
    }

    private void renderHistory(DashboardSnapshot snapshot) {
        historyContainer.removeAllViews();
        historyEmptyText.setVisibility(snapshot.recentSessions().isEmpty() ? View.VISIBLE : View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (DashboardRecentSession session : snapshot.recentSessions()) {
            View row = inflater.inflate(R.layout.item_dashboard_session_history, historyContainer, false);
            TextView dateText = row.findViewById(R.id.historyDateText);
            TextView durationText = row.findViewById(R.id.historyDurationText);
            TextView badgeText = row.findViewById(R.id.historyBadgeText);

            dateText.setText(formatSessionDate(session.sessionDate(), session.checkInTimeMillis()));
            durationText.setText(getString(R.string.dashboard_history_duration, session.durationMinutes()));
            badgeText.setText(session.active()
                    ? getString(R.string.dashboard_history_status_active)
                    : getString(R.string.dashboard_history_status_verified));
            applyChipStyle(
                    badgeText,
                    session.active() ? R.color.dashboard_info_soft : R.color.dashboard_success_soft,
                    session.active() ? R.color.onboarding_accent : R.color.dashboard_success
            );

            historyContainer.addView(row);
        }
    }

    private void onPrimarySessionAction() {
        if (currentSnapshot != null && currentSnapshot.activeSession() != null) {
            onCheckOutClicked();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            performCheckIn();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void performCheckIn() {
        showSessionMessage(getString(R.string.dashboard_session_checking_in), false);
        renderSessionCard(currentSnapshot);
        sessionPrimaryButton.setEnabled(false);
        sessionSecondaryButton.setEnabled(false);
        requestFreshLocation();
    }

    private void requestFreshLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager == null) {
            renderLocationFailure(getString(R.string.dashboard_location_unavailable));
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            showSessionMessage(getString(R.string.dashboard_permission_required), true);
            renderSessionCard(currentSnapshot);
            return;
        }

        cancelCurrentLocationRequest();
        int requestToken = ++currentLocationRequestToken;
        requestCurrentLocation(locationManager, LocationManager.GPS_PROVIDER, requestToken, true);
    }

    private void requestCurrentLocation(
            LocationManager locationManager,
            String provider,
            int requestToken,
            boolean allowNetworkFallback
    ) {
        if (!isProviderUsable(locationManager, provider)) {
            if (allowNetworkFallback && LocationManager.GPS_PROVIDER.equals(provider)) {
                requestCurrentLocation(locationManager, LocationManager.NETWORK_PROVIDER, requestToken, false);
                return;
            }
            renderLocationFailure(getString(R.string.dashboard_location_unavailable));
            return;
        }

        CancellationSignal cancellationSignal = new CancellationSignal();
        currentLocationCancellationSignal = cancellationSignal;

        Runnable timeoutRunnable = () -> {
            if (!isActiveLocationRequest(requestToken, cancellationSignal)) {
                return;
            }
            cancellationSignal.cancel();
            clearCurrentLocationRequest(cancellationSignal, currentLocationTimeoutRunnable);
            renderLocationFailure(getString(R.string.dashboard_location_unavailable));
        };
        currentLocationTimeoutRunnable = timeoutRunnable;
        handler.postDelayed(timeoutRunnable, LOCATION_TIMEOUT_MS);

        LocationManagerCompat.getCurrentLocation(
                locationManager,
                provider,
                cancellationSignal,
                executor,
                location -> {
                    if (!isActiveLocationRequest(requestToken, cancellationSignal)) {
                        return;
                    }

                    clearCurrentLocationRequest(cancellationSignal, timeoutRunnable);

                    if (location != null) {
                        handleResolvedLocation(location);
                        return;
                    }

                    if (allowNetworkFallback && LocationManager.GPS_PROVIDER.equals(provider)) {
                        requestCurrentLocation(locationManager, LocationManager.NETWORK_PROVIDER, requestToken, false);
                        return;
                    }

                    renderLocationFailure(getString(R.string.dashboard_location_unavailable));
                }
        );
    }

    private boolean isProviderUsable(LocationManager locationManager, String provider) {
        if (!LocationManagerCompat.hasProvider(locationManager, provider)) {
            return false;
        }

        try {
            return locationManager.isProviderEnabled(provider);
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isActiveLocationRequest(int requestToken, CancellationSignal cancellationSignal) {
        return requestToken == currentLocationRequestToken
                && cancellationSignal == currentLocationCancellationSignal
                && !cancellationSignal.isCanceled();
    }

    private void clearCurrentLocationRequest(
            CancellationSignal cancellationSignal,
            Runnable timeoutRunnable
    ) {
        if (currentLocationCancellationSignal == cancellationSignal) {
            currentLocationCancellationSignal = null;
        }
        if (currentLocationTimeoutRunnable == timeoutRunnable) {
            handler.removeCallbacks(timeoutRunnable);
            currentLocationTimeoutRunnable = null;
        } else {
            handler.removeCallbacks(timeoutRunnable);
        }
    }

    private void cancelCurrentLocationRequest() {
        if (currentLocationTimeoutRunnable != null) {
            handler.removeCallbacks(currentLocationTimeoutRunnable);
            currentLocationTimeoutRunnable = null;
        }
        if (currentLocationCancellationSignal != null) {
            currentLocationCancellationSignal.cancel();
            currentLocationCancellationSignal = null;
        }
    }

    private void handleResolvedLocation(Location location) {
        CheckInStatus status = new LocationVerifier(
                database.gymSessionDao(),
                database.userInfoDao()
        ).verifyAndCheckInDetailed(location);

        runOnUiThread(() -> handleCheckInStatus(status));
    }

    private void renderLocationFailure(String message) {
        runOnUiThread(() -> {
            showSessionMessage(message, true);
            renderSessionCard(currentSnapshot);
        });
    }

    private void handleCheckInStatus(CheckInStatus status) {
        switch (status) {
            case SUCCESS:
                showSessionMessage(getString(R.string.dashboard_checked_in_success), false);
                loadDashboard();
                break;
            case ALREADY_CHECKED_IN:
                showSessionMessage(getString(R.string.dashboard_already_checked_in), true);
                renderSessionCard(currentSnapshot);
                break;
            case POOR_ACCURACY:
                showSessionMessage(getString(R.string.dashboard_accuracy_low), true);
                renderSessionCard(currentSnapshot);
                break;
            case OUTSIDE_GYM:
                showSessionMessage(getString(R.string.dashboard_outside_gym), true);
                renderSessionCard(currentSnapshot);
                break;
            default:
                showSessionMessage(getString(R.string.dashboard_gym_not_configured), true);
                renderSessionCard(currentSnapshot);
                break;
        }
    }

    private void onCheckOutClicked() {
        showSessionMessage(getString(R.string.dashboard_session_checking_out), false);
        renderSessionCard(currentSnapshot);
        sessionPrimaryButton.setEnabled(false);
        sessionSecondaryButton.setEnabled(false);

        executor.execute(() -> {
            boolean success = new LocationVerifier(
                    database.gymSessionDao(),
                    database.userInfoDao()
            ).checkOut();
            runOnUiThread(() -> {
                if (success) {
                    showSessionMessage(getString(R.string.dashboard_checked_out_success), false);
                    loadDashboard();
                } else {
                    showSessionMessage(getString(R.string.dashboard_no_active_session), true);
                    renderSessionCard(currentSnapshot);
                }
            });
        });
    }

    private void onUseFreezeTokenClicked() {
        useFreezeTokenButton.setEnabled(false);

        executor.execute(() -> {
            MainDashboardRepository.FreezeTokenUnlockResult result =
                    dashboardRepository.useFreezeTokenToUnlock();
            runOnUiThread(() -> handleFreezeTokenUnlockResult(result));
        });
    }

    private void handleFreezeTokenUnlockResult(MainDashboardRepository.FreezeTokenUnlockResult result) {
        int messageResId;
        switch (result) {
            case SUCCESS:
                messageResId = R.string.dashboard_freeze_token_unlock_success;
                break;
            case NO_TOKENS:
                messageResId = R.string.dashboard_freeze_token_unlock_no_tokens;
                break;
            default:
                messageResId = R.string.dashboard_freeze_token_unlock_not_locked;
                break;
        }

        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
        loadDashboard();
    }

    private void openOnboardingStep(OnboardingStep step) {
        Intent intent = new Intent(this, OnboardingActivity.class);
        intent.putExtra(OnboardingActivity.EXTRA_START_STEP, step.name());
        startActivity(intent);
    }

    private void showSessionMessage(String message, boolean isError) {
        sessionMessage = message;
        sessionMessageIsError = isError;
    }

    private void startElapsedTimer() {
        stopElapsedTimer();
        elapsedTimerRunnable.run();
    }

    private void stopElapsedTimer() {
        handler.removeCallbacks(elapsedTimerRunnable);
    }

    private void applyChipStyle(TextView textView, int backgroundColorRes, int textColorRes) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(getColor(backgroundColorRes));
        drawable.setCornerRadius(dp(999));
        textView.setBackground(drawable);
        textView.setTextColor(getColor(textColorRes));
    }

    private void applyInfoSurface(TextView textView, boolean isError) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(getColor(isError ? R.color.onboarding_error_soft : R.color.dashboard_info_soft));
        drawable.setCornerRadius(dp(20));
        textView.setBackground(drawable);
        textView.setTextColor(getColor(isError ? R.color.onboarding_error : R.color.onboarding_text_primary));
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private String formatSessionDate(String sessionDate, Long checkInTimeMillis) {
        if (checkInTimeMillis != null && sessionDate != null) {
            return DateFormat.getDateInstance(DateFormat.MEDIUM)
                    .format(new Date(checkInTimeMillis));
        }
        return sessionDate == null ? "" : sessionDate;
    }

    private String formatDuration(long totalSeconds) {
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        return hours > 0
                ? String.format(Locale.getDefault(), "%dh %02dm %02ds", hours, minutes, seconds)
                : String.format(Locale.getDefault(), "%dm %02ds", minutes, seconds);
    }

    private void scheduleMidnightWorker() {
        Calendar nextMidnight = Calendar.getInstance();
        nextMidnight.add(Calendar.DAY_OF_YEAR, 1);
        nextMidnight.set(Calendar.HOUR_OF_DAY, 0);
        nextMidnight.set(Calendar.MINUTE, 0);
        nextMidnight.set(Calendar.SECOND, 0);
        nextMidnight.set(Calendar.MILLISECOND, 0);

        long delayMs = nextMidnight.getTimeInMillis() - System.currentTimeMillis();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                MidnightCheckWorker.class,
                1,
                TimeUnit.DAYS
        ).setInitialDelay(delayMs, TimeUnit.MILLISECONDS).build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "midnight_check",
                ExistingPeriodicWorkPolicy.KEEP,
                request
        );
    }
}
