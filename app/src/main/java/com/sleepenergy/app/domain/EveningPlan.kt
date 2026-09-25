package com.sleepenergy.app.domain

import java.time.LocalDate
import java.time.LocalDateTime

/** Виды сигналов. Вечерних ровно три — концепция запрещает присылать больше. */
enum class SignalKind(val evening: Boolean) {
    FINISH(evening = true),
    WIND_DOWN(evening = true),
    BRAIN_DUMP(evening = true),
    MORNING(evening = false),
}

/** Этапы вечера: закончить дела → своё время → подготовка ко сну → выгрузка мыслей → отбой. */
enum class Stage { DAY, FINISH_UP, OWN_TIME, WIND_DOWN, BRAIN_DUMP, BEDTIME }

/** Настройки, из которых строится план любой ночи. */
data class PlanSettings(
    val sleepNeedMinutes: Int,
    val ownTimeMinutes: Int,
    /** Насколько текущая цель позже идеала (постепенный сдвиг), минуты ≥ 0. */
    val shiftOffsetMinutes: Int,
)

/**
 * Вечерний сценарий одной ночи. Всё считается от времени подъёма:
 * отбой = подъём − сон + сдвиг; от отбоя назад — подготовка ко сну (45 мин),
 * запас (15 мин), чтобы спокойно досмотреть серию, своё время
 * и сигнал «закругляемся» за 20 минут до него.
 */
data class EveningPlan(
    /** Ночь — дата подъёма. */
    val night: LocalDate,
    val wake: LocalDateTime,
    val bedtime: LocalDateTime,
    val finishSignalAt: LocalDateTime,
    val ownTimeStart: LocalDateTime,
    val ownTimeEnd: LocalDateTime,
    val windDownStart: LocalDateTime,
    val brainDumpAt: LocalDateTime,
    val morningSignalAt: LocalDateTime,
    val ownTimeMinutes: Int,
) {
    fun signalTime(kind: SignalKind): LocalDateTime = when (kind) {
        SignalKind.FINISH -> finishSignalAt
        SignalKind.WIND_DOWN -> windDownStart
        SignalKind.BRAIN_DUMP -> brainDumpAt
        SignalKind.MORNING -> morningSignalAt
    }

    /** Запас после своего времени считается его частью: досматриваем и закругляемся. */
    fun stageAt(now: LocalDateTime): Stage = when {
        now < finishSignalAt -> Stage.DAY
        now < ownTimeStart -> Stage.FINISH_UP
        now < windDownStart -> Stage.OWN_TIME
        now < brainDumpAt -> Stage.WIND_DOWN
        now < bedtime -> Stage.BRAIN_DUMP
        else -> Stage.BEDTIME
    }

    companion object {
        const val WIND_DOWN_MINUTES = 45L
        const val BUFFER_MINUTES = 15L
        const val FINISH_LEAD_MINUTES = 20L
        const val BRAIN_DUMP_LEAD_MINUTES = 10L
        const val MORNING_DELAY_MINUTES = 10L

        fun build(night: LocalDate, wakeMinutes: Int, settings: PlanSettings): EveningPlan {
            val wake = night.atStartOfDay().plusMinutes(wakeMinutes.toLong())
            val bedtime = wake
                .minusMinutes(settings.sleepNeedMinutes.toLong())
                .plusMinutes(settings.shiftOffsetMinutes.toLong())
            val windDown = bedtime.minusMinutes(WIND_DOWN_MINUTES)
            val ownEnd = windDown.minusMinutes(BUFFER_MINUTES)
            val ownStart = ownEnd.minusMinutes(settings.ownTimeMinutes.toLong())
            return EveningPlan(
                night = night,
                wake = wake,
                bedtime = bedtime,
                finishSignalAt = ownStart.minusMinutes(FINISH_LEAD_MINUTES),
                ownTimeStart = ownStart,
                ownTimeEnd = ownEnd,
                windDownStart = windDown,
                brainDumpAt = bedtime.minusMinutes(BRAIN_DUMP_LEAD_MINUTES),
                morningSignalAt = wake.plusMinutes(MORNING_DELAY_MINUTES),
                ownTimeMinutes = settings.ownTimeMinutes,
            )
        }
    }
}

/** Источник планов: недельное расписание подъёма плюс переопределения отдельных ночей. */
class Planner(
    private val week: List<Int>,
    private val overrides: Map<LocalDate, Int>,
    val settings: PlanSettings,
) {
    fun wakeMinutes(night: LocalDate): Int = WakeSchedule.wakeMinutesFor(night, week, overrides[night])

    fun plan(night: LocalDate): EveningPlan = EveningPlan.build(night, wakeMinutes(night), settings)

    /** Цель, к которой ведёт постепенный сдвиг: тот же план без смещения. */
    fun goalPlan(night: LocalDate): EveningPlan =
        EveningPlan.build(night, wakeMinutes(night), settings.copy(shiftOffsetMinutes = 0))
}
