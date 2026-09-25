package com.sleepenergy.app

import com.sleepenergy.app.domain.Outcome
import com.sleepenergy.app.domain.Progress
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressTest {

    private val first = LocalDate.of(2026, 9, 1)

    @Test
    fun `неделя - доля вовремя и средняя оценка против прошлой недели`() {
        val stats = (0L until 14L).associate { i ->
            val night = first.plusDays(i)
            val onTime = i >= 7 && i % 2 == 1L
            night to Progress.NightStat(
                outcome = if (onTime) Outcome.ON_TIME else Outcome.LATE,
                rating = if (i < 7) 5 else 7,
            )
        }

        val week = Progress.week(stats, first, first.plusDays(13))

        assertEquals(7, week.nights)
        assertEquals(4, week.onTime)
        assertEquals(7.0, week.avgRating!!, 0.001)
        assertEquals(5.0, week.prevAvgRating!!, 0.001)
    }

    @Test
    fun `первые дни - неделя короче, прошлой недели нет`() {
        val stats = mapOf(first to Progress.NightStat(Outcome.ON_TIME, 8))

        val week = Progress.week(stats, first, first.plusDays(1))

        assertEquals(2, week.nights)
        assertEquals(1, week.onTime)
        assertEquals(8.0, week.avgRating!!, 0.001)
        assertNull(week.prevAvgRating)
    }
}
