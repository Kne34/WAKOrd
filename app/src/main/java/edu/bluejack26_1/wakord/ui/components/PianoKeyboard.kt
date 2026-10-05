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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakTheme

private val WHITE_OFFSETS = listOf(0, 2, 4, 5, 7, 9, 11)

@Composable
fun PianoKeyboard(
    highlighted: Set<Int>,
    modifier: Modifier = Modifier,
    startMidi: Int = 48,
    octaves: Int = 2,
) {
    val c = WakTheme.colors
    val whiteKey = if (c.isDark) c.text else c.s1
    val blackKey = if (c.isDark) c.bg else c.text
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp),
    ) {
        val whiteCount = 7 * octaves
        val keyW = size.width / whiteCount
        val gap = 1.dp.toPx()
        for (i in 0 until whiteCount) {
            val midi = startMidi + (i / 7) * 12 + WHITE_OFFSETS[i % 7]
            drawRoundRect(
                color = if (midi in highlighted) c.primary else whiteKey,
                topLeft = Offset(i * keyW + gap, 0f),
                size = Size(keyW - gap * 2, size.height),
                cornerRadius = CornerRadius(6.dp.toPx()),
            )
        }
        for (i in 0 until whiteCount) {
            val pos = i % 7
            if (pos == 2 || pos == 6 || i == whiteCount - 1) continue
            val midi = startMidi + (i / 7) * 12 + WHITE_OFFSETS[pos] + 1
            drawRoundRect(
                color = if (midi in highlighted) c.peach else blackKey,
                topLeft = Offset((i + 1) * keyW - keyW * 0.3f, 0f),
                size = Size(keyW * 0.6f, size.height * 0.6f),
                cornerRadius = CornerRadius(4.dp.toPx()),
            )
        }
    }
}

@Preview(name = "Piano - Cmaj7 dark")
@Composable
private fun PianoKeyboardDarkPreview() {
    WAKordTheme(darkTheme = true) {
        PianoKeyboard(
            highlighted = setOf(60, 64, 67, 71),
            modifier = Modifier
                .background(WakTheme.colors.bg)
                .padding(16.dp),
        )
    }
}

@Preview(name = "Piano - F#m7 light")
@Composable
private fun PianoKeyboardLightPreview() {
    WAKordTheme(darkTheme = false) {
        PianoKeyboard(
            highlighted = setOf(54, 57, 61, 64),
            modifier = Modifier
                .background(WakTheme.colors.bg)
                .padding(16.dp),
        )
    }
}
