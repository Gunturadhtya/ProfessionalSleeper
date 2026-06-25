package com.gntr.domain.alarm

import com.gntr.domain.model.SleepSession

interface IPreSessionNotificationScheduler {
    fun schedulePreSessionNotification(session: SleepSession)
    fun cancelPreSessionNotification(session: SleepSession)
}