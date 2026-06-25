package com.gntr.ui.schedule.sectograph

import androidx.compose.ui.graphics.Color

data class SectographSector(
    val color: Color,
    val outerRadiusRatio: Float,
    val innerRadiusRatio: Float,
    val startAngle: Float,
    val sweepAngle: Float
)