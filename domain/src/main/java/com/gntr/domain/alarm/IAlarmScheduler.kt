package com.gntr.domain.alarm

import com.gntr.domain.model.SleepSession

interface IAlarmScheduler {
    fun scheduleAlarm(session: SleepSession)
    fun cancelAlarm(session: SleepSession)
}