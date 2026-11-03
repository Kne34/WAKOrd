package edu.bluejack26_1.wakord.ui.feature.tuner

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.core.audio.NoteMapper
import edu.bluejack26_1.wakord.domain.model.TuneStatus
import edu.bluejack26_1.wakord.domain.model.TunerInstrument
import edu.bluejack26_1.wakord.ui.components.BackgroundArt
import edu.bluejack26_1.wakord.ui.components.NotificationBell
import edu.bluejack26_1.wakord.ui.components.WakButton
import edu.bluejack26_1.wakord.ui.components.glassBorder
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import edu.bluejack26_1.wakord.ui.theme.jakarta
import edu.bluejack26_1.wakord.ui.theme.spaceGrotesk
import java.util.Locale
import kotlin.math.roundToInt

@StringRes
fun TunerInstrument.nameRes(): Int = when (this) {
    TunerInstrument.GUITAR -> R.string.instrument_guitar
    TunerInstrument.UKULELE -> R.string.instrument_ukulele
    TunerInstrument.BASS -> R.string.instrument_bass
    TunerInstrument.MANDOLIN -> R.string.instrument_mandolin
    TunerInstrument.VIOLIN -> R.string.instrument_violin
}

internal fun noteName(note: String): String = NoteMapper.pretty(NoteMapper.nameOf(NoteMapper.midiOf(note)))

internal fun noteOctave(note: String): Int = NoteMapper.octaveOf(NoteMapper.midiOf(note))

private fun signed(cents: Float): String {
    val value = cents.roundToInt()
    return if (value > 0) "+$value" else "$value"
}

@Composable
fun TunerScreen(
    state: TunerUiState,
    onAction: (TunerAction) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier,
    unreadNotifications: Int = 0,
) {
    val c = WakTheme.colors
    val micOff = state.micPermission == MicPermission.DENIED && !state.pitchPipeMode
    val alert = !micOff && !state.pitchPipeMode && (state.status == TuneStatus.FLAT || state.status == TuneStatus.SHARP)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg),
    ) {
        BackgroundArt(if (alert) R.drawable.bg_tuner_alert else R.drawable.bg_tuner, height = 600.dp)
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            val minHeight = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = minHeight),
            ) {
                TunerHeader(state, onAction, unreadNotifications, onOpenNotifications)
                if (micOff) {
                    MicOffState(
                        onOpenSettings = onOpenSettings,
                        onUsePitchPipe = { onAction(TunerAction.UsePitchPipe) },
                    )
                } else {
                    ReadingBlock(state, onOpenSettings)
                    if (!state.pitchPipeMode) {
                        MeterBlock(state)
                    }
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.height(24.dp))
                    StringCard(state, onAction)
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    when (state.sheet) {
        TunerSheet.INSTRUMENT -> InstrumentPickerSheet(state, onAction)
        TunerSheet.CUSTOM_TUNING -> CustomTuningSheet(state, onAction)
        null -> Unit
    }
}

@Composable
private fun TunerHeader(
    state: TunerUiState,
    onAction: (TunerAction) -> Unit,
    unreadNotifications: Int,
    onOpenNotifications: () -> Unit,
) {
    val c = WakTheme.colors
    val instrumentName = stringResource(state.instrument.nameRes())
    val instrumentCd = stringResource(R.string.tuner_instrument_cd, instrumentName)
    val tuningCd = stringResource(R.string.tuner_tuning_cd, state.tuning.name)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HeaderChip(
            text = instrumentName,
            onClick = { onAction(TunerAction.OpenSheet(TunerSheet.INSTRUMENT)) },
            modifier = Modifier.semantics { contentDescription = instrumentCd },
            leading = {
                Icon(painterResource(R.drawable.ic_guitar), contentDescription = null, tint = c.text, modifier = Modifier.size(18.dp))
            },
        )
        Box(Modifier.weight(1f)) {
            HeaderChip(
                text = state.tuning.name,
                onClick = { onAction(TunerAction.OpenSheet(TunerSheet.CUSTOM_TUNING)) },
                modifier = Modifier.semantics { contentDescription = tuningCd },
            )
        }
        NotificationBell(unreadCount = unreadNotifications, onClick = onOpenNotifications)
    }
}

@Composable
private fun HeaderChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    val c = WakTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = CircleShape,
        color = c.s3,
        border = BorderStroke(1.dp, c.line),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = jakarta(16.sp),
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(8.dp))
            Icon(painterResource(R.drawable.ic_chevron_down), contentDescription = null, tint = c.text, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ReadingBlock(state: TunerUiState, onOpenSettings: () -> Unit) {
    val c = WakTheme.colors
    val notes = state.tuning.notes
    val pipeIndex = state.selectedString.coerceIn(notes.indices)
    val active = state.activeString
    val overline = when {
        state.pitchPipeMode -> stringResource(R.string.tuner_pitch_pipe)
        active != null -> stringResource(R.string.tuner_string_label, notes.size - active, NoteMapper.pretty(notes[active]))
        else -> stringResource(R.string.tuner_play_a_string)
    }
    val reading = state.reading
    val noteText: String
    val octaveText: String?
    val hzText: String?
    when {
        state.pitchPipeMode -> {
            val note = notes[pipeIndex]
            noteText = noteName(note)
            octaveText = noteOctave(note).toString()
            hzText = String.format(Locale.US, "%.1f Hz", NoteMapper.frequencyOf(NoteMapper.midiOf(note), state.referencePitch))
        }
        reading != null -> {
            noteText = NoteMapper.pretty(reading.name)
            octaveText = reading.octave.toString()
            hzText = String.format(Locale.US, "%.1f Hz", reading.frequencyHz)
        }
        else -> {
            noteText = "–"
            octaveText = null
            hzText = null
        }
    }
    val noteColor by animateColorAsState(
        targetValue = when {
            state.pitchPipeMode -> c.primaryText
            state.status == TuneStatus.IN_TUNE -> c.primaryText
            state.status == TuneStatus.FLAT || state.status == TuneStatus.SHARP -> c.peachText
            else -> c.text3
        },
        label = "noteColor",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(overline, style = spaceGrotesk(11.sp, FontWeight.Medium), color = c.text3)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.heightIn(min = 132.dp)) {
            Text(noteText.take(1), style = spaceGrotesk(132.sp).copy(lineHeight = 132.sp), color = noteColor)
            Column(Modifier.padding(start = 2.dp, top = 14.dp)) {
                if (noteText.length > 1) {
                    Text(noteText.drop(1), style = spaceGrotesk(48.sp).copy(lineHeight = 52.sp), color = noteColor)
                }
                if (octaveText != null) {
                    Text(octaveText, style = spaceGrotesk(28.sp, FontWeight.Medium), color = c.text3, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(hzText ?: " ", style = spaceGrotesk(15.sp, FontWeight.Medium), color = c.text2)
        Spacer(Modifier.height(8.dp))
        StatusTag(state)
        if (state.pitchPipeMode && state.micPermission != MicPermission.GRANTED) {
            Spacer(Modifier.height(8.dp))
            Surface(onClick = onOpenSettings, shape = RoundedCornerShape(14.dp), color = Color.Transparent) {
                Text(
                    stringResource(R.string.tuner_turn_on_mic),
                    style = spaceGrotesk(14.sp),
                    color = c.primaryText,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                )
            }
        }
    }
}

@Composable
private fun StatusTag(state: TunerUiState) {
    val c = WakTheme.colors
    val cents = signed(state.centsOff)
    val (text, isAlert) = when {
        state.pitchPipeMode -> stringResource(R.string.tuner_pitch_pipe_hint) to false
        state.status == TuneStatus.IN_TUNE -> stringResource(R.string.tuner_in_tune, cents) to false
        state.status == TuneStatus.FLAT -> stringResource(R.string.tuner_flat, cents) to true
        state.status == TuneStatus.SHARP -> stringResource(R.string.tuner_sharp, cents) to true
        else -> stringResource(R.string.tuner_listening) to false
    }
    val idle = !state.pitchPipeMode && state.status == TuneStatus.IDLE
    val content = when {
        isAlert -> c.peachText
        idle -> c.text3
        else -> c.primaryText
    }
    Row(
        modifier = Modifier
            .height(30.dp)
            .background(
                color = when {
                    isAlert -> c.peachSoft
                    idle -> c.s3
                    else -> c.primarySoft
                },
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!idle) {
            Icon(
                painter = painterResource(
                    when {
                        isAlert -> R.drawable.ic_alert_circle
                        state.pitchPipeMode && state.micPermission == MicPermission.GRANTED -> R.drawable.ic_music_note
                        state.pitchPipeMode -> R.drawable.ic_mic_off
                        else -> R.drawable.ic_check
                    },
                ),
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(5.dp))
        }
        Text(text, style = spaceGrotesk(12.sp, FontWeight.Medium), color = content)
    }
}

@Composable
private fun MeterBlock(state: TunerUiState) {
    val c = WakTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp),
    ) {
        TuningMeter(cents = state.centsOff, status = state.status)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.tuner_flat_scale), style = jakarta(12.sp), color = c.text3, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.tuner_zero_cents), style = jakarta(12.sp), color = c.primaryText)
            Text(
                stringResource(R.string.tuner_sharp_scale),
                style = jakarta(12.sp),
                color = c.text3,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StringCard(state: TunerUiState, onAction: (TunerAction) -> Unit) {
    val c = WakTheme.colors
    val notes = state.tuning.notes
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(28.dp),
        color = c.glass,
        border = glassBorder(c),
    ) {
        Column(Modifier.padding(17.dp)) {
            ModeSelector(
                playMode = state.pitchPipeMode,
                onListen = { onAction(TunerAction.UseListening) },
                onPlay = { onAction(TunerAction.UsePitchPipe) },
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.tuner_strings_low_to_high),
                    style = spaceGrotesk(11.sp, FontWeight.Medium),
                    color = c.text3,
                    modifier = Modifier.weight(1f),
                )
                if (!state.pitchPipeMode) {
                    Text(stringResource(R.string.tuner_auto), style = jakarta(12.sp), color = c.text3)
                    Spacer(Modifier.width(8.dp))
                    AutoSwitch(checked = state.autoDetect, onToggle = { onAction(TunerAction.ToggleAuto) })
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                notes.forEachIndexed { index, note ->
                    val isNow = when {
                        state.pitchPipeMode -> index == (state.pitchPipeString ?: state.selectedString)
                        state.autoDetect -> index == state.activeString
                        else -> index == state.selectedString
                    }
                    StringTile(
                        note = note,
                        number = notes.size - index,
                        isNow = isNow,
                        isTuned = index in state.tunedStrings,
                        onClick = {
                            if (state.pitchPipeMode) onAction(TunerAction.PlayPitchPipe(index)) else onAction(TunerAction.SelectString(index))
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeSelector(playMode: Boolean, onListen: () -> Unit, onPlay: () -> Unit) {
    val c = WakTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(c.s3, RoundedCornerShape(14.dp))
            .border(1.dp, c.line, RoundedCornerShape(14.dp))
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ModeOption(painterResource(R.drawable.ic_mic), stringResource(R.string.tuner_mode_listen), !playMode, onListen, Modifier.weight(1f))
        ModeOption(painterResource(R.drawable.ic_music_note), stringResource(R.string.tuner_mode_play), playMode, onPlay, Modifier.weight(1f))
    }
}

@Composable
private fun ModeOption(icon: Painter, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    val content = if (selected) c.text else c.text3
    Row(
        modifier = modifier
            .fillMaxHeight()
            .background(if (selected) c.s1 else Color.Transparent, RoundedCornerShape(10.dp))
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) c.primaryText else c.text3, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = jakarta(13.sp), color = content)
    }
}

@Composable
private fun StringTile(
    note: String,
    number: Int,
    isNow: Boolean,
    isTuned: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = WakTheme.colors
    val name = noteName(note)
    val description = stringResource(R.string.tuner_string_cd, number, name)
    val container = when {
        isNow -> c.primary
        isTuned -> c.s3
        else -> Color.Transparent
    }
    val border = when {
        isNow -> c.primary
        isTuned -> c.line
        else -> c.outline
    }
    val content = if (isNow) c.onPrimary else c.text
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .semantics { contentDescription = description },
        shape = RoundedCornerShape(18.dp),
        color = container,
        border = BorderStroke(1.dp, border),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(name, style = spaceGrotesk(18.sp).copy(lineHeight = 20.sp), color = content, maxLines = 1)
            Text(
                text = when {
                    isNow -> stringResource(R.string.tuner_now)
                    isTuned -> stringResource(R.string.tuner_tuned)
                    else -> number.toString()
                },
                style = jakarta(10.sp),
                color = content,
                maxLines = 1,
                modifier = Modifier.alpha(0.8f),
            )
        }
    }
}

@Composable
private fun AutoSwitch(checked: Boolean, onToggle: () -> Unit) {
    val c = WakTheme.colors
    val knobOffset by animateDpAsState(if (checked) 23.dp else 3.dp, label = "knob")
    val description = stringResource(R.string.tuner_auto_cd)
    Box(
        modifier = Modifier
            .size(width = 50.dp, height = 30.dp)
            .background(if (checked) c.primary else c.s5, CircleShape)
            .toggleable(value = checked, role = Role.Switch, onValueChange = { onToggle() })
            .semantics { contentDescription = description },
    ) {
        Box(
            Modifier
                .offset { IntOffset(knobOffset.roundToPx(), 3.dp.roundToPx()) }
                .size(24.dp)
                .background(if (checked) c.onPrimary else c.text2, CircleShape),
        )
    }
}

@Composable
private fun MicOffState(onOpenSettings: () -> Unit, onUsePitchPipe: () -> Unit) {
    val c = WakTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp)
            .padding(top = 122.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(c.peachSoft, RoundedCornerShape(32.dp))
                .border(1.dp, c.line, RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_mic_off), contentDescription = null, tint = c.peachText, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.mic_off_title),
            style = spaceGrotesk(20.sp),
            color = c.text,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.mic_off_tuner_message),
            style = MaterialTheme.typography.bodyMedium,
            color = c.text2,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(17.dp))
        WakButton(
            text = stringResource(R.string.open_settings),
            onClick = onOpenSettings,
            modifier = Modifier.widthIn(min = 152.dp),
        )
        Spacer(Modifier.height(12.dp))
        Surface(onClick = onUsePitchPipe, shape = RoundedCornerShape(14.dp), color = Color.Transparent) {
            Text(
                stringResource(R.string.use_pitch_pipe),
                style = spaceGrotesk(14.sp),
                color = c.primaryText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
            )
        }
    }
}

private val inTuneState = TunerUiState(
    micPermission = MicPermission.GRANTED,
    reading = NoteMapper.fromFrequency(109.9f),
    activeString = 1,
    centsOff = -2f,
    status = TuneStatus.IN_TUNE,
    tunedStrings = setOf(0),
)

@Preview(name = "Tuner - in tune", heightDp = 790)
@Composable
private fun TunerInTunePreview() {
    WAKordTheme(darkTheme = true) {
        TunerScreen(inTuneState, {}, {}, {}, unreadNotifications = 3)
    }
}

@Preview(name = "Tuner - flat", heightDp = 790)
@Composable
private fun TunerFlatPreview() {
    WAKordTheme(darkTheme = true) {
        TunerScreen(
            state = TunerUiState(
                micPermission = MicPermission.GRANTED,
                reading = NoteMapper.fromFrequency(145.6f),
                activeString = 2,
                centsOff = -14f,
                status = TuneStatus.FLAT,
                tunedStrings = setOf(0, 1),
            ),
            onAction = {},
            onOpenSettings = {},
            onOpenNotifications = {},
            unreadNotifications = 3,
        )
    }
}

@Preview(name = "Tuner - mic off", heightDp = 790)
@Composable
private fun TunerMicOffPreview() {
    WAKordTheme(darkTheme = true) {
        TunerScreen(TunerUiState(micPermission = MicPermission.DENIED), {}, {}, {}, unreadNotifications = 3)
    }
}

@Preview(name = "Tuner - pitch pipe", heightDp = 790)
@Composable
private fun TunerPitchPipePreview() {
    WAKordTheme(darkTheme = true) {
        TunerScreen(
            TunerUiState(micPermission = MicPermission.DENIED, pitchPipeMode = true, selectedString = 1),
            {},
            {},
            {},
        )
    }
}

@Preview(name = "Tuner - light", heightDp = 790)
@Composable
private fun TunerLightPreview() {
    WAKordTheme(darkTheme = false) {
        TunerScreen(inTuneState, {}, {}, {}, unreadNotifications = 3)
    }
}
