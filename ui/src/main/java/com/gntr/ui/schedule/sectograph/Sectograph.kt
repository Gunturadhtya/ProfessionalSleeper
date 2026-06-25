package com.gntr.ui.schedule.sectograph

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import java.time.ZonedDateTime
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Sectograph(
    sleepSectors: List<SectographSector>,
    calendarSectors: List<SectographSector>,
    currentTime: ZonedDateTime,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    val ringColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val tickColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val needleColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
    val labelStyle = TextStyle(
        fontSize = MaterialTheme.typography.labelSmall.fontSize,
        color = labelColor
    )

    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(16.dp)
            .drawBehind {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val center = Offset(canvasWidth / 2, canvasHeight / 2)

                val radius = (size.minDimension / 2f) - 36.dp.toPx()

                rotate(degrees = -90f, pivot = center) {

                    drawStaticLayer(radius, center, textMeasurer, ringColor, tickColor, labelStyle)

                    val innerRadiusPx = radius * 0.6f
                    val innerStrokeWidth = radius * 0.15f
                    calendarSectors.forEach { sector ->
                        drawSectorArc(sector, innerRadiusPx, innerStrokeWidth, center)
                    }

                    val outerRadiusPx = radius * 0.8f
                    val outerStrokeWidth = radius * 0.2f
                    sleepSectors.forEach { sector ->
                        drawSectorArc(sector, outerRadiusPx, outerStrokeWidth, center)
                    }

                    drawCurrentTimeNeedle(currentTime, radius, center, needleColor)
                }
            }
    )
}

private fun DrawScope.drawStaticLayer(
    radius: Float,
    center: Offset,
    textMeasurer: TextMeasurer,
    ringColor: Color,
    tickColor: Color,
    labelStyle: TextStyle
) {
    drawCircle(
        color = ringColor,
        radius = radius,
        center = center,
        style = Stroke(width = 1.dp.toPx())
    )

    for (i in 0 until 24) {
        val angle = i * 15f
        val isMainHour = i % 6 == 0

        val lineLength = if (isMainHour) 12.dp.toPx() else 6.dp.toPx()
        val strokeWidth = if (isMainHour) 2.dp.toPx() else 1.dp.toPx()

        rotate(degrees = angle, pivot = center) {
            drawLine(
                color = tickColor,
                start = Offset(center.x + radius - lineLength, center.y),
                end = Offset(center.x + radius, center.y),
                strokeWidth = strokeWidth
            )
        }
    }

    val labelRadius = radius + 24.dp.toPx()
    val hours = listOf(0, 6, 12, 18)

    hours.forEach { hour ->
        val angleDeg = (hour * 15f)
        val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()

        val x = center.x + labelRadius * cos(angleRad)
        val y = center.y + labelRadius * sin(angleRad)

        val textLayoutResult = textMeasurer.measure(
            text = String.format("%02d:00", hour),
            style = labelStyle
        )

        rotate(degrees = 90f, pivot = Offset(x, y)) {
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    x - (textLayoutResult.size.width / 2),
                    y - (textLayoutResult.size.height / 2)
                )
            )
        }
    }
}

private fun DrawScope.drawSectorArc(
    sector: SectographSector,
    radiusPx: Float,
    strokeWidthPx: Float,
    center: Offset
) {
    val topLeft = Offset(center.x - radiusPx, center.y - radiusPx)
    val size = Size(radiusPx * 2, radiusPx * 2)

    drawArc(
        color = sector.color,
        startAngle = sector.startAngle,
        sweepAngle = sector.sweepAngle,
        useCenter = false,
        topLeft = topLeft,
        size = size,
        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Butt)
    )
}

@RequiresApi(Build.VERSION_CODES.O)
private fun DrawScope.drawCurrentTimeNeedle(
    currentTime: ZonedDateTime,
    radius: Float,
    center: Offset,
    needleColor: Color
) {
    val totalMinutes = (currentTime.hour * 60) + currentTime.minute
    val angleDeg = (totalMinutes / 1440f) * 360f

    rotate(degrees = angleDeg, pivot = center) {
        drawLine(
            color = needleColor,
            start = center,
            end = Offset(center.x + radius * 0.85f, center.y),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        drawCircle(
            color = needleColor,
            radius = 6.dp.toPx(),
            center = center
        )
    }
}