package edu.bluejack26_1.wakord.ui.feature.progression

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.core.music.Chord
import edu.bluejack26_1.wakord.core.music.ChordTheory
import edu.bluejack26_1.wakord.core.music.NoteNames
import edu.bluejack26_1.wakord.domain.model.ChordVoicing
import edu.bluejack26_1.wakord.ui.components.ChordDiagram
import edu.bluejack26_1.wakord.ui.components.prettyChord
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import edu.bluejack26_1.wakord.ui.theme.jakarta
import edu.bluejack26_1.wakord.ui.theme.spaceGrotesk

private val StringNames = listOf(
    R.string.string_low_e,
    R.string.string_a,
    R.string.string_d,
    R.string.string_g,
    R.string.string_b,
    R.string.string_high_e,
)

fun chordNotes(symbol: String): List<String> {
    val chord = Chord.parse(symbol) ?: return emptyList()
    val root = NoteNames.pitchClass(chord.root)
    val flats = chord.root.contains('b') || NoteNames.prefersFlats(chord.root)
    val tones = ChordTheory.intervals(chord.suffix).map { NoteNames.name(root + it, flats) } + listOfNotNull(chord.bass)
    return tones.distinct().map { NoteNames.pretty(it) }
}

fun fingerText(voicing: ChordVoicing): String =
    voicing.frets.mapIndexed { index, fret ->
        when {
            fret < 0 -> "x"
            fret == 0 -> "0"
            else -> voicing.fingers.getOrNull(index)?.takeIf { it > 0 }?.toString() ?: "0"
        }
    }.joinToString(" · ")

@Composable
private fun voicingLabel(voicing: ChordVoicing): String {
    val position = voicing.position
    if (position <= 0) return stringResource(R.string.voicing_open)
    val res = when {
        position % 100 in 11..13 -> R.string.voicing_fret_th
        position % 10 == 1 -> R.string.voicing_fret_st
        position % 10 == 2 -> R.string.voicing_fret_nd
        position % 10 == 3 -> R.string.voicing_fret_rd
        else -> R.string.voicing_fret_th
    }
    return stringResource(res, position)
}

@Composable
private fun muteText(voicing: ChordVoicing): String {
    val muted = voicing.frets.withIndex().filter { it.value < 0 }.mapNotNull { StringNames.getOrNull(it.index) }.map { stringResource(it) }
    return when (muted.size) {
        0 -> stringResource(R.string.chord_mute_none)
        1 -> muted.first()
        else -> stringResource(R.string.chord_mute_and, muted.dropLast(1).joinToString(", "), muted.last())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChordDetailSheet(state: ProgressionDetailUiState, onAction: (ProgressionDetailAction) -> Unit) {
    val c = WakTheme.colors
    val shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    val close = { onAction(ProgressionDetailAction.CloseChord) }
    ModalBottomSheet(
        onDismissRequest = close,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.border(1.dp, c.text2.copy(alpha = 0.10f), shape),
        shape = shape,
        containerColor = c.s2,
        scrimColor = Color(0xFF03060F).copy(alpha = 0.66f),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 11.dp, bottom = 18.dp)
                    .size(width = 40.dp, height = 5.dp)
                    .background(c.s5, CircleShape),
            )
        },
    ) {
        ChordDetailContent(state, onAction, close)
    }
}

@Composable
private fun ChordDetailContent(state: ProgressionDetailUiState, onAction: (ProgressionDetailAction) -> Unit, onClose: () -> Unit) {
    val c = WakTheme.colors
    val symbol = state.selectedChord.orEmpty()
    val voicing = state.voicings.getOrNull(state.selectedVoicing)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
            .navigationBarsPadding(),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(prettyChord(symbol), style = spaceGrotesk(40.sp).copy(lineHeight = 44.sp), color = c.text)
                Spacer(Modifier.height(4.dp))
                Text(chordNotes(symbol).joinToString(" · "), style = spaceGrotesk(12.sp, FontWeight.Medium), color = c.text3)
            }
            Surface(
                onClick = onClose,
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = c.s3,
                border = BorderStroke(1.dp, c.text2.copy(alpha = 0.10f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.close), tint = c.text, modifier = Modifier.size(20.dp))
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        if (voicing == null) {
            Text(
                stringResource(R.string.chord_no_voicing),
                style = jakarta(14.sp),
                color = c.text2,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
            )
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .background(c.s2, RoundedCornerShape(24.dp))
                    .border(1.dp, c.text2.copy(alpha = 0.10f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 7.dp, vertical = 8.dp),
            ) {
                ChordDiagram(
                    frets = voicing.frets,
                    fingers = voicing.fingers,
                    baseFret = voicing.baseFret,
                    chordName = symbol,
                    width = 175.dp,
                )
            }
            Spacer(Modifier.width(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailLine(
                    stringResource(R.string.chord_voicing),
                    stringResource(R.string.voicing_position, voicingLabel(voicing), state.selectedVoicing + 1, state.voicings.size),
                    emphasize = true,
                )
                DetailLine(stringResource(R.string.chord_fingers), fingerText(voicing))
                DetailLine(stringResource(R.string.chord_mute), muteText(voicing))
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.voicings.forEachIndexed { index, option ->
                val selected = index == state.selectedVoicing
                Surface(
                    onClick = { onAction(ProgressionDetailAction.SelectVoicing(index)) },
                    modifier = Modifier.height(38.dp),
                    shape = CircleShape,
                    color = if (selected) c.primary else Color.Transparent,
                    border = BorderStroke(1.dp, if (selected) c.primary else c.outline),
                ) {
                    Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                        Text(voicingLabel(option), style = jakarta(16.sp), color = if (selected) c.onPrimary else c.text)
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                onClick = { onAction(ProgressionDetailAction.PreviewChord) },
                modifier = Modifier
                    .weight(1.35f)
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp),
                color = c.s3,
                border = BorderStroke(1.dp, c.outline),
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_play), contentDescription = null, tint = c.text, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.chord_play_preview), style = jakarta(16.sp, FontWeight.SemiBold), color = c.text)
                }
            }
            Surface(
                onClick = { onAction(ProgressionDetailAction.ToggleChordSaved) },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp),
                color = if (state.isChordSaved) c.primary else c.s3,
                border = BorderStroke(1.dp, if (state.isChordSaved) c.primary else c.outline),
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(if (state.isChordSaved) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark),
                        contentDescription = null,
                        tint = if (state.isChordSaved) c.onPrimary else c.text,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(if (state.isChordSaved) R.string.chord_saved else R.string.chord_save),
                        style = jakarta(16.sp, FontWeight.SemiBold),
                        color = if (state.isChordSaved) c.onPrimary else c.text,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String, emphasize: Boolean = false) {
    val c = WakTheme.colors
    Column {
        Text(label, style = spaceGrotesk(11.sp, FontWeight.Medium), color = c.text3)
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = if (emphasize) spaceGrotesk(16.sp) else spaceGrotesk(13.sp, FontWeight.Medium),
            color = if (emphasize) c.text else c.text2,
        )
    }
}

@Preview(name = "Chord detail sheet")
@Composable
private fun ChordDetailPreview() {
    WAKordTheme(darkTheme = true) {
        Box(Modifier.background(WakTheme.colors.s2).padding(top = 20.dp)) {
            ChordDetailContent(
                state = ProgressionDetailUiState(
                    selectedChord = "Cmaj9",
                    voicings = listOf(
                        ChordVoicing(listOf(-1, 3, 2, 4, 3, -1), listOf(0, 2, 1, 4, 3, 0), 1, 0),
                        ChordVoicing(listOf(-1, 3, 5, 4, 3, 3), listOf(0, 1, 3, 2, 1, 1), 3, 3),
                        ChordVoicing(listOf(8, -1, 9, 7, 8, -1), listOf(2, 0, 3, 1, 2, 0), 7, 7),
                    ),
                    isChordSaved = true,
                ),
                onAction = {},
                onClose = {},
            )
        }
    }
}
