package com.sleepenergy.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sleepenergy.app.domain.WakeSchedule

/** Единственная строка настроек (id = 0). */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 0,
    val onboarded: Boolean = false,
    /** Первая ночь (дата подъёма), которую застало приложение. */
    val firstNightEpochDay: Long = 0,
    /** Подъём по дням недели, Пн..Вс, минуты от полуночи. */
    val wakeTimesCsv: String = WakeSchedule.encode(List(WakeSchedule.DAYS) { WakeSchedule.DEFAULT_WAKE }),
    val sleepNeedMinutes: Int = 480,
    val ownTimeMinutes: Int = 60,
    /** Постепенный сдвиг: насколько нынешняя цель позже идеала. */
    val shiftOffsetMinutes: Int = 0,
    /** Ночь, на которой был последний шаг сдвига (или старт). */
    val lastShiftEpochDay: Long = 0,
    /** Ответ «сейчас ложусь в…» — от него стартовал сдвиг. */
    val baselineBedMinutes: Int? = null,
    /** Чек-лист среды, биты [EnvChecklist]. */
    val envChecklistMask: Int = 0,
)

/** Одна ночь; ключ — дата подъёма. */
@Entity(tableName = "nights")
data class NightEntity(
    @PrimaryKey val nightEpochDay: Long,
    /** «Завтра подъём в…» — только для этой ночи. */
    val wakeOverrideMinutes: Int? = null,
    /** Снимок подъёма и цели на момент отбоя: метрики не меняются вслед за настройками. */
    val wakeMinutes: Int? = null,
    val targetBedAt: Long? = null,
    val bedAt: Long? = null,
    val bedSource: Int = BedSource.NONE,
    /** Выгрузка мыслей: дела на завтра, по строке на пункт. */
    val brainDump: String? = null,
    val rating: Int? = null,
    val checkinAt: Long? = null,
    /** Какие сигналы этой ночи уже отработали (бит = SignalKind.ordinal) — повторно не приходят. */
    val signalsSentMask: Int = 0,
)

object BedSource {
    const val NONE = 0
    /** Кнопка «Ложусь спать» вечером. */
    const val EVENING = 1
    /** Ответ «во сколько лёг» утром. */
    const val MORNING = 2
}

/** Чек-лист среды: одна настройка при старте вместо силы воли каждый вечер. */
object EnvChecklist {
    const val CHARGER_AWAY = 1
    const val DIM_LIGHT = 2
    const val FRESH_AIR = 4

    val items = listOf(
        CHARGER_AWAY to "Зарядка для телефона — не у кровати",
        DIM_LIGHT to "Вечером свет потеплее и потише",
        FRESH_AIR to "Проветрить комнату перед сном",
    )
}
