package com.gntr.framework.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.gntr.framework.alarm.AlarmConstants.EXTRA_SESSION_ID
import timber.log.Timber

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("OS AlarmManager triggered AlarmReceiver.")

        val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L)
        if (sessionId == -1L) {
            Timber.e("AlarmReceiver invoked without a valid session ID. Aborting.")
            return
        }

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra(EXTRA_SESSION_ID, sessionId)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to launch AlarmService from BroadcastReceiver.")
        }
    }
}