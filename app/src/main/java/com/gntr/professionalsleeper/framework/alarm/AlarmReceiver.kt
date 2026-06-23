package com.gntr.professionalsleeper.framework.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.gntr.professionalsleeper.data.local.dao.SleepSessionDao
import com.gntr.professionalsleeper.data.local.entity.SessionStatus
import com.gntr.professionalsleeper.data.local.entity.SleepSession
import com.gntr.professionalsleeper.domain.alarm.IAlarmScheduler
import com.gntr.professionalsleeper.framework.alarm.AlarmConstants.EXTRA_SESSION_ID
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class AlarmReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AlarmReceiverEntryPoint {
        fun sleepSessionDao(): SleepSessionDao
        fun alarmScheduler(): IAlarmScheduler
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("onReceive dipicu oleh AlarmManager")

        val sessionId = intent.getIntExtra(EXTRA_SESSION_ID, -1)
        if (sessionId == -1) {
            Timber.e("Pemicu diabaikan: EXTRA_SESSION_ID bernilai -1")
            return
        }

        Timber.i("Mendelegasikan ke AlarmService untuk sessionId: $sessionId")
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra(EXTRA_SESSION_ID, sessionId)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        scheduleNextDaySession(context, sessionId)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun scheduleNextDaySession(context: Context, firedSessionId: Int) {
        val pendingResult = goAsync()

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            AlarmReceiverEntryPoint::class.java
        )
        val dao = entryPoint.sleepSessionDao()
        val scheduler = entryPoint.alarmScheduler()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val firedSession: SleepSession = dao.getSessionById(firedSessionId)
                    ?: run {
                        Timber.e("Sesi $firedSessionId tidak ditemukan, tidak bisa menjadwalkan ulang")
                        return@launch
                    }

                val nextSession = firedSession.copy(
                    id = 0,
                    startTime = firedSession.startTime.plusDays(1),
                    endTime = firedSession.endTime.plusDays(1),
                    status = SessionStatus.SCHEDULED,
                    snoozeCount = 0
                )

                val insertedId = dao.insertSession(nextSession)
                val nextSessionWithId = nextSession.copy(id = insertedId.toInt())
                scheduler.scheduleAlarm(nextSessionWithId)

                Timber.i("Sesi berikutnya dijadwalkan: id=$insertedId, endTime=${nextSessionWithId.endTime}")
            } catch (e: Exception) {
                Timber.e(e, "Gagal menjadwalkan sesi berikutnya")
            } finally {
                pendingResult.finish()
            }
        }
    }
}