package com.gntr.professionalsleeper.framework.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.gntr.professionalsleeper.framework.alarm.AlarmConstants.EXTRA_SESSION_ID
import timber.log.Timber

class AlarmReceiver : BroadcastReceiver() {
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
            Timber.d("Mengeksekusi startForegroundService (API 26+)")
            context.startForegroundService(serviceIntent)
        } else {
            Timber.d("Mengeksekusi startService (Pre-API 26)")
            context.startService(serviceIntent)
        }
    }
}