package com.sleepenergy.app

import com.sleepenergy.app.data.NightEntity
import com.sleepenergy.app.data.isDone
import com.sleepenergy.app.data.outcome
import com.sleepenergy.app.data.withSignalSent
import com.sleepenergy.app.domain.Outcome
import com.sleepenergy.app.domain.SignalKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NightEntityTest {

    private val night = NightEntity(nightEpochDay = 20_000)

    @Test
    fun `отработавший сигнал второй раз не нужен, остальные - нужны`() {
        val sent = night.withSignalSent(SignalKind.FINISH)

        assertFalse(night.isDone(SignalKind.FINISH))
        assertTrue(sent.isDone(SignalKind.FINISH))
        assertFalse(sent.isDone(SignalKind.WIND_DOWN))
    }

    @Test
    fun `после отбоя вечерние сигналы не нужны, утренний - нужен`() {
        val inBed = night.copy(bedAt = 1L, targetBedAt = 1L)

        assertTrue(inBed.isDone(SignalKind.FINISH))
        assertTrue(inBed.isDone(SignalKind.WIND_DOWN))
        assertTrue(inBed.isDone(SignalKind.BRAIN_DUMP))
        assertFalse(inBed.isDone(SignalKind.MORNING))
    }

    @Test
    fun `записанная выгрузка снимает только её сигнал`() {
        val dumped = night.copy(brainDump = "позвонить маме")

        assertTrue(dumped.isDone(SignalKind.BRAIN_DUMP))
        assertFalse(dumped.isDone(SignalKind.WIND_DOWN))
    }

    @Test
    fun `исход ночи - по снимку цели`() {
        val target = 1_000_000_000L

        assertEquals(Outcome.ON_TIME, night.copy(bedAt = target + 30 * 60_000, targetBedAt = target).outcome)
        assertEquals(Outcome.LATE, night.copy(bedAt = target + 31 * 60_000, targetBedAt = target).outcome)
        assertEquals(Outcome.UNKNOWN, night.outcome)
    }
}
