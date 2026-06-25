package com.gntr.domain.alarm

import com.gntr.domain.model.SleepSession

interface IPreSessionNotificationScheduler {
    fun schedulePreSessionNotification(session: SleepSession, leadSeconds: Long)
    fun cancelPreSessionNotification(session: SleepSession)
}