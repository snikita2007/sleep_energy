package com.sleepenergy.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sleepenergy.app.NightRow
import com.sleepenergy.app.UiState
import com.sleepenergy.app.domain.Outcome
import com.sleepenergy.app.domain.RuText
import com.sleepenergy.app.domain.StreakMath
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/** Прогресс без графиков: серия, «вовремя» за неделю, самочувствие и последние ночи. */
@Composable
fun ProgressScreen(ui: UiState) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { StreakCard(ui) }
        item {
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionCard(Modifier.weight(1f).fillMaxHeight()) {
                    Hint("Вовремя за неделю")
                    Text(
                        if (ui.week.nights == 0) "—" else "${ui.week.onTime} из ${ui.week.nights}",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Hint("не позже цели + 30 мин")
                }
                SectionCard(Modifier.weight(1f).fillMaxHeight()) {
                    Hint("Самочувствие")
                    Text(
                        ui.week.avgRating?.let { oneDecimal(it) } ?: "—",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Hint(ratingTrend(ui.week.avgRating, ui.week.prevAvgRating))
                }
            }
        }
        item {
            Text(
                "Последние ночи",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (ui.history.isEmpty()) {
            item { Hint("Здесь появятся ночи после первого вечера с Sleep energy.") }
        }
        items(ui.history, key = { it.night.toEpochDay() }) { NightItem(it) }
    }
}

@Composable
private fun StreakCard(ui: UiState) {
    SectionCard(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Hint("Серия")
        Text(RuText.days(ui.streak.days), style = MaterialTheme.typography.headlineMedium)
        Hint(spareLine(ui))
    }
}

private fun spareLine(ui: UiState): String {
    if (ui.spareAvailable) return "Запасной вечер есть: один срыв за 7 дней не обнулит серию."
    val back = ui.streak.lastForgiven
        ?.plusDays(StreakMath.SPARE_EVERY_NIGHTS)
        ?.let { ChronoUnit.DAYS.between(ui.eveningNight, it) }
        ?.takeIf { it > 0 }
    return if (back != null) {
        "Запасной вечер уже использован — снова появится через ${RuText.days(back.toInt())}."
    } else {
        "Запасной вечер уже использован."
    }
}

private fun ratingTrend(avg: Double?, prev: Double?): String = when {
    avg == null -> "оценок пока нет"
    prev == null -> "среднее за неделю"
    abs(avg - prev) < 0.05 -> "как на прошлой неделе"
    avg > prev -> "↑ ${oneDecimal(avg - prev)} к прошлой неделе"
    else -> "↓ ${oneDecimal(prev - avg)} к прошлой неделе"
}

@Composable
private fun NightItem(row: NightRow) {
    val color = when (row.outcome) {
        Outcome.ON_TIME -> MaterialTheme.colorScheme.tertiary
        Outcome.LATE -> MaterialTheme.colorScheme.error
        Outcome.UNKNOWN -> MaterialTheme.colorScheme.outlineVariant
    }
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(dayLabel(row.night.minusDays(1)), style = MaterialTheme.typography.titleSmall)
                Hint(
                    if (row.bedAt != null && row.target != null && row.deviation != null) {
                        "цель ${hhmm(row.target)} · лёг ${hhmm(row.bedAt)} (${deviationLabel(row.deviation)})"
                    } else {
                        "нет данных об отбое"
                    },
                )
            }
            Text(
                row.rating?.let { "$it/10" } ?: "",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
