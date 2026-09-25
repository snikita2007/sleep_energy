package com.sleepenergy.app

import com.sleepenergy.app.domain.EveningPlan
import com.sleepenergy.app.domain.PlanSettings
import com.sleepenergy.app.domain.Planner
import com.sleepenergy.app.domain.SignalKind
import com.sleepenergy.app.domain.Stage
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class EveningPlanTest {

    private val night = LocalDate.of(2026, 9, 29)
    private val evening = night.minusDays(1)
    private val settings = PlanSettings(sleepNeedMinutes = 480, ownTimeMinutes = 60, shiftOffsetMinutes = 0)

    @Test
    fun `пример из концепции - подъём в 7-30, отбой в 23-30`() {
        val plan = EveningPlan.build(night, 7 * 60 + 30, settings)

        assertEquals(evening.atTime(21, 10), plan.finishSignalAt)
        assertEquals(evening.atTime(21, 30), plan.ownTimeStart)
        assertEquals(evening.atTime(22, 30), plan.ownTimeEnd)
        assertEquals(evening.atTime(22, 45), plan.windDownStart)
        assertEquals(evening.atTime(23, 20), plan.brainDumpAt)
        assertEquals(evening.atTime(23, 30), plan.bedtime)
        assertEquals(night.atTime(7, 40), plan.morningSignalAt)
    }

    @Test
    fun `вечерних сигналов ровно три`() {
        assertEquals(3, SignalKind.entries.count { it.evening })
    }

    @Test
    fun `сдвиг переносит отбой через полночь`() {
        val plan = EveningPlan.build(night, 7 * 60 + 30, settings.copy(shiftOffsetMinutes = 45))

        assertEquals(night.atTime(0, 15), plan.bedtime)
        assertEquals(evening.atTime(23, 30), plan.windDownStart)
        assertEquals(evening.atTime(21, 55), plan.finishSignalAt)
    }

    @Test
    fun `длинное своё время начинается раньше, первый сигнал тоже`() {
        val plan = EveningPlan.build(night, 7 * 60 + 30, settings.copy(ownTimeMinutes = 90))

        assertEquals(evening.atTime(21, 0), plan.ownTimeStart)
        assertEquals(evening.atTime(20, 40), plan.finishSignalAt)
        assertEquals(evening.atTime(23, 30), plan.bedtime)
    }

    @Test
    fun `этапы вечера по времени`() {
        val plan = EveningPlan.build(night, 7 * 60 + 30, settings)

        assertEquals(Stage.DAY, plan.stageAt(evening.atTime(15, 0)))
        assertEquals(Stage.FINISH_UP, plan.stageAt(evening.atTime(21, 15)))
        assertEquals(Stage.OWN_TIME, plan.stageAt(evening.atTime(22, 0)))
        assertEquals(Stage.OWN_TIME, plan.stageAt(evening.atTime(22, 40)))
        assertEquals(Stage.WIND_DOWN, plan.stageAt(evening.atTime(22, 45)))
        assertEquals(Stage.BRAIN_DUMP, plan.stageAt(evening.atTime(23, 25)))
        assertEquals(Stage.BEDTIME, plan.stageAt(night.atTime(0, 10)))
    }

    @Test
    fun `переопределение подъёма важнее недели, цель - план без сдвига`() {
        val planner = Planner(
            week = List(7) { 7 * 60 + 30 },
            overrides = mapOf(night to 9 * 60),
            settings = settings.copy(shiftOffsetMinutes = 30),
        )

        assertEquals(night.atTime(1, 30), planner.plan(night).bedtime)
        assertEquals(night.atTime(1, 0), planner.goalPlan(night).bedtime)
        assertEquals(night.plusDays(1).atTime(0, 0), planner.plan(night.plusDays(1)).bedtime)
    }
}
