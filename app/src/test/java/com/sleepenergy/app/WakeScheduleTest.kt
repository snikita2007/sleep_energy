package com.sleepenergy.app

import com.sleepenergy.app.domain.WakeSchedule
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class WakeScheduleTest {

    @Test
    fun `кодек расписания туда-обратно`() {
        val week = listOf(450, 450, 540, 450, 450, 600, 600)

        assertEquals(week, WakeSchedule.decode(WakeSchedule.encode(week)))
    }

    @Test
    fun `битая строка даёт расписание по умолчанию`() {
        val fallback = List(7) { WakeSchedule.DEFAULT_WAKE }

        assertEquals(fallback, WakeSchedule.decode(""))
        assertEquals(fallback, WakeSchedule.decode("450,450"))
        assertEquals(fallback, WakeSchedule.decode("450,450,450,450,450,450,9999"))
    }

    @Test
    fun `будни и выходные`() {
        assertEquals(listOf(450, 450, 450, 450, 450, 510, 510), WakeSchedule.weekdaysAndWeekend(450, 510))
    }

    @Test
    fun `подъём ночи берётся по дню недели даты подъёма, переопределение важнее`() {
        val week = listOf(450, 460, 470, 480, 490, 600, 610)
        val monday = LocalDate.of(2026, 9, 28)

        assertEquals(450, WakeSchedule.wakeMinutesFor(monday, week, null))
        assertEquals(610, WakeSchedule.wakeMinutesFor(monday.minusDays(1), week, null))
        assertEquals(540, WakeSchedule.wakeMinutesFor(monday, week, 540))
    }

    @Test
    fun `типичный подъём - самый частый`() {
        assertEquals(450, WakeSchedule.typicalWake(listOf(450, 450, 450, 450, 450, 510, 510)))
        assertEquals(480, WakeSchedule.typicalWake(listOf(480, 480, 500, 500, 520, 540, 560)))
    }
}
