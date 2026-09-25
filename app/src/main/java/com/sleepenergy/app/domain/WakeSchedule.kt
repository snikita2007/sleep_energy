package com.sleepenergy.app.domain

import java.time.LocalDate

/** Время подъёма по дням недели: минуты от полуночи, индекс 0 — понедельник. */
object WakeSchedule {
    const val DAYS = 7
    const val MINUTES_PER_DAY = 24 * 60
    const val DEFAULT_WAKE = 7 * 60 + 30

    fun encode(week: List<Int>): String = week.joinToString(",")

    /** Битую строку не превращаем в падение — возвращаем расписание по умолчанию. */
    fun decode(csv: String): List<Int> {
        val parsed = csv.split(",").map { it.trim().toIntOrNull() }
        val valid = parsed.size == DAYS && parsed.all { it != null && it in 0 until MINUTES_PER_DAY }
        return if (valid) parsed.map { it!! } else List(DAYS) { DEFAULT_WAKE }
    }

    fun weekdaysAndWeekend(weekday: Int, weekend: Int): List<Int> =
        List(DAYS) { if (it < 5) weekday else weekend }

    /** Переопределение конкретной ночи важнее недельного расписания. */
    fun wakeMinutesFor(night: LocalDate, week: List<Int>, override: Int?): Int =
        override ?: week[night.dayOfWeek.value - 1]

    /** Самое частое время подъёма (при равенстве — то, что раньше по неделе). */
    fun typicalWake(week: List<Int>): Int = week.maxBy { time -> week.count { it == time } }
}
