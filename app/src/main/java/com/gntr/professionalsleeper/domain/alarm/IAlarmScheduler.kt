package com.gntr.professionalsleeper.domain.alarm

import com.gntr.professionalsleeper.data.local.entity.SleepSessionEntity

interface IAlarmScheduler {
    fun scheduleAlarm(session: SleepSessionEntity)
    fun cancelAlarm(session: SleepSessionEntity)
}