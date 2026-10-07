package edu.bluejack26_1.wakord.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.bluejack26_1.wakord.ui.theme.SpaceGrotesk
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakTheme

@Composable
fun ChordDiagram(
    frets: List<Int>,
    fingers: List<Int>,
    modifier: Modifier = Modifier,
    baseFret: Int = 1,
    fretCount: Int = 4,
    chordName: String = "",
) {
    val c = WakTheme.colors
    val measurer = rememberTextMeasurer()
    val markerStyle = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 8.sp, color = c.text3)
    val fretStyle = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 7.sp, color = c.text3)
    val fingerStyle = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 7.sp, color = c.onPrimary)
    Canvas(
        modifier = modifier
            .size(width = 98.dp, height = 126.dp)
            .semantics { contentDescription = "$chordName ${frets.joinToString(" ") { if (it < 0) "x" else "$it" }}" },
    ) {
        val strings = frets.size
        if (strings < 2) return@Canvas
        val left = 21.dp.toPx()
        val gridWidth = 62.3.dp.toPx()
        val top = 35.dp.toPx()
        val fretGap = 21.dp.toPx()
        val gridHeight = fretGap * fretCount
        val stringGap = gridWidth / (strings - 1)
        val line = 1.dp.toPx()

        for (s in 0 until strings) {
            drawRect(c.s5, Offset(left + s * stringGap - line / 2, top), Size(line, gridHeight))
        }
        for (f in 1..fretCount) {
            drawRect(c.s5, Offset(left, top + f * fretGap - line / 2), Size(gridWidth, line))
        }
        if (baseFret == 1) {
            val nut = 3.5.dp.toPx()
            drawRect(c.text2, Offset(left, top - nut + line), Size(gridWidth, nut))
        } else {
            drawRect(c.text2, Offset(left, top), Size(gridWidth, line))
            val label = measurer.measure("${baseFret}fr", fretStyle)
            drawText(label, topLeft = Offset(left - 7.5.dp.toPx() - label.size.width, top + fretGap / 2 - label.size.height / 2f))
        }

        val dotRadius = 5.95.dp.toPx()
        val markerY = 26.dp.toPx()
        frets.forEachIndexed { s, fret ->
            val x = left + s * stringGap
            when {
                fret < 0 -> drawCenteredText(measurer, "×", Offset(x, markerY), markerStyle)
                fret == 0 -> drawCenteredText(measurer, "○", Offset(x, markerY), markerStyle)
                else -> {
                    val row = fret - baseFret + 1
                    if (row in 1..fretCount) {
                        val center = Offset(x, top + (row - 0.5f) * fretGap)
                        drawCircle(c.primary, radius = dotRadius, center = center)
                        val finger = fingers.getOrNull(s) ?: 0
                        if (finger > 0) drawCenteredText(measurer, "$finger", center, fingerStyle)
                    }
                }
            }
        }
    }
}

@Composable
fun ChordCard(
    name: String,
    frets: List<Int>,
    fingers: List<Int>,
    modifier: Modifier = Modifier,
    baseFret: Int = 1,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val c = WakTheme.colors
    val shape = RoundedCornerShape(20.dp)
    val border = BorderStroke(1.dp, if (selected) c.primary else c.text2.copy(alpha = 0.10f))
    val content: @Composable () -> Unit = {
        Column(Modifier.padding(start = 9.dp, end = 9.dp, top = 11.dp, bottom = 13.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 20.sp),
                color = c.text,
                modifier = Modifier.padding(start = 5.dp),
            )
            ChordDiagram(frets = frets, fingers = fingers, baseFret = baseFret, chordName = name, modifier = Modifier.padding(top = 4.dp))
        }
    }
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = c.s3, border = border) { content() }
    } else {
        Surface(modifier = modifier, shape = shape, color = c.s3, border = border) { content() }
    }
}

private fun DrawScope.drawCenteredText(measurer: TextMeasurer, text: String, center: Offset, style: TextStyle) {
    val layout = measurer.measure(text, style)
    drawText(layout, topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f))
}

@Preview(name = "Chord cards")
@Composable
private fun ChordCardPreview() {
    WAKordTheme(darkTheme = true) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ChordCard(name = "Cadd9", frets = listOf(-1, 3, 2, 0, 3, 3), fingers = listOf(0, 2, 1, 0, 3, 4))
            ChordCard(name = "G13", frets = listOf(3, -1, 3, 4, 5, 5), fingers = listOf(1, 0, 1, 2, 3, 4), baseFret = 3, selected = true)
        }
    }
}
