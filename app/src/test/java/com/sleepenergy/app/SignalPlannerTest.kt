package com.sleepenergy.app

import com.sleepenergy.app.domain.PlanSettings
import com.sleepenergy.app.domain.Planner
import com.sleepenergy.app.domain.SignalKind
import com.sleepenergy.app.domain.SignalPlanner
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class SignalPlannerTest {

    private val planner = Planner(List(7) { 7 * 60 + 30 }, emptyMap(), PlanSettings(480, 60, 0))
    private val day = LocalDate.of(2026, 9, 28)

    @Test
    fun `днём ближайший сигнал - сегодняшний вечер`() {
        val next = SignalPlanner.next(SignalKind.FINISH, day.atTime(15, 0), planner::plan) { _, _ -> false }

        assertEquals(day.atTime(21, 10), next?.at)
        assertEquals(day.plusDays(1), next?.night)
    }

    @Test
    fun `прошедший сигнал переносится на завтра`() {
        val next = SignalPlanner.next(SignalKind.FINISH, day.atTime(21, 16), planner::plan) { _, _ -> false }

        assertEquals(day.plusDays(1).atTime(21, 10), next?.at)
    }

    @Test
    fun `после «Ложусь» сигналы этой ночи пропускаются`() {
        val tonight = day.plusDays(1)
        val next = SignalPlanner.next(SignalKind.BRAIN_DUMP, day.atTime(22, 50), planner::plan) { night, _ ->
            night == tonight
        }

        assertEquals(day.plusDays(1).atTime(23, 20), next?.at)
    }

    @Test
    fun `утренний сигнал - через 10 минут после подъёма`() {
        val next = SignalPlanner.next(SignalKind.MORNING, day.atTime(23, 0), planner::plan) { _, _ -> false }

        assertEquals(day.plusDays(1).atTime(7, 40), next?.at)
    }

    @Test
    fun `пока окно доставки открыто, неотработавший сигнал остаётся следующим`() {
        val due = SignalPlanner.next(SignalKind.FINISH, day.atTime(21, 13), planner::plan) { _, _ -> false }
        assertEquals(day.atTime(21, 10), due?.at)

        val sent = SignalPlanner.next(SignalKind.FINISH, day.atTime(21, 13), planner::plan) { night, _ ->
            night == day.plusDays(1)
        }
        assertEquals(day.plusDays(1).atTime(21, 10), sent?.at)

        val closed = SignalPlanner.next(SignalKind.FINISH, day.atTime(21, 16), planner::plan) { _, _ -> false }
        assertEquals(day.plusDays(1).atTime(21, 10), closed?.at)
    }

    @Test
    fun `вердикт - в окне вовремя, сильно раньше - чужой, позже 30 минут - устарел`() {
        val at = day.atTime(21, 10)

        assertEquals(SignalPlanner.Verdict.DUE, SignalPlanner.verdict(at, day.atTime(21, 5)))
        assertEquals(SignalPlanner.Verdict.DUE, SignalPlanner.verdict(at, day.atTime(21, 40)))
        assertEquals(SignalPlanner.Verdict.STALE, SignalPlanner.verdict(at, day.atTime(21, 41)))
        assertEquals(SignalPlanner.Verdict.EARLY, SignalPlanner.verdict(at, day.atTime(21, 3)))
        assertEquals(SignalPlanner.Verdict.EARLY, SignalPlanner.verdict(day.plusDays(1).atTime(0, 30), day.atTime(1, 1)))
    }
}
