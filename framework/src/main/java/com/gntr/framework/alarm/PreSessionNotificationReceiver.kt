package com.gntr.framework.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import timber.log.Timber

class PreSessionNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getLongExtra(PreSessionNotificationScheduler.EXTRA_SESSION_ID, -1L)
        val sessionType = intent.getStringExtra(PreSessionNotificationScheduler.EXTRA_SESSION_TYPE) ?: "Session"
        val leadSeconds = intent.getLongExtra(PreSessionNotificationScheduler.EXTRA_LEAD_SECONDS, 300L)

        if (sessionId == -1L) return

        val leadText = when {
            leadSeconds < 60 -> "in $leadSeconds seconds"
            else -> "in ${leadSeconds / 60} minutes"
        }

        val typeLabel = if (sessionType == "NAP") "nap" else "core sleep session"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Ensure channel exists
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                PreSessionNotificationScheduler.PRE_SESSION_CHANNEL_ID,
                "Upcoming Sleep Sessions",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, PreSessionNotificationScheduler.PRE_SESSION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Sleep session starting $leadText")
            .setContentText("Your $typeLabel is about to begin. Get ready!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .build()

        nm.notify(sessionId.toInt(), notification)
        Timber.d("Pre-session notification fired for session $sessionId ($typeLabel).")
    }
}