package edu.bluejack26_1.wakord.ui.feature.training

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.core.audio.NoteMapper
import edu.bluejack26_1.wakord.domain.model.DailyWorkout
import edu.bluejack26_1.wakord.domain.model.LevelProgress
import edu.bluejack26_1.wakord.domain.model.LevelStatus
import edu.bluejack26_1.wakord.domain.model.PitchReading
import edu.bluejack26_1.wakord.domain.model.PitchStatus
import edu.bluejack26_1.wakord.ui.components.WakButton
import edu.bluejack26_1.wakord.ui.components.glassBorder
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import edu.bluejack26_1.wakord.ui.theme.jakarta
import edu.bluejack26_1.wakord.ui.theme.spaceGrotesk
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

val ReferenceNotes = listOf(60, 64, 69, 72)

private const val TRACE_POINTS = 48
private const val TRACE_RANGE_CENTS = 50f
private const val TRACE_MAX_CENTS = 200

@Composable
fun Overline(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = spaceGrotesk(11.sp).copy(letterSpacing = 1.1.sp, lineHeight = 14.sp),
        color = WakTheme.colors.text3,
        modifier = modifier,
    )
}

@Composable
fun StatusTag(text: String, container: Color, content: Color, modifier: Modifier = Modifier, leading: @Composable (() -> Unit)? = null) {
    Row(
        modifier = modifier
            .height(24.dp)
            .background(container, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(5.dp))
        }
        Text(text, style = spaceGrotesk(11.sp), color = content)
    }
}

@Composable
fun WorkoutCard(workout: DailyWorkout, onResume: () -> Unit, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    val progress = if (workout.goalMinutes > 0) (workout.minutesToday.toFloat() / workout.goalMinutes).coerceIn(0f, 1f) else 0f
    val left = (workout.goalMinutes - workout.minutesToday).coerceAtLeast(0)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = c.glass,
        border = glassBorder(c),
    ) {
        Column(Modifier.padding(19.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(88.dp)) {
                        val stroke = 10.dp.toPx()
                        val inset = stroke / 2
                        val arc = Size(size.width - stroke, size.height - stroke)
                        drawArc(c.s5, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
                        if (progress > 0f) {
                            drawArc(c.primary, -90f, 360f * progress, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.training_minutes_value, workout.minutesToday, workout.goalMinutes),
                            style = spaceGrotesk(24.sp),
                            color = c.text,
                        )
                        Text(stringResource(R.string.training_min), style = jakarta(12.sp), color = c.text3)
                    }
                }
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Overline(stringResource(R.string.training_daily_workout))
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (left > 0) pluralStringResource(R.plurals.training_minutes_to_go, left, left) else stringResource(R.string.training_goal_done),
                        style = spaceGrotesk(17.sp),
                        color = c.text,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        WorkoutStat(stringResource(R.string.training_percent, (workout.accuracyToday * 100).roundToInt()), stringResource(R.string.training_accuracy))
                        WorkoutStat(workout.trialsToday.toString(), stringResource(R.string.training_trials))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            WakButton(
                text = stringResource(R.string.training_resume),
                onClick = onResume,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = painterResource(R.drawable.ic_play),
            )
        }
    }
}

@Composable
private fun WorkoutStat(value: String, label: String) {
    val c = WakTheme.colors
    Column {
        Text(value, style = spaceGrotesk(18.sp), color = c.text)
        Text(label, style = jakarta(12.sp), color = c.text3)
    }
}

@Composable
fun PitchMonitorCard(
    state: PitchMonitorUiState,
    onAction: (PitchMonitorAction) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = WakTheme.colors
    val target = NoteMapper.pretty(NoteMapper.labelOf(state.referenceMidi))
    val targetHz = NoteMapper.frequencyOf(state.referenceMidi, state.referencePitch)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = c.s2,
        border = BorderStroke(1.dp, c.line),
    ) {
        Column(Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.pitch_monitor_title), style = spaceGrotesk(16.sp), color = c.text, modifier = Modifier.weight(1f))
                LiveToggle(isListening = state.isListening, onToggle = { onAction(PitchMonitorAction.ToggleListening) })
            }
            Spacer(Modifier.height(14.dp))
            if (state.microphoneOff) {
                MicOffNotice(onOpenSettings)
            } else {
                PitchTrace(
                    reading = state.reading,
                    isListening = state.isListening,
                    label = stringResource(R.string.pitch_target, target, String.format(Locale.US, "%.0f", targetHz)),
                )
                Spacer(Modifier.height(14.dp))
                SungNoteRow(state.reading, state.isListening)
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Overline(stringResource(R.string.pitch_reference_note), Modifier.weight(1f))
                Surface(
                    onClick = { onAction(PitchMonitorAction.PlayReference) },
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = c.s3,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painterResource(R.drawable.ic_music_note),
                            contentDescription = stringResource(R.string.pitch_play_reference),
                            tint = c.primaryText,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            ReferenceSelector(selected = state.referenceMidi, onSelect = { onAction(PitchMonitorAction.SelectReference(it)) })
        }
    }
}

@Composable
private fun LiveToggle(isListening: Boolean, onToggle: () -> Unit) {
    val c = WakTheme.colors
    val cd = stringResource(if (isListening) R.string.pitch_stop_cd else R.string.pitch_start_cd)
    Box(
        modifier = Modifier
            .semantics { contentDescription = cd }
            .clickable(role = Role.Switch, onClick = onToggle)
            .padding(vertical = 6.dp),
    ) {
        StatusTag(
            text = stringResource(if (isListening) R.string.pitch_live else R.string.pitch_start),
            container = if (isListening) c.primarySoft else c.s3,
            content = if (isListening) c.primaryText else c.text3,
            leading = {
                Box(
                    Modifier
                        .size(7.dp)
                        .background(if (isListening) c.primary else c.text3, CircleShape),
                )
            },
        )
    }
}

@Composable
private fun PitchTrace(reading: PitchReading?, isListening: Boolean, label: String) {
    val c = WakTheme.colors
    val points = remember { mutableStateListOf<Float?>() }
    LaunchedEffect(reading, isListening) {
        if (!isListening) {
            points.clear()
        } else {
            points.add(reading?.cents?.takeIf { abs(it) <= TRACE_MAX_CENTS }?.toFloat())
            while (points.size > TRACE_POINTS) points.removeAt(0)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .background(c.s1, RoundedCornerShape(16.dp))
            .border(1.dp, c.line, RoundedCornerShape(16.dp)),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val mid = size.height / 2
            drawLine(c.primary.copy(alpha = 0.5f), Offset(0f, mid), Offset(size.width, mid), strokeWidth = 1.dp.toPx())
            if (points.size > 1) {
                val step = size.width / (TRACE_POINTS - 1)
                val start = TRACE_POINTS - points.size
                val path = Path()
                var drawing = false
                points.forEachIndexed { i, cents ->
                    if (cents == null) {
                        drawing = false
                    } else {
                        val x = (start + i) * step
                        val y = mid - (cents.coerceIn(-TRACE_RANGE_CENTS, TRACE_RANGE_CENTS) / TRACE_RANGE_CENTS) * (mid - 6.dp.toPx())
                        if (drawing) path.lineTo(x, y) else path.moveTo(x, y)
                        drawing = true
                    }
                }
                drawPath(path, c.orange, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        Text(
            label,
            style = spaceGrotesk(12.sp, FontWeight.Medium),
            color = c.primaryText,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 7.dp, end = 10.dp)
                .background(c.s1, RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun SungNoteRow(reading: PitchReading?, isListening: Boolean) {
    val c = WakTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.pitch_you_are_singing), style = jakarta(12.sp), color = c.text3)
            Spacer(Modifier.height(1.dp))
            if (reading?.sungNote != null) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(NoteMapper.pretty(reading.sungNote), style = spaceGrotesk(20.sp), color = c.text)
                    reading.cents?.let { cents ->
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.pitch_cents, if (cents > 0) "+$cents" else cents.toString()),
                            style = spaceGrotesk(14.sp),
                            color = if (reading.status == PitchStatus.ON_PITCH) c.primaryText else c.peachText,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                }
            } else {
                Text(
                    stringResource(if (isListening) R.string.pitch_no_signal else R.string.pitch_tap_start),
                    style = jakarta(14.sp),
                    color = c.text2,
                )
            }
        }
        when (reading?.status) {
            PitchStatus.ON_PITCH -> StatusTag(stringResource(R.string.pitch_on), c.primarySoft, c.primaryText)
            PitchStatus.FLAT -> StatusTag(stringResource(R.string.pitch_flat), c.peachSoft, c.peachText)
            PitchStatus.SHARP -> StatusTag(stringResource(R.string.pitch_sharp), c.peachSoft, c.peachText)
            else -> Unit
        }
    }
}

@Composable
private fun MicOffNotice(onOpenSettings: () -> Unit) {
    val c = WakTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.s1, RoundedCornerShape(16.dp))
            .border(1.dp, c.line, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(c.peachSoft, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_mic_off), contentDescription = null, tint = c.peachText, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.mic_off_title), style = spaceGrotesk(16.sp), color = c.text)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.pitch_mic_off_message), style = jakarta(13.sp), color = c.text2)
        Spacer(Modifier.height(12.dp))
        WakButton(text = stringResource(R.string.open_settings), onClick = onOpenSettings)
    }
}

@Composable
private fun ReferenceSelector(selected: Int, onSelect: (Int) -> Unit) {
    val c = WakTheme.colors
    var showSheet by rememberSaveable { mutableStateOf(false) }
    val custom = selected !in ReferenceNotes
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(c.s3, RoundedCornerShape(16.dp))
            .border(1.dp, c.line, RoundedCornerShape(16.dp))
            .padding(5.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ReferenceNotes.forEach { midi ->
            ReferenceSegment(NoteMapper.labelOf(midi), midi == selected, onClick = { onSelect(midi) })
        }
        ReferenceSegment(
            label = if (custom) NoteMapper.pretty(NoteMapper.labelOf(selected)) else stringResource(R.string.pitch_more),
            selected = custom,
            onClick = { showSheet = true },
        )
    }
    if (showSheet) {
        ReferenceNoteSheet(
            selected = selected,
            onSelect = {
                onSelect(it)
                showSheet = false
            },
            onClose = { showSheet = false },
        )
    }
}

@Composable
private fun RowScope.ReferenceSegment(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = WakTheme.colors
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .background(if (selected) c.s1 else Color.Transparent, RoundedCornerShape(12.dp))
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = jakarta(13.sp), color = if (selected) c.text else c.text3, maxLines = 1)
    }
}

@Composable
fun CurriculumHeader(mastered: Int, total: Int, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.curriculum_title), style = spaceGrotesk(20.sp), color = c.text, modifier = Modifier.weight(1f))
        Text(pluralStringResource(R.plurals.curriculum_mastered, total, mastered, total), style = jakarta(12.sp), color = c.text3)
    }
}

@Composable
fun LevelCard(progress: LevelProgress, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    val number = progress.level.number
    val title = stringResource(progress.level.titleRes)
    val inProgress = progress.status == LevelStatus.IN_PROGRESS
    Surface(
        onClick = onOpen,
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (progress.status == LevelStatus.LOCKED) 0.6f else 1f),
        shape = RoundedCornerShape(22.dp),
        color = c.s2,
        border = BorderStroke(1.dp, if (inProgress) c.primary else c.line),
    ) {
        Column(Modifier.padding(horizontal = 17.dp, vertical = 15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LevelIcon(progress.status)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when (progress.status) {
                            LevelStatus.MASTERED -> stringResource(R.string.level_number, number)
                            LevelStatus.IN_PROGRESS -> stringResource(R.string.level_in_progress, number)
                            LevelStatus.UNLOCKED -> stringResource(R.string.level_unlocked, number)
                            LevelStatus.LOCKED -> stringResource(R.string.level_locked, number)
                        },
                        style = jakarta(12.sp),
                        color = c.text3,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(title, style = spaceGrotesk(16.sp), color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (progress.status == LevelStatus.LOCKED && number > 1) {
                        Spacer(Modifier.height(2.dp))
                        Text(stringResource(R.string.level_unlock_hint, number - 1), style = jakarta(12.sp), color = c.text3)
                    }
                }
                Spacer(Modifier.width(10.dp))
                when (progress.status) {
                    LevelStatus.MASTERED -> StatusTag(
                        text = stringResource(R.string.level_mastered),
                        container = c.primarySoft,
                        content = c.primaryText,
                        leading = { Icon(painterResource(R.drawable.ic_star), contentDescription = null, tint = c.primaryText, modifier = Modifier.size(11.dp)) },
                    )
                    LevelStatus.IN_PROGRESS -> Surface(
                        onClick = onOpen,
                        shape = RoundedCornerShape(14.dp),
                        color = c.primary,
                    ) {
                        Text(
                            stringResource(R.string.level_continue),
                            style = spaceGrotesk(14.sp),
                            color = c.onPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                    }
                    LevelStatus.UNLOCKED -> Text(
                        stringResource(R.string.level_start),
                        style = spaceGrotesk(14.sp),
                        color = c.primaryText,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                    )
                    LevelStatus.LOCKED -> Unit
                }
            }
            if (inProgress) {
                val done = (progress.bestAccuracy * 10).roundToInt().coerceIn(0, 10)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(c.s5, CircleShape),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(done / 10f)
                                .height(4.dp)
                                .background(c.primary, CircleShape),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.level_progress, done), style = spaceGrotesk(12.sp, FontWeight.Medium), color = c.text3)
                }
            }
        }
    }
}

@Composable
private fun LevelIcon(status: LevelStatus) {
    val c = WakTheme.colors
    val (icon: Painter, tint: Color) = when (status) {
        LevelStatus.MASTERED -> painterResource(R.drawable.ic_check) to c.onPrimary
        LevelStatus.IN_PROGRESS -> painterResource(R.drawable.ic_ear) to c.primaryText
        LevelStatus.UNLOCKED -> painterResource(R.drawable.ic_music_note) to c.text
        LevelStatus.LOCKED -> painterResource(R.drawable.ic_lock) to c.text
    }
    val mastered = status == LevelStatus.MASTERED
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(if (mastered) c.primary else c.s3, RoundedCornerShape(14.dp))
            .then(if (mastered) Modifier else Modifier.border(1.dp, c.line, RoundedCornerShape(14.dp))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(if (mastered) 18.dp else 20.dp))
    }
}
