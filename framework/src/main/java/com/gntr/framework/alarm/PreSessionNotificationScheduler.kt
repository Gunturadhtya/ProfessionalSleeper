package com.gntr.framework.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.gntr.domain.alarm.IPreSessionNotificationScheduler
import com.gntr.domain.model.SleepSession
import timber.log.Timber

class PreSessionNotificationScheduler(private val context: Context) : IPreSessionNotificationScheduler{

    companion object {
        const val PRE_SESSION_CHANNEL_ID = "PRE_SESSION_CHANNEL_ID"
        const val EXTRA_SESSION_ID = "PRE_SESSION_EXTRA_SESSION_ID"
        const val EXTRA_SESSION_TYPE = "PRE_SESSION_EXTRA_SESSION_TYPE"
        const val EXTRA_LEAD_SECONDS = "PRE_SESSION_EXTRA_LEAD_SECONDS"
//        val LEAD_TIME_OPTIONS_SECONDS = listOf(2L, 10L, 300L)

        fun pendingIntentFor(context: Context, sessionId: Long): PendingIntent {
            val intent = Intent(context, PreSessionNotificationReceiver::class.java).apply {
                putExtra(EXTRA_SESSION_ID, sessionId)
            }
            return PendingIntent.getBroadcast(
                context,
                (sessionId + 1_000_000L).toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    @RequiresApi(Build.VERSION_CODES.O)
    override fun schedulePreSessionNotification(
        session: SleepSession,
        leadSeconds: Long
    ) {
//        val leadSeconds = LEAD_TIME_OPTIONS_SECONDS[(session.id % LEAD_TIME_OPTIONS_SECONDS.size).toInt()]
        val triggerAtMillis = session.startTime.toInstant().toEpochMilli() - (leadSeconds * 1_000L)

        if (triggerAtMillis <= System.currentTimeMillis()) {
            Timber.w("Pre-session notification skipped for session ${session.id}: trigger time is in the past.")
            return
        }

        val intent = Intent(context, PreSessionNotificationReceiver::class.java).apply {
            putExtra(EXTRA_SESSION_ID, session.id)
            putExtra(EXTRA_SESSION_TYPE, session.type.name)
            putExtra(EXTRA_LEAD_SECONDS, leadSeconds)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (session.id + 1_000_000L).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Timber.w("Cannot schedule exact alarms – falling back to inexact.")
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                return
            }
        }

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        Timber.d("Pre-session notification scheduled for session ${session.id} in ${leadSeconds}s before start.")
    }

    override fun cancelPreSessionNotification(session: SleepSession) {
        alarmManager.cancel(pendingIntentFor(context, session.id))
        Timber.d("Pre-session notification cancelled for session ${session.id}.")
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                PRE_SESSION_CHANNEL_ID,
                "Upcoming Sleep Sessions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies you before a sleep session is about to start"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}