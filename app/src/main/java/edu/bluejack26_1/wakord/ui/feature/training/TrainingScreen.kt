package edu.bluejack26_1.wakord.ui.feature.training

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.core.training.Curriculum
import edu.bluejack26_1.wakord.domain.model.DailyWorkout
import edu.bluejack26_1.wakord.domain.model.LevelProgress
import edu.bluejack26_1.wakord.domain.model.LevelStatus
import edu.bluejack26_1.wakord.domain.model.PitchReading
import edu.bluejack26_1.wakord.domain.model.PitchStatus
import edu.bluejack26_1.wakord.domain.model.TrainingStats
import edu.bluejack26_1.wakord.ui.components.BackgroundArt
import edu.bluejack26_1.wakord.ui.components.DialogTone
import edu.bluejack26_1.wakord.ui.components.GlassIconButton
import edu.bluejack26_1.wakord.ui.components.NotificationBell
import edu.bluejack26_1.wakord.ui.components.WakDialog
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import edu.bluejack26_1.wakord.ui.theme.jakarta
import edu.bluejack26_1.wakord.ui.theme.spaceGrotesk

@Composable
fun TrainingScreen(
    state: TrainingUiState,
    onAction: (TrainingAction) -> Unit,
    pitch: PitchMonitorUiState,
    onPitchAction: (PitchMonitorAction) -> Unit,
    unreadNotifications: Int,
    onBack: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = WakTheme.colors
    val workout = state.stats?.workout
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg),
    ) {
        BackgroundArt(R.drawable.bg_training, height = 560.dp)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            Header(
                streakDays = workout?.streakDays ?: 0,
                unreadNotifications = unreadNotifications,
                onBack = onBack,
                onOpenStats = onOpenStats,
                onOpenNotifications = onOpenNotifications,
            )
            Column(Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.training_title), style = spaceGrotesk(26.sp), color = c.text)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.training_subtitle), style = jakarta(14.sp), color = c.text2)
                Spacer(Modifier.height(18.dp))
                if (state.isLoading || workout == null) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(212.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = c.primary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                    }
                } else {
                    WorkoutCard(workout, onResume = { onAction(TrainingAction.Resume) })
                }
                Spacer(Modifier.height(16.dp))
                PitchMonitorCard(pitch, onPitchAction, onOpenSettings)
                Spacer(Modifier.height(26.dp))
                CurriculumHeader(
                    mastered = state.levels.count { it.status == LevelStatus.MASTERED },
                    total = state.levels.size,
                )
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.levels.forEach { progress ->
                        LevelCard(progress, onOpen = { onAction(TrainingAction.OpenLevel(progress.level.number)) })
                    }
                }
            }
        }
    }

    state.lockedLevel?.let { locked ->
        val previous = Curriculum.level(locked.number - 1)
        WakDialog(
            title = stringResource(R.string.level_locked_title, locked.number),
            message = stringResource(R.string.level_locked_message, previous.number, stringResource(previous.titleRes), stringResource(locked.titleRes)),
            primaryText = stringResource(R.string.level_go_to, previous.number),
            onPrimary = {
                onAction(TrainingAction.DismissLockedLevel)
                onAction(TrainingAction.OpenLevel(previous.number))
            },
            onDismissRequest = { onAction(TrainingAction.DismissLockedLevel) },
            secondaryText = stringResource(R.string.not_now),
            icon = painterResource(R.drawable.ic_lock),
            tone = DialogTone.Info,
        )
    }
}

@Composable
private fun Header(
    streakDays: Int,
    unreadNotifications: Int,
    onBack: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenNotifications: () -> Unit,
) {
    val c = WakTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        GlassIconButton(
            painterResource(R.drawable.ic_back),
            stringResource(R.string.back),
            onBack,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        Row(
            modifier = Modifier
                .height(36.dp)
                .background(c.peachSoft, CircleShape)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_flame), contentDescription = null, tint = c.peachText, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(5.dp))
            Text(pluralStringResource(R.plurals.workout_streak, streakDays, streakDays), style = spaceGrotesk(13.sp), color = c.peachText)
        }
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlassIconButton(painterResource(R.drawable.ic_stats), stringResource(R.string.training_stats), onOpenStats)
            NotificationBell(unreadCount = unreadNotifications, onClick = onOpenNotifications)
        }
    }
}

private val previewLevels = listOf(
    LevelProgress(Curriculum.level(1), 0.92f, LevelStatus.MASTERED),
    LevelProgress(Curriculum.level(2), 0.6f, LevelStatus.IN_PROGRESS),
    LevelProgress(Curriculum.level(3), 0f, LevelStatus.UNLOCKED),
    LevelProgress(Curriculum.level(4), 0f, LevelStatus.LOCKED),
)

private val previewState = TrainingUiState(
    isLoading = false,
    stats = TrainingStats(
        workout = DailyWorkout(minutesToday = 5, goalMinutes = 8, trialsToday = 42, accuracyToday = 0.94f, streakDays = 5, currentLevel = 2),
        longestStreak = 14,
        overallAccuracy = 0.86f,
        history = emptyList(),
        accuracyBySkill = emptyMap(),
        levels = previewLevels,
    ),
)

private val previewPitch = PitchMonitorUiState(
    referenceMidi = 69,
    isListening = true,
    reading = PitchReading(targetNote = "A4", sungNote = "A4", frequencyHz = 440.8f, cents = 3, status = PitchStatus.ON_PITCH),
)

@Preview(name = "Training", heightDp = 1400)
@Composable
private fun TrainingPreview() {
    WAKordTheme(darkTheme = true) {
        TrainingScreen(previewState, {}, previewPitch, {}, 3, {}, {}, {}, {})
    }
}

@Preview(name = "Training - paused, light", heightDp = 1400)
@Composable
private fun TrainingLightPreview() {
    WAKordTheme(darkTheme = false) {
        TrainingScreen(previewState, {}, PitchMonitorUiState(), {}, 0, {}, {}, {}, {})
    }
}

@Preview(name = "Training - mic off")
@Composable
private fun TrainingMicOffPreview() {
    WAKordTheme(darkTheme = true) {
        Box(Modifier.padding(20.dp)) {
            PitchMonitorCard(PitchMonitorUiState(microphoneOff = true), {}, {})
        }
    }
}

@Preview(name = "Training - locked level")
@Composable
private fun TrainingLockedPreview() {
    WAKordTheme(darkTheme = true) {
        TrainingScreen(previewState.copy(lockedLevel = Curriculum.level(4)), {}, previewPitch, {}, 3, {}, {}, {}, {})
    }
}
