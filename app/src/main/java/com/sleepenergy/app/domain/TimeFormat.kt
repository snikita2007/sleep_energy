package com.sleepenergy.app.domain

import java.time.LocalDateTime
import java.util.Locale

object TimeFormat {
    /** Минуты от полуночи → «07:30»; значения за пределами суток заворачиваются. */
    fun hhmm(minutes: Int): String {
        val m = Math.floorMod(minutes, WakeSchedule.MINUTES_PER_DAY)
        return String.format(Locale.ROOT, "%02d:%02d", m / 60, m % 60)
    }

    fun hhmm(time: LocalDateTime): String = hhmm(minutesOfDay(time))

    fun minutesOfDay(time: LocalDateTime): Int = time.hour * 60 + time.minute
}
