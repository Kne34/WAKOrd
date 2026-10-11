package edu.bluejack26_1.wakord.ui.feature.progression

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import edu.bluejack26_1.wakord.domain.model.ChordVoicing
import edu.bluejack26_1.wakord.ui.components.ChordSheet
import edu.bluejack26_1.wakord.ui.components.ChordSheetContent
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakTheme

@Composable
fun ChordDetailSheet(state: ProgressionDetailUiState, onAction: (ProgressionDetailAction) -> Unit) {
    ChordSheet(
        symbol = state.selectedChord.orEmpty(),
        voicings = state.voicings,
        selectedVoicing = state.selectedVoicing,
        isSaved = state.isChordSaved,
        onSelectVoicing = { onAction(ProgressionDetailAction.SelectVoicing(it)) },
        onPreview = { onAction(ProgressionDetailAction.PreviewChord) },
        onToggleSaved = { onAction(ProgressionDetailAction.ToggleChordSaved) },
        onClose = { onAction(ProgressionDetailAction.CloseChord) },
        tuning = state.chordTuning,
    )
}

@Preview(name = "Chord detail sheet")
@Composable
private fun ChordDetailPreview() {
    WAKordTheme(darkTheme = true) {
        Box(Modifier.background(WakTheme.colors.s2).padding(top = 20.dp)) {
            ChordSheetContent(
                symbol = "Cmaj9",
                voicings = listOf(
                    ChordVoicing(listOf(-1, 3, 2, 4, 3, -1), listOf(0, 2, 1, 4, 3, 0), 1, 0),
                    ChordVoicing(listOf(-1, 3, 5, 4, 3, 3), listOf(0, 1, 3, 2, 1, 1), 3, 3),
                    ChordVoicing(listOf(8, -1, 9, 7, 8, -1), listOf(2, 0, 3, 1, 2, 0), 7, 7),
                ),
                selectedVoicing = 0,
                isSaved = true,
                onSelectVoicing = {},
                onPreview = {},
                onToggleSaved = {},
                onClose = {},
            )
        }
    }
}
