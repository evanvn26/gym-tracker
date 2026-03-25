package com.group15.gymtracker.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.group15.gymtracker.database.AppDatabase;
import com.group15.gymtracker.database.entities.DailyTargetEntity;
import com.group15.gymtracker.database.dao.UserInfoDao;
import com.group15.gymtracker.domain.AppLocker;
import com.group15.gymtracker.domain.StreakTracker;
import com.group15.gymtracker.onboarding.OnboardingUtils;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Calendar;
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
        Set<String> blockedPackages = Collections.emptySet();

        try {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());

            DailyTargetEntity target = db.dailyTargetDao().getTargetForDay(yesterdayDay);
            if (target == null) {
                Log.d(TAG, "No target found for " + yesterdayDay + ", treat as non-gym day");
            } else {
                Log.d(TAG, "Target minutes = " + target.targetMinutes);
                isGymDay = target.targetMinutes > 0;
            }

            blockedPackages = readBlockedPackages(db.userInfoDao());
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
            if (blockedPackages.isEmpty()) {
                Log.w(TAG, "Skipping app lock because no persisted blocked packages were found");
            } else {
                locker.lockApps(blockedPackages, result.reason, getNextMidnightMillis());
                Log.d(TAG, "Apps locked. Reason = " + result.reason);
            }
        }

        Log.d(TAG, "New streak = " + result.newState.currentStreak
                + ", longest = " + result.newState.longestStreak
                + ", freeze = " + result.newState.freezeTokens);

        return Result.success();
    }

    static Set<String> readBlockedPackages(UserInfoDao userInfoDao) {
        return OnboardingUtils.deserializeBlockedPackages(userInfoDao.getBlockedApps());
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
