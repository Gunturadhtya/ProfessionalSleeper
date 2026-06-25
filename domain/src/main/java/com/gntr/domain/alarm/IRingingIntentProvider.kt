package com.gntr.domain.alarm

import android.app.PendingIntent

interface IRingingIntentProvider {
    fun getRingingPendingIntent(sessionId: Long): PendingIntent
    fun launchRingingActivity(sessionId: Long)
}