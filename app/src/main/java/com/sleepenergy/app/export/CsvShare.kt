package com.sleepenergy.app.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.sleepenergy.app.data.BedSource
import com.sleepenergy.app.data.NightEntity
import com.sleepenergy.app.data.SettingsEntity
import com.sleepenergy.app.data.brainDumpItems
import com.sleepenergy.app.data.firstNight
import com.sleepenergy.app.data.night
import com.sleepenergy.app.data.outcome
import com.sleepenergy.app.data.planner
import com.sleepenergy.app.data.toLocalDateTime
import com.sleepenergy.app.domain.CsvExport
import com.sleepenergy.app.domain.StreakMath
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Выгрузка ночей в CSV и отправка через «Поделиться». */
object CsvShare {
    private const val DIR = "export"

    /** Строка на каждую ночь от первой до [lastNight] — пропуски тоже важны для исследования. */
    fun rows(settings: SettingsEntity, nights: List<NightEntity>, lastNight: LocalDate): List<CsvExport.Row> {
        val first = settings.firstNight
        if (lastNight < first) return emptyList()
        val byNight = nights.associateBy { it.night }
        val outcomes = nights.associate { it.night to it.outcome }
        val planner = settings.planner(nights)
        return generateSequence(first) { it.plusDays(1) }
            .takeWhile { it <= lastNight }
            .map { night ->
                val record = byNight[night]
                CsvExport.Row(
                    night = night,
                    wakeMinutes = record?.wakeMinutes ?: planner.wakeMinutes(night),
                    target = record?.targetBedAt?.toLocalDateTime(),
                    bedtime = record?.bedAt?.toLocalDateTime(),
                    bedtimeSource = when (record?.bedSource) {
                        BedSource.EVENING -> "evening"
                        BedSource.MORNING -> "morning"
                        else -> ""
                    },
                    rating = record?.rating,
                    checkinAt = record?.checkinAt?.toLocalDateTime(),
                    brainDumpItems = record?.brainDumpItems?.size ?: 0,
                    streak = StreakMath.compute(outcomes, first, night).days,
                )
            }
            .toList()
    }

    suspend fun write(context: Context, csv: String, fileName: String): Uri = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, DIR).apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() } // старые выгрузки не копим
        val file = File(dir, fileName)
        file.writeText(csv)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun shareIntent(uri: Uri): Intent {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Sleep energy — данные")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Экспорт данных")
    }
}
