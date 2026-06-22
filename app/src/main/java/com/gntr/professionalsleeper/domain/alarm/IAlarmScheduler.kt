package com.gntr.professionalsleeper.domain.alarm

import com.gntr.professionalsleeper.data.local.entity.SleepSession

interface IAlarmScheduler {
    fun scheduleAlarm(session: SleepSession)
    fun cancelAlarm(session: SleepSession)
}