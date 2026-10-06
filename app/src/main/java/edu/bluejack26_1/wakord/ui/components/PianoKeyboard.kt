package edu.bluejack26_1.wakord.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.bluejack26_1.wakord.ui.theme.SpaceGrotesk
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakTheme

private val WHITE_OFFSETS = listOf(0, 2, 4, 5, 7, 9, 11)
private val WHITE_LETTERS = listOf("C", "D", "E", "F", "G", "A", "B")
private val BLACK_NAMES = mapOf(1 to "C♯", 3 to "D♯", 6 to "F♯", 8 to "G♯", 10 to "A♯")

private val PianoFrame = Color(0xFF080D1D)
private val IvoryKey = Color(0xFFF3EFE6)
private val EbonyKey = Color(0xFF141A2C)
private val BadgeFill = Color(0xFF0E1323)
private val WhiteLabel = Color(0xFF3C494B)
private val WhiteLabelActive = Color(0xFF2B0C00)

@Composable
fun PianoKeyboard(
    highlighted: Set<Int>,
    modifier: Modifier = Modifier,
    intervals: Map<Int, String> = emptyMap(),
    startMidi: Int = 48,
    octaves: Int = 2,
) {
    val c = WakTheme.colors
    val measurer = rememberTextMeasurer()
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(176.dp)
            .semantics { contentDescription = highlighted.sorted().joinToString(" ") },
    ) {
        val pad = 4.dp.toPx()
        val gap = 2.dp.toPx()
        drawRoundRect(PianoFrame, cornerRadius = CornerRadius(14.dp.toPx()))

        val whiteCount = 7 * octaves
        val keyWidth = (size.width - pad * 2 - gap * (whiteCount - 1)) / whiteCount
        val keyHeight = size.height - pad * 2

        for (i in 0 until whiteCount) {
            val position = i % 7
            val midi = startMidi + (i / 7) * 12 + WHITE_OFFSETS[position]
            val active = midi in highlighted
            val left = pad + i * (keyWidth + gap)
            drawBottomRounded(Rect(left, pad, left + keyWidth, pad + keyHeight), 5.dp.toPx(), if (active) c.peach else IvoryKey)
            val centerX = left + keyWidth / 2
            val interval = intervals[midi]
            if (active && interval != null) {
                drawBadge(measurer, interval, Offset(centerX, pad + keyHeight * 135f / 168f), 10.dp.toPx(), c.peach, 8.sp)
            }
            val label = if (position == 0 || active) "${WHITE_LETTERS[position]}${midi / 12 - 1}" else WHITE_LETTERS[position]
            drawLabel(measurer, label, Offset(centerX, pad + keyHeight * 156.5f / 168f), if (active) WhiteLabelActive else WhiteLabel, 9.sp)
        }

        val blackWidth = keyWidth * 15f / 20.4f
        val blackHeight = keyHeight * 106f / 168f
        for (i in 0 until whiteCount - 1) {
            val position = i % 7
            if (position == 2 || position == 6) continue
            val midi = startMidi + (i / 7) * 12 + WHITE_OFFSETS[position] + 1
            val active = midi in highlighted
            val centerX = pad + (i + 1) * (keyWidth + gap) - gap / 2
            val rect = Rect(centerX - blackWidth / 2, pad, centerX + blackWidth / 2, pad + blackHeight)
            drawBottomRounded(rect.translate(0f, 1.5.dp.toPx()), 4.dp.toPx(), Color.Black.copy(alpha = 0.35f))
            drawBottomRounded(rect, 4.dp.toPx(), if (active) c.primary else EbonyKey)
            val interval = intervals[midi]
            if (active && interval != null) {
                drawBadge(measurer, interval, Offset(centerX, pad + blackHeight * 79f / 106f), 9.dp.toPx(), c.primary, 7.sp)
            }
            drawLabel(measurer, BLACK_NAMES.getValue(midi % 12), Offset(centerX, pad + blackHeight * 96.5f / 106f), if (active) c.onPrimary else c.text3, 7.sp)
        }
    }
}

private fun DrawScope.drawBottomRounded(rect: Rect, radius: Float, color: Color) {
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                rect = rect,
                topLeft = CornerRadius.Zero,
                topRight = CornerRadius.Zero,
                bottomRight = CornerRadius(radius),
                bottomLeft = CornerRadius(radius),
            ),
        )
    }
    drawPath(path, color)
}

private fun DrawScope.drawLabel(measurer: TextMeasurer, text: String, center: Offset, color: Color, size: TextUnit) {
    val layout = measurer.measure(text, TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = size, color = color))
    drawText(layout, topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f))
}

private fun DrawScope.drawBadge(measurer: TextMeasurer, text: String, center: Offset, radius: Float, color: Color, size: TextUnit) {
    drawCircle(BadgeFill, radius = radius, center = center)
    drawLabel(measurer, text, center, color, size)
}

@Preview(name = "Piano - F#m7(9) live chord")
@Composable
private fun PianoKeyboardPreview() {
    WAKordTheme(darkTheme = true) {
        PianoKeyboard(
            highlighted = setOf(54, 57, 61, 64, 68),
            intervals = mapOf(54 to "R", 57 to "m3", 61 to "5th", 64 to "m7", 68 to "9th"),
            modifier = Modifier
                .background(WakTheme.colors.bg)
                .padding(16.dp),
        )
    }
}

@Preview(name = "Piano - light theme")
@Composable
private fun PianoKeyboardLightPreview() {
    WAKordTheme(darkTheme = false) {
        PianoKeyboard(
            highlighted = setOf(60, 64, 67, 71),
            intervals = mapOf(60 to "R", 64 to "3", 67 to "5th", 71 to "7"),
            modifier = Modifier
                .background(WakTheme.colors.bg)
                .padding(16.dp),
        )
    }
}
