package edu.bluejack26_1.wakord.ui.feature.tuner

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.core.audio.NoteMapper
import edu.bluejack26_1.wakord.domain.model.TunerInstrument
import edu.bluejack26_1.wakord.domain.model.Tuning
import edu.bluejack26_1.wakord.domain.model.TuningPresets
import edu.bluejack26_1.wakord.ui.components.SheetHeader
import edu.bluejack26_1.wakord.ui.components.WakButton
import edu.bluejack26_1.wakord.ui.components.WakSheet
import edu.bluejack26_1.wakord.ui.components.WakTextField
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import edu.bluejack26_1.wakord.ui.theme.jakarta
import edu.bluejack26_1.wakord.ui.theme.spaceGrotesk

private const val SEMITONE_RANGE = 12

@Composable
private fun stringCountLabel(instrument: TunerInstrument): String =
    if (instrument.isPaired) {
        pluralStringResource(R.plurals.tuner_pair_count, instrument.stringCount, instrument.stringCount)
    } else {
        pluralStringResource(R.plurals.tuner_string_count, instrument.stringCount, instrument.stringCount)
    }

@Composable
fun InstrumentPickerSheet(state: TunerUiState, onAction: (TunerAction) -> Unit) {
    var pending by rememberSaveable { mutableStateOf(state.instrument) }
    val close = { onAction(TunerAction.CloseSheet) }
    WakSheet(onDismiss = close) {
        SheetHeader(
            title = stringResource(R.string.tuner_instrument_title),
            subtitle = stringResource(R.string.tuner_instrument_subtitle),
            onClose = close,
        )
        Spacer(Modifier.height(18.dp))
        InstrumentList(selected = pending, onSelect = { pending = it })
        Spacer(Modifier.height(18.dp))
        WakButton(
            text = stringResource(R.string.tuner_use_item, stringResource(pending.nameRes())),
            onClick = {
                if (pending == state.instrument) close() else onAction(TunerAction.SelectInstrument(pending))
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun InstrumentList(selected: TunerInstrument, onSelect: (TunerInstrument) -> Unit) {
    val c = WakTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TunerInstrument.entries.forEach { instrument ->
            val isSelected = instrument == selected
            val letters = TuningPresets.default(instrument).notes.joinToString(" ") { noteName(it) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(if (isSelected) c.s3 else Color.Transparent, RoundedCornerShape(18.dp))
                    .border(1.dp, if (isSelected) c.primary else c.text2.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(instrument) })
                    .padding(horizontal = 17.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_guitar), contentDescription = null, tint = c.text, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(instrument.nameRes()), style = spaceGrotesk(16.sp), color = c.text)
                    Spacer(Modifier.height(2.dp))
                    Text("${stringCountLabel(instrument)} · $letters", style = spaceGrotesk(12.sp, FontWeight.Medium), color = c.text3)
                }
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .border(1.dp, if (isSelected) c.primary else c.outline, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .background(c.primary, CircleShape),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomTuningSheet(state: TunerUiState, onAction: (TunerAction) -> Unit) {
    val c = WakTheme.colors
    val instrument = state.instrument
    val standard = TuningPresets.default(instrument).notes
    val draft = remember(state.tuning) { mutableStateListOf(*state.tuning.notes.toTypedArray()) }
    var name by rememberSaveable { mutableStateOf(if (state.tuning.isCustom) state.tuning.name else "") }
    var nameError by remember { mutableStateOf(false) }
    val options = state.presets + state.customForInstrument
    val match = options.firstOrNull { it.notes == draft.toList() }
    val close = { onAction(TunerAction.CloseSheet) }

    WakSheet(onDismiss = close) {
        SheetHeader(
            title = stringResource(R.string.tuner_custom_title),
            subtitle = "${stringResource(instrument.nameRes())} · ${stringCountLabel(instrument)}",
            onClose = close,
        )
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { tuning ->
                TuningChip(
                    label = tuning.shortName,
                    selected = tuning == match,
                    onClick = {
                        draft.clear()
                        draft.addAll(tuning.notes)
                    },
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            draft.forEachIndexed { index, note ->
                val base = standard.getOrNull(index) ?: note
                StringNoteRow(
                    number = draft.size - index,
                    note = note,
                    standardNote = base,
                    onStep = { step ->
                        val midi = NoteMapper.midiOf(note) + step
                        val baseMidi = NoteMapper.midiOf(base)
                        if (midi in (baseMidi - SEMITONE_RANGE)..(baseMidi + SEMITONE_RANGE)) {
                            draft[index] = NoteMapper.labelOf(midi)
                        }
                    },
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        WakTextField(
            value = name,
            onValueChange = {
                name = it
                nameError = false
            },
            label = stringResource(R.string.tuner_name_label),
            placeholder = stringResource(R.string.tuner_name_placeholder),
            error = if (nameError) stringResource(R.string.tuner_name_required) else null,
        )
        Spacer(Modifier.height(18.dp))
        val useExisting = match != null && (name.isBlank() || name.trim() == match.name)
        WakButton(
            text = if (useExisting) stringResource(R.string.tuner_use_item, match.shortName) else stringResource(R.string.tuner_save),
            onClick = {
                when {
                    useExisting -> onAction(TunerAction.SelectTuning(match))
                    name.isBlank() -> nameError = true
                    else -> onAction(TunerAction.SaveCustomTuning(name, draft.toList()))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TuningChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = WakTheme.colors
    Surface(
        onClick = onClick,
        modifier = Modifier.height(38.dp),
        shape = CircleShape,
        color = if (selected) c.primary else Color.Transparent,
        border = BorderStroke(1.dp, if (selected) c.primary else c.outline),
    ) {
        Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(label, style = jakarta(16.sp), color = if (selected) c.onPrimary else c.text, maxLines = 1)
        }
    }
}

@Composable
private fun StringNoteRow(number: Int, note: String, standardNote: String, onStep: (Int) -> Unit) {
    val c = WakTheme.colors
    val changed = NoteMapper.midiOf(note) != NoteMapper.midiOf(standardNote)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(c.s3, RoundedCornerShape(16.dp))
            .border(1.dp, if (changed) c.orange else c.text2.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .padding(start = 15.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.tuner_string_number, number),
            style = spaceGrotesk(12.sp, FontWeight.Medium),
            color = c.text3,
            modifier = Modifier.width(72.dp),
        )
        Text(
            if (changed) stringResource(R.string.tuner_from_note, NoteMapper.pretty(standardNote)) else stringResource(R.string.tuner_standard),
            style = jakarta(12.sp),
            color = c.text3,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onStep(-1) }, modifier = Modifier.size(40.dp)) {
            Icon(painterResource(R.drawable.ic_minus), contentDescription = stringResource(R.string.tuner_lower_cd, number), tint = c.text, modifier = Modifier.size(18.dp))
        }
        Text(
            NoteMapper.pretty(note),
            style = spaceGrotesk(18.sp),
            color = if (changed) c.peachText else c.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(52.dp),
        )
        IconButton(onClick = { onStep(1) }, modifier = Modifier.size(40.dp)) {
            Icon(painterResource(R.drawable.ic_plus), contentDescription = stringResource(R.string.tuner_raise_cd, number), tint = c.text, modifier = Modifier.size(18.dp))
        }
    }
}

@Preview(name = "Instrument list")
@Composable
private fun InstrumentListPreview() {
    WAKordTheme(darkTheme = true) {
        Column(
            Modifier
                .background(WakTheme.colors.s2)
                .padding(20.dp),
        ) {
            SheetHeader(stringResource(R.string.tuner_instrument_title), stringResource(R.string.tuner_instrument_subtitle), {})
            Spacer(Modifier.height(18.dp))
            InstrumentList(selected = TunerInstrument.GUITAR, onSelect = {})
        }
    }
}

@Preview(name = "Custom tuning rows")
@Composable
private fun CustomTuningRowsPreview() {
    WAKordTheme(darkTheme = true) {
        val standard = TuningPresets.default(TunerInstrument.GUITAR).notes
        val dropD = TuningPresets.forInstrument(TunerInstrument.GUITAR)[1]
        Column(
            modifier = Modifier
                .background(WakTheme.colors.s2)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TuningPresets.forInstrument(TunerInstrument.GUITAR).take(3).forEach { tuning: Tuning ->
                    TuningChip(tuning.shortName, tuning == dropD, {})
                }
            }
            dropD.notes.forEachIndexed { index, note ->
                StringNoteRow(number = 6 - index, note = note, standardNote = standard[index], onStep = {})
            }
        }
    }
}
