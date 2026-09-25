package com.sleepenergy.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sleepenergy.app.CheckinUi
import com.sleepenergy.app.R
import com.sleepenergy.app.UiState
import com.sleepenergy.app.data.toLocalDateTime
import com.sleepenergy.app.domain.EveningPlan
import com.sleepenergy.app.domain.NightMath
import com.sleepenergy.app.domain.RuText
import com.sleepenergy.app.domain.ShiftMath
import com.sleepenergy.app.domain.Stage
import com.sleepenergy.app.domain.TimeFormat
import java.time.Duration

/** Главный экран: план вечера от подъёма, текущий этап и кнопки ритуала. */
@Composable
fun EveningScreen(
    ui: UiState,
    onOpenCheckin: () -> Unit,
    onOpenBrainDump: () -> Unit,
    onGoToBed: () -> Unit,
    onUndoBed: () -> Unit,
    onWakeOverride: (Int?) -> Unit,
) {
    var pickWake by remember { mutableStateOf(false) }
    val evening = ui.phase is NightMath.Evening

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ui.checkin?.let { checkin -> item { CheckinCard(checkin, ui, onOpenCheckin) } }

        if (ui.sleeping) {
            item { NightCard(ui, onUndoBed) }
        } else {
            item { HeaderCard(ui, onChangeWake = { pickWake = true }, onResetWake = { onWakeOverride(null) }) }
            item { StageCard(ui, onOpenBrainDump, onGoToBed) }
            item {
                SectionCard {
                    Text("План вечера", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Timeline(ui.plan, current = ui.stage.takeIf { evening })
                }
            }
            if (ui.todayList.isNotEmpty()) {
                item {
                    SectionCard {
                        Text("На сегодня из вчерашней выгрузки", style = MaterialTheme.typography.titleMedium)
                        Bullets(ui.todayList)
                    }
                }
            }
        }
    }

    if (pickWake) {
        TimePickerDialog(
            title = "Подъём, ${dayLabel(ui.eveningNight)}",
            initialMinutes = TimeFormat.minutesOfDay(ui.plan.wake),
            onDismiss = { pickWake = false },
        ) { minutes ->
            onWakeOverride(minutes)
            pickWake = false
        }
    }
}

@Composable
private fun CheckinCard(checkin: CheckinUi, ui: UiState, onOpen: () -> Unit) {
    val morning = ui.phase is NightMath.Morning
    SectionCard(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
        Text(
            if (morning) "Доброе утро! Как самочувствие?" else "Как прошла ночь на ${dayLabel(checkin.night)}?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Text(
            "Одно касание — и готово.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Button(onClick = onOpen) { Text("Отметить утро") }
    }
}

@Composable
private fun NightCard(ui: UiState, onUndo: () -> Unit) {
    val bedAt = ui.eveningRecord?.bedAt?.toLocalDateTime()
    SectionCard(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Icon(
            painterResource(R.drawable.ic_moon),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.tertiary,
        )
        Text("Спокойной ночи", style = MaterialTheme.typography.headlineSmall)
        if (bedAt != null) Text("Отбой записан в ${hhmm(bedAt)}.", style = MaterialTheme.typography.bodyLarge)
        Hint("Телефон — на зарядку. Утром оценишь самочувствие одним касанием.")
        OutlinedButton(onClick = onUndo) { Text("Я ещё не сплю") }
    }
}

@Composable
private fun HeaderCard(ui: UiState, onChangeWake: () -> Unit, onResetWake: () -> Unit) {
    val offset = ui.settings.shiftOffsetMinutes
    SectionCard {
        Text(
            "Вечер · ${dayLabel(ui.eveningNight.minusDays(1))}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("Отбой в ${hhmm(ui.plan.bedtime)}", style = MaterialTheme.typography.headlineMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Подъём в ${weekdayShort(ui.eveningNight)} в ${hhmm(ui.plan.wake)}",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (ui.eveningRecord?.wakeOverrideMinutes != null) {
                TextButton(onClick = onResetWake) { Text("Как обычно") }
            }
            TextButton(onClick = onChangeWake) { Text("Изменить") }
        }
        if (offset > 0) {
            Hint(
                "Цель двигается к ${hhmm(ui.goalPlan.bedtime)} по ${ShiftMath.STEP_MINUTES} минут — " +
                    "осталось ${RuText.steps(ShiftMath.stepsLeft(offset))}.",
            )
        }
    }
}

@Composable
private fun StageCard(ui: UiState, onOpenBrainDump: () -> Unit, onGoToBed: () -> Unit) {
    val plan = ui.plan
    val evening = ui.phase is NightMath.Evening
    val stage = if (evening) ui.stage else Stage.DAY
    val (title, text) = stageText(stage, plan, ui)
    SectionCard(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (stage != Stage.DAY) {
            Spacer(Modifier.height(4.dp))
            // Основная (закрашенная) кнопка — та, что нужна на этом этапе.
            val dumpLabel = if (stage == Stage.BRAIN_DUMP) "Записать дела на завтра" else "Выгрузка мыслей"
            if (stage == Stage.BEDTIME) {
                Button(onClick = onGoToBed, modifier = Modifier.fillMaxWidth()) { Text("Ложусь спать") }
                OutlinedButton(onClick = onOpenBrainDump, modifier = Modifier.fillMaxWidth()) { Text(dumpLabel) }
            } else {
                if (stage == Stage.BRAIN_DUMP) {
                    Button(onClick = onOpenBrainDump, modifier = Modifier.fillMaxWidth()) { Text(dumpLabel) }
                } else {
                    OutlinedButton(onClick = onOpenBrainDump, modifier = Modifier.fillMaxWidth()) { Text(dumpLabel) }
                }
                OutlinedButton(onClick = onGoToBed, modifier = Modifier.fillMaxWidth()) { Text("Ложусь спать") }
            }
        }
    }
}

private fun stageText(stage: Stage, plan: EveningPlan, ui: UiState): Pair<String, String> = when (stage) {
    Stage.DAY -> "Вечер начнётся в ${hhmm(plan.finishSignalAt)}" to
        "Я напомню закругляться за 20 минут до своего времени."

    Stage.FINISH_UP -> {
        val left = Duration.between(ui.now, plan.ownTimeStart).toMinutes().coerceAtLeast(1)
        "Заканчиваем дела" to
            "Через ${RuText.minutes(left)} начнётся своё время — ${RuText.ownTime(plan.ownTimeMinutes)} на себя."
    }

    Stage.OWN_TIME -> if (ui.now < plan.ownTimeEnd) {
        "Своё время · до ${hhmm(plan.ownTimeEnd)}" to
            "Сериал, переписка, игры — без упрёков. Это часть плана."
    } else {
        "Закругляемся" to "Досматриваем серию. В ${hhmm(plan.windDownStart)} — подготовка ко сну."
    }

    Stage.WIND_DOWN -> "Подготовка ко сну" to
        "Душ, телефон на зарядку, свет потише. Отбой в ${hhmm(plan.bedtime)}."

    Stage.BRAIN_DUMP -> "Выгрузка мыслей" to
        "Минута, чтобы записать дела на завтра, — и голова отпустит их до утра."

    Stage.BEDTIME -> if (Duration.between(plan.bedtime, ui.now).toMinutes() < NightMath.GRACE_MINUTES) {
        "Отбой" to "Самое время ложиться. Спокойной ночи!"
    } else {
        "Вечер затянулся" to "Ничего страшного — ложись сейчас. Утром будет спокойный план на следующий вечер."
    }
}
