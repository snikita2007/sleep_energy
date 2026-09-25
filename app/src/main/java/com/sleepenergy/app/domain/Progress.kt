package com.sleepenergy.app.domain

import java.time.LocalDate

/** Недельная сводка для экрана «Прогресс»: без графиков, только главное. */
object Progress {
    const val WEEK = 7L

    data class NightStat(val outcome: Outcome, val rating: Int?)

    data class Week(
        /** Сколько ночей этой недели приложение уже застало. */
        val nights: Int,
        val onTime: Int,
        val avgRating: Double?,
        val prevAvgRating: Double?,
    )

    fun week(stats: Map<LocalDate, NightStat>, firstNight: LocalDate, lastNight: LocalDate): Week {
        val current = nightsBack(lastNight, from = 0, to = WEEK, firstNight)
        val previous = nightsBack(lastNight, from = WEEK, to = WEEK * 2, firstNight)
        return Week(
            nights = current.size,
            onTime = current.count { stats[it]?.outcome == Outcome.ON_TIME },
            avgRating = current.mapNotNull { stats[it]?.rating }.averageOrNull(),
            prevAvgRating = previous.mapNotNull { stats[it]?.rating }.averageOrNull(),
        )
    }

    private fun nightsBack(lastNight: LocalDate, from: Long, to: Long, firstNight: LocalDate): List<LocalDate> =
        (from until to).map { lastNight.minusDays(it) }.filter { it >= firstNight }

    private fun List<Int>.averageOrNull(): Double? = if (isEmpty()) null else average()
}
