package com.sleepenergy.app

import com.sleepenergy.app.domain.Outcome.LATE
import com.sleepenergy.app.domain.Outcome.ON_TIME
import com.sleepenergy.app.domain.Outcome.UNKNOWN
import com.sleepenergy.app.domain.ShiftMath
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShiftMathTest {

    private val wake = 7 * 60 + 30
    private val sleep = 480
    private val lastShift = LocalDate.of(2026, 9, 10)

    @Test
    fun `старт на шаг раньше нынешнего отбоя - 00-30 даёт цель 00-15`() {
        assertEquals(45, ShiftMath.initialOffset(30, wake, sleep))
    }

    @Test
    fun `кто уже ложится вовремя, сразу получает идеал`() {
        assertEquals(0, ShiftMath.initialOffset(23 * 60 + 30, wake, sleep))
        assertEquals(0, ShiftMath.initialOffset(22 * 60, wake, sleep))
        assertEquals(0, ShiftMath.initialOffset(23 * 60 + 45, wake, sleep))
    }

    @Test
    fun `опоздание округляется вниз до 15 минут и ограничено тремя часами`() {
        assertEquals(45, ShiftMath.initialOffset(40, wake, sleep))
        assertEquals(180, ShiftMath.initialOffset(5 * 60, wake, sleep))
    }

    @Test
    fun `шаг - через 3 дня, если 2 из 3 ночей вовремя`() {
        val night = lastShift.plusDays(3)

        assertEquals(30, ShiftMath.nextOffset(45, lastShift, night, listOf(ON_TIME, LATE, ON_TIME)))
    }

    @Test
    fun `раньше 3 дней не шагаем`() {
        assertNull(ShiftMath.nextOffset(45, lastShift, lastShift.plusDays(2), listOf(ON_TIME, ON_TIME, ON_TIME)))
    }

    @Test
    fun `одной удачной ночи из трёх мало, цель не отодвигается назад`() {
        assertNull(ShiftMath.nextOffset(45, lastShift, lastShift.plusDays(5), listOf(ON_TIME, LATE, UNKNOWN)))
    }

    @Test
    fun `на идеале шагать некуда, ниже нуля не уходим`() {
        assertNull(ShiftMath.nextOffset(0, lastShift, lastShift.plusDays(9), listOf(ON_TIME, ON_TIME, ON_TIME)))
        assertEquals(0, ShiftMath.nextOffset(10, lastShift, lastShift.plusDays(3), listOf(ON_TIME, ON_TIME, ON_TIME)))
    }

    @Test
    fun `осталось шагов`() {
        assertEquals(3, ShiftMath.stepsLeft(45))
        assertEquals(1, ShiftMath.stepsLeft(10))
        assertEquals(0, ShiftMath.stepsLeft(0))
    }
}
