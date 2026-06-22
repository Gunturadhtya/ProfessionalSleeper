package com.gntr.professionalsleeper.framework.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.gntr.professionalsleeper.data.local.entity.SleepSession
import com.gntr.professionalsleeper.domain.alarm.IAlarmScheduler
import com.gntr.professionalsleeper.framework.alarm.AlarmConstants.EXTRA_SESSION_ID

class AlarmSchedulerImpl (
    private val context: Context
) : IAlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun scheduleAlarm(session: SleepSession) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_SESSION_ID, session.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            session.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            session.endTime,
            pendingIntent
        )
    }

    override fun cancelAlarm(session: SleepSession) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            session.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

}