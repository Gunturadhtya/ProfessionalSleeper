package com.gntr.framework.alarm

import android.content.Context
import android.content.Intent
import com.gntr.domain.alarm.IAlarmController
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidAlarmController @Inject constructor(
    @ApplicationContext private val context: Context
) : IAlarmController {
    override fun startRinging() {
        context.startService(Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START_RINGING
        })
    }

    override fun stopRinging() {
        context.stopService(Intent(context, AlarmService::class.java))
    }
}