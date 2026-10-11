package edu.bluejack26_1.wakord.ui.feature.analyze

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.domain.model.AnalysisResult
import edu.bluejack26_1.wakord.domain.model.AnalysisSource
import edu.bluejack26_1.wakord.domain.model.DetectedChord
import edu.bluejack26_1.wakord.ui.components.BackgroundArt
import edu.bluejack26_1.wakord.ui.components.NotificationBell
import edu.bluejack26_1.wakord.ui.components.WakButton
import edu.bluejack26_1.wakord.ui.components.alertContainer
import edu.bluejack26_1.wakord.ui.components.prettyChord
import edu.bluejack26_1.wakord.ui.components.relativeTime
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import edu.bluejack26_1.wakord.ui.theme.jakarta
import edu.bluejack26_1.wakord.ui.theme.spaceGrotesk
import java.util.Locale

private val OnOrange = Color(0xFF2B0C00)
private const val LEVEL_DOTS = 28
private const val PREVIEW_COUNT = 3

fun formatClock(ms: Long): String {
    val seconds = (ms / 1000).coerceAtLeast(0)
    return String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60)
}

@Composable
fun chordLine(result: AnalysisResult): String = result.uniqueChords.joinToString(" · ") { prettyChord(it) }

@Composable
fun AnalyzeScreen(
    state: AnalyzeUiState,
    onAction: (AnalyzeAction) -> Unit,
    microphoneOff: Boolean,
    unreadNotifications: Int,
    onToggleRecording: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = WakTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg),
    ) {
        BackgroundArt(R.drawable.bg_training, height = 640.dp)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.analyze_title), style = spaceGrotesk(26.sp), color = c.text, modifier = Modifier.weight(1f))
                NotificationBell(unreadCount = unreadNotifications, onClick = onOpenNotifications)
            }
            ModeSelector(Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.analyze_record_intro),
                style = jakarta(14.sp),
                color = c.text2,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            if (microphoneOff) {
                MicOffBlock(onOpenSettings)
            } else {
                RecordBlock(state, onToggleRecording)
                RecentAnalyses(state.history, onOpen = { onAction(AnalyzeAction.OpenHistory(it)) })
            }
        }
    }
}

@Composable
private fun ModeSelector(modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(c.s3, RoundedCornerShape(16.dp))
            .border(1.dp, c.text2.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .padding(5.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ModeOption(painterResource(R.drawable.ic_mic), stringResource(R.string.analyze_mode_record), selected = true, enabled = true, Modifier.weight(1f))
        ModeOption(painterResource(R.drawable.ic_nav_analyze), stringResource(R.string.analyze_mode_live), selected = false, enabled = false, Modifier.weight(1f))
    }
}

@Composable
private fun ModeOption(icon: Painter, label: String, selected: Boolean, enabled: Boolean, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    val content = if (selected) c.text else c.text3
    Row(
        modifier = modifier
            .fillMaxSize()
            .alpha(if (enabled) 1f else 0.5f)
            .background(if (selected) c.s1 else Color.Transparent, RoundedCornerShape(12.dp))
            .selectable(selected = selected, enabled = enabled, onClick = {}, role = Role.Tab),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = jakarta(13.sp), color = content)
    }
}

@Composable
private fun RecordBlock(state: AnalyzeUiState, onToggle: () -> Unit) {
    val c = WakTheme.colors
    val recording = state.isRecording
    val accent by animateColorAsState(if (recording) c.orange else c.primary, label = "recordAccent")
    val ring by animateColorAsState(if (recording) c.orange.copy(alpha = 0.35f) else c.outline, label = "recordRing")
    val levels = remember { mutableStateListOf<Float>() }
    LaunchedEffect(state.elapsedMs, recording) {
        if (!recording) {
            levels.clear()
        } else {
            levels.add(state.inputLevel)
            while (levels.size > LEVEL_DOTS) levels.removeAt(0)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(236.dp)
                .border(1.dp, ring, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(184.dp)
                    .border(1.dp, ring, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(132.dp)
                        .background(accent.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        onClick = onToggle,
                        modifier = Modifier.size(104.dp),
                        shape = CircleShape,
                        color = accent,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painterResource(if (recording) R.drawable.ic_stop else R.drawable.ic_mic),
                                contentDescription = stringResource(if (recording) R.string.analyze_stop_cd else R.string.analyze_start_cd),
                                tint = if (recording) OnOrange else c.onPrimary,
                                modifier = Modifier.size(40.dp),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(formatClock(state.elapsedMs), style = spaceGrotesk(36.sp), color = c.text)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(if (recording) R.string.analyze_listening else R.string.analyze_tap_to_start),
            style = jakarta(12.sp),
            color = c.text3,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.height(44.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(LEVEL_DOTS) { i ->
                val level = levels.getOrNull(i - (LEVEL_DOTS - levels.size))
                val height = if (level == null) 4f else (4f + (level * 4f).coerceIn(0f, 1f) * 40f)
                Box(
                    Modifier
                        .size(width = 4.dp, height = height.dp)
                        .background(if (level == null) c.s5 else c.orange, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun RecentAnalyses(history: List<AnalysisResult>, onOpen: (AnalysisResult) -> Unit) {
    val c = WakTheme.colors
    var expanded by rememberSaveable { mutableStateOf(false) }
    val shown = if (expanded) history else history.take(PREVIEW_COUNT)
    Column(Modifier.padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.analyze_recent), style = spaceGrotesk(16.sp), color = c.text, modifier = Modifier.weight(1f))
            if (history.size > PREVIEW_COUNT) {
                Text(
                    stringResource(if (expanded) R.string.see_less else R.string.see_all),
                    style = jakarta(13.sp),
                    color = c.text,
                    modifier = Modifier
                        .clickable { expanded = !expanded }
                        .padding(12.dp),
                )
            }
        }
        if (history.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.analyze_recent_empty), style = jakarta(13.sp), color = c.text3)
        }
        shown.forEach { result -> HistoryRow(result, onClick = { onOpen(result) }) }
    }
}

@Composable
private fun HistoryRow(result: AnalysisResult, onClick: () -> Unit) {
    val c = WakTheme.colors
    val days = ((System.currentTimeMillis() - result.createdAt) / 86_400_000L).toInt()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(c.s3, RoundedCornerShape(14.dp))
                .border(1.dp, c.text2.copy(alpha = 0.10f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_nav_analyze), contentDescription = null, tint = c.primaryText, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.analyze_voice_memo, formatClock(result.durationMs)), style = jakarta(13.sp), color = c.text, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(chordLine(result), style = spaceGrotesk(12.sp), color = c.text3, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(10.dp))
        Text(
            if (days == 1) stringResource(R.string.analyze_yesterday) else stringResource(R.string.time_ago, relativeTime(result.createdAt)),
            style = jakarta(12.sp),
            color = c.text3,
        )
    }
}

@Composable
private fun MicOffBlock(onOpenSettings: () -> Unit) {
    val c = WakTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp)
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(alertContainer(c), RoundedCornerShape(32.dp))
                .border(1.dp, c.text2.copy(alpha = 0.10f), RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_mic_off), contentDescription = null, tint = c.peachText, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.mic_off_title), style = spaceGrotesk(20.sp), color = c.text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.analyze_mic_off_message), style = jakarta(14.sp), color = c.text2, textAlign = TextAlign.Center)
        Spacer(Modifier.height(17.dp))
        WakButton(text = stringResource(R.string.open_settings), onClick = onOpenSettings, modifier = Modifier.widthIn(min = 152.dp))
    }
}

val previewAnalysis = AnalysisResult(
    id = "a1",
    createdAt = System.currentTimeMillis() - 86_400_000L,
    durationMs = 24_000L,
    key = "G",
    confidence = 0.92f,
    bpm = 96,
    chords = listOf(
        DetectedChord("G", 0, 7_000),
        DetectedChord("D", 7_000, 12_000),
        DetectedChord("Em", 12_000, 17_000),
        DetectedChord("C", 17_000, 24_000),
    ),
    source = AnalysisSource.AI,
)

@Preview(name = "Analyze - idle", heightDp = 900)
@Composable
private fun AnalyzeIdlePreview() {
    WAKordTheme(darkTheme = true) {
        AnalyzeScreen(AnalyzeUiState(history = listOf(previewAnalysis)), {}, false, 3, {}, {}, {})
    }
}

@Preview(name = "Analyze - recording", heightDp = 900)
@Composable
private fun AnalyzeRecordingPreview() {
    WAKordTheme(darkTheme = true) {
        AnalyzeScreen(AnalyzeUiState(isRecording = true, elapsedMs = 12_000, inputLevel = 0.15f), {}, false, 0, {}, {}, {})
    }
}

@Preview(name = "Analyze - mic off", heightDp = 900)
@Composable
private fun AnalyzeMicOffPreview() {
    WAKordTheme(darkTheme = false) {
        AnalyzeScreen(AnalyzeUiState(), {}, true, 0, {}, {}, {})
    }
}
