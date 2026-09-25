package com.sleepenergy.app.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.sleepenergy.app.MainViewModel
import com.sleepenergy.app.UiState
import com.sleepenergy.app.data.EnvChecklist
import com.sleepenergy.app.data.week
import com.sleepenergy.app.domain.EveningPlan
import com.sleepenergy.app.domain.ShiftMath
import com.sleepenergy.app.domain.TimeFormat
import com.sleepenergy.app.reminders.Signals

/** Что редактируется в диалоге выбора времени. */
private sealed interface TimeEdit {
    data class Day(val index: Int) : TimeEdit
    data object AllDays : TimeEdit
    data object CurrentBed : TimeEdit
}

@Composable
fun SettingsScreen(ui: UiState, viewModel: MainViewModel) {
    val context = LocalContext.current
    val settings = ui.settings
    val week = settings.week
    var editing by remember { mutableStateOf<TimeEdit?>(null) }
    var canNotify by remember { mutableStateOf(Signals.canNotify(context)) }
    var exactAlarms by remember { mutableStateOf(Signals.canScheduleExact(context)) }
    LifecycleResumeEffect(Unit) {
        canNotify = Signals.canNotify(context)
        exactAlarms = Signals.canScheduleExact(context)
        onPauseOrDispose { }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        canNotify = Signals.canNotify(context)
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard {
                Text("Подъём", style = MaterialTheme.typography.titleMedium)
                Hint("От него строится вечер. Для одной ночи время можно поменять на главном экране.")
                WEEKDAYS_FULL.forEachIndexed { index, name ->
                    TimeRow(name, week[index]) { editing = TimeEdit.Day(index) }
                }
                TextButton(onClick = { editing = TimeEdit.AllDays }) { Text("Одно время для всех дней") }
            }
        }
        item {
            SectionCard {
                Text("Сон", style = MaterialTheme.typography.titleMedium)
                ChoiceChips(SLEEP_NEED_OPTIONS, settings.sleepNeedMinutes, viewModel::setSleepNeed)
            }
        }
        item {
            SectionCard {
                Text("Своё время", style = MaterialTheme.typography.titleMedium)
                ChoiceChips(OWN_TIME_OPTIONS, settings.ownTimeMinutes, viewModel::setOwnTime)
                Hint("Сигнал «закругляемся» придёт за ${EveningPlan.FINISH_LEAD_MINUTES} минут до него.")
            }
        }
        item {
            SectionCard {
                Text("Постепенный сдвиг", style = MaterialTheme.typography.titleMedium)
                val offset = settings.shiftOffsetMinutes
                Hint(
                    if (offset > 0) {
                        "Сейчас цель на ${offset} мин позже идеала. Шаг — ${ShiftMath.STEP_MINUTES} минут, " +
                            "когда 2 вечера из 3 удаются. Назад цель сама не отодвигается."
                    } else {
                        "Цель совпадает с идеалом: подъём минус сон. Держим режим."
                    },
                )
                settings.baselineBedMinutes?.let { Hint("Старт был от отбоя в ${TimeFormat.hhmm(it)}.") }
                TextButton(onClick = { editing = TimeEdit.CurrentBed }) { Text("Я сейчас ложусь в…") }
            }
        }
        item {
            SectionCard {
                Text("Сигналы", style = MaterialTheme.typography.titleMedium)
                Hint(
                    if (canNotify) {
                        "Уведомления включены: не больше трёх за вечер и одно утром."
                    } else {
                        "Уведомления выключены — сигналы не придут."
                    },
                )
                if (!canNotify && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Button(onClick = { permission.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                        Text("Разрешить уведомления")
                    }
                }
                OutlinedButton(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                    )
                }) { Text("Настройки уведомлений") }
                OutlinedButton(onClick = viewModel::sendTestSignal, enabled = canNotify) { Text("Пробный сигнал") }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Hint(
                        if (exactAlarms) {
                            "Сигналы приходят минута в минуту."
                        } else {
                            "Сигналы приходят с точностью ±5 минут. Для точного времени включите " +
                                "«Будильники и напоминания»."
                        },
                    )
                    if (!exactAlarms) {
                        OutlinedButton(onClick = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    Uri.parse("package:${context.packageName}"),
                                ),
                            )
                        }) { Text("Точное время сигналов") }
                    }
                }
            }
        }
        item {
            SectionCard {
                Text("Среда", style = MaterialTheme.typography.titleMedium)
                val mask = settings.envChecklistMask
                EnvChecklist.items.forEach { (bit, text) ->
                    CheckRow(text, mask and bit != 0) { checked ->
                        viewModel.setEnvChecklist(if (checked) mask or bit else mask and bit.inv())
                    }
                }
            }
        }
        item {
            SectionCard {
                Text("Данные", style = MaterialTheme.typography.titleMedium)
                Hint(
                    "Всё хранится только на этом телефоне. В CSV — цель, время отбоя и оценки по ночам; " +
                        "текст выгрузки мыслей не попадает.",
                )
                Button(
                    onClick = { viewModel.exportCsv { intent -> context.startActivity(intent) } },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Экспорт в CSV") }
            }
        }
    }

    editing?.let { edit ->
        val (title, initial) = when (edit) {
            is TimeEdit.Day -> "Подъём: ${WEEKDAYS_FULL[edit.index].lowercase()}" to week[edit.index]
            TimeEdit.AllDays -> "Подъём во все дни" to week[0]
            TimeEdit.CurrentBed -> "Сейчас обычно ложусь в" to (settings.baselineBedMinutes ?: 30)
        }
        TimePickerDialog(title, initial, onDismiss = { editing = null }) { minutes ->
            when (edit) {
                is TimeEdit.Day -> viewModel.setWakeTime(edit.index, minutes)
                TimeEdit.AllDays -> viewModel.setAllWakeTimes(minutes)
                TimeEdit.CurrentBed -> viewModel.rebaseline(minutes)
            }
            editing = null
        }
    }
}
