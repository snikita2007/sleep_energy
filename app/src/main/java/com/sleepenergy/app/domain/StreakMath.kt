package com.sleepenergy.app.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Серия «вовремя + запасной вечер»: считаем ночи, когда лёг не позже цели + 30 минут.
 * Один срыв (поздно или нет данных) раз в 7 ночей прощается — серия не растёт,
 * но и не обнуляется: сорванный вечер не отменяет прогресс.
 */
object StreakMath {
    const val SPARE_EVERY_NIGHTS = 7L

    data class Streak(
        val days: Int,
        /** Самый свежий прощённый срыв внутри серии. */
        val lastForgiven: LocalDate?,
    )

    /**
     * @param firstNight первая ночь, которую застало приложение (после онбординга)
     * @param lastNight последняя наступившая ночь
     * @param neutralFrom ночи начиная с этой без данных ещё не закрыты (отбой можно
     *   указать утром) — они не считаются срывом
     */
    fun compute(
        outcomes: Map<LocalDate, Outcome>,
        firstNight: LocalDate,
        lastNight: LocalDate,
        neutralFrom: LocalDate = lastNight.plusDays(1),
    ): Streak {
        var days = 0
        var newestForgiven: LocalDate? = null
        var oldestForgiven: LocalDate? = null
        var night = lastNight
        while (night >= firstNight) {
            val outcome = outcomes[night] ?: Outcome.UNKNOWN
            if (outcome == Outcome.ON_TIME) {
                days++
            } else if (outcome == Outcome.UNKNOWN && night >= neutralFrom) {
                // Ночь ещё открыта — не срыв.
            } else if (oldestForgiven == null ||
                ChronoUnit.DAYS.between(night, oldestForgiven) >= SPARE_EVERY_NIGHTS
            ) {
                oldestForgiven = night
                if (newestForgiven == null) newestForgiven = night
            } else {
                break
            }
            night = night.minusDays(1)
        }
        return Streak(days, newestForgiven)
    }

    /** Простит ли серия срыв в ночь [tonight], то есть остался ли запасной вечер. */
    fun spareAvailable(
        outcomes: Map<LocalDate, Outcome>,
        firstNight: LocalDate,
        lastNight: LocalDate,
        neutralFrom: LocalDate,
        tonight: LocalDate,
    ): Boolean {
        val current = compute(outcomes, firstNight, lastNight, neutralFrom).days
        val missed = compute(
            outcomes + (tonight to Outcome.LATE),
            firstNight,
            maxOf(lastNight, tonight),
            neutralFrom,
        ).days
        return missed >= current
    }
}
