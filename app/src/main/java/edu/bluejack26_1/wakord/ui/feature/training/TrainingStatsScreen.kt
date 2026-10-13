package edu.bluejack26_1.wakord.ui.feature.training

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.core.training.Curriculum
import edu.bluejack26_1.wakord.domain.model.AccuracyBucket
import edu.bluejack26_1.wakord.domain.model.AccuracySeries
import edu.bluejack26_1.wakord.domain.model.DailyWorkout
import edu.bluejack26_1.wakord.domain.model.DayStat
import edu.bluejack26_1.wakord.domain.model.SkillType
import edu.bluejack26_1.wakord.domain.model.TrainingStats
import edu.bluejack26_1.wakord.ui.components.GlassIconButton
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import edu.bluejack26_1.wakord.ui.theme.jakarta
import edu.bluejack26_1.wakord.ui.theme.spaceGrotesk
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs
import kotlin.math.roundToInt

enum class StatsPeriod { DAY, MONTH, YEAR }

private const val CHART_HEIGHT = 100
private const val CHART_FLOOR = 0.5f
private const val MONTHS_TO_PICK = 12

@Composable
fun TrainingStatsScreen(
    stats: TrainingStats?,
    month: YearMonth,
    selectedDay: LocalDate?,
    period: StatsPeriod,
    onMonthChange: (YearMonth) -> Unit,
    onSelectDay: (LocalDate) -> Unit,
    onPeriodChange: (StatsPeriod) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = WakTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassIconButton(painterResource(R.drawable.ic_back), stringResource(R.string.back), onBack)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.training_stats), style = spaceGrotesk(20.sp), color = c.text, modifier = Modifier.weight(1f))
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StreakCard(
                    icon = painterResource(R.drawable.ic_flame),
                    tint = c.peachText,
                    label = stringResource(R.string.stats_current_streak),
                    days = stats?.workout?.streakDays ?: 0,
                    modifier = Modifier.weight(1f),
                )
                StreakCard(
                    icon = painterResource(R.drawable.ic_star),
                    tint = c.primaryText,
                    label = stringResource(R.string.stats_best_streak),
                    days = stats?.longestStreak ?: 0,
                    modifier = Modifier.weight(1f),
                )
            }
            val history = stats?.history.orEmpty()
            SectionTitle(stringResource(R.string.stats_streak_history)) {
                MonthChip(month, availableMonths(history), onMonthChange)
            }
            StatsCard { StreakCalendar(history, month, selectedDay, onSelectDay) }
            val series = when (period) {
                StatsPeriod.DAY -> stats?.daily
                StatsPeriod.MONTH -> stats?.weekly
                StatsPeriod.YEAR -> stats?.monthly
            } ?: AccuracySeries()
            SectionTitle(stringResource(R.string.stats_accuracy_over_time)) { ChangeText(series.change, period) }
            StatsCard {
                PeriodSelector(period, onPeriodChange)
                Spacer(Modifier.height(16.dp))
                AccuracyChart(series, period)
            }
            SectionTitle(stringResource(R.string.stats_by_skill)) {}
            StatsCard { SkillBreakdown(stats?.accuracyBySkill.orEmpty()) }
        }
    }
}

fun availableMonths(history: List<DayStat>): List<YearMonth> {
    val current = YearMonth.now()
    val yearAgo = current.minusMonths(MONTHS_TO_PICK - 1L)
    val firstSession = history.firstOrNull { it.sessions > 0 }?.let { YearMonth.from(LocalDate.ofEpochDay(it.epochDay)) }
    val first = if (firstSession != null && firstSession.isBefore(yearAgo)) firstSession else yearAgo
    return generateSequence(current) { it.minusMonths(1) }.takeWhile { !it.isBefore(first) }.toList()
}

@Composable
private fun MonthChip(month: YearMonth, months: List<YearMonth>, onMonthChange: (YearMonth) -> Unit) {
    val c = WakTheme.colors
    val locale = LocalConfiguration.current.locales.get(0)
    val format = DateTimeFormatter.ofPattern("MMMM yyyy", locale)
    var open by rememberSaveable { mutableStateOf(false) }
    val label = month.format(format)
    val description = stringResource(R.string.stats_month_cd, label)
    Box {
        Surface(
            onClick = { open = true },
            modifier = Modifier
                .height(40.dp)
                .semantics { contentDescription = description },
            shape = CircleShape,
            color = c.s3,
            border = BorderStroke(1.dp, c.line),
        ) {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = jakarta(14.sp), color = c.text)
                Spacer(Modifier.width(8.dp))
                Icon(painterResource(R.drawable.ic_chevron_down), contentDescription = null, tint = c.text, modifier = Modifier.size(16.dp))
            }
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = c.s3,
            modifier = Modifier.heightIn(max = 320.dp),
        ) {
            months.forEach { value ->
                DropdownMenuItem(
                    text = {
                        Text(
                            value.format(format),
                            style = jakarta(14.sp, if (value == month) FontWeight.SemiBold else FontWeight.Normal),
                            color = if (value == month) c.primaryText else c.text,
                        )
                    },
                    onClick = {
                        open = false
                        onMonthChange(value)
                    },
                )
            }
        }
    }
}

@Composable
private fun StreakCard(icon: Painter, tint: Color, label: String, days: Int, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    Surface(
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(22.dp),
        color = c.s2,
        border = BorderStroke(1.dp, c.line),
    ) {
        Column(Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(label, style = jakarta(12.sp), color = c.text3)
            }
            Spacer(Modifier.height(4.dp))
            Text(pluralStringResource(R.plurals.workout_streak, days, days), style = spaceGrotesk(28.sp), color = c.text)
        }
    }
}

@Composable
private fun SectionTitle(title: String, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = spaceGrotesk(16.sp), color = WakTheme.colors.text, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun StatsCard(content: @Composable () -> Unit) {
    val c = WakTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = c.s2,
        border = BorderStroke(1.dp, c.line),
    ) {
        Column(Modifier.padding(17.dp)) { content() }
    }
}

private fun minutesColor(minutes: Int, primary: Color, primary2: Color, empty: Color): Color = when {
    minutes <= 0 -> empty
    minutes < 3 -> primary.copy(alpha = 0.12f)
    minutes < Curriculum.DAILY_GOAL_MINUTES -> primary2
    else -> primary
}

@Composable
private fun StreakCalendar(history: List<DayStat>, month: YearMonth, selectedDay: LocalDate?, onSelectDay: (LocalDate) -> Unit) {
    val c = WakTheme.colors
    val locale = LocalConfiguration.current.locales.get(0)
    val byDay = history.associateBy { it.epochDay }
    val today = LocalDate.now()
    val start = month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val end = month.atEndOfMonth().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    val weeks = ((end.toEpochDay() - start.toEpochDay() + 1) / 7).toInt()
    val dateFormat = DateTimeFormatter.ofPattern("EEE d MMM", locale)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        DayOfWeek.entries.forEach { dow ->
            Text(
                dow.getDisplayName(TextStyle.NARROW, locale),
                style = jakarta(12.sp),
                color = c.text3,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
    Spacer(Modifier.height(6.dp))
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(weeks) { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(7) { offset ->
                    val date = start.plusDays((week * 7 + offset).toLong())
                    val inMonth = YearMonth.from(date) == month
                    val future = date.isAfter(today)
                    val active = inMonth && !future
                    val minutes = byDay[date.toEpochDay()]?.minutes ?: 0
                    val description = pluralStringResource(R.plurals.stats_day_minutes_cd, minutes, date.format(dateFormat), minutes)
                    val shape = RoundedCornerShape(10.dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .then(
                                if (active) {
                                    Modifier
                                        .background(minutesColor(minutes, c.primary, c.primary2, c.s3), shape)
                                        .then(
                                            when (date) {
                                                today -> Modifier.border(1.5.dp, c.orange, shape)
                                                selectedDay -> Modifier.border(1.5.dp, c.text, shape)
                                                else -> Modifier
                                            },
                                        )
                                        .clip(shape)
                                        .clickable { onSelectDay(date) }
                                        .clearAndSetSemantics { contentDescription = description }
                                } else {
                                    Modifier.clearAndSetSemantics {}
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            date.dayOfMonth.toString(),
                            style = jakarta(11.sp, if (date == today || date == selectedDay) FontWeight.SemiBold else FontWeight.Normal),
                            color = when {
                                !active -> c.text3.copy(alpha = 0.45f)
                                minutes >= 3 -> c.onPrimary
                                minutes > 0 -> c.primaryText
                                else -> c.text3
                            },
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.stats_minutes_per_day), style = jakarta(12.sp), color = c.text3, modifier = Modifier.weight(1f))
        Text(stringResource(R.string.stats_less), style = jakarta(12.sp), color = c.text3)
        Spacer(Modifier.width(6.dp))
        listOf(0, 1, 3, Curriculum.DAILY_GOAL_MINUTES).forEach { sample ->
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .size(14.dp)
                    .background(minutesColor(sample, c.primary, c.primary2, c.s3), RoundedCornerShape(4.dp)),
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.stats_more), style = jakarta(12.sp), color = c.text3)
    }
    if (selectedDay != null && YearMonth.from(selectedDay) == month) {
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(thickness = 1.dp, color = c.line)
        Spacer(Modifier.height(14.dp))
        DayDetail(selectedDay, byDay[selectedDay.toEpochDay()], isToday = selectedDay == today)
    }
}

@Composable
private fun DayDetail(date: LocalDate, stat: DayStat?, isToday: Boolean) {
    val c = WakTheme.colors
    val locale = LocalConfiguration.current.locales.get(0)
    val title = if (isToday) {
        stringResource(R.string.stats_today_date, date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale)))
    } else {
        date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale))
    }
    Text(title, style = spaceGrotesk(15.sp), color = c.text)
    Spacer(Modifier.height(4.dp))
    if (stat == null || stat.sessions == 0) {
        Text(stringResource(R.string.stats_no_training), style = jakarta(13.sp), color = c.text3)
    } else {
        Text(
            stringResource(
                R.string.stats_day_detail,
                stringResource(R.string.stats_minutes, stat.minutes),
                pluralStringResource(R.plurals.stats_sets, stat.sessions, stat.sessions),
                (stat.accuracy * 100).roundToInt(),
            ),
            style = jakarta(13.sp),
            color = c.text2,
        )
    }
}

@Composable
private fun ChangeText(change: Float?, period: StatsPeriod) {
    val c = WakTheme.colors
    if (change == null) return
    val points = (change * 100).roundToInt()
    val signed = stringResource(R.string.stats_signed_percent, if (points > 0) "+" else if (points < 0) "-" else "", abs(points))
    Text(
        stringResource(
            when (period) {
                StatsPeriod.DAY -> R.string.stats_vs_yesterday
                StatsPeriod.MONTH -> R.string.stats_vs_last_month
                StatsPeriod.YEAR -> R.string.stats_vs_last_year
            },
            signed,
        ),
        style = jakarta(12.sp),
        color = if (points >= 0) c.primaryText else c.peachText,
    )
}

@Composable
private fun PeriodSelector(period: StatsPeriod, onPeriodChange: (StatsPeriod) -> Unit) {
    val c = WakTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(c.s3, RoundedCornerShape(14.dp))
            .border(1.dp, c.line, RoundedCornerShape(14.dp))
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        StatsPeriod.entries.forEach { value ->
            val selected = value == period
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(if (selected) c.s1 else Color.Transparent, RoundedCornerShape(10.dp))
                    .selectable(selected = selected, onClick = { onPeriodChange(value) }, role = Role.RadioButton),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(
                        when (value) {
                            StatsPeriod.DAY -> R.string.stats_period_day
                            StatsPeriod.MONTH -> R.string.stats_period_month
                            StatsPeriod.YEAR -> R.string.stats_period_year
                        },
                    ),
                    style = jakarta(13.sp),
                    color = if (selected) c.text else c.text3,
                )
            }
        }
    }
}

@Composable
private fun AccuracyChart(series: AccuracySeries, period: StatsPeriod) {
    val c = WakTheme.colors
    val locale = LocalConfiguration.current.locales.get(0)
    val buckets = series.buckets
    val labelFormat = when (period) {
        StatsPeriod.DAY, StatsPeriod.MONTH -> DateTimeFormatter.ofPattern("d MMM", locale)
        StatsPeriod.YEAR -> DateTimeFormatter.ofPattern("MMM yyyy", locale)
    }
    if (buckets.all { it.accuracy == null }) {
        Text(stringResource(R.string.stats_empty), style = jakarta(13.sp), color = c.text3, modifier = Modifier.padding(vertical = 24.dp))
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height((CHART_HEIGHT + 20).dp),
        horizontalArrangement = Arrangement.spacedBy(if (buckets.size > 8) 5.dp else 10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        buckets.forEachIndexed { i, bucket ->
            ChartBar(bucket, isCurrent = i == buckets.lastIndex, label = LocalDate.ofEpochDay(bucket.startDay).format(labelFormat), modifier = Modifier.weight(1f))
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth()) {
        val (first, last) = when (period) {
            StatsPeriod.DAY -> stringResource(R.string.stats_first_day) to stringResource(R.string.stats_today)
            StatsPeriod.MONTH -> stringResource(R.string.stats_first_week) to stringResource(R.string.stats_this_week)
            StatsPeriod.YEAR -> {
                val months = DateTimeFormatter.ofPattern("MMM", locale)
                LocalDate.ofEpochDay(buckets.first().startDay).format(months) to LocalDate.ofEpochDay(buckets.last().startDay).format(months)
            }
        }
        Text(first, style = jakarta(12.sp), color = c.text3, modifier = Modifier.weight(1f))
        Text(last, style = jakarta(12.sp), color = c.text3)
    }
}

@Composable
private fun ChartBar(bucket: AccuracyBucket, isCurrent: Boolean, label: String, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    val accuracy = bucket.accuracy
    val description = if (accuracy == null) {
        stringResource(R.string.stats_bar_empty_cd, label)
    } else {
        stringResource(R.string.stats_bar_cd, label, (accuracy * 100).roundToInt())
    }
    Column(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (accuracy != null) {
            Text((accuracy * 100).roundToInt().toString(), style = spaceGrotesk(10.sp, FontWeight.Medium), color = c.text3, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            val fraction = ((accuracy - CHART_FLOOR) / (1f - CHART_FLOOR)).coerceIn(0.06f, 1f)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height((CHART_HEIGHT * fraction).dp)
                    .background(if (isCurrent) c.primary else c.s5, RoundedCornerShape(8.dp)),
            )
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(c.s3, RoundedCornerShape(8.dp)),
            )
        }
    }
}

@Composable
private fun SkillBreakdown(bySkill: Map<SkillType, Float>) {
    val c = WakTheme.colors
    val skills = SkillType.entries.filter { it in bySkill }
    if (skills.isEmpty()) {
        Text(stringResource(R.string.stats_empty), style = jakarta(13.sp), color = c.text3, modifier = Modifier.padding(vertical = 8.dp))
        return
    }
    skills.forEachIndexed { i, skill ->
        val accuracy = (bySkill[skill] ?: 0f).coerceIn(0f, 1f)
        if (i > 0) HorizontalDivider(thickness = 1.dp, color = c.line)
        Column(Modifier.padding(vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(skillName(skill)), style = jakarta(13.sp), color = c.text, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.training_percent, (accuracy * 100).roundToInt()), style = spaceGrotesk(13.sp), color = c.text)
            }
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(c.s5, CircleShape),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(accuracy)
                        .height(4.dp)
                        .background(c.primary, CircleShape),
                )
            }
        }
    }
}

private fun previewStats(): TrainingStats {
    val today = LocalDate.now().toEpochDay()
    val history = (55 downTo 0).map { back -> DayStat(today - back, if (back % 3 == 0) 0 else 1, 0.8f, (back * 7) % 9) }
    fun series(size: Int, step: Long, change: Float) = AccuracySeries(
        buckets = (size - 1 downTo 0).map { back -> AccuracyBucket(today - back * step, if (back == 3) null else 0.92f - back * 0.03f) },
        change = change,
    )
    return TrainingStats(
        workout = DailyWorkout(5, 5, 30, 0.9f, 6, 2),
        longestStreak = 14,
        overallAccuracy = 0.84f,
        history = history,
        accuracyBySkill = mapOf(SkillType.SINGLE_NOTE to 0.96f, SkillType.INTERVAL to 0.81f, SkillType.CHORD_QUALITY to 0.64f),
        levels = emptyList(),
        daily = series(7, 1, -0.05f),
        weekly = series(8, 7, 0.12f),
        monthly = series(12, 30, 0.08f),
    )
}

@Preview(name = "Training stats", heightDp = 1100)
@Composable
private fun TrainingStatsPreview() {
    WAKordTheme(darkTheme = true) {
        TrainingStatsScreen(previewStats(), YearMonth.now(), LocalDate.now(), StatsPeriod.MONTH, {}, {}, {}, {})
    }
}

@Preview(name = "Training stats - year, light", heightDp = 1100)
@Composable
private fun TrainingStatsYearPreview() {
    WAKordTheme(darkTheme = false) {
        TrainingStatsScreen(previewStats(), YearMonth.now().minusMonths(1), LocalDate.now().minusMonths(1), StatsPeriod.YEAR, {}, {}, {}, {})
    }
}

@Preview(name = "Training stats - empty", heightDp = 900)
@Composable
private fun TrainingStatsEmptyPreview() {
    WAKordTheme(darkTheme = true) {
        TrainingStatsScreen(null, YearMonth.now(), null, StatsPeriod.DAY, {}, {}, {}, {})
    }
}
