package edu.bluejack26_1.wakord.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import kotlin.math.min

@Composable
fun ChordDiagram(
    frets: List<Int>,
    fingers: List<Int>,
    modifier: Modifier = Modifier,
    baseFret: Int = 1,
    fretCount: Int = 5,
    chordName: String = "",
) {
    val c = WakTheme.colors
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall
    Canvas(
        modifier = modifier
            .width(120.dp)
            .aspectRatio(0.8f)
            .semantics { contentDescription = "$chordName ${frets.joinToString(" ") { if (it < 0) "x" else "$it" }}" },
    ) {
        val strings = frets.size
        if (strings < 2) return@Canvas
        val top = size.height * 0.16f
        val left = size.width * 0.2f
        val right = size.width * 0.06f
        val bottom = size.height * 0.04f
        val gridW = size.width - left - right
        val gridH = size.height - top - bottom
        val stringGap = gridW / (strings - 1)
        val fretGap = gridH / fretCount
        val line = 1.5.dp.toPx()

        for (f in 0..fretCount) {
            val y = top + f * fretGap
            val isNut = f == 0 && baseFret == 1
            drawLine(c.text3, Offset(left, y), Offset(left + gridW, y), strokeWidth = if (isNut) line * 3 else line)
        }
        for (s in 0 until strings) {
            val x = left + s * stringGap
            drawLine(c.text3, Offset(x, top), Offset(x, top + gridH), strokeWidth = line)
        }
        if (baseFret > 1) {
            drawCenteredText(measurer, "${baseFret}fr", Offset(left / 2, top + fretGap / 2), labelStyle.copy(color = c.text2))
        }

        val dotRadius = min(stringGap, fretGap) * 0.36f
        val markerY = top - dotRadius * 1.4f
        frets.forEachIndexed { s, fret ->
            val x = left + s * stringGap
            when {
                fret < 0 -> {
                    val r = dotRadius * 0.5f
                    drawLine(c.text2, Offset(x - r, markerY - r), Offset(x + r, markerY + r), line, StrokeCap.Round)
                    drawLine(c.text2, Offset(x - r, markerY + r), Offset(x + r, markerY - r), line, StrokeCap.Round)
                }
                fret == 0 -> drawCircle(c.text2, radius = dotRadius * 0.5f, center = Offset(x, markerY), style = Stroke(line))
                else -> {
                    val row = fret - baseFret + 1
                    if (row in 1..fretCount) {
                        val center = Offset(x, top + (row - 0.5f) * fretGap)
                        drawCircle(c.primary, radius = dotRadius, center = center)
                        val finger = fingers.getOrNull(s) ?: 0
                        if (finger > 0) {
                            drawCenteredText(measurer, "$finger", center, labelStyle.copy(color = c.onPrimary))
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawCenteredText(measurer: TextMeasurer, text: String, center: Offset, style: TextStyle) {
    val layout = measurer.measure(text, style)
    drawText(layout, topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f))
}

@Preview
@Composable
private fun ChordDiagramPreview() {
    WAKordTheme {
        ChordDiagram(frets = listOf(-1, 3, 2, 0, 3, 3), fingers = listOf(0, 2, 1, 0, 3, 4), chordName = "Cadd9")
    }
}
