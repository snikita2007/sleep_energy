package com.sleepenergy.app.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Постепенный сдвиг: цель начинается на шаг раньше нынешнего отбоя и сдвигается
 * к идеалу на 15 минут, когда из последних 3 ночей хотя бы 2 удались.
 * Сама цель никогда не отодвигается назад — без наказаний.
 */
object ShiftMath {
    const val STEP_MINUTES = 15
    const val EVERY_DAYS = 3L
    const val WINDOW_NIGHTS = 3
    const val NEEDED_ON_TIME = 2
    const val MAX_OFFSET_MINUTES = 180

    /**
     * Стартовое смещение от идеала (подъём − сон): на шаг раньше, чем человек
     * ложится сейчас, с округлением вниз до 15 минут.
     */
    fun initialOffset(currentBedMinutes: Int, wakeMinutes: Int, sleepNeedMinutes: Int): Int {
        val ideal = wakeMinutes - sleepNeedMinutes
        // Время до полуночи — это ещё вчерашний вечер относительно даты подъёма.
        val current = if (currentBedMinutes >= 12 * 60) currentBedMinutes - 24 * 60 else currentBedMinutes
        val late = current - ideal
        if (late <= 0) return 0
        val stepped = late / STEP_MINUTES * STEP_MINUTES - STEP_MINUTES
        return stepped.coerceIn(0, MAX_OFFSET_MINUTES)
    }

    /**
     * Новое смещение после утренней отметки ночи [night] или null, если шагать рано.
     * @param recent исходы ночей начиная с [night] и назад
     */
    fun nextOffset(offset: Int, lastShiftNight: LocalDate, night: LocalDate, recent: List<Outcome>): Int? {
        if (offset <= 0) return null
        if (ChronoUnit.DAYS.between(lastShiftNight, night) < EVERY_DAYS) return null
        if (recent.take(WINDOW_NIGHTS).count { it == Outcome.ON_TIME } < NEEDED_ON_TIME) return null
        return (offset - STEP_MINUTES).coerceAtLeast(0)
    }

    fun stepsLeft(offset: Int): Int = (offset + STEP_MINUTES - 1) / STEP_MINUTES
}
