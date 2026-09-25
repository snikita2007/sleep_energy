package com.sleepenergy.app

import com.sleepenergy.app.domain.CsvExport
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CsvExportTest {

    private val night = LocalDate.of(2026, 9, 29)

    @Test
    fun `заголовок и строка с данными`() {
        val csv = CsvExport.build(
            listOf(
                CsvExport.Row(
                    night = night,
                    wakeMinutes = 7 * 60 + 30,
                    target = night.minusDays(1).atTime(23, 30),
                    bedtime = night.atTime(0, 12),
                    bedtimeSource = "evening",
                    rating = 7,
                    checkinAt = night.atTime(7, 41),
                    brainDumpItems = 3,
                    streak = 4,
                ),
            ),
        )
        val lines = csv.trimEnd().lines()

        assertEquals(CsvExport.HEADER.joinToString(";"), lines[0])
        assertEquals(
            "2026-09-29;Tue;07:30;2026-09-28 23:30;2026-09-29 00:12;evening;42;0;0;7;2026-09-29 07:41;3;4",
            lines[1],
        )
    }

    @Test
    fun `ночь без данных - пустые поля, но строка есть`() {
        val csv = CsvExport.build(
            listOf(
                CsvExport.Row(
                    night = night,
                    wakeMinutes = 9 * 60,
                    target = null,
                    bedtime = null,
                    bedtimeSource = "",
                    rating = null,
                    checkinAt = null,
                    brainDumpItems = 0,
                    streak = 0,
                ),
            ),
        )

        val expected = (listOf("2026-09-29", "Tue", "09:00") + List(8) { "" } + listOf("0", "0"))
            .joinToString(";")
        assertEquals(expected, csv.trimEnd().lines()[1])
    }
}
