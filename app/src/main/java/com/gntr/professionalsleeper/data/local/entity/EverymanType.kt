package com.gntr.professionalsleeper.data.local.entity

enum class EverymanType(
    val displayName: String,
    val coreSleepMinutes: Int,
    val napCount: Int,
    val napDurationMinutes: Int,
    val napIntervalHours: Long
) {
    EVERYMAN_1("Everyman 1", 360, 1, 25, 8),
    EVERYMAN_2("Everyman 2", 270, 2, 25, 4),
    EVERYMAN_3("Everyman 3", 180, 3, 25, 4),
    EVERYMAN_4("Everyman 4", 90, 4, 25, 4),
    EVERYMAN_5("Everyman 5", 90, 5, 25, 4)
}