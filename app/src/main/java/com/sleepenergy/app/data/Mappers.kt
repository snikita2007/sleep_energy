package com.sleepenergy.app.data

import com.sleepenergy.app.domain.NightMath
import com.sleepenergy.app.domain.Outcome
import com.sleepenergy.app.domain.PlanSettings
import com.sleepenergy.app.domain.Planner
import com.sleepenergy.app.domain.SignalKind
import com.sleepenergy.app.domain.WakeSchedule
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

fun Long.toLocalDateTime(): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()

fun LocalDateTime.toEpochMillis(): Long =
    atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

val SettingsEntity.week: List<Int> get() = WakeSchedule.decode(wakeTimesCsv)

val SettingsEntity.firstNight: LocalDate get() = LocalDate.ofEpochDay(firstNightEpochDay)

val SettingsEntity.planSettings: PlanSettings
    get() = PlanSettings(sleepNeedMinutes, ownTimeMinutes, shiftOffsetMinutes)

/** Планировщик ночей с учётом «завтра подъём в…» из записей ночей. */
fun SettingsEntity.planner(nights: Collection<NightEntity>): Planner = Planner(
    week = week,
    overrides = nights
        .mapNotNull { n -> n.wakeOverrideMinutes?.let { LocalDate.ofEpochDay(n.nightEpochDay) to it } }
        .toMap(),
    settings = planSettings,
)

val NightEntity.night: LocalDate get() = LocalDate.ofEpochDay(nightEpochDay)

val NightEntity.deviationMinutes: Long?
    get() = if (bedAt != null && targetBedAt != null) NightMath.deviationMinutesOfMillis(bedAt - targetBedAt) else null

val NightEntity.outcome: Outcome get() = NightMath.outcome(deviationMinutes)

val NightEntity.brainDumpItems: List<String>
    get() = brainDump?.lines()?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()

fun NightEntity.signalSent(kind: SignalKind): Boolean = signalsSentMask and (1 shl kind.ordinal) != 0

fun NightEntity.withSignalSent(kind: SignalKind): NightEntity =
    copy(signalsSentMask = signalsSentMask or (1 shl kind.ordinal))

/** Сигнал не нужен: он уже отработал или сделано то, о чём он напоминает. */
fun NightEntity.isDone(kind: SignalKind): Boolean = signalSent(kind) || when (kind) {
    SignalKind.FINISH, SignalKind.WIND_DOWN -> bedAt != null
    SignalKind.BRAIN_DUMP -> bedAt != null || brainDump != null
    SignalKind.MORNING -> rating != null
}
