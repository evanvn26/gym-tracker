package com.group15.gymtracker

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit
import com.group15.gymtracker.domain.AppLocker
import com.group15.gymtracker.onboarding.SharedPreferencesOnboardingFlagStore
import com.group15.gymtracker.ui.checkin.CheckinActivity
import com.group15.gymtracker.ui.onboarding.OnboardingActivity
import com.group15.gymtracker.ui.stats.StatsActivity
import com.group15.gymtracker.ui.theme.GymTrackerTheme
import com.group15.gymtracker.workers.MidnightCheckWorker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!SharedPreferencesOnboardingFlagStore(this).isComplete()) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        scheduleMidnightWorker()

        setContent {
            GymTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(
                        onRunWorker = {
                            val request = OneTimeWorkRequest.Builder(MidnightCheckWorker::class.java).build()
                            WorkManager.getInstance(this).enqueue(request)
                            Toast.makeText(this, "MidnightCheckWorker enqueued", Toast.LENGTH_SHORT).show()
                        },
                        onClearLock = {
                            AppLocker(this).unlockApps()
                            Toast.makeText(this, "Lock state cleared", Toast.LENGTH_SHORT).show()
                        },
                        onOpenStats = {
                            startActivity(Intent(this, StatsActivity::class.java))
                        },
                        onOpenCheckin = {
                            startActivity(Intent(this, CheckinActivity::class.java))
                        }
                    )
                }
            }
        }
    }
    private fun scheduleMidnightWorker() {
        val nextMidnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val delayMs = nextMidnight - System.currentTimeMillis()

        val request = PeriodicWorkRequest.Builder(MidnightCheckWorker::class.java, 1, TimeUnit.DAYS)
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "midnight_check",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}

@Composable
private fun HomeScreen(
    onRunWorker: () -> Unit,
    onClearLock: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenCheckin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Gym Tracker",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Accessibility lock testing is now manual, so the app no longer locks itself on startup.",
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)
        )
        Button(onClick = onRunWorker) {
            Text("Run Midnight Check")
        }
        Button(
            onClick = onClearLock,
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text("Clear Lock State")
        }
        Button(
            onClick = onOpenCheckin,
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text("Check In")
        }
        Button(
            onClick = onOpenStats,
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text("Open Statistics")
        }
    }
}
