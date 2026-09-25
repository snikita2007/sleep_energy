package com.sleepenergy.app

import com.sleepenergy.app.domain.Outcome
import com.sleepenergy.app.domain.Outcome.LATE
import com.sleepenergy.app.domain.Outcome.ON_TIME
import com.sleepenergy.app.domain.Outcome.UNKNOWN
import com.sleepenergy.app.domain.StreakMath
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakMathTest {

    private val first = LocalDate.of(2026, 9, 1)

    /** Исходы ночей подряд, начиная с [first]. */
    private fun nights(vararg outcomes: Outcome): Map<LocalDate, Outcome> =
        outcomes.withIndex().associate { (i, o) -> first.plusDays(i.toLong()) to o }

    private fun last(count: Int): LocalDate = first.plusDays(count - 1L)

    @Test
    fun `все ночи вовремя - серия равна числу ночей`() {
        val streak = StreakMath.compute(nights(ON_TIME, ON_TIME, ON_TIME, ON_TIME, ON_TIME), first, last(5))

        assertEquals(5, streak.days)
        assertNull(streak.lastForgiven)
    }

    @Test
    fun `один срыв прощается - серия не растёт, но и не обнуляется`() {
        val streak = StreakMath.compute(nights(ON_TIME, ON_TIME, LATE, ON_TIME, ON_TIME), first, last(5))

        assertEquals(4, streak.days)
        assertEquals(first.plusDays(2), streak.lastForgiven)
    }

    @Test
    fun `второй срыв ближе 7 ночей обрывает серию`() {
        val streak = StreakMath.compute(nights(ON_TIME, LATE, ON_TIME, ON_TIME, LATE, ON_TIME), first, last(6))

        assertEquals(3, streak.days)
    }

    @Test
    fun `срывы через 7 ночей прощаются оба`() {
        val outcomes = nights(LATE, ON_TIME, ON_TIME, ON_TIME, ON_TIME, ON_TIME, ON_TIME, LATE, ON_TIME)

        assertEquals(7, StreakMath.compute(outcomes, first, last(9)).days)
    }

    @Test
    fun `ночь без данных - тоже срыв, если она уже закрыта`() {
        val streak = StreakMath.compute(nights(ON_TIME, UNKNOWN, UNKNOWN, ON_TIME), first, last(4))

        assertEquals(1, streak.days)
    }

    @Test
    fun `открытая ночь без данных нейтральна`() {
        val outcomes = nights(ON_TIME, ON_TIME, ON_TIME, UNKNOWN)

        val open = StreakMath.compute(outcomes, first, last(4), neutralFrom = last(4))
        assertEquals(3, open.days)
        assertNull(open.lastForgiven)

        val closed = StreakMath.compute(outcomes, first, last(4))
        assertEquals(3, closed.days)
        assertEquals(last(4), closed.lastForgiven)
    }

    @Test
    fun `ночи до онбординга не считаются`() {
        val outcomes = nights(ON_TIME, ON_TIME, ON_TIME, ON_TIME)

        assertEquals(2, StreakMath.compute(outcomes, first.plusDays(2), last(4)).days)
    }

    @Test
    fun `запасной вечер есть, пока срывов не было`() {
        val outcomes = nights(ON_TIME, ON_TIME, ON_TIME, ON_TIME, ON_TIME)

        assertTrue(StreakMath.spareAvailable(outcomes, first, last(5), last(6), tonight = last(6)))
    }

    @Test
    fun `после недавнего срыва запасного вечера нет`() {
        val outcomes = nights(ON_TIME, ON_TIME, LATE, ON_TIME, ON_TIME)

        assertFalse(StreakMath.spareAvailable(outcomes, first, last(5), last(6), tonight = last(6)))
    }
}
