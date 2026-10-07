package edu.bluejack26_1.wakord.ui.feature.tuner

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import edu.bluejack26_1.wakord.domain.model.TuneStatus
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import kotlin.math.abs

private const val MAX_CENTS = 50f
private const val TICKS = 21
private const val CENTER = TICKS / 2

@Composable
fun TuningMeter(cents: Float, status: TuneStatus, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    val offset by animateFloatAsState(
        targetValue = if (status == TuneStatus.IDLE) 0f else (cents / MAX_CENTS).coerceIn(-1f, 1f),
        label = "meter",
    )
    val indicator by animateColorAsState(
        targetValue = when (status) {
            TuneStatus.IN_TUNE -> c.primary
            TuneStatus.FLAT, TuneStatus.SHARP -> c.orange
            TuneStatus.IDLE -> c.text3.copy(alpha = 0.5f)
        },
        label = "meterColor",
    )
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        val tickWidth = 2.dp.toPx()
        val top = 6.dp.toPx()
        val rowHeight = 40.dp.toPx()
        val gap = (size.width - tickWidth) / (TICKS - 1)
        for (i in 0 until TICKS) {
            val distance = abs(i - CENTER)
            val height = when {
                distance == 0 -> 40.dp
                distance == 5 || distance == 10 -> 28.dp
                else -> 16.dp
            }.toPx()
            val color = when (distance) {
                0 -> c.primary
                1 -> c.text3
                else -> c.s5
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(i * gap, top + rowHeight - height),
                size = Size(tickWidth, height),
                cornerRadius = CornerRadius(tickWidth / 2),
            )
        }
        val barWidth = 4.dp.toPx()
        val centerX = size.width / 2 + offset * (size.width / 2 - barWidth / 2)
        drawRoundRect(
            color = indicator,
            topLeft = Offset(centerX - barWidth / 2, 0f),
            size = Size(barWidth, size.height),
            cornerRadius = CornerRadius(barWidth / 2),
        )
    }
}
