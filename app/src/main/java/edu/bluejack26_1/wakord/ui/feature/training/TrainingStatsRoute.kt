package edu.bluejack26_1.wakord.ui.feature.training

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun TrainingStatsRoute(
    onBack: () -> Unit,
    viewModel: TrainingViewModel = viewModel(factory = TrainingViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var month by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var selectedDay by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var period by rememberSaveable { mutableStateOf(StatsPeriod.MONTH) }
    TrainingStatsScreen(
        stats = state.stats,
        month = YearMonth.parse(month),
        selectedDay = LocalDate.ofEpochDay(selectedDay),
        period = period,
        onMonthChange = { value ->
            month = value.toString()
            val today = LocalDate.now()
            selectedDay = (if (value == YearMonth.from(today)) today else value.atEndOfMonth()).toEpochDay()
        },
        onSelectDay = { selectedDay = it.toEpochDay() },
        onPeriodChange = { period = it },
        onBack = onBack,
    )
}
