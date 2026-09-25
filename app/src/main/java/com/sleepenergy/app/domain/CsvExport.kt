package com.sleepenergy.app.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Выгрузка ночей для исследования. Разделитель «;» — так файл сразу открывается
 * в Excel с русской локалью. Текст выгрузки мыслей не экспортируется (личное),
 * только число пунктов.
 */
object CsvExport {
    const val SEPARATOR = ";"

    val HEADER = listOf(
        "night_date", "weekday", "wake_time", "target_bedtime", "bedtime", "bedtime_source",
        "deviation_min", "on_time", "within_30", "rating", "checkin_at", "brain_dump_items", "streak",
    )

    data class Row(
        val night: LocalDate,
        val wakeMinutes: Int,
        val target: LocalDateTime?,
        val bedtime: LocalDateTime?,
        /** evening — кнопка «Ложусь», morning — ответ утром, пусто — нет данных. */
        val bedtimeSource: String,
        val rating: Int?,
        val checkinAt: LocalDateTime?,
        val brainDumpItems: Int,
        val streak: Int,
    )

    private val dateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun build(rows: List<Row>): String = buildString {
        appendLine(HEADER.joinToString(SEPARATOR))
        rows.forEach { appendLine(line(it)) }
    }

    private fun line(row: Row): String {
        val deviation = if (row.bedtime != null && row.target != null) {
            NightMath.deviationMinutes(row.bedtime, row.target)
        } else {
            null
        }
        return listOf(
            row.night.toString(),
            row.night.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
            TimeFormat.hhmm(row.wakeMinutes),
            row.target?.format(dateTime).orEmpty(),
            row.bedtime?.format(dateTime).orEmpty(),
            row.bedtimeSource,
            deviation?.toString().orEmpty(),
            deviation?.let { flag(NightMath.isOnTime(it)) }.orEmpty(),
            deviation?.let { flag(NightMath.isWithin30(it)) }.orEmpty(),
            row.rating?.toString().orEmpty(),
            row.checkinAt?.format(dateTime).orEmpty(),
            row.brainDumpItems.toString(),
            row.streak.toString(),
        ).joinToString(SEPARATOR)
    }

    private fun flag(value: Boolean): String = if (value) "1" else "0"
}
