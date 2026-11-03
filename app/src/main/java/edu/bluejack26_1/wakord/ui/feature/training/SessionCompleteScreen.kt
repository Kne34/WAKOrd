package edu.bluejack26_1.wakord.ui.feature.training

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.core.training.Curriculum
import edu.bluejack26_1.wakord.domain.model.TrainingLevel
import edu.bluejack26_1.wakord.ui.components.BackgroundArt
import edu.bluejack26_1.wakord.ui.components.GlassIconButton
import edu.bluejack26_1.wakord.ui.components.WakButton
import edu.bluejack26_1.wakord.ui.components.WakButtonVariant
import edu.bluejack26_1.wakord.ui.components.glassBorder
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import edu.bluejack26_1.wakord.ui.theme.jakarta
import edu.bluejack26_1.wakord.ui.theme.spaceGrotesk
import java.util.Locale
import kotlin.math.roundToInt

fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000).coerceAtLeast(0)
    return String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60)
}

@Composable
fun SessionCompleteScreen(
    level: TrainingLevel,
    summary: SessionSummary,
    onClose: () -> Unit,
    onShare: () -> Unit,
    onRetry: () -> Unit,
    onNextLevel: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = WakTheme.colors
    val mastered = summary.levelAfter >= Curriculum.UNLOCK_ACCURACY
    val next = Curriculum.levels.firstOrNull { it.number == level.number + 1 }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg),
    ) {
        BackgroundArt(R.drawable.bg_exercise, height = 620.dp)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                GlassIconButton(
                    painterResource(R.drawable.ic_close),
                    stringResource(R.string.close),
                    onClose,
                    modifier = Modifier.align(Alignment.CenterStart),
                )
                Text(stringResource(R.string.complete_title), style = spaceGrotesk(14.sp), color = c.text)
                GlassIconButton(
                    painterResource(R.drawable.ic_share),
                    stringResource(R.string.complete_share_cd),
                    onShare,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
            Spacer(Modifier.height(18.dp))
            ScoreRing(summary.correct, summary.total, summary.accuracy)
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(
                    when {
                        summary.accuracy >= Curriculum.UNLOCK_ACCURACY -> R.string.complete_great
                        summary.accuracy >= 0.5f -> R.string.complete_nice
                        else -> R.string.complete_keep
                    },
                ),
                style = spaceGrotesk(26.sp),
                color = c.text,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.complete_subtitle, stringResource(skillName(level.skill)), level.number),
                style = jakarta(14.sp),
                color = c.text2,
            )
            Spacer(Modifier.height(22.dp))
            StatsCard(summary, Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(20.dp))
            Column(Modifier.padding(horizontal = 20.dp)) {
                Overline(stringResource(R.string.complete_answers))
                Spacer(Modifier.height(10.dp))
                AnswerChips(summary.answers)
                Spacer(Modifier.height(18.dp))
                LevelProgressCard(level, summary)
                Spacer(Modifier.height(24.dp))
                WakButton(
                    text = when {
                        mastered && next != null -> stringResource(R.string.complete_next_level, next.number)
                        mastered -> stringResource(R.string.complete_practice_again)
                        else -> stringResource(R.string.complete_try_again)
                    },
                    onClick = { if (mastered && next != null) onNextLevel(next.number) else onRetry() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                WakButton(
                    text = stringResource(R.string.complete_back),
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
                    variant = WakButtonVariant.Secondary,
                )
                if (mastered && next != null) {
                    Spacer(Modifier.height(4.dp))
                    WakButton(
                        text = stringResource(R.string.complete_retry_level, level.number),
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth(),
                        variant = WakButtonVariant.Text,
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoreRing(correct: Int, total: Int, accuracy: Float) {
    val c = WakTheme.colors
    Box(Modifier.size(168.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(144.dp)) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2
            val arc = Size(size.width - stroke, size.height - stroke)
            drawArc(c.s5, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
            if (accuracy > 0f) {
                drawArc(c.primary, -90f, 360f * accuracy.coerceIn(0f, 1f), false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.complete_score, correct, total), style = spaceGrotesk(40.sp), color = c.text)
            Text(stringResource(R.string.complete_correct), style = jakarta(12.sp), color = c.text3)
        }
    }
}

@Composable
private fun StatsCard(summary: SessionSummary, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(74.dp),
        shape = RoundedCornerShape(22.dp),
        color = c.glass,
        border = glassBorder(c),
    ) {
        Row(Modifier.padding(vertical = 15.dp)) {
            Stat(stringResource(R.string.training_percent, (summary.accuracy * 100).roundToInt()), stringResource(R.string.training_accuracy), c.text)
            VerticalDivider(color = c.line, modifier = Modifier.fillMaxHeight())
            Stat(formatDuration(summary.durationMs), stringResource(R.string.complete_time), c.text)
            VerticalDivider(color = c.line, modifier = Modifier.fillMaxHeight())
            Stat(
                pluralStringResource(R.plurals.workout_streak, summary.streakDays, summary.streakDays),
                stringResource(R.string.complete_streak),
                c.peachText,
            )
        }
    }
}

@Composable
private fun RowScope.Stat(value: String, label: String, valueColor: Color) {
    val c = WakTheme.colors
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = spaceGrotesk(20.sp), color = valueColor)
        Text(label, style = jakarta(12.sp), color = c.text3)
    }
}

@Composable
private fun AnswerChips(answers: List<Boolean>) {
    val c = WakTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        answers.forEachIndexed { i, right ->
            val description = stringResource(if (right) R.string.complete_answer_right else R.string.complete_answer_wrong, i + 1)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .background(if (right) c.primary else c.peachSoft, RoundedCornerShape(9.dp))
                    .semantics { contentDescription = description },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(if (right) R.drawable.ic_check else R.drawable.ic_close),
                    contentDescription = null,
                    tint = if (right) c.onPrimary else c.peachText,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun LevelProgressCard(level: TrainingLevel, summary: SessionSummary) {
    val c = WakTheme.colors
    val best = summary.levelAfter.coerceIn(0f, 1f)
    val next = Curriculum.levels.firstOrNull { it.number == level.number + 1 }
    val caption = when {
        summary.unlockedLevel != null -> stringResource(R.string.complete_unlocked, summary.unlockedLevel.number, stringResource(summary.unlockedLevel.titleRes))
        best >= Curriculum.UNLOCK_ACCURACY -> stringResource(R.string.complete_mastered)
        next != null -> stringResource(R.string.complete_unlock_hint, next.number, stringResource(next.titleRes))
        else -> stringResource(R.string.complete_master_hint)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = c.s2,
        border = BorderStroke(1.dp, c.line),
    ) {
        Column(Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.complete_level_progress, level.number), style = spaceGrotesk(15.sp), color = c.text, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.complete_best, (best * 100).roundToInt()), style = spaceGrotesk(12.sp, FontWeight.Medium), color = c.text3)
            }
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(c.s5, CircleShape),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(best)
                        .height(4.dp)
                        .background(c.primary, CircleShape),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(caption, style = jakarta(12.sp), color = if (summary.unlockedLevel != null) c.primaryText else c.text3, textAlign = TextAlign.Start)
        }
    }
}

private val previewSummary = SessionSummary(
    correct = 8,
    total = 10,
    accuracy = 0.8f,
    durationMs = 252_000L,
    streakDays = 6,
    answers = listOf(true, true, false, true, true, true, true, false, true, true),
    levelBefore = 0.7f,
    levelAfter = 0.8f,
    unlockedLevel = Curriculum.level(4),
)

@Preview(name = "Session complete", heightDp = 900)
@Composable
private fun SessionCompletePreview() {
    WAKordTheme(darkTheme = true) {
        SessionCompleteScreen(Curriculum.level(3), previewSummary, {}, {}, {}, {})
    }
}

@Preview(name = "Session complete - keep practicing", heightDp = 900)
@Composable
private fun SessionKeepPracticingPreview() {
    WAKordTheme(darkTheme = true) {
        SessionCompleteScreen(
            Curriculum.level(3),
            previewSummary.copy(correct = 4, accuracy = 0.4f, levelAfter = 0.6f, unlockedLevel = null, answers = List(10) { it % 3 == 0 }),
            {}, {}, {}, {},
        )
    }
}
