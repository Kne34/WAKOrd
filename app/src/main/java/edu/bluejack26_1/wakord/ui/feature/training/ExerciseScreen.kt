package edu.bluejack26_1.wakord.ui.feature.training

import androidx.annotation.StringRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.core.audio.NoteMapper
import edu.bluejack26_1.wakord.core.training.Curriculum
import edu.bluejack26_1.wakord.domain.model.AnswerChoice
import edu.bluejack26_1.wakord.domain.model.AnswerFeedback
import edu.bluejack26_1.wakord.domain.model.Question
import edu.bluejack26_1.wakord.domain.model.SkillType
import edu.bluejack26_1.wakord.ui.components.BackgroundArt
import edu.bluejack26_1.wakord.ui.components.DialogTone
import edu.bluejack26_1.wakord.ui.components.GlassIconButton
import edu.bluejack26_1.wakord.ui.components.WakButton
import edu.bluejack26_1.wakord.ui.components.WakDialog
import edu.bluejack26_1.wakord.ui.components.alertContainer
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import edu.bluejack26_1.wakord.ui.theme.jakarta
import edu.bluejack26_1.wakord.ui.theme.spaceGrotesk

@StringRes
fun skillName(skill: SkillType): Int = when (skill) {
    SkillType.SINGLE_NOTE -> R.string.skill_notes
    SkillType.INTERVAL -> R.string.skill_intervals
    SkillType.CHORD_QUALITY -> R.string.skill_chord_quality
    SkillType.PROGRESSION -> R.string.skill_progressions
}

@StringRes
private fun prompt(skill: SkillType): Int = when (skill) {
    SkillType.SINGLE_NOTE -> R.string.exercise_prompt_note
    SkillType.INTERVAL -> R.string.exercise_prompt_interval
    SkillType.CHORD_QUALITY -> R.string.exercise_prompt_quality
    SkillType.PROGRESSION -> R.string.exercise_prompt_progression
}

@Composable
fun choiceLabel(choice: AnswerChoice): String = choice.labelRes?.let { stringResource(it) } ?: choice.text.orEmpty()

@Composable
fun ExerciseScreen(
    state: ExerciseUiState,
    onAction: (ExerciseAction) -> Unit,
    showQuitDialog: Boolean,
    onQuit: () -> Unit,
    onConfirmQuit: () -> Unit,
    onDismissQuit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = WakTheme.colors
    val level = state.level ?: return
    val question = state.question
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg),
    ) {
        BackgroundArt(R.drawable.bg_exercise, height = 560.dp)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            ExerciseHeader(
                skill = stringResource(skillName(level.skill)),
                level = level.number,
                index = state.index,
                total = state.total,
                onQuit = onQuit,
            )
            ProgressSegments(answers = state.answers, index = state.index, total = state.total)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(34.dp))
                Overline(stringResource(R.string.exercise_listen))
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(prompt(level.skill)),
                    style = spaceGrotesk(26.sp),
                    color = c.text,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(26.dp))
                PlayButton(isPlaying = state.isPlaying, onClick = { onAction(ExerciseAction.PlayQuestion) })
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (question != null) {
                        ExerciseChip(
                            text = stringResource(R.string.exercise_hear_first, NoteMapper.pretty(NoteMapper.labelOf(question.referenceMidi))),
                            showIcon = true,
                            onClick = { onAction(ExerciseAction.HearReference) },
                        )
                    }
                    ExerciseChip(stringResource(R.string.exercise_play_slower), showIcon = false, onClick = { onAction(ExerciseAction.PlaySlower) })
                }
                Spacer(Modifier.height(26.dp))
                if (question != null) {
                    AnswerGrid(question, state.selectedId, state.feedback, onSelect = { onAction(ExerciseAction.Select(it)) })
                }
                Spacer(Modifier.height(20.dp))
            }
            Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp)) {
                val feedback = state.feedback
                if (feedback == null) {
                    WakButton(
                        text = stringResource(R.string.exercise_check),
                        onClick = { onAction(ExerciseAction.Check) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.canCheck,
                    )
                } else {
                    FeedbackNotice(feedback)
                    Spacer(Modifier.height(12.dp))
                    WakButton(
                        text = stringResource(if (state.isLast) R.string.exercise_finish else R.string.exercise_next),
                        onClick = { onAction(ExerciseAction.Next) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (showQuitDialog) {
        WakDialog(
            title = stringResource(R.string.exercise_quit_title),
            message = stringResource(R.string.exercise_quit_message),
            primaryText = stringResource(R.string.exercise_quit),
            onPrimary = onConfirmQuit,
            onDismissRequest = onDismissQuit,
            secondaryText = stringResource(R.string.exercise_keep_going),
            icon = painterResource(R.drawable.ic_close),
            tone = DialogTone.Destructive,
        )
    }
}

@Composable
private fun ExerciseHeader(skill: String, level: Int, index: Int, total: Int, onQuit: () -> Unit) {
    val c = WakTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        GlassIconButton(
            painterResource(R.drawable.ic_close),
            stringResource(R.string.exercise_quit_cd),
            onQuit,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(skill, style = spaceGrotesk(15.sp), color = c.text)
            Text(stringResource(R.string.level_number, level), style = jakarta(12.sp), color = c.text3)
        }
        Text(
            stringResource(R.string.exercise_counter, (index + 1).coerceAtMost(total), total),
            style = spaceGrotesk(16.sp),
            color = c.text,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Composable
private fun ProgressSegments(answers: List<Boolean>, index: Int, total: Int) {
    val c = WakTheme.colors
    val description = stringResource(R.string.exercise_progress_cd, (index + 1).coerceAtMost(total), total)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(total) { i ->
            val color = when {
                i < answers.size -> if (answers[i]) c.primary else c.orange
                i == index -> c.text3
                else -> c.s5
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(5.dp)
                    .background(color, CircleShape),
            )
        }
    }
}

@Composable
private fun PlayButton(isPlaying: Boolean, onClick: () -> Unit) {
    val c = WakTheme.colors
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulseScale",
    )
    Box(
        modifier = Modifier
            .size(168.dp)
            .border(1.dp, c.outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(128.dp)
                .scale(if (isPlaying) pulse else 1f)
                .background(c.primary.copy(alpha = 0.12f), CircleShape),
        )
        Surface(
            onClick = onClick,
            modifier = Modifier.size(96.dp),
            shape = CircleShape,
            color = c.primary,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painterResource(R.drawable.ic_play),
                    contentDescription = stringResource(R.string.exercise_play_cd),
                    tint = c.onPrimary,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
    }
}

@Composable
private fun ExerciseChip(text: String, showIcon: Boolean, onClick: () -> Unit) {
    val c = WakTheme.colors
    Surface(
        onClick = onClick,
        modifier = Modifier.height(36.dp),
        shape = CircleShape,
        color = c.s3,
        border = BorderStroke(1.dp, c.text2.copy(alpha = 0.10f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showIcon) {
                Icon(painterResource(R.drawable.ic_music_note), contentDescription = null, tint = c.text, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = jakarta(15.sp), color = c.text)
        }
    }
}

@Composable
private fun AnswerGrid(question: Question, selectedId: String?, feedback: AnswerFeedback?, onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        question.choices.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { choice ->
                    AnswerButton(
                        choice = choice,
                        state = when {
                            feedback != null && choice.id == question.answerId -> AnswerState.CORRECT
                            feedback != null && choice.id == selectedId -> AnswerState.WRONG
                            choice.id == selectedId -> AnswerState.SELECTED
                            else -> AnswerState.IDLE
                        },
                        enabled = feedback == null,
                        onClick = { onSelect(choice.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private enum class AnswerState { IDLE, SELECTED, CORRECT, WRONG }

@Composable
private fun AnswerButton(choice: AnswerChoice, state: AnswerState, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    val container = when (state) {
        AnswerState.CORRECT -> c.primary
        AnswerState.WRONG -> alertContainer(c)
        AnswerState.SELECTED -> c.primary.copy(alpha = 0.12f)
        AnswerState.IDLE -> c.s2
    }
    val border = when (state) {
        AnswerState.CORRECT, AnswerState.SELECTED -> c.primary
        AnswerState.WRONG -> c.orange
        AnswerState.IDLE -> c.text2.copy(alpha = 0.10f)
    }
    val content = when (state) {
        AnswerState.CORRECT -> c.onPrimary
        AnswerState.WRONG -> c.peachText
        AnswerState.SELECTED -> c.primaryText
        AnswerState.IDLE -> c.text
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(20.dp),
        color = container,
        border = BorderStroke(1.dp, border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state == AnswerState.CORRECT || state == AnswerState.WRONG) {
                Icon(
                    painterResource(if (state == AnswerState.CORRECT) R.drawable.ic_check else R.drawable.ic_close),
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(choiceLabel(choice), style = spaceGrotesk(16.sp), color = content, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

@Composable
private fun FeedbackNotice(feedback: AnswerFeedback) {
    val c = WakTheme.colors
    val content = if (feedback.isCorrect) c.primaryText else c.peachText
    val label = choiceLabel(feedback.correct)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (feedback.isCorrect) c.primary.copy(alpha = 0.12f) else alertContainer(c), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Icon(
            painterResource(if (feedback.isCorrect) R.drawable.ic_check_circle else R.drawable.ic_alert_circle),
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                stringResource(if (feedback.isCorrect) R.string.exercise_correct else R.string.exercise_wrong, label),
                style = spaceGrotesk(15.sp),
                color = content,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(feedback.explanationRes, label, *feedback.explanationArgs.toTypedArray()),
                style = jakarta(13.sp),
                color = content,
            )
        }
    }
}

private val previewQuestion = Question(
    skill = SkillType.INTERVAL,
    referenceMidi = 60,
    sounds = listOf(listOf(60), listOf(67)),
    choices = listOf(
        AnswerChoice("i4", labelRes = R.string.interval_4),
        AnswerChoice("i5", labelRes = R.string.interval_5),
        AnswerChoice("i7", labelRes = R.string.interval_7),
        AnswerChoice("i8", labelRes = R.string.interval_8),
    ),
    answerId = "i7",
    semitones = 7,
)

private val previewExercise = ExerciseUiState(
    level = Curriculum.level(3),
    index = 6,
    total = 10,
    question = previewQuestion,
    answers = listOf(true, true, false, true, true, true),
)

@Preview(name = "Exercise", heightDp = 844)
@Composable
private fun ExercisePreview() {
    WAKordTheme(darkTheme = true) {
        ExerciseScreen(previewExercise.copy(selectedId = "i5"), {}, false, {}, {}, {})
    }
}

@Preview(name = "Exercise - correct", heightDp = 844)
@Composable
private fun ExerciseCorrectPreview() {
    WAKordTheme(darkTheme = true) {
        ExerciseScreen(
            previewExercise.copy(
                selectedId = "i7",
                feedback = AnswerFeedback(true, previewQuestion.choices[2], R.string.explain_interval, listOf(7)),
                answers = previewExercise.answers + true,
            ),
            {}, false, {}, {}, {},
        )
    }
}

@Preview(name = "Exercise - wrong", heightDp = 844)
@Composable
private fun ExerciseWrongPreview() {
    WAKordTheme(darkTheme = true) {
        ExerciseScreen(
            previewExercise.copy(
                selectedId = "i5",
                feedback = AnswerFeedback(false, previewQuestion.choices[2], R.string.explain_interval, listOf(7)),
                answers = previewExercise.answers + false,
            ),
            {}, false, {}, {}, {},
        )
    }
}

@Preview(name = "Exercise - quit")
@Composable
private fun ExerciseQuitPreview() {
    WAKordTheme(darkTheme = true) {
        ExerciseScreen(previewExercise, {}, true, {}, {}, {})
    }
}
