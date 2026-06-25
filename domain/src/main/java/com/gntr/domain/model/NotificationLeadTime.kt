package com.gntr.domain.model

object NotificationLeadTime {
    val OPTIONS_SECONDS = listOf(2L, 10L, 300L)

    fun forSession(session: SleepSession): Long =
        OPTIONS_SECONDS[(session.id % OPTIONS_SECONDS.size).toInt()]
}