package com.group15.gymtracker.ui.onboarding;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;

import com.group15.gymtracker.MainActivity;
import com.group15.gymtracker.R;
import com.group15.gymtracker.database.AppDatabase;
import com.group15.gymtracker.onboarding.CoordinateValidationError;
import com.group15.gymtracker.onboarding.CuratedBlockedApp;
import com.group15.gymtracker.onboarding.OnboardingDefaults;
import com.group15.gymtracker.onboarding.OnboardingRepository;
import com.group15.gymtracker.onboarding.OnboardingStep;
import com.group15.gymtracker.onboarding.OnboardingUiState;
import com.group15.gymtracker.onboarding.OnboardingUtils;
import com.group15.gymtracker.onboarding.SelectedGymCoordinates;
import com.group15.gymtracker.onboarding.SharedPreferencesOnboardingFlagStore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Drives the app's four-step onboarding flow: collects the user's training targets,
 * blocked apps, and gym coordinates, keeps that temporary state in sync with the UI,
 * and saves the finished setup before sending the user to the home screen.
 */
public class OnboardingActivity extends ComponentActivity {

    public static final String EXTRA_START_STEP = "extra_start_step";

    private static final String STATE_STEP = "state_step";
    private static final String STATE_SELECTED_DAYS = "state_selected_days";
    private static final String STATE_SESSION_HOURS = "state_session_hours";
    private static final String STATE_BLOCKED_PACKAGES = "state_blocked_packages";
    private static final String STATE_LOCATION_CONFIRMED = "state_location_confirmed";
    private static final String STATE_HAS_COORDINATES = "state_has_coordinates";
    private static final String STATE_COORDINATE_LATITUDE = "state_coordinate_latitude";
    private static final String STATE_COORDINATE_LONGITUDE = "state_coordinate_longitude";
    private static final String STATE_COORDINATE_LATITUDE_TEXT = "state_coordinate_latitude_text";
    private static final String STATE_COORDINATE_LONGITUDE_TEXT = "state_coordinate_longitude_text";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final OnboardingUiState uiState = new OnboardingUiState();
    private final Map<String, CheckBox> dayCheckBoxes = new LinkedHashMap<>();
    private final Map<String, View> blockedAppRows = new LinkedHashMap<>();
    private final Map<String, CheckBox> blockedAppCheckBoxes = new LinkedHashMap<>();

    private OnboardingRepository repository;
    private OnboardingStep currentStep = OnboardingStep.TARGETS;

    private ViewFlipper onboardingViewFlipper;
    private android.widget.ProgressBar onboardingProgressBar;
    private TextView onboardingErrorText;
    private Button onboardingBackButton;
    private Button onboardingNextButton;
    private TextView targetsDurationValueText;
    private SeekBar sessionHoursSeekBar;
    private LinearLayout blockedAppsContainer;
    private EditText locationLatitudeInput;
    private EditText locationLongitudeInput;
    private TextView locationStatusText;
    private TextView locationSelectedLabelText;
    private TextView completeTargetsSummaryText;
    private TextView completeBlockedAppsSummaryText;
    private TextView completeLocationSummaryText;

    private boolean bindingViews;
    private boolean bindingCoordinateInputs;
    private boolean saveInFlight;
    private boolean preloadInFlight;
    private String locationErrorMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppDatabase database = AppDatabase.getInstance(getApplicationContext());
        repository = new OnboardingRepository(
                database.dailyTargetDao(),
                database.userInfoDao(),
                new SharedPreferencesOnboardingFlagStore(getApplicationContext())
        );

        setContentView(R.layout.activity_onboarding);
        bindViews();
        bindTargetsViews();
        bindBlockedAppViews();
        bindListeners();
        restoreState(savedInstanceState);
        if (!preloadInFlight) {
            renderAll();
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (preloadInFlight) {
                    return;
                }
                if (currentStep == OnboardingStep.TARGETS) {
                    finish();
                    return;
                }
                showPreviousStep();
            }
        });
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(STATE_STEP, currentStep.name());
        outState.putStringArrayList(STATE_SELECTED_DAYS, new ArrayList<>(uiState.getSelectedDays()));
        outState.putFloat(STATE_SESSION_HOURS, uiState.getSessionHours());
        outState.putStringArrayList(STATE_BLOCKED_PACKAGES, new ArrayList<>(uiState.getBlockedPackages()));
        outState.putBoolean(STATE_LOCATION_CONFIRMED, uiState.isLocationConfirmed());
        outState.putString(STATE_COORDINATE_LATITUDE_TEXT, locationLatitudeInput.getText().toString());
        outState.putString(STATE_COORDINATE_LONGITUDE_TEXT, locationLongitudeInput.getText().toString());

        SelectedGymCoordinates coordinates = uiState.getSelectedGymCoordinates();
        outState.putBoolean(STATE_HAS_COORDINATES, coordinates != null);
        if (coordinates != null) {
            outState.putDouble(STATE_COORDINATE_LATITUDE, coordinates.latitude());
            outState.putDouble(STATE_COORDINATE_LONGITUDE, coordinates.longitude());
        }
    }

    private void bindViews() {
        onboardingViewFlipper = findViewById(R.id.onboardingViewFlipper);
        onboardingProgressBar = findViewById(R.id.onboardingProgressBar);
        onboardingErrorText = findViewById(R.id.onboardingErrorText);
        onboardingBackButton = findViewById(R.id.onboardingBackButton);
        onboardingNextButton = findViewById(R.id.onboardingNextButton);
        targetsDurationValueText = findViewById(R.id.targetsDurationValueText);
        sessionHoursSeekBar = findViewById(R.id.sessionHoursSeekBar);
        blockedAppsContainer = findViewById(R.id.blockedAppsContainer);
        locationLatitudeInput = findViewById(R.id.locationLatitudeInput);
        locationLongitudeInput = findViewById(R.id.locationLongitudeInput);
        locationStatusText = findViewById(R.id.locationStatusText);
        locationSelectedLabelText = findViewById(R.id.locationSelectedLabelText);
        completeTargetsSummaryText = findViewById(R.id.completeTargetsSummaryText);
        completeBlockedAppsSummaryText = findViewById(R.id.completeBlockedAppsSummaryText);
        completeLocationSummaryText = findViewById(R.id.completeLocationSummaryText);
    }

    private void bindTargetsViews() {
        dayCheckBoxes.put("MONDAY", findViewById(R.id.targetDayMondayCheck));
        dayCheckBoxes.put("TUESDAY", findViewById(R.id.targetDayTuesdayCheck));
        dayCheckBoxes.put("WEDNESDAY", findViewById(R.id.targetDayWednesdayCheck));
        dayCheckBoxes.put("THURSDAY", findViewById(R.id.targetDayThursdayCheck));
        dayCheckBoxes.put("FRIDAY", findViewById(R.id.targetDayFridayCheck));
        dayCheckBoxes.put("SATURDAY", findViewById(R.id.targetDaySaturdayCheck));
        dayCheckBoxes.put("SUNDAY", findViewById(R.id.targetDaySundayCheck));

        CompoundButton.OnCheckedChangeListener listener = (buttonView, isChecked) -> {
            if (bindingViews) {
                return;
            }

            String day = (String) buttonView.getTag();
            if (isChecked) {
                uiState.getSelectedDays().add(day);
            } else {
                uiState.getSelectedDays().remove(day);
            }
            clearGlobalError();
            renderTargetsSection();
            renderCompletionSection();
            updateNavigation();
        };

        for (Map.Entry<String, CheckBox> entry : dayCheckBoxes.entrySet()) {
            entry.getValue().setTag(entry.getKey());
            entry.getValue().setOnCheckedChangeListener(listener);
        }

        sessionHoursSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (bindingViews) {
                    return;
                }
                uiState.setSessionHours(progressToHours(progress));
                clearGlobalError();
                renderTargetsSection();
                renderCompletionSection();
                updateNavigation();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void bindBlockedAppViews() {
        LayoutInflater inflater = LayoutInflater.from(this);
        blockedAppsContainer.removeAllViews();
        blockedAppRows.clear();
        blockedAppCheckBoxes.clear();

        for (CuratedBlockedApp app : OnboardingDefaults.CURATED_BLOCKED_APPS) {
            View row = inflater.inflate(R.layout.item_onboarding_blocked_app, blockedAppsContainer, false);
            TextView nameText = row.findViewById(R.id.onboardingBlockedAppNameText);
            CheckBox checkBox = row.findViewById(R.id.onboardingBlockedAppCheck);

            nameText.setText(app.name());

            checkBox.setTag(app.packageName());
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (bindingViews) {
                    return;
                }

                String packageName = (String) buttonView.getTag();
                if (isChecked) {
                    uiState.getBlockedPackages().add(packageName);
                } else {
                    uiState.getBlockedPackages().remove(packageName);
                }
                updateBlockedAppRowState(row, isChecked);
                clearGlobalError();
                renderBlockedAppsSection();
                renderCompletionSection();
                updateNavigation();
            });

            row.setOnClickListener(view -> checkBox.toggle());

            blockedAppRows.put(app.packageName(), row);
            blockedAppCheckBoxes.put(app.packageName(), checkBox);
            blockedAppsContainer.addView(row);
        }
    }

    private void bindListeners() {
        onboardingBackButton.setOnClickListener(view -> showPreviousStep());
        onboardingNextButton.setOnClickListener(view -> handleNextAction());

        TextWatcher coordinateWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (bindingCoordinateInputs) {
                    return;
                }
                handleCoordinateInputsChanged();
            }
        };
        locationLatitudeInput.addTextChangedListener(coordinateWatcher);
        locationLongitudeInput.addTextChangedListener(coordinateWatcher);
    }

    private void restoreState(Bundle savedInstanceState) {
        OnboardingStep requestedStartStep = resolveRequestedStartStep();

        if (savedInstanceState == null) {
            currentStep = requestedStartStep;
            if (new SharedPreferencesOnboardingFlagStore(getApplicationContext()).isComplete()) {
                loadExistingStateAsync(requestedStartStep);
            }
            return;
        }

        String stepName = savedInstanceState.getString(STATE_STEP);
        if (stepName != null) {
            currentStep = OnboardingStep.valueOf(stepName);
        }

        ArrayList<String> selectedDays = savedInstanceState.getStringArrayList(STATE_SELECTED_DAYS);
        if (selectedDays != null) {
            uiState.setSelectedDays(new LinkedHashSet<>(selectedDays));
        }

        uiState.setSessionHours(savedInstanceState.getFloat(STATE_SESSION_HOURS, 1f));

        ArrayList<String> blockedPackages = savedInstanceState.getStringArrayList(STATE_BLOCKED_PACKAGES);
        if (blockedPackages != null) {
            uiState.setBlockedPackages(new LinkedHashSet<>(blockedPackages));
        }

        uiState.setLocationConfirmed(savedInstanceState.getBoolean(STATE_LOCATION_CONFIRMED, false));

        if (savedInstanceState.getBoolean(STATE_HAS_COORDINATES, false)) {
            uiState.setSelectedGymCoordinates(new SelectedGymCoordinates(
                    savedInstanceState.getDouble(STATE_COORDINATE_LATITUDE),
                    savedInstanceState.getDouble(STATE_COORDINATE_LONGITUDE)
            ));
        }

        setCoordinateInputText(locationLatitudeInput, savedInstanceState.getString(STATE_COORDINATE_LATITUDE_TEXT, ""));
        setCoordinateInputText(locationLongitudeInput, savedInstanceState.getString(STATE_COORDINATE_LONGITUDE_TEXT, ""));
    }

    private OnboardingStep resolveRequestedStartStep() {
        String stepName = getIntent().getStringExtra(EXTRA_START_STEP);
        if (stepName == null) {
            return OnboardingStep.TARGETS;
        }
        try {
            return OnboardingStep.valueOf(stepName);
        } catch (IllegalArgumentException exception) {
            return OnboardingStep.TARGETS;
        }
    }

    private void loadExistingStateAsync(OnboardingStep startStep) {
        preloadInFlight = true;
        currentStep = startStep;
        onboardingViewFlipper.setVisibility(View.INVISIBLE);
        showGlobalMessage(getString(R.string.onboarding_loading_existing_state), false);
        renderHeader();
        updateNavigation();

        executor.execute(() -> {
            try {
                OnboardingUiState storedState = repository.loadExistingState();
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    applyExistingOnboardingState(storedState);
                    preloadInFlight = false;
                    clearGlobalError();
                    onboardingViewFlipper.setVisibility(View.VISIBLE);
                    renderAll();
                    showStep(startStep);
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    preloadInFlight = false;
                    onboardingViewFlipper.setVisibility(View.VISIBLE);
                    showGlobalMessage(getString(R.string.onboarding_load_existing_error), true);
                    renderAll();
                    showStep(startStep);
                });
            }
        });
    }

    private void applyExistingOnboardingState(OnboardingUiState storedState) {
        uiState.setSelectedDays(storedState.getSelectedDays());
        uiState.setSessionHours(storedState.getSessionHours());
        uiState.setBlockedPackages(storedState.getBlockedPackages());
        uiState.setSelectedGymCoordinates(storedState.getSelectedGymCoordinates());
        uiState.setLocationConfirmed(storedState.isLocationConfirmed());
        locationErrorMessage = null;

        SelectedGymCoordinates coordinates = storedState.getSelectedGymCoordinates();
        if (coordinates != null) {
            setCoordinateInputText(locationLatitudeInput, String.valueOf(coordinates.latitude()));
            setCoordinateInputText(locationLongitudeInput, String.valueOf(coordinates.longitude()));
        }
    }

    private void handleNextAction() {
        switch (currentStep) {
            case TARGETS:
                showStep(OnboardingStep.BLOCKED_APPS);
                break;
            case BLOCKED_APPS:
                showStep(OnboardingStep.LOCATION);
                break;
            case LOCATION:
                showStep(OnboardingStep.COMPLETE);
                break;
            case COMPLETE:
                persistOnboarding();
                break;
        }
    }

    private void showPreviousStep() {
        switch (currentStep) {
            case BLOCKED_APPS:
                showStep(OnboardingStep.TARGETS);
                break;
            case LOCATION:
                showStep(OnboardingStep.BLOCKED_APPS);
                break;
            case COMPLETE:
                showStep(OnboardingStep.LOCATION);
                break;
            case TARGETS:
                finish();
                break;
        }
    }

    private void showStep(OnboardingStep step) {
        currentStep = step;
        onboardingViewFlipper.setDisplayedChild(step.ordinal());
        renderHeader();
        updateNavigation();
    }

    private void renderAll() {
        renderHeader();
        renderTargetsSection();
        renderBlockedAppsSection();
        renderLocationSection();
        renderCompletionSection();
        onboardingViewFlipper.setDisplayedChild(currentStep.ordinal());
        updateNavigation();
    }

    private void renderHeader() {
        int stepIndex = currentStep.ordinal() + 1;
        onboardingProgressBar.setProgress(stepIndex);
    }

    private void renderTargetsSection() {
        bindingViews = true;
        for (Map.Entry<String, CheckBox> entry : dayCheckBoxes.entrySet()) {
            entry.getValue().setChecked(uiState.getSelectedDays().contains(entry.getKey()));
        }
        sessionHoursSeekBar.setProgress(hoursToProgress(uiState.getSessionHours()));
        bindingViews = false;

        targetsDurationValueText.setText(
                String.format(
                        Locale.getDefault(),
                        "%s (%d minutes)",
                        formatHours(uiState.getSessionHours()),
                        OnboardingUtils.sessionHoursToMinutes(uiState.getSessionHours())
                )
        );
    }

    private void renderBlockedAppsSection() {
        bindingViews = true;
        for (Map.Entry<String, CheckBox> entry : blockedAppCheckBoxes.entrySet()) {
            boolean selected = uiState.getBlockedPackages().contains(entry.getKey());
            entry.getValue().setChecked(selected);
            updateBlockedAppRowState(blockedAppRows.get(entry.getKey()), selected);
        }
        bindingViews = false;
    }

    private void renderLocationSection() {
        SelectedGymCoordinates coordinates = uiState.getSelectedGymCoordinates();
        locationLatitudeInput.setEnabled(!saveInFlight);
        locationLongitudeInput.setEnabled(!saveInFlight);
        locationSelectedLabelText.setVisibility(coordinates != null ? View.VISIBLE : View.GONE);
        if (coordinates != null) {
            locationSelectedLabelText.setText(
                    buildSummaryCardText(
                            getString(R.string.onboarding_location_selected_label),
                            getString(
                                    R.string.onboarding_location_selected_value,
                                    coordinates.latitude(),
                                    coordinates.longitude()
                            )
                    )
            );
        }

        if (locationErrorMessage != null) {
            locationStatusText.setVisibility(View.VISIBLE);
            locationStatusText.setText(locationErrorMessage);
            locationStatusText.setTextColor(getColor(R.color.onboarding_error));
        } else {
            locationStatusText.setVisibility(View.VISIBLE);
            locationStatusText.setText(R.string.onboarding_location_helper);
            locationStatusText.setTextColor(getColor(R.color.onboarding_text_secondary));
        }
    }

    private void renderCompletionSection() {
        int selectedDays = uiState.getSelectedDays().size();
        String targetsSummary = selectedDays + " day" + (selectedDays == 1 ? "" : "s")
                + " per week at " + formatHours(uiState.getSessionHours()) + " each";
        int blockedCount = uiState.getBlockedPackages().size();
        String blockedSummary = blockedCount + " curated app" + (blockedCount == 1 ? "" : "s") + " selected";
        SelectedGymCoordinates coordinates = uiState.getSelectedGymCoordinates();
        String locationSummary = coordinates == null
                ? getString(R.string.onboarding_complete_location_missing)
                : getString(
                        R.string.onboarding_complete_location_value,
                        coordinates.latitude(),
                        coordinates.longitude()
                );

        completeTargetsSummaryText.setText(
                buildSummaryCardText(getString(R.string.onboarding_complete_targets_label), targetsSummary)
        );
        completeBlockedAppsSummaryText.setText(
                buildSummaryCardText(getString(R.string.onboarding_complete_blocked_apps_label), blockedSummary)
        );
        completeLocationSummaryText.setText(
                buildSummaryCardText(getString(R.string.onboarding_complete_location_label), locationSummary)
        );
    }

    private void updateNavigation() {
        onboardingBackButton.setVisibility(currentStep == OnboardingStep.TARGETS ? View.GONE : View.VISIBLE);
        onboardingBackButton.setEnabled(!saveInFlight && !preloadInFlight);
        onboardingNextButton.setEnabled(canMoveForward() && !saveInFlight && !preloadInFlight);
        onboardingNextButton.setText(getNextButtonText());
    }

    private boolean canMoveForward() {
        switch (currentStep) {
            case TARGETS:
                return !uiState.getSelectedDays().isEmpty() && uiState.getSessionHours() > 0f;
            case BLOCKED_APPS:
                return !uiState.getBlockedPackages().isEmpty();
            case LOCATION:
                return OnboardingUtils.isLocationStepComplete(uiState);
            case COMPLETE:
                return true;
            default:
                return false;
        }
    }

    private CharSequence getNextButtonText() {
        if (preloadInFlight) {
            return getString(R.string.onboarding_loading_existing_state);
        }
        if (currentStep == OnboardingStep.COMPLETE) {
            if (saveInFlight) {
                return getString(R.string.onboarding_saving);
            }
            return getString(R.string.onboarding_begin);
        }
        return getString(R.string.onboarding_continue);
    }

    private void handleCoordinateInputsChanged() {
        clearGlobalError();

        String latitudeText = locationLatitudeInput.getText().toString();
        String longitudeText = locationLongitudeInput.getText().toString();
        if (latitudeText.trim().isEmpty() && longitudeText.trim().isEmpty()) {
            uiState.setSelectedGymCoordinates(null);
            locationErrorMessage = null;
            renderLocationSection();
            renderCompletionSection();
            updateNavigation();
            return;
        }

        CoordinateValidationError error = OnboardingUtils.validateGymCoordinates(latitudeText, longitudeText);
        if (error == CoordinateValidationError.NONE) {
            uiState.setSelectedGymCoordinates(OnboardingUtils.parseGymCoordinates(latitudeText, longitudeText));
            locationErrorMessage = null;
        } else {
            uiState.setSelectedGymCoordinates(null);
            locationErrorMessage = getCoordinateValidationMessage(error);
        }

        renderLocationSection();
        renderCompletionSection();
        updateNavigation();
    }

    private void persistOnboarding() {
        saveInFlight = true;
        clearGlobalError();
        updateNavigation();
        renderLocationSection();

        executor.execute(() -> {
            try {
                repository.persist(uiState);
                runOnUiThread(this::openHome);
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    saveInFlight = false;
                    showGlobalError(getString(R.string.onboarding_save_error));
                    updateNavigation();
                    renderLocationSection();
                });
            }
        });
    }

    private void openHome() {
        saveInFlight = false;
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void updateBlockedAppRowState(View row, boolean selected) {
        if (row == null) {
            return;
        }
        row.setBackgroundResource(
                selected
                        ? R.drawable.bg_onboarding_surface_selected
                        : R.drawable.bg_onboarding_surface
        );
    }

    private void showGlobalError(String message) {
        showGlobalMessage(message, true);
    }

    private void showGlobalMessage(String message, boolean isError) {
        onboardingErrorText.setBackgroundResource(
                isError ? R.drawable.bg_onboarding_error : R.drawable.bg_onboarding_surface_alt
        );
        onboardingErrorText.setTextColor(getColor(
                isError ? R.color.onboarding_error : R.color.onboarding_text_secondary
        ));
        onboardingErrorText.setText(message);
        onboardingErrorText.setVisibility(View.VISIBLE);
    }

    private void clearGlobalError() {
        onboardingErrorText.setVisibility(View.GONE);
    }

    private void setCoordinateInputText(EditText editText, String value) {
        bindingCoordinateInputs = true;
        editText.setText(value);
        editText.setSelection(editText.getText().length());
        bindingCoordinateInputs = false;
    }

    private String getCoordinateValidationMessage(CoordinateValidationError error) {
        switch (error) {
            case MISSING_VALUE:
                return getString(R.string.onboarding_location_validation_missing);
            case INVALID_DECIMAL:
                return getString(R.string.onboarding_location_validation_invalid_decimal);
            case LATITUDE_OUT_OF_RANGE:
                return getString(R.string.onboarding_location_validation_latitude_range);
            case LONGITUDE_OUT_OF_RANGE:
                return getString(R.string.onboarding_location_validation_longitude_range);
            case NONE:
            default:
                return null;
        }
    }

    private int hoursToProgress(float hours) {
        return Math.max(0, Math.min(7, Math.round((hours - 0.5f) / 0.5f)));
    }

    private float progressToHours(int progress) {
        return 0.5f + (progress * 0.5f);
    }

    private String formatHours(float hours) {
        if (hours % 1f == 0f) {
            int wholeHours = (int) hours;
            return wholeHours + " hour" + (wholeHours == 1 ? "" : "s");
        }
        return String.format(Locale.getDefault(), "%.1f hours", hours);
    }

    private CharSequence buildSummaryCardText(String label, String value) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(label);
        builder.setSpan(new StyleSpan(Typeface.BOLD), 0, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.append('\n');
        builder.append(value);
        return builder;
    }
}
