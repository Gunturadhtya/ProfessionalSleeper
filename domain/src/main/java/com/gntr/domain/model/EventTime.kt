package com.gntr.domain.model

import java.time.LocalDate

sealed interface EventTime {
    data class Exact(val epochMilli: Long) : EventTime
    data class AllDay(val date: LocalDate) : EventTime
}