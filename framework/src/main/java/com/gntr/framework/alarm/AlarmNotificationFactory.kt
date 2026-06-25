package com.gntr.framework.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.gntr.domain.alarm.IRingingIntentProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

class AlarmNotificationFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ringingIntentProvider: IRingingIntentProvider
) {
    fun createNotification(sessionId: Long, title: String, content: String): Notification {
        createNotificationChannel()

        val fullScreenPendingIntent = ringingIntentProvider.getRingingPendingIntent(sessionId)

        return NotificationCompat.Builder(context, AlarmConstants.ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                AlarmConstants.ALARM_CHANNEL_ID,
                "Sleep Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for sleep schedules"
                setBypassDnd(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}