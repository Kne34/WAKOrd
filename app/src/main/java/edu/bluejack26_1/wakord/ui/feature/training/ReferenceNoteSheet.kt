package edu.bluejack26_1.wakord.ui.feature.training

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.core.audio.NoteMapper
import edu.bluejack26_1.wakord.ui.components.SheetHeader
import edu.bluejack26_1.wakord.ui.components.WakChip
import edu.bluejack26_1.wakord.ui.components.WakSheet
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import edu.bluejack26_1.wakord.ui.theme.jakarta

private val Octaves = 2..5

@Composable
fun ReferenceNoteSheet(selected: Int, onSelect: (Int) -> Unit, onClose: () -> Unit) {
    WakSheet(onDismiss = onClose) {
        ReferenceNoteContent(selected, onSelect, onClose)
    }
}

@Composable
private fun ReferenceNoteContent(selected: Int, onSelect: (Int) -> Unit, onClose: () -> Unit) {
    var octave by rememberSaveable { mutableIntStateOf(NoteMapper.octaveOf(selected).coerceIn(Octaves.first, Octaves.last)) }
    Column {
        SheetHeader(
            title = stringResource(R.string.pitch_reference_note),
            subtitle = stringResource(R.string.pitch_note_range),
            onClose = onClose,
        )
        Spacer(Modifier.height(22.dp))
        Overline(stringResource(R.string.pitch_octave))
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Octaves.forEach { value ->
                WakChip(label = value.toString(), selected = value == octave, onClick = { octave = value })
            }
        }
        Spacer(Modifier.height(18.dp))
        val base = (octave + 1) * 12
        (0 until 12).chunked(6).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { step ->
                    val midi = base + step
                    NoteKey(
                        label = NoteMapper.pretty(NoteMapper.nameOf(midi)),
                        selected = midi == selected,
                        onClick = { onSelect(midi) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun NoteKey(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) c.primary else c.s3,
        border = if (selected) null else BorderStroke(1.dp, c.line),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = jakarta(14.sp), color = if (selected) c.onPrimary else c.text)
        }
    }
}

@Preview(name = "Reference note sheet", widthDp = 390)
@Composable
private fun ReferenceNotePreview() {
    WAKordTheme(darkTheme = true) {
        Surface(color = WakTheme.colors.s2) {
            Box(Modifier.padding(20.dp)) {
                ReferenceNoteContent(selected = 67, onSelect = {}, onClose = {})
            }
        }
    }
}
