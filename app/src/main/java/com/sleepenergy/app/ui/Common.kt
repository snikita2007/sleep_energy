package com.sleepenergy.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.sleepenergy.app.domain.EveningPlan
import com.sleepenergy.app.domain.RuText
import com.sleepenergy.app.domain.Stage
import com.sleepenergy.app.domain.TimeFormat
import com.sleepenergy.app.domain.WakeSchedule
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

private val MONTHS = listOf("янв", "фев", "мар", "апр", "мая", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")
private val WEEKDAYS_SHORT = listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")
val WEEKDAYS_FULL = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье")

val SLEEP_NEED_OPTIONS = listOf(420 to "7 ч", 450 to "7,5 ч", 480 to "8 ч", 510 to "8,5 ч", 540 to "9 ч")
val OWN_TIME_OPTIONS = listOf(30 to "30 мин", 45 to "45 мин", 60 to "1 ч", 90 to "1,5 ч", 120 to "2 ч")

/** «пн, 28 сен». */
fun dayLabel(date: LocalDate): String =
    "${WEEKDAYS_SHORT[date.dayOfWeek.value - 1]}, ${date.dayOfMonth} ${MONTHS[date.monthValue - 1]}"

/** «в пн» — для подписи подъёма. */
fun weekdayShort(date: LocalDate): String = WEEKDAYS_SHORT[date.dayOfWeek.value - 1]

fun hhmm(time: LocalDateTime): String = TimeFormat.hhmm(time)

/** «+12 мин», «−1 ч 5 мин», «минута в минуту». */
fun deviationLabel(deviation: Long): String = when {
    deviation > 0 -> "+${RuText.duration(deviation)}"
    deviation < 0 -> "−${RuText.duration(deviation)}"
    else -> "минута в минуту"
}

fun oneDecimal(value: Double): String = String.format(Locale.forLanguageTag("ru"), "%.1f", value)

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChoiceChips(options: List<Pair<T, String>>, selected: T?, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(label) })
        }
    }
}

@Composable
fun TimeRow(label: String, minutes: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(
            TimeFormat.hhmm(minutes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun CheckRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .toggleable(value = checked, onValueChange = onChange, role = Role.Checkbox)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun Bullets(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { Text("•  $it", style = MaterialTheme.typography.bodyLarge) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    title: String,
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val minutes = Math.floorMod(initialMinutes, WakeSchedule.MINUTES_PER_DAY)
    val state = rememberTimePickerState(initialHour = minutes / 60, initialMinute = minutes % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("Готово") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

private data class TimelineItem(val time: String, val title: String, val signal: Boolean, val stage: Stage)

private fun timelineItems(plan: EveningPlan): List<TimelineItem> = listOf(
    TimelineItem(hhmm(plan.finishSignalAt), "Заканчиваем дела", signal = true, Stage.FINISH_UP),
    TimelineItem("${hhmm(plan.ownTimeStart)}–${hhmm(plan.ownTimeEnd)}", "Своё время", signal = false, Stage.OWN_TIME),
    TimelineItem(hhmm(plan.windDownStart), "Подготовка ко сну", signal = true, Stage.WIND_DOWN),
    TimelineItem(hhmm(plan.brainDumpAt), "Выгрузка мыслей", signal = true, Stage.BRAIN_DUMP),
    TimelineItem(hhmm(plan.bedtime), "Отбой", signal = false, Stage.BEDTIME),
)

/** Таймлайн вечера; колокольчик — у трёх моментов с сигналом. */
@Composable
fun Timeline(plan: EveningPlan, current: Stage?) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        timelineItems(plan).forEach { item ->
            val active = item.stage == current
            val accent = MaterialTheme.colorScheme.tertiary
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (active) accent else MaterialTheme.colorScheme.outlineVariant),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    item.time,
                    modifier = Modifier.width(108.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (active) accent else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    item.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.signal) {
                    Icon(
                        Icons.Filled.Notifications,
                        contentDescription = "сигнал",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}
