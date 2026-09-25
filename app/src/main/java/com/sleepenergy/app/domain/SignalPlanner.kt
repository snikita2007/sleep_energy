package com.sleepenergy.app.domain

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/** Когда сработает следующий сигнал каждого вида и уместен ли сработавший. */
object SignalPlanner {
    /** Без точных будильников сигнал приходит в окне ±5 минут вокруг нужного времени. */
    const val WINDOW_HALF_MINUTES = 5L
    const val STALE_MINUTES = 30L
    private const val LOOKAHEAD_NIGHTS = 3L

    data class Planned(val kind: SignalKind, val night: LocalDate, val at: LocalDateTime)

    enum class Verdict {
        /** Сработал заметно раньше своего времени: данные от более нового расписания — не наш. */
        EARLY,
        DUE,
        /** Сильно опоздал (телефон спал) — уже не к месту. */
        STALE,
    }

    /**
     * Ближайшее срабатывание [kind]. Ночи, где то, о чём сигнал, уже сделано
     * ([isDone]: лёг, записал выгрузку, отметил утро, сигнал отработал), пропускаются.
     * Сигнал, чьё окно доставки ещё открыто, остаётся «следующим» — так перепланирование
     * в эти минуты его не теряет.
     */
    fun next(
        kind: SignalKind,
        now: LocalDateTime,
        planFor: (LocalDate) -> EveningPlan,
        isDone: (night: LocalDate, kind: SignalKind) -> Boolean,
    ): Planned? {
        val threshold = now.minusMinutes(WINDOW_HALF_MINUTES)
        val today = now.toLocalDate()
        for (offset in 0L..LOOKAHEAD_NIGHTS) {
            val night = today.plusDays(offset)
            val at = planFor(night).signalTime(kind)
            if (at > threshold && !isDone(night, kind)) return Planned(kind, night, at)
        }
        return null
    }

    fun verdict(scheduledAt: LocalDateTime, firedAt: LocalDateTime): Verdict {
        val lateMinutes = Duration.between(scheduledAt, firedAt).toMinutes()
        return when {
            lateMinutes < -(WINDOW_HALF_MINUTES + 1) -> Verdict.EARLY
            lateMinutes > STALE_MINUTES -> Verdict.STALE
            else -> Verdict.DUE
        }
    }
}
