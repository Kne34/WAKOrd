package edu.bluejack26_1.wakord.ui.feature.analyze

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.core.analysis.ChordRecognizer
import edu.bluejack26_1.wakord.core.analysis.RomanNumerals
import edu.bluejack26_1.wakord.core.music.Chord
import edu.bluejack26_1.wakord.core.music.ChordTheory
import edu.bluejack26_1.wakord.core.music.NoteNames
import edu.bluejack26_1.wakord.core.music.VoicingGenerator
import edu.bluejack26_1.wakord.domain.model.ChordTrackBar
import edu.bluejack26_1.wakord.domain.model.ChordVoicing
import edu.bluejack26_1.wakord.domain.model.LiveChordReading
import edu.bluejack26_1.wakord.ui.components.ChordDiagram
import edu.bluejack26_1.wakord.ui.components.PianoKeyboard
import edu.bluejack26_1.wakord.ui.components.SegmentedControl
import edu.bluejack26_1.wakord.ui.components.WakDialog
import edu.bluejack26_1.wakord.ui.components.WakSheetFrame
import edu.bluejack26_1.wakord.ui.components.fingerText
import edu.bluejack26_1.wakord.ui.components.glassBorder
import edu.bluejack26_1.wakord.ui.components.keyLabel
import edu.bluejack26_1.wakord.ui.components.prettyChord
import edu.bluejack26_1.wakord.ui.components.voicingLabel
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakColors
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import edu.bluejack26_1.wakord.ui.theme.jakarta
import edu.bluejack26_1.wakord.ui.theme.spaceGrotesk
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class LiveInstrument { Piano, Guitar, Ukulele }

private val LevelHeights = listOf(8, 14, 20, 24, 18, 12, 16, 10)
private const val VOICING_LIMIT = 6
private const val LEVEL_GAIN = 8f
private val SYMBOL_HEIGHT = 68.dp
private val NAME_HEIGHT = 22.dp
private val ALTERNATIVE_HEIGHT = 18.dp

private data class Tone(val midi: Int, val note: String, val code: String) {
    val isColor: Boolean get() = code in setOf("m3", "M3", "M6", "m7", "M7")
    val badge: String
        get() = when (code) {
            "P5" -> "5th"
            "d5" -> "♭5"
            "A5" -> "♯5"
            "M2" -> "2nd"
            "P4" -> "4th"
            "M6" -> "6th"
            "9" -> "9th"
            else -> code
        }
    val role: String
        get() = when (code) {
            "R" -> ""
            "m3", "M3" -> "3rd"
            "P5", "d5", "A5" -> "5th"
            "m7", "M7" -> "7th"
            else -> badge
        }
}

private fun chordTones(reading: LiveChordReading): List<Tone> =
    reading.notes.mapIndexed { index, note -> Tone(0, note, reading.intervals.getOrElse(index) { "" }) }

private fun keyTones(reading: LiveChordReading?, keys: Set<Int>?): List<Tone> {
    val chord = reading?.let { Chord.parse(it.symbol) }
    val rootPc = chord?.let { NoteNames.pitchClass(it.root) }
    val steps = chord?.let { ChordTheory.intervals(it.suffix).map { interval -> interval % 12 } }.orEmpty()
    val midis = keys?.sorted() ?: reading?.let { VoicingGenerator.pianoNotes(it.symbol) }.orEmpty()
    return midis.map { midi ->
        val index = if (rootPc == null) -1 else steps.indexOf(Math.floorMod(midi - rootPc, 12))
        Tone(
            midi = midi,
            note = reading?.notes?.getOrNull(index) ?: NoteNames.name(midi % 12, preferFlats = false),
            code = reading?.intervals?.getOrNull(index).orEmpty(),
        )
    }
}

private fun prettyRoman(roman: String): String =
    roman.replace(Regex("^b"), "♭").replace(Regex("^#"), "♯")

private fun extensionText(reading: LiveChordReading): String {
    if (reading.quality == "suspended") return reading.symbol.substringAfter("sus", "").take(1)
    return reading.extensions.joinToString(" ") {
        when (it) {
            "9" -> "add 9"
            "maj7" -> if (reading.quality == "major") "7" else "maj 7"
            else -> it.replace("b", "♭").replace("#", "♯")
        }
    }
}

private fun homeKey(reading: LiveChordReading): String =
    reading.root + if (reading.quality == "minor" || reading.quality == "diminished") "m" else ""

@Composable
private fun roleName(tone: Tone): String = if (tone.code == "R") stringResource(R.string.live_root) else tone.role

@Composable
private fun chordName(reading: LiveChordReading): String =
    listOf(prettyChord(reading.root), qualityName(reading), extensionText(reading)).filter { it.isNotBlank() }.joinToString(" ")

@Composable
private fun qualityName(reading: LiveChordReading): String = stringResource(
    when {
        reading.quality == "major" && reading.extensions == listOf("7") -> R.string.live_quality_dominant
        reading.quality == "minor" -> R.string.live_quality_minor
        reading.quality == "diminished" -> R.string.live_quality_diminished
        reading.quality == "augmented" -> R.string.live_quality_augmented
        reading.quality == "suspended" -> R.string.live_quality_suspended
        else -> R.string.live_quality_major
    },
)

@Composable
fun LiveChordContent(
    state: LiveChordUiState,
    onAction: (LiveChordAction) -> Unit,
    isChordSaved: Boolean,
    onPlayNote: (Int) -> Unit,
    onPlayChord: (List<Int>) -> Unit,
    onToggleChordSaved: () -> Unit,
    onOpenProgression: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var dismissedSaved by rememberSaveable(state.savedProgressionId) { mutableStateOf(false) }
    var instrument by rememberSaveable { mutableStateOf(LiveInstrument.Piano) }
    var openBar by rememberSaveable { mutableStateOf<Int?>(null) }
    var lastReading by remember { mutableStateOf(state.shownReading) }
    val reading = state.shownReading
    LaunchedEffect(reading, state.isBuilding) {
        if (reading != null && !state.isBuilding) lastReading = reading
    }
    Column(modifier.padding(horizontal = 20.dp)) {
        StatusCard(state, onAction)
        Spacer(Modifier.height(14.dp))
        DetectedCard(reading, state.trackKey, state.isBuilding)
        Spacer(Modifier.height(12.dp))
        InstrumentCard(
            state = state,
            reading = if (state.isBuilding) reading else reading ?: lastReading,
            instrument = instrument,
            onSelectInstrument = { instrument = it },
            isSaved = isChordSaved,
            onAction = onAction,
            onPlayNote = onPlayNote,
            onPlayChord = onPlayChord,
            onToggleSaved = onToggleChordSaved,
        )
        Spacer(Modifier.height(12.dp))
        TrackCard(state, onAction, onSave = { showSaveDialog = true }, onOpenBar = { openBar = it })
    }

    val barIndex = openBar
    val sheetBar = barIndex?.let { state.bars.getOrNull(it) }
    if (barIndex != null && sheetBar != null) {
        BarSheet(
            index = barIndex,
            bar = sheetBar,
            initialInstrument = instrument,
            onPlayNote = onPlayNote,
            onPlayChord = onPlayChord,
            onClose = { openBar = null },
        )
    }

    if (showSaveDialog) {
        SaveDialog(
            isSaving = state.isSaving,
            onSave = { title -> onAction(LiveChordAction.SaveAsProgression(title)) },
            onDismiss = { showSaveDialog = false },
        )
    }
    val savedId = state.savedProgressionId
    LaunchedEffect(savedId) {
        if (savedId != null) showSaveDialog = false
    }
    if (savedId != null && !dismissedSaved) {
        WakDialog(
            title = stringResource(R.string.result_saved_title),
            message = stringResource(R.string.result_saved_message),
            primaryText = stringResource(R.string.result_open),
            onPrimary = {
                dismissedSaved = true
                onOpenProgression(savedId)
            },
            onDismissRequest = { dismissedSaved = true },
            secondaryText = stringResource(R.string.not_now),
            icon = painterResource(R.drawable.ic_check_circle),
        )
    }
}

@Composable
private fun StatusCard(state: LiveChordUiState, onAction: (LiveChordAction) -> Unit) {
    val c = WakTheme.colors
    val listening = state.isListening
    val title = when {
        state.isBuilding -> R.string.live_building
        listening -> R.string.live_listening
        else -> R.string.live_paused
    }
    val hint = when {
        state.isBuilding -> R.string.live_building_hint
        listening -> R.string.live_hint
        else -> R.string.live_tap_resume
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(20.dp),
        color = c.glass,
        border = glassBorder(c),
    ) {
        Row(Modifier.padding(end = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onAction(LiveChordAction.ToggleListening) }
                    .padding(start = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(9.dp)
                        .background(if (listening) c.orange else c.s5, CircleShape),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(title),
                        style = jakarta(13.sp),
                        color = c.text,
                        maxLines = 1,
                    )
                    Text(
                        stringResource(hint),
                        style = jakarta(12.sp),
                        color = c.text3,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                LevelBars(if (listening) state.inputLevel else 0f)
            }
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.live_lock), style = jakarta(12.sp), color = c.text3)
            Spacer(Modifier.width(6.dp))
            LockSwitch(
                checked = state.isLocked,
                enabled = !state.isBuilding && (state.isLocked || state.reading != null),
                onToggle = { onAction(LiveChordAction.ToggleLock) },
            )
        }
    }
}

@Composable
private fun LevelBars(level: Float) {
    val c = WakTheme.colors
    val lit = ((level * LEVEL_GAIN).coerceIn(0f, 1f) * LevelHeights.size).roundToInt()
    Row(
        modifier = Modifier.height(24.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LevelHeights.forEachIndexed { index, height ->
            Box(
                Modifier
                    .size(width = 3.dp, height = height.dp)
                    .background(if (index < lit) c.primary else c.text.copy(alpha = 0.14f), CircleShape),
            )
        }
    }
}

@Composable
private fun LockSwitch(checked: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    val c = WakTheme.colors
    val description = stringResource(R.string.live_lock_cd)
    Box(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.5f)
            .size(width = 42.dp, height = 26.dp)
            .clip(CircleShape)
            .background(if (checked) c.primary else c.s5)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = { onToggle() })
            .semantics { contentDescription = description }
            .padding(1.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .size(24.dp)
                .background(if (checked) c.onPrimary else c.text3, CircleShape),
        )
    }
}

@Composable
private fun Overline(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = spaceGrotesk(11.sp).copy(letterSpacing = 1.1.sp),
        color = WakTheme.colors.text3,
        modifier = modifier,
        maxLines = 1,
    )
}

@Composable
private fun DetectedCard(reading: LiveChordReading?, trackKey: String?, building: Boolean) {
    val c = WakTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = c.glass,
        border = glassBorder(c),
    ) {
        Column(Modifier.padding(19.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Overline(stringResource(if (building) R.string.live_built else R.string.live_detected), Modifier.weight(1f))
                if (reading != null && !building) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(c.primarySoft, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(R.string.live_match, reading.matchPercent), style = spaceGrotesk(11.sp), color = c.primaryText)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (reading == null) {
                Text("—", style = spaceGrotesk(56.sp).copy(lineHeight = 60.sp), color = c.text3, maxLines = 1, modifier = Modifier.height(SYMBOL_HEIGHT))
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(if (building) R.string.live_pick_keys else R.string.live_waiting),
                    style = jakarta(14.sp),
                    color = c.text2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.height(NAME_HEIGHT),
                )
                Spacer(Modifier.height(4.dp))
                Spacer(Modifier.height(ALTERNATIVE_HEIGHT))
                Spacer(Modifier.height(16.dp))
                Spacer(Modifier.height(58.dp))
                return@Column
            }
            Text(
                prettyChord(reading.symbol),
                style = spaceGrotesk(56.sp).copy(lineHeight = 60.sp),
                color = c.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(SYMBOL_HEIGHT),
            )
            Spacer(Modifier.height(4.dp))
            val name = chordName(reading)
            Text(
                if (trackKey == null) name else stringResource(R.string.live_description_in, name, prettyRoman(RomanNumerals.of(reading.symbol, trackKey)), keyLabel(trackKey)),
                style = jakarta(14.sp),
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(NAME_HEIGHT),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                reading.alternative?.let { stringResource(R.string.live_also_reads, prettyChord(it)) }.orEmpty(),
                style = jakarta(12.sp),
                color = c.text3,
                maxLines = 1,
                modifier = Modifier.height(ALTERNATIVE_HEIGHT),
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                chordTones(reading).forEach { tone -> ToneChip(tone, Modifier.weight(1f, fill = false)) }
            }
        }
    }
}

@Composable
private fun ToneChip(tone: Tone, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    val root = tone.code == "R"
    val extension = !root && !tone.isColor && tone.role !in setOf("5th", "")
    Column(
        modifier = modifier
            .widthIn(max = 58.dp)
            .fillMaxWidth()
            .height(58.dp)
            .background(if (root) c.primary else c.s3, RoundedCornerShape(16.dp))
            .border(1.dp, c.line, RoundedCornerShape(16.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            prettyChord(tone.note),
            style = spaceGrotesk(17.sp),
            color = when {
                root -> c.onPrimary
                extension -> c.peachText
                else -> c.text
            },
            maxLines = 1,
        )
        Text(
            if (root) stringResource(R.string.live_root) else tone.badge,
            style = jakarta(11.sp),
            color = if (root) c.onPrimary.copy(alpha = 0.72f) else c.text3,
            maxLines = 1,
        )
    }
}

@Composable
private fun InstrumentCard(
    state: LiveChordUiState,
    reading: LiveChordReading?,
    instrument: LiveInstrument,
    onSelectInstrument: (LiveInstrument) -> Unit,
    isSaved: Boolean,
    onAction: (LiveChordAction) -> Unit,
    onPlayNote: (Int) -> Unit,
    onPlayChord: (List<Int>) -> Unit,
    onToggleSaved: () -> Unit,
) {
    val c = WakTheme.colors
    val building = state.isBuilding
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = c.s2,
        border = BorderStroke(1.dp, c.line),
    ) {
        Column(Modifier.padding(start = 15.dp, end = 15.dp, top = 13.dp, bottom = 15.dp)) {
            InstrumentPanel(
                reading = reading,
                keys = if (building) state.builtNotes else null,
                instrument = instrument,
                onSelectInstrument = onSelectInstrument,
                onPlayChord = onPlayChord,
                piano = { tones ->
                    PianoView(
                        tones = tones,
                        building = building,
                        onKeyTap = { midi -> if (building) onAction(LiveChordAction.ToggleKey(midi)) else onPlayNote(midi) },
                        onStartBuilding = { onAction(LiveChordAction.StartBuilding) },
                        onClear = { onAction(LiveChordAction.ClearKeys) },
                        onDone = { onAction(LiveChordAction.FinishBuilding) },
                    )
                },
            ) {
                CardButton(
                    text = stringResource(if (isSaved) R.string.live_saved_chord else R.string.live_save_chord),
                    icon = if (isSaved) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark,
                    active = isSaved,
                    enabled = reading != null,
                    onClick = onToggleSaved,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun InstrumentPanel(
    reading: LiveChordReading?,
    keys: Set<Int>?,
    instrument: LiveInstrument,
    onSelectInstrument: (LiveInstrument) -> Unit,
    onPlayChord: (List<Int>) -> Unit,
    piano: @Composable (List<Tone>) -> Unit,
    extraButton: @Composable RowScope.() -> Unit = {},
) {
    val tones = remember(reading, keys) { keyTones(reading, keys) }
    val tuning = if (instrument == LiveInstrument.Ukulele) VoicingGenerator.UKULELE_STANDARD else VoicingGenerator.GUITAR_STANDARD
    val symbol = reading?.symbol
    val request = symbol to instrument
    val computed by produceState<Pair<Pair<String?, LiveInstrument>, List<ChordVoicing>>?>(null, request) {
        val found = if (instrument == LiveInstrument.Piano || symbol == null) {
            emptyList()
        } else {
            withContext(Dispatchers.Default) { VoicingGenerator.voicings(symbol, tuning, VOICING_LIMIT) }
        }
        value = request to found
    }
    val ready = computed?.first == request
    val voicings = if (ready) computed?.second.orEmpty() else emptyList()
    var voicingIndex by rememberSaveable(symbol, instrument) { mutableIntStateOf(0) }
    val voicing = voicings.getOrNull(voicingIndex % voicings.size.coerceAtLeast(1))
    SegmentedControl(
        labels = listOf(stringResource(R.string.live_piano), stringResource(R.string.live_guitar), stringResource(R.string.live_ukulele)),
        selectedIndex = instrument.ordinal,
        onSelect = { onSelectInstrument(LiveInstrument.entries[it]) },
        description = stringResource(R.string.live_show_on),
    )
    Spacer(Modifier.height(14.dp))
    if (instrument == LiveInstrument.Piano) {
        piano(tones)
    } else {
        FretView(reading, voicing, ready, instrument, tuning, canCycle = voicings.size > 1, onMore = { voicingIndex = (voicingIndex + 1) % voicings.size })
    }
    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CardButton(
            text = stringResource(R.string.live_hear),
            icon = R.drawable.ic_play,
            active = false,
            enabled = tones.isNotEmpty(),
            onClick = {
                val midis = if (voicing != null && instrument != LiveInstrument.Piano) {
                    VoicingGenerator.midiNotes(voicing, tuning)
                } else {
                    tones.map { it.midi }
                }
                onPlayChord(midis)
            },
            modifier = Modifier.weight(1f),
        )
        extraButton()
    }
}

@Composable
private fun BarSheet(
    index: Int,
    bar: ChordTrackBar,
    initialInstrument: LiveInstrument,
    onPlayNote: (Int) -> Unit,
    onPlayChord: (List<Int>) -> Unit,
    onClose: () -> Unit,
) {
    val c = WakTheme.colors
    val reading = remember(bar.symbol) { ChordRecognizer.readingOf(bar.symbol) }
    var instrument by rememberSaveable(index) { mutableStateOf(initialInstrument) }
    WakSheetFrame(onDismiss = onClose) {
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
                    Text(
                        listOf(stringResource(R.string.track_bar, index + 1), prettyRoman(bar.roman)).filter { it.isNotBlank() }.joinToString(" · "),
                        style = jakarta(12.sp),
                        color = c.text3,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(prettyChord(bar.symbol), style = spaceGrotesk(40.sp).copy(lineHeight = 44.sp), color = c.text, maxLines = 1)
                    if (reading != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(chordName(reading), style = jakarta(14.sp), color = c.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Surface(
                    onClick = onClose,
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = c.s3,
                    border = BorderStroke(1.dp, c.line),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.close), tint = c.text, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            InstrumentPanel(
                reading = reading,
                keys = null,
                instrument = instrument,
                onSelectInstrument = { instrument = it },
                onPlayChord = onPlayChord,
                piano = { tones -> PianoView(tones = tones, building = false, onKeyTap = onPlayNote, onStartBuilding = null) },
            )
        }
    }
}

@Composable
private fun PianoView(
    tones: List<Tone>,
    building: Boolean,
    onKeyTap: (Int) -> Unit,
    onStartBuilding: (() -> Unit)?,
    onClear: () -> Unit = {},
    onDone: () -> Unit = {},
) {
    val c = WakTheme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.live_piano_range), style = spaceGrotesk(12.sp), color = c.text)
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(R.string.live_keys),
            style = spaceGrotesk(11.sp),
            color = c.text2,
            modifier = Modifier
                .background(c.s4, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
        Spacer(Modifier.weight(1f))
        Text(pluralStringResource(R.plurals.live_notes_voiced, tones.size, tones.size), style = jakarta(12.sp), color = c.primaryText)
    }
    Spacer(Modifier.height(10.dp))
    PianoKeyboard(
        highlighted = tones.map { it.midi }.toSet(),
        intervals = tones.filter { it.code.isNotEmpty() }.associate { it.midi to if (it.code == "R") "R" else it.badge },
        keyColors = tones.associate { it.midi to toneColor(it, c) },
        onKeyTap = onKeyTap,
    )
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (building) {
            Text(
                stringResource(R.string.live_build_hint),
                style = jakarta(12.sp),
                color = c.text3,
                maxLines = 2,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            ChipButton(stringResource(R.string.live_clear), primary = false, onClick = onClear)
            Spacer(Modifier.width(6.dp))
            ChipButton(stringResource(R.string.live_done), primary = true, onClick = onDone)
        } else {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val named = tones.filter { it.code.isNotEmpty() }
                val (color, core) = named.partition { it.isColor }
                if (core.isNotEmpty()) Legend(c.primary, core.map { roleName(it) }.distinct().joinToString(" · "))
                if (color.isNotEmpty()) Legend(c.peach, color.map { roleName(it) }.distinct().joinToString(" · "))
            }
            if (onStartBuilding != null) {
                Spacer(Modifier.width(8.dp))
                ChipButton(stringResource(R.string.live_build), primary = false, icon = R.drawable.ic_plus, onClick = onStartBuilding)
            }
        }
    }
}

@Composable
private fun ChipButton(text: String, primary: Boolean, onClick: () -> Unit, icon: Int? = null) {
    val c = WakTheme.colors
    val content = if (primary) c.onPrimary else c.text
    Surface(
        onClick = onClick,
        modifier = Modifier.height(32.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (primary) c.primary else c.s3,
        border = if (primary) null else BorderStroke(1.dp, c.line),
    ) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(text, style = jakarta(12.sp), color = content, maxLines = 1)
        }
    }
}

private fun toneColor(tone: Tone, c: WakColors): Color = when {
    tone.code.isEmpty() -> c.lavender
    tone.isColor -> c.peach
    else -> c.primary
}

@Composable
private fun Legend(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(3.dp)),
        )
        Spacer(Modifier.width(6.dp))
        Text(text, style = jakarta(12.sp), color = WakTheme.colors.text3, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun FretView(
    reading: LiveChordReading?,
    voicing: ChordVoicing?,
    ready: Boolean,
    instrument: LiveInstrument,
    tuning: List<Int>,
    canCycle: Boolean,
    onMore: () -> Unit,
) {
    val c = WakTheme.colors
    if (reading == null) {
        Text(
            stringResource(R.string.live_waiting),
            style = jakarta(14.sp),
            color = c.text2,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 60.dp),
        )
        return
    }
    if (!ready) {
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(171.dp),
        )
        return
    }
    if (voicing == null) {
        Text(
            stringResource(R.string.live_no_voicing),
            style = jakarta(14.sp),
            color = c.text2,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 60.dp),
        )
        return
    }
    val sounding = VoicingGenerator.midiNotes(voicing, tuning).map { it % 12 }.toSet()
    val missing = chordTones(reading).filter { NoteNames.pitchClass(it.note) !in sounding }
    Row(verticalAlignment = Alignment.CenterVertically) {
        ChordDiagram(
            frets = voicing.frets,
            fingers = voicing.fingers,
            baseFret = voicing.baseFret,
            chordName = reading.symbol,
            width = 133.dp,
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Overline(stringResource(if (instrument == LiveInstrument.Ukulele) R.string.live_play_on_ukulele else R.string.live_play_on_guitar))
            Spacer(Modifier.height(2.dp))
            Text(stringResource(R.string.live_voicing, voicingLabel(voicing)), style = spaceGrotesk(16.sp), color = c.text)
            Spacer(Modifier.height(2.dp))
            Text(
                if (missing.isEmpty()) {
                    stringResource(R.string.live_all_notes)
                } else {
                    stringResource(R.string.live_left_out, missing.map { "${roleName(it)} (${prettyChord(it.note)})" }.joinToString(", "))
                },
                style = jakarta(12.sp),
                color = c.text3,
            )
            Spacer(Modifier.height(10.dp))
            Overline(stringResource(R.string.live_fingers))
            Spacer(Modifier.height(2.dp))
            Text(fingerText(voicing), style = spaceGrotesk(13.sp, FontWeight.Medium), color = c.text2)
            if (canCycle) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    onClick = onMore,
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = c.s3,
                    border = BorderStroke(1.dp, c.line),
                ) {
                    Row(Modifier.padding(horizontal = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.live_more_voicings), style = jakarta(12.sp), color = c.text)
                        Spacer(Modifier.width(8.dp))
                        Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = c.text, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CardButton(text: String, icon: Int, active: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    val content = if (active) c.onPrimary else c.text
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .alpha(if (enabled) 1f else 0.5f)
            .height(42.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (active) c.primary else c.s3,
        border = BorderStroke(1.dp, if (active) c.primary else c.outline),
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(10.dp))
            Text(text, style = jakarta(14.sp), color = content, maxLines = 1)
        }
    }
}

@Composable
private fun TrackCard(state: LiveChordUiState, onAction: (LiveChordAction) -> Unit, onSave: () -> Unit, onOpenBar: (Int) -> Unit) {
    val c = WakTheme.colors
    val reading = state.shownReading
    val hasBars = state.bars.isNotEmpty()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = c.s2,
        border = BorderStroke(1.dp, c.line),
    ) {
        Column(Modifier.padding(horizontal = 17.dp, vertical = 15.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.track_title),
                    style = spaceGrotesk(16.sp),
                    color = c.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                StepButton(R.drawable.ic_minus, stringResource(R.string.track_slower_cd), state.bpm > MIN_TRACK_BPM) {
                    onAction(LiveChordAction.SetBpm(state.bpm - BPM_STEP))
                }
                Text(
                    stringResource(R.string.track_meta, state.bpm, state.bars.size, MAX_TRACK_BARS),
                    style = spaceGrotesk(12.sp),
                    color = c.text3,
                    maxLines = 1,
                )
                StepButton(R.drawable.ic_plus, stringResource(R.string.track_faster_cd), state.bpm < MAX_TRACK_BPM) {
                    onAction(LiveChordAction.SetBpm(state.bpm + BPM_STEP))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.bars.forEachIndexed { index, bar ->
                    BarCard(
                        index = index,
                        bar = bar,
                        playing = state.playingBar == index,
                        onOpen = { onOpenBar(index) },
                        onRemove = { onAction(LiveChordAction.RemoveBar(index)) },
                    )
                }
                if (reading != null) {
                    val key = state.trackKey ?: homeKey(reading)
                    TrackTile(
                        label = stringResource(R.string.track_now),
                        value = prettyChord(reading.symbol),
                        caption = prettyRoman(RomanNumerals.of(reading.symbol, key)),
                        container = c.primarySoft,
                        border = c.primary,
                        valueColor = c.primaryText,
                    )
                }
                val canAdd = state.canAddBar && reading != null
                TrackTile(
                    label = stringResource(R.string.track_add),
                    value = "+",
                    caption = stringResource(R.string.track_add_hint),
                    container = Color.Transparent,
                    border = c.outline,
                    valueColor = c.text3,
                    modifier = Modifier
                        .alpha(if (canAdd) 1f else 0.5f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(enabled = canAdd) { reading?.let { onAction(LiveChordAction.AddBar(it.symbol)) } },
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(if (hasBars) R.string.track_tap_bar else R.string.track_empty),
                style = jakarta(12.sp),
                color = c.text3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = { onAction(LiveChordAction.PlayTrack) },
                    enabled = hasBars,
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = if (hasBars) c.primary else c.primary.copy(alpha = 0.4f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painterResource(if (state.isPlayingTrack) R.drawable.ic_stop else R.drawable.ic_play),
                            contentDescription = stringResource(if (state.isPlayingTrack) R.string.track_stop_cd else R.string.track_play_cd),
                            tint = c.onPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Surface(
                    checked = state.isLooping,
                    onCheckedChange = { onAction(LiveChordAction.ToggleLoop) },
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = if (state.isLooping) c.primary.copy(alpha = 0.16f) else c.s3,
                    border = BorderStroke(1.dp, if (state.isLooping) c.primary else c.line),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painterResource(R.drawable.ic_loop),
                            contentDescription = stringResource(R.string.track_loop_cd),
                            tint = if (state.isLooping) c.primaryText else c.text,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                val canSave = hasBars && state.trackKey != null
                Surface(
                    onClick = onSave,
                    enabled = canSave,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = if (canSave) c.primary else c.primary.copy(alpha = 0.4f),
                ) {
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_bookmark), contentDescription = null, tint = c.onPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.result_save), style = spaceGrotesk(14.sp), color = c.onPrimary, maxLines = 1)
                    }
                }
            }
        }
    }
}

private const val BPM_STEP = 4

@Composable
private fun StepButton(icon: Int, description: String, enabled: Boolean, onClick: () -> Unit) {
    val c = WakTheme.colors
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.4f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = description, tint = c.text2, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun BarCard(index: Int, bar: ChordTrackBar, playing: Boolean, onOpen: () -> Unit, onRemove: () -> Unit) {
    val c = WakTheme.colors
    Box {
        TrackTile(
            label = stringResource(R.string.track_bar, index + 1),
            value = prettyChord(bar.symbol),
            caption = prettyRoman(bar.roman),
            container = if (playing) c.primarySoft else c.s3,
            border = if (playing) c.primary else c.line,
            valueColor = if (playing) c.primaryText else c.text,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClickLabel = stringResource(R.string.track_show_shape), onClick = onOpen),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(28.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.track_remove_cd, index + 1),
                tint = c.text3,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

@Composable
private fun TrackTile(
    label: String,
    value: String,
    caption: String,
    container: Color,
    border: Color,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    val c = WakTheme.colors
    Column(
        modifier = modifier
            .size(width = 73.dp, height = 74.dp)
            .background(container, RoundedCornerShape(16.dp))
            .border(1.dp, border, RoundedCornerShape(16.dp))
            .padding(horizontal = 9.dp, vertical = 9.dp),
    ) {
        Text(label, style = jakarta(10.sp), color = c.text3, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        Text(value, style = spaceGrotesk(15.sp), color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(caption, style = jakarta(10.sp), color = c.text3, maxLines = 1)
    }
}

private val previewReading = LiveChordReading(
    symbol = "F#m7",
    root = "F#",
    quality = "minor",
    extensions = listOf("7"),
    notes = listOf("F#", "A", "C#", "E"),
    intervals = listOf("R", "m3", "P5", "m7"),
    matchPercent = 92,
    alternative = "A6",
)

@Preview(name = "Live chords", heightDp = 1200)
@Composable
private fun LiveChordPreview() {
    WAKordTheme(darkTheme = true) {
        Box(Modifier.background(WakTheme.colors.bg)) {
            LiveChordContent(
                state = LiveChordUiState(
                    isListening = true,
                    inputLevel = 0.15f,
                    reading = previewReading,
                    bars = listOf(ChordTrackBar("Bm7", "iv"), ChordTrackBar("E7", "VII")),
                    trackKey = "Bm",
                ),
                onAction = {},
                isChordSaved = false,
                onPlayNote = {},
                onPlayChord = {},
                onToggleChordSaved = {},
                onOpenProgression = {},
            )
        }
    }
}
