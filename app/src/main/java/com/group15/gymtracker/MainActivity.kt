package com.group15.gymtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.group15.gymtracker.workers.MidnightCheckWorker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val request = OneTimeWorkRequest.Builder(MidnightCheckWorker::class.java).build()
        WorkManager.getInstance(this).enqueue(request)

        setContent {
            //default compose staff
        }
    }
}