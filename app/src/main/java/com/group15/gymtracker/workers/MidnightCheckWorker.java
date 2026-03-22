package com.group15.gymtracker.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import com.group15.gymtracker.database.AppDatabase;
import com.group15.gymtracker.database.entities.DailyTargetEntity;

public class MidnightCheckWorker extends Worker {

    private static final String TAG = "MidnightCheckWorker";

    public MidnightCheckWorker(
            @NonNull Context context,
            @NonNull WorkerParameters workerParams
    ) {
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

        AppDatabase db = Room.databaseBuilder(
                getApplicationContext(),
                AppDatabase.class,
                "gym_tracker_db"
        ).build();

        DailyTargetEntity target = db.dailyTargetDao().getTargetForDay(yesterdayDay);

        if (target == null) {
            Log.d(TAG, "No target found for " + yesterdayDay + ", treat as non-gym day");
            return Result.success();
        }

        Log.d(TAG, "Target minutes = " + target.targetMinutes);

        if (target.targetMinutes <= 0) {
            Log.d(TAG, "Yesterday was not a gym day");
            return Result.success();
        }

        Log.d(TAG, "Yesterday WAS a gym day");

        return Result.success();
    }
}