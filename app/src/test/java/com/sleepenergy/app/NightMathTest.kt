package com.sleepenergy.app

import com.sleepenergy.app.domain.NightMath
import com.sleepenergy.app.domain.Outcome
import com.sleepenergy.app.domain.PlanSettings
import com.sleepenergy.app.domain.Planner
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NightMathTest {

    private val day = LocalDate.of(2026, 9, 28)
    private val wake = day.atTime(7, 30)
    private val planner = Planner(List(7) { 7 * 60 + 30 }, emptyMap(), PlanSettings(480, 60, 0))

    @Test
    fun `вечер относится к ночи следующей даты`() {
        assertEquals(day.plusDays(1), NightMath.nightFor(day.atTime(23, 30)))
        assertEquals(day.plusDays(1), NightMath.nightFor(day.atTime(12, 0)))
    }

    @Test
    fun `после полуночи и до полудня - та же дата`() {
        assertEquals(day, NightMath.nightFor(day.atTime(0, 40)))
        assertEquals(day, NightMath.nightFor(day.atTime(11, 59)))
    }

    @Test
    fun `фазы - вечер, ночь после полуночи, утро`() {
        assertEquals(NightMath.Evening(day.plusDays(1)), NightMath.phase(day.atTime(15, 0), wake))
        assertEquals(NightMath.Evening(day), NightMath.phase(day.atTime(1, 0), wake))
        assertEquals(NightMath.Morning(day), NightMath.phase(day.atTime(4, 30), wake))
        assertEquals(NightMath.Morning(day), NightMath.phase(day.atTime(11, 0), wake))
    }

    @Test
    fun `утром кнопки относятся к предстоящему вечеру`() {
        assertEquals(day.plusDays(1), NightMath.eveningNight(NightMath.Morning(day)))
        assertEquals(day, NightMath.eveningNight(NightMath.Evening(day)))
    }

    @Test
    fun `момент отбоя по времени суток - вечер накануне, ночь в дату подъёма`() {
        assertEquals(day.minusDays(1).atTime(23, 40), NightMath.bedMoment(day, 23 * 60 + 40))
        assertEquals(day.atTime(0, 30), NightMath.bedMoment(day, 30))
    }

    @Test
    fun `отклонение считается через полночь`() {
        val target = day.minusDays(1).atTime(23, 30)

        assertEquals(40L, NightMath.deviationMinutes(day.atTime(0, 10), target))
        assertEquals(-15L, NightMath.deviationMinutes(day.minusDays(1).atTime(23, 15), target))
    }

    @Test
    fun `отклонение округляется до ближайшей минуты одинаково в обе стороны`() {
        val target = day.minusDays(1).atTime(23, 30)

        assertEquals(-8L, NightMath.deviationMinutes(target.minusSeconds(479).minusNanos(800_000_000), target))
        assertEquals(-7L, NightMath.deviationMinutes(target.minusSeconds(425), target))
        assertEquals(30L, NightMath.deviationMinutes(target.plusSeconds(30 * 60 + 29), target))
        assertEquals(31L, NightMath.deviationMinutes(target.plusSeconds(30 * 60 + 31), target))
        assertEquals(-8L, NightMath.deviationMinutesOfMillis(-479_800))
    }

    @Test
    fun `вовремя - не позже цели плюс 30 минут, раньше тоже вовремя`() {
        assertTrue(NightMath.isOnTime(30))
        assertFalse(NightMath.isOnTime(31))
        assertTrue(NightMath.isOnTime(-90))
        assertEquals(Outcome.UNKNOWN, NightMath.outcome(null))
        assertEquals(Outcome.LATE, NightMath.outcome(45))
    }

    @Test
    fun `метрика концепции - в пределах плюс-минус 30 минут`() {
        assertTrue(NightMath.isWithin30(-30))
        assertFalse(NightMath.isWithin30(-31))
        assertFalse(NightMath.isWithin30(31))
    }

    @Test
    fun `утро можно отметить до первого сигнала следующего вечера`() {
        val morning = day.atTime(8, 0)
        assertEquals(day, NightMath.checkinNight(morning, NightMath.phase(morning, wake), planner::plan))

        val afternoon = day.atTime(15, 0)
        assertEquals(day, NightMath.checkinNight(afternoon, NightMath.phase(afternoon, wake), planner::plan))

        val evening = day.atTime(21, 15)
        assertNull(NightMath.checkinNight(evening, NightMath.phase(evening, wake), planner::plan))
    }

    @Test
    fun `глубокой ночью отмечать нечего`() {
        val lateNight = day.atTime(1, 0)
        assertNull(NightMath.checkinNight(lateNight, NightMath.phase(lateNight, wake), planner::plan))
    }
}
