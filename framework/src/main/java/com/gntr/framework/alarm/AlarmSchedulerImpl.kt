package com.gntr.framework.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.gntr.domain.alarm.IAlarmScheduler
import com.gntr.domain.model.SleepSession
import com.gntr.framework.alarm.AlarmConstants.EXTRA_SESSION_ID
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AlarmSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : IAlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    @RequiresApi(Build.VERSION_CODES.O)
    override fun scheduleAlarm(session: SleepSession) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_SESSION_ID, session.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            session.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            session.endTime.toInstant().toEpochMilli(),
            pendingIntent
        )
    }

    override fun cancelAlarm(session: SleepSession) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            session.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

}