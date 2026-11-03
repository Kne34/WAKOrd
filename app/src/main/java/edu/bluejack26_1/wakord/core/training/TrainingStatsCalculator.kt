package edu.bluejack26_1.wakord.core.training

import edu.bluejack26_1.wakord.domain.model.AccuracyBucket
import edu.bluejack26_1.wakord.domain.model.AccuracySeries
import edu.bluejack26_1.wakord.domain.model.DailyWorkout
import edu.bluejack26_1.wakord.domain.model.DayStat
import edu.bluejack26_1.wakord.domain.model.LevelProgress
import edu.bluejack26_1.wakord.domain.model.LevelStatus
import edu.bluejack26_1.wakord.domain.model.TrainingSession
import edu.bluejack26_1.wakord.domain.model.TrainingStats
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object TrainingStatsCalculator {
    private const val HISTORY_DAYS = 56
    private const val DAILY_POINTS = 7
    private const val WEEKLY_POINTS = 8
    private const val MONTHLY_POINTS = 12

    fun calculate(sessions: List<TrainingSession>, now: Long, zone: ZoneId): TrainingStats {
        val today = epochDay(now, zone)
        val byDay = sessions.groupBy { epochDay(it.startedAt, zone) }
        val todays = byDay[today].orEmpty()
        val levels = levelProgress(sessions)
        return TrainingStats(
            workout = DailyWorkout(
                minutesToday = (todays.sumOf { it.durationMs } / 60_000L).toInt(),
                goalMinutes = Curriculum.DAILY_GOAL_MINUTES,
                trialsToday = todays.sumOf { it.total },
                accuracyToday = accuracy(todays),
                streakDays = currentStreak(byDay.keys, today),
                currentLevel = currentLevel(levels),
            ),
            longestStreak = longestStreak(byDay.keys),
            overallAccuracy = accuracy(sessions),
            history = (minOf(today - HISTORY_DAYS + 1, byDay.keys.minOrNull() ?: today)..today).map { day ->
                val list = byDay[day].orEmpty()
                DayStat(day, list.size, accuracy(list), (list.sumOf { it.durationMs } / 60_000L).toInt())
            },
            accuracyBySkill = sessions.groupBy { it.skill }.mapValues { accuracy(it.value) },
            levels = levels,
            daily = dailySeries(sessions, now, zone),
            weekly = weeklySeries(sessions, now, zone),
            monthly = monthlySeries(sessions, now, zone),
        )
    }

    fun dailySeries(sessions: List<TrainingSession>, now: Long, zone: ZoneId): AccuracySeries {
        val today = localDate(now, zone)
        val byDay = sessions.groupBy { localDate(it.startedAt, zone) }
        val buckets = (DAILY_POINTS - 1 downTo 0).map { back ->
            val day = today.minusDays(back.toLong())
            AccuracyBucket(day.toEpochDay(), accuracyOrNull(byDay[day].orEmpty()))
        }
        return AccuracySeries(buckets, change(byDay[today].orEmpty(), byDay[today.minusDays(1)].orEmpty()))
    }

    fun weeklySeries(sessions: List<TrainingSession>, now: Long, zone: ZoneId): AccuracySeries {
        val today = localDate(now, zone)
        val thisWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val byWeek = sessions.groupBy { localDate(it.startedAt, zone).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
        val buckets = (WEEKLY_POINTS - 1 downTo 0).map { back ->
            val week = thisWeek.minusWeeks(back.toLong())
            AccuracyBucket(week.toEpochDay(), accuracyOrNull(byWeek[week].orEmpty()))
        }
        val byMonth = sessions.groupBy { YearMonth.from(localDate(it.startedAt, zone)) }
        val month = YearMonth.from(today)
        return AccuracySeries(buckets, change(byMonth[month].orEmpty(), byMonth[month.minusMonths(1)].orEmpty()))
    }

    fun monthlySeries(sessions: List<TrainingSession>, now: Long, zone: ZoneId): AccuracySeries {
        val today = localDate(now, zone)
        val month = YearMonth.from(today)
        val byMonth = sessions.groupBy { YearMonth.from(localDate(it.startedAt, zone)) }
        val buckets = (MONTHLY_POINTS - 1 downTo 0).map { back ->
            val m = month.minusMonths(back.toLong())
            AccuracyBucket(m.atDay(1).toEpochDay(), accuracyOrNull(byMonth[m].orEmpty()))
        }
        val byYear = sessions.groupBy { localDate(it.startedAt, zone).year }
        return AccuracySeries(buckets, change(byYear[today.year].orEmpty(), byYear[today.year - 1].orEmpty()))
    }

    private fun accuracyOrNull(sessions: List<TrainingSession>): Float? = if (sessions.isEmpty()) null else accuracy(sessions)

    private fun change(current: List<TrainingSession>, previous: List<TrainingSession>): Float? {
        if (current.isEmpty() || previous.isEmpty()) return null
        return accuracy(current) - accuracy(previous)
    }

    private fun localDate(millis: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun levelProgress(sessions: List<TrainingSession>): List<LevelProgress> {
        val best = sessions.groupBy { it.level }.mapValues { (_, list) -> list.maxOf { it.accuracy } }
        var previousMastered = true
        return Curriculum.levels.map { level ->
            val score = best[level.number]
            val status = when {
                !previousMastered -> LevelStatus.LOCKED
                score == null -> LevelStatus.UNLOCKED
                score >= Curriculum.UNLOCK_ACCURACY -> LevelStatus.MASTERED
                else -> LevelStatus.IN_PROGRESS
            }
            previousMastered = status == LevelStatus.MASTERED
            LevelProgress(level, score ?: 0f, status)
        }
    }

    fun currentLevel(levels: List<LevelProgress>): Int =
        levels.firstOrNull { it.status == LevelStatus.IN_PROGRESS || it.status == LevelStatus.UNLOCKED }?.level?.number
            ?: levels.last().level.number

    fun accuracy(sessions: List<TrainingSession>): Float {
        val total = sessions.sumOf { it.total }
        return if (total == 0) 0f else sessions.sumOf { it.correct }.toFloat() / total
    }

    fun epochDay(millis: Long, zone: ZoneId): Long = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().toEpochDay()

    private fun currentStreak(days: Set<Long>, today: Long): Int {
        var day = if (today in days) today else today - 1
        var streak = 0
        while (day in days) {
            streak++
            day--
        }
        return streak
    }

    private fun longestStreak(days: Set<Long>): Int {
        var longest = 0
        days.forEach { start ->
            if (start - 1 !in days) {
                var length = 0
                while (start + length in days) length++
                longest = maxOf(longest, length)
            }
        }
        return longest
    }
}
