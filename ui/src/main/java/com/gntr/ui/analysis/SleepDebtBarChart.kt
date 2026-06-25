package com.gntr.ui.analysis

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.gntr.ui.theme.JetBrainsMono
import kotlin.math.abs

@Composable
fun SleepDebtBarChart(
    values: List<Float>,
    dateLabels: List<String>,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(140.dp)
) {
    val textMeasurer = rememberTextMeasurer()

    val debtBarColor = MaterialTheme.colorScheme.error
    val surplusBarColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val baselineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = TextStyle(
        fontFamily = JetBrainsMono,
        fontSize = MaterialTheme.typography.labelSmall.fontSize,
        color = labelColor
    )

    androidx.compose.foundation.layout.Spacer(
        modifier = modifier.drawBehind {
            if (values.isEmpty()) return@drawBehind
            drawDebtChart(
                values = values,
                labels = dateLabels,
                measurer = textMeasurer,
                debtBarColor = debtBarColor,
                surplusBarColor = surplusBarColor,
                gridColor = gridColor,
                baselineColor = baselineColor,
                labelStyle = labelStyle
            )
        }
    )
}

private fun DrawScope.drawDebtChart(
    values: List<Float>,
    labels: List<String>,
    measurer: TextMeasurer,
    debtBarColor: Color,
    surplusBarColor: Color,
    gridColor: Color,
    baselineColor: Color,
    labelStyle: TextStyle
) {
    val labelHeight = 20.dp.toPx()
    val paddingLeft = 40.dp.toPx()
    val paddingRight = 8.dp.toPx()
    val paddingTop = 8.dp.toPx()
    val paddingBottom = labelHeight + 8.dp.toPx()

    val chartW = size.width - paddingLeft - paddingRight
    val chartH = size.height - paddingTop - paddingBottom

    val maxAbs = values.maxOfOrNull { abs(it) }?.coerceAtLeast(30f) ?: 30f

    val baselineY = paddingTop + chartH / 2f
    fun yFor(minutes: Float): Float = baselineY - (minutes / maxAbs) * (chartH / 2f)

    listOf(-maxAbs, 0f, maxAbs).forEach { level ->
        val y = yFor(level)
        drawLine(
            color = if (level == 0f) baselineColor else gridColor,
            start = Offset(paddingLeft, y),
            end = Offset(size.width - paddingRight, y),
            strokeWidth = if (level == 0f) 1.5f.dp.toPx() else 1.dp.toPx()
        )
        val label = "${level.toInt()} m"
        val measured = measurer.measure(label, labelStyle)
        drawText(
            textLayoutResult = measured,
            topLeft = Offset(
                x = paddingLeft - measured.size.width - 4.dp.toPx(),
                y = y - measured.size.height / 2f
            )
        )
    }

    val barCount = values.size
    val totalBarWidth = chartW / barCount
    val barWidth = (totalBarWidth * 0.55f).coerceAtLeast(4.dp.toPx())
    val barRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())

    values.forEachIndexed { i, debt ->
        val centerX = paddingLeft + i * totalBarWidth + totalBarWidth / 2f
        val barTop = yFor(debt)
        val barHeight = abs(barTop - baselineY).coerceAtLeast(2.dp.toPx())
        val topY = minOf(barTop, baselineY)
        val color = if (debt >= 0f) debtBarColor else surplusBarColor

        drawRoundRect(
            color = color,
            topLeft = Offset(centerX - barWidth / 2f, topY),
            size = Size(barWidth, barHeight),
            cornerRadius = barRadius
        )
    }

    val labelEvery = when {
        barCount <= 7 -> 1
        barCount <= 14 -> 2
        else -> 5
    }
    labels.forEachIndexed { i, text ->
        if (i % labelEvery != 0) return@forEachIndexed
        val centerX = paddingLeft + i * totalBarWidth + totalBarWidth / 2f
        val measured = measurer.measure(text, labelStyle)
        drawText(
            textLayoutResult = measured,
            topLeft = Offset(
                x = centerX - measured.size.width / 2f,
                y = size.height - labelHeight
            )
        )
    }
}