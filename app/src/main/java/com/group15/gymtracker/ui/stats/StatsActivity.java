package com.group15.gymtracker.ui.stats;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.ComponentActivity;

import com.group15.gymtracker.R;
import com.group15.gymtracker.database.AppDatabase;
import com.group15.gymtracker.domain.StatsCalculator;
import com.group15.gymtracker.domain.StatsSummary;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StatsActivity extends ComponentActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private TextView statusText;
    private View statsContent;
    private TextView currentStreakValue;
    private TextView longestStreakValue;
    private TextView targetDaysValue;
    private TextView totalHoursValue;
    private TextView averageSessionValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        bindViews();
        loadStats();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private void bindViews() {
        statusText = findViewById(R.id.statsStatusText);
        statsContent = findViewById(R.id.statsContent);
        currentStreakValue = findViewById(R.id.currentStreakValueText);
        longestStreakValue = findViewById(R.id.longestStreakValueText);
        targetDaysValue = findViewById(R.id.targetDaysValueText);
        totalHoursValue = findViewById(R.id.totalHoursValueText);
        averageSessionValue = findViewById(R.id.averageSessionValueText);
    }

    private void loadStats() {
        showLoadingState();

        executor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                StatsSummary summary = new StatsCalculator(
                        db.gymSessionDao(),
                        db.dailyTargetDao(),
                        db.userInfoDao()
                ).calculateCurrentMonthSummary();

                runOnUiThread(() -> renderStats(summary));
            } catch (Exception exception) {
                runOnUiThread(this::showErrorState);
            }
        });
    }

    private void showLoadingState() {
        statusText.setVisibility(View.VISIBLE);
        statusText.setText("Loading statistics...");
        statsContent.setVisibility(View.GONE);
    }

    private void showErrorState() {
        statusText.setVisibility(View.VISIBLE);
        statusText.setText("Unable to load statistics.");
        statsContent.setVisibility(View.GONE);
    }

    private void renderStats(StatsSummary summary) {
        statusText.setVisibility(View.GONE);
        statsContent.setVisibility(View.VISIBLE);

        currentStreakValue.setText(String.valueOf(summary.currentStreak()));
        longestStreakValue.setText(String.valueOf(summary.longestStreak()));
        targetDaysValue.setText(String.format(
                Locale.getDefault(),
                "%d / %d",
                summary.hitTargetDaysThisMonth(),
                summary.targetDaysThisMonth()
        ));
        totalHoursValue.setText(String.format(Locale.getDefault(), "%.1f h", summary.totalGymHours()));
        averageSessionValue.setText(String.format(
                Locale.getDefault(),
                "%d min",
                summary.averageSessionMinutes()
        ));
    }
}
