package com.sleepenergy.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sleepenergy.app.CheckinResult
import com.sleepenergy.app.CheckinUi
import com.sleepenergy.app.data.toLocalDateTime
import com.sleepenergy.app.domain.NightMath
import com.sleepenergy.app.domain.RuText
import com.sleepenergy.app.domain.StreakMath
import com.sleepenergy.app.domain.TimeFormat
import java.time.LocalDateTime

/** Утро в одно касание: оценка 1–10; время отбоя — только если вечером его не отметили. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MorningScreen(
    checkin: CheckinUi,
    onSave: (rating: Int, bedEstimate: LocalDateTime?) -> Unit,
    onClose: () -> Unit,
) {
    val target = checkin.record?.targetBedAt?.toLocalDateTime() ?: checkin.plan.bedtime
    val bedAt = checkin.record?.bedAt?.toLocalDateTime()
    var estimate by remember { mutableStateOf<LocalDateTime?>(null) }
    var pickOther by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Закрыть") }
        Text("Доброе утро!", style = MaterialTheme.typography.headlineMedium)

        if (bedAt != null) {
            SectionCard {
                Text("Отбой в ${hhmm(bedAt)} · цель ${hhmm(target)}", style = MaterialTheme.typography.titleMedium)
                Hint(outcomeLine(NightMath.deviationMinutes(bedAt, target)))
            }
        } else {
            Text("Во сколько лёг?", style = MaterialTheme.typography.titleMedium)
            val options = listOf(0L, 30L, 60L, 90L).map { target.plusMinutes(it) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEachIndexed { index, moment ->
                    FilterChip(
                        selected = estimate == moment,
                        onClick = { estimate = moment },
                        label = { Text(if (index == 0) "Вовремя · ${hhmm(moment)}" else hhmm(moment)) },
                    )
                }
                val custom = estimate?.takeIf { it !in options }
                FilterChip(
                    selected = custom != null,
                    onClick = { pickOther = true },
                    label = { Text(custom?.let { hhmm(it) } ?: "Другое…") },
                )
            }
            Hint("Можно не указывать — тогда ночь останется без данных.")
        }

        Spacer(Modifier.height(4.dp))
        Text("Как самочувствие?", style = MaterialTheme.typography.titleMedium)
        RatingGrid { rating -> onSave(rating, if (bedAt == null) estimate else null) }
    }

    if (pickOther) {
        TimePickerDialog(
            title = "Во сколько лёг?",
            initialMinutes = TimeFormat.minutesOfDay(target),
            onDismiss = { pickOther = false },
        ) { minutes ->
            estimate = NightMath.bedMoment(checkin.night, minutes)
            pickOther = false
        }
    }
}

@Composable
private fun RatingGrid(onRate: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (row in 0..1) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (column in 1..5) {
                    val value = row * 5 + column
                    FilledTonalButton(
                        onClick = { onRate(value) },
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text("$value", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Hint("совсем разбит")
            Spacer(Modifier.weight(1f))
            Hint("полон сил")
        }
    }
}

private fun outcomeLine(deviation: Long): String = when {
    deviation <= 0 -> "Вовремя — так держать."
    NightMath.isOnTime(deviation) -> "В пределах получаса от цели — засчитано."
    else -> "На ${RuText.duration(deviation)} позже цели."
}

/** Итог утра: оценка, серия и спокойный план на вечер — без упрёков за срыв. */
@Composable
fun CheckinResultScreen(
    result: CheckinResult,
    streak: StreakMath.Streak,
    spareAvailable: Boolean,
    onDone: () -> Unit,
) {
    val tonight = hhmm(result.tonightBedtime)
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            "${result.rating} из 10",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
        Text("Серия — ${RuText.days(streak.days)}", style = MaterialTheme.typography.titleLarge)
        if (streak.lastForgiven != null && !spareAvailable) {
            Hint("Запасной вечер уже помог серии на этой неделе.")
        }

        val deviation = result.deviation
        SectionCard {
            Text(
                when {
                    deviation == null -> "Время отбоя не отмечено — ничего страшного."
                    NightMath.isOnTime(deviation) -> "Лёг вовремя. Так и выглядит режим."
                    else -> "Лёг на ${RuText.duration(deviation)} позже цели. Ничего страшного — это не обнуляет прогресс."
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            Hint(
                if (deviation != null && !NightMath.isOnTime(deviation)) {
                    "Спокойный план: сегодня отбой в $tonight. Начни своё время чуть раньше, чтобы досмотреть без спешки."
                } else {
                    "Сегодня отбой в $tonight."
                },
            )
        }

        if (result.shifted) {
            SectionCard(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                Text(
                    if (result.goalReached) {
                        "Цель достигнута: отбой в $tonight. Дальше просто держим режим."
                    } else {
                        "Новый шаг: с сегодняшнего вечера отбой в $tonight — на 15 минут раньше."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }

        if (result.todayList.isNotEmpty()) {
            SectionCard {
                Text("На сегодня ты записал", style = MaterialTheme.typography.titleMedium)
                Bullets(result.todayList)
            }
        }

        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Хорошего дня") }
    }
}
