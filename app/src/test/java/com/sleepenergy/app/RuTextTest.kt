package com.sleepenergy.app

import com.sleepenergy.app.domain.EveningPlan
import com.sleepenergy.app.domain.PlanSettings
import com.sleepenergy.app.domain.RuText
import com.sleepenergy.app.domain.SignalKind
import com.sleepenergy.app.domain.TimeFormat
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class RuTextTest {

    @Test
    fun `склонение дней`() {
        assertEquals("1 день", RuText.days(1))
        assertEquals("4 дня", RuText.days(4))
        assertEquals("5 дней", RuText.days(5))
        assertEquals("11 дней", RuText.days(11))
        assertEquals("21 день", RuText.days(21))
        assertEquals("22 дня", RuText.days(22))
        assertEquals("112 дней", RuText.days(112))
        assertEquals("0 дней", RuText.days(0))
    }

    @Test
    fun `длительность`() {
        assertEquals("1 ч 10 мин", RuText.duration(70))
        assertEquals("45 мин", RuText.duration(-45))
        assertEquals("2 ч", RuText.duration(120))
    }

    @Test
    fun `сигналы говорят о своём времени и отбое`() {
        val plan = EveningPlan.build(LocalDate.of(2026, 9, 29), 7 * 60 + 30, PlanSettings(480, 90, 0))

        assertEquals("Через 20 минут заканчиваем дела", RuText.signal(SignalKind.FINISH, plan).title)
        assertEquals(
            "Дальше полтора часа на себя — это часть плана",
            RuText.signal(SignalKind.FINISH, plan).text,
        )
        assertEquals(
            "Запиши дела на завтра — и отбой в 23:30",
            RuText.signal(SignalKind.BRAIN_DUMP, plan).text,
        )
    }

    @Test
    fun `формат времени заворачивает сутки`() {
        assertEquals("07:30", TimeFormat.hhmm(450))
        assertEquals("23:30", TimeFormat.hhmm(-30))
        assertEquals("00:15", TimeFormat.hhmm(24 * 60 + 15))
    }
}
