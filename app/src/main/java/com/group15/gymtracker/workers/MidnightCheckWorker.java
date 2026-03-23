package com.group15.gymtracker.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.group15.gymtracker.database.AppDatabase;
import com.group15.gymtracker.database.entities.DailyTargetEntity;
import com.group15.gymtracker.domain.AppLocker;
import com.group15.gymtracker.domain.StreakTracker;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class MidnightCheckWorker extends Worker {

    private static final String TAG = "MidnightCheckWorker";
    private static final boolean FORCE_TEST_LOCK = true;

    public MidnightCheckWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Midnight check started");

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -1);

        String yesterdayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                .format(calendar.getTime());

        String yesterdayDay = new SimpleDateFormat("EEEE", Locale.ENGLISH)
                .format(calendar.getTime())
                .toUpperCase(Locale.ENGLISH);

        Log.d(TAG, "Yesterday date = " + yesterdayDate);
        Log.d(TAG, "Yesterday day = " + yesterdayDay);

        boolean isGymDay = false;
        boolean targetMet = false;

        try {
            AppDatabase db = Room.databaseBuilder(
                    getApplicationContext(),
                    AppDatabase.class,
                    "gym_tracker_db"
            ).build();

            DailyTargetEntity target = db.dailyTargetDao().getTargetForDay(yesterdayDay);
            if (target == null) {
                Log.d(TAG, "No target found for " + yesterdayDay + ", treat as non-gym day");
            } else {
                Log.d(TAG, "Target minutes = " + target.targetMinutes);
                isGymDay = target.targetMinutes > 0;
            }
        } catch (Exception e) {
            Log.e(TAG, "Database read failed", e);
            return Result.failure();
        }

        if (FORCE_TEST_LOCK) {
            isGymDay = true;
            targetMet = false;
            Log.d(TAG, "FORCE_TEST_LOCK enabled");
        }

        StreakTracker.State oldState = new StreakTracker.State(3, 5, 0);
        StreakTracker tracker = new StreakTracker();
        StreakTracker.Result result = tracker.evaluate(isGymDay, targetMet, oldState);

        AppLocker locker = new AppLocker(getApplicationContext());
        if (result.shouldUnlock) {
            locker.unlockApps();
            Log.d(TAG, "Apps unlocked. Reason = " + result.reason);
        }

        if (result.shouldLock) {
            Set<String> blocked = new HashSet<>(Arrays.asList(
                    "com.google.android.youtube"
            ));
            locker.lockApps(blocked, result.reason, getNextMidnightMillis());
            Log.d(TAG, "Apps locked. Reason = " + result.reason);
        }

        Log.d(TAG, "New streak = " + result.newState.currentStreak
                + ", longest = " + result.newState.longestStreak
                + ", freeze = " + result.newState.freezeTokens);

        return Result.success();
    }

    private long getNextMidnightMillis() {
        Calendar next = Calendar.getInstance();
        next.add(Calendar.DAY_OF_YEAR, 1);
        next.set(Calendar.HOUR_OF_DAY, 0);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        return next.getTimeInMillis();
    }
}
