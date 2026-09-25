package com.sleepenergy.app.domain

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.abs

/** Исход ночи для серии и сдвига. */
enum class Outcome { ON_TIME, LATE, UNKNOWN }

/**
 * Ночь определяется датой подъёма: вечер дня D относится к ночи D+1.
 * Граница суток для приложения — полдень: до него ещё длится прошлая ночь и утро,
 * после — начинается следующий вечер.
 */
object NightMath {
    const val DAY_BOUNDARY_HOUR = 12
    const val MORNING_LEAD_MINUTES = 180L
    const val GRACE_MINUTES = 30L

    sealed interface Phase {
        val night: LocalDate
    }

    /** Вечер и ночь перед подъёмом [night]. */
    data class Evening(override val night: LocalDate) : Phase

    /** Утро после ночи [night]: время отметить самочувствие. */
    data class Morning(override val night: LocalDate) : Phase

    /** К какой ночи относится момент — так записывается нажатие «Ложусь». */
    fun nightFor(moment: LocalDateTime): LocalDate =
        if (moment.hour < DAY_BOUNDARY_HOUR) moment.toLocalDate() else moment.toLocalDate().plusDays(1)

    /** Момент отбоя ночи [night] по времени суток: вечер — накануне, после полуночи — в дату подъёма. */
    fun bedMoment(night: LocalDate, minutesOfDay: Int): LocalDateTime {
        val date = if (minutesOfDay >= DAY_BOUNDARY_HOUR * 60) night.minusDays(1) else night
        return date.atStartOfDay().plusMinutes(minutesOfDay.toLong())
    }

    /** Утро начинается за 3 часа до подъёма. */
    fun morningStart(wake: LocalDateTime): LocalDateTime = wake.minusMinutes(MORNING_LEAD_MINUTES)

    fun phase(now: LocalDateTime, wakeToday: LocalDateTime): Phase {
        val today = now.toLocalDate()
        return when {
            now.hour >= DAY_BOUNDARY_HOUR -> Evening(today.plusDays(1))
            now < morningStart(wakeToday) -> Evening(today)
            else -> Morning(today)
        }
    }

    /** Вечер, к которому относятся план и кнопки: утром это уже предстоящий вечер. */
    fun eveningNight(phase: Phase): LocalDate = when (phase) {
        is Evening -> phase.night
        is Morning -> phase.night.plusDays(1)
    }

    /**
     * Ночь, которую можно отметить утром прямо сейчас: окно открыто
     * с «подъём − 3 ч» до первого сигнала следующего вечера.
     */
    fun checkinNight(now: LocalDateTime, phase: Phase, planFor: (LocalDate) -> EveningPlan): LocalDate? {
        val candidate = when (phase) {
            is Morning -> phase.night
            is Evening -> phase.night.minusDays(1)
        }
        val opensAt = morningStart(planFor(candidate).wake)
        val closesAt = planFor(candidate.plusDays(1)).finishSignalAt
        return candidate.takeIf { now >= opensAt && now < closesAt }
    }

    /** Положительное — лёг позже цели, отрицательное — раньше. */
    fun deviationMinutes(bedAt: LocalDateTime, target: LocalDateTime): Long =
        deviationMinutesOfMillis(Duration.between(target, bedAt).toMillis())

    /** Одно правило для экрана и CSV: округление до ближайшей минуты в обе стороны. */
    fun deviationMinutesOfMillis(diffMillis: Long): Long = Math.round(diffMillis / 60_000.0)

    /** «Вовремя» для серии и сдвига: не позже цели + 30 минут (раньше — тоже хорошо). */
    fun isOnTime(deviation: Long): Boolean = deviation <= GRACE_MINUTES

    /** Метрика концепции: в пределах ±30 минут от цели. */
    fun isWithin30(deviation: Long): Boolean = abs(deviation) <= GRACE_MINUTES

    fun outcome(deviation: Long?): Outcome = when {
        deviation == null -> Outcome.UNKNOWN
        isOnTime(deviation) -> Outcome.ON_TIME
        else -> Outcome.LATE
    }
}
