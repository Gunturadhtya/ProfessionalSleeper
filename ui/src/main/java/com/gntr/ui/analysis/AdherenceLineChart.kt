package com.gntr.ui.analysis

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gntr.ui.theme.CoreSleepColor
import com.gntr.ui.theme.JetBrainsMono
import kotlin.math.roundToInt

private val LineColor = CoreSleepColor
private val GridColor = Color.Gray.copy(alpha = 0.15f)
private val FillStart = LineColor.copy(alpha = 0.25f)
private val FillEnd = LineColor.copy(alpha = 0f)
private val DotColor = Color.White
private val LabelColor = Color.Gray

@Composable
fun AdherenceLineChart(
    points: List<Float>,
    dateLabels: List<String>,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(160.dp)
) {
    val textMeasurer = rememberTextMeasurer()

    androidx.compose.foundation.layout.Spacer(
        modifier = modifier.drawBehind {
            if (points.size < 2) return@drawBehind
            drawAdherenceChart(points, dateLabels, textMeasurer)
        }
    )
}


private fun DrawScope.drawAdherenceChart(
    points: List<Float>,
    labels: List<String>,
    measurer: TextMeasurer
) {
    val labelHeight = 20.dp.toPx()
    val paddingLeft = 36.dp.toPx()
    val paddingRight = 8.dp.toPx()
    val paddingTop = 8.dp.toPx()
    val paddingBottom = labelHeight + 8.dp.toPx()

    val chartW = size.width - paddingLeft - paddingRight
    val chartH = size.height - paddingTop - paddingBottom

    val xStep = if (points.size > 1) chartW / (points.size - 1) else chartW

    fun xAt(i: Int): Float = paddingLeft + i * xStep
    fun yAt(v: Float): Float = paddingTop + chartH * (1f - v.coerceIn(0f, 1f))

    val gridLevels = listOf(0f, 0.5f, 1f)
    val labelStyle = TextStyle(
        fontFamily = JetBrainsMono,
        fontSize = 9.sp,
        color = LabelColor
    )
    gridLevels.forEach { level ->
        val y = yAt(level)
        drawLine(
            color = GridColor,
            start = Offset(paddingLeft, y),
            end = Offset(size.width - paddingRight, y),
            strokeWidth = 1.dp.toPx()
        )
        val label = "${(level * 100).roundToInt()}%"
        val measured = measurer.measure(label, labelStyle)
        drawText(
            textLayoutResult = measured,
            topLeft = Offset(
                x = paddingLeft - measured.size.width - 4.dp.toPx(),
                y = y - measured.size.height / 2f
            )
        )
    }

    val fillPath = Path().apply {
        moveTo(xAt(0), yAt(points[0]))
        for (i in 1 until points.size) {
            cubicBezierTo(
                path = this,
                x0 = xAt(i - 1), y0 = yAt(points[i - 1]),
                x1 = xAt(i), y1 = yAt(points[i])
            )
        }
        lineTo(xAt(points.lastIndex), paddingTop + chartH)
        lineTo(xAt(0), paddingTop + chartH)
        close()
    }
    drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
            colors = listOf(FillStart, FillEnd),
            startY = paddingTop,
            endY = paddingTop + chartH
        )
    )

    val linePath = Path().apply {
        moveTo(xAt(0), yAt(points[0]))
        for (i in 1 until points.size) {
            cubicBezierTo(
                path = this,
                x0 = xAt(i - 1), y0 = yAt(points[i - 1]),
                x1 = xAt(i), y1 = yAt(points[i])
            )
        }
    }
    drawPath(
        path = linePath,
        color = LineColor,
        style = Stroke(
            width = 2.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    val dotRadius = 3.dp.toPx()
    points.forEachIndexed { i, v ->
        drawCircle(color = LineColor, radius = dotRadius + 1.dp.toPx(), center = Offset(xAt(i), yAt(v)))
        drawCircle(color = DotColor, radius = dotRadius, center = Offset(xAt(i), yAt(v)))
    }

    val labelEvery = when {
        points.size <= 7 -> 1
        points.size <= 14 -> 2
        else -> 5
    }
    labels.forEachIndexed { i, text ->
        if (i % labelEvery != 0) return@forEachIndexed
        val measured = measurer.measure(text, labelStyle)
        drawText(
            textLayoutResult = measured,
            topLeft = Offset(
                x = xAt(i) - measured.size.width / 2f,
                y = size.height - labelHeight
            )
        )
    }
}

private fun cubicBezierTo(
    path: Path,
    x0: Float, y0: Float,
    x1: Float, y1: Float
) {
    val tension = (x1 - x0) / 3f
    path.cubicTo(
        x0 + tension, y0,
        x1 - tension, y1,
        x1, y1
    )
}