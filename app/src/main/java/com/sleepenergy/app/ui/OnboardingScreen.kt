package com.sleepenergy.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sleepenergy.app.OnboardingAnswers
import com.sleepenergy.app.R
import com.sleepenergy.app.data.EnvChecklist
import com.sleepenergy.app.domain.EveningPlan
import com.sleepenergy.app.domain.PlanSettings
import com.sleepenergy.app.domain.ShiftMath
import com.sleepenergy.app.domain.TimeFormat
import java.time.LocalDate

private const val STEPS = 6

private enum class TimeField { WEEKDAY, WEEKEND, CURRENT_BED }

/** Настройка за 2 минуты: подъём, сон, своё время, среда, сигналы. */
@Composable
fun OnboardingScreen(onDone: (OnboardingAnswers) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var weekdayWake by rememberSaveable { mutableIntStateOf(7 * 60 + 30) }
    var weekendWake by rememberSaveable { mutableIntStateOf(8 * 60 + 30) }
    var weekendTouched by rememberSaveable { mutableStateOf(false) }
    var sleepNeed by rememberSaveable { mutableIntStateOf(480) }
    var currentBed by rememberSaveable { mutableIntStateOf(30) }
    var ownTime by rememberSaveable { mutableIntStateOf(60) }
    var envMask by rememberSaveable { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<TimeField?>(null) }

    val answers = OnboardingAnswers(weekdayWake, weekendWake, sleepNeed, currentBed, ownTime, envMask)
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        onDone(answers)
    }
    val finish = {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) permission.launch(Manifest.permission.POST_NOTIFICATIONS) else onDone(answers)
    }

    BackHandler(enabled = step > 0) { step-- }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        StepDots(step)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (step) {
                0 -> WelcomeStep()
                1 -> WakeStep(weekdayWake, weekendWake) { editing = it }
                2 -> SleepStep(weekdayWake, sleepNeed, currentBed, onSleepNeed = { sleepNeed = it }) {
                    editing = TimeField.CURRENT_BED
                }
                3 -> OwnTimeStep(weekdayWake, sleepNeed, currentBed, ownTime) { ownTime = it }
                4 -> EnvironmentStep(envMask) { envMask = it }
                else -> SignalsStep()
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) TextButton(onClick = { step-- }) { Text("Назад") }
            Spacer(Modifier.weight(1f))
            if (step == STEPS - 1) {
                TextButton(onClick = { onDone(answers) }) { Text("Без сигналов") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = finish) { Text("Начать") }
            } else {
                Button(onClick = { step++ }) { Text(if (step == 0) "Настроить" else "Дальше") }
            }
        }
    }

    editing?.let { field ->
        val (title, initial) = when (field) {
            TimeField.WEEKDAY -> "Подъём в будни" to weekdayWake
            TimeField.WEEKEND -> "Подъём в выходные" to weekendWake
            TimeField.CURRENT_BED -> "Обычно ложусь в" to currentBed
        }
        TimePickerDialog(title, initial, onDismiss = { editing = null }) { minutes ->
            when (field) {
                TimeField.WEEKDAY -> {
                    weekdayWake = minutes
                    // Пока выходные не трогали, держим их на час позже будней.
                    if (!weekendTouched) weekendWake = (minutes + 60).coerceAtMost(23 * 60 + 59)
                }
                TimeField.WEEKEND -> {
                    weekendWake = minutes
                    weekendTouched = true
                }
                TimeField.CURRENT_BED -> currentBed = minutes
            }
            editing = null
        }
    }
}

@Composable
private fun StepDots(step: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(STEPS) { index ->
            Box(
                Modifier
                    .height(6.dp)
                    .width(if (index == step) 24.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (index <= step) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.outlineVariant,
                    ),
            )
        }
    }
}

@Composable
private fun StepTitle(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.headlineSmall)
    Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun WelcomeStep() {
    Spacer(Modifier.height(24.dp))
    Icon(
        painterResource(R.drawable.ic_moon),
        contentDescription = null,
        modifier = Modifier.size(56.dp),
        tint = MaterialTheme.colorScheme.tertiary,
    )
    Text("Sleep energy", style = MaterialTheme.typography.headlineMedium)
    Text(
        "Помогает вовремя закончить вечер и лечь спать — без запретов и без опоры на силу воли.",
        style = MaterialTheme.typography.bodyLarge,
    )
    SectionCard(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Text("Закончить дела → честно отдохнуть → лечь вовремя", style = MaterialTheme.typography.titleMedium)
        Hint("Отдых встроен в план, поэтому его не нужно отвоёвывать у сна.")
    }
    Hint("Настройка — пара минут. Дальше каждый день нужно только одно касание утром.")
}

@Composable
private fun WakeStep(weekdayWake: Int, weekendWake: Int, onEdit: (TimeField) -> Unit) {
    StepTitle("Во сколько встаёшь?", "От времени подъёма строится весь вечер.")
    SectionCard {
        TimeRow("В будни", weekdayWake) { onEdit(TimeField.WEEKDAY) }
        TimeRow("В выходные", weekendWake) { onEdit(TimeField.WEEKEND) }
    }
    Hint(
        "В выходные лучше вставать не больше чем на час позже — так понедельник не станет джетлагом. " +
            "Разное время по дням недели можно задать в настройках.",
    )
}

@Composable
private fun SleepStep(
    weekdayWake: Int,
    sleepNeed: Int,
    currentBed: Int,
    onSleepNeed: (Int) -> Unit,
    onEditBed: () -> Unit,
) {
    StepTitle("Сколько сна тебе нужно?", "Большинству взрослых — 7–9 часов.")
    ChoiceChips(SLEEP_NEED_OPTIONS, sleepNeed, onSleepNeed)
    Spacer(Modifier.height(8.dp))
    Text("Во сколько обычно ложишься сейчас?", style = MaterialTheme.typography.titleMedium)
    SectionCard {
        TimeRow("Обычно ложусь", currentBed, onEditBed)
    }
    val offset = ShiftMath.initialOffset(currentBed, weekdayWake, sleepNeed)
    val goal = TimeFormat.hhmm(weekdayWake - sleepNeed)
    SectionCard(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
        if (offset > 0) {
            Text(
                "Начнём с отбоя в ${TimeFormat.hhmm(weekdayWake - sleepNeed + offset)}",
                style = MaterialTheme.typography.titleMedium,
            )
            Hint(
                "Резкий сдвиг на пару часов не работает. Будем двигать отбой на 15 минут, " +
                    "когда 2 вечера из 3 удаются, — пока не дойдём до $goal.",
            )
        } else {
            Text("Цель — отбой в $goal", style = MaterialTheme.typography.titleMedium)
            Hint("Ты уже близко к цели — просто держим режим.")
        }
    }
}

@Composable
private fun OwnTimeStep(weekdayWake: Int, sleepNeed: Int, currentBed: Int, ownTime: Int, onOwnTime: (Int) -> Unit) {
    StepTitle(
        "Своё время",
        "Отдых — часть плана: сериал, переписка, игры без упрёков. Сколько времени тебе нужно вечером?",
    )
    ChoiceChips(OWN_TIME_OPTIONS, ownTime, onOwnTime)
    val plan = EveningPlan.build(
        LocalDate.now(),
        weekdayWake,
        PlanSettings(sleepNeed, ownTime, ShiftMath.initialOffset(currentBed, weekdayWake, sleepNeed)),
    )
    SectionCard {
        Text("Твой вечер в будни", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Timeline(plan, current = null)
    }
}

@Composable
private fun EnvironmentStep(mask: Int, onMask: (Int) -> Unit) {
    StepTitle("Среда важнее силы воли", "Настрой обстановку один раз — и вечером не придётся себя заставлять.")
    SectionCard {
        EnvChecklist.items.forEach { (bit, text) ->
            CheckRow(text, mask and bit != 0) { checked -> onMask(if (checked) mask or bit else mask and bit.inv()) }
        }
    }
    Hint("Можно отметить позже — чек-лист останется в настройках.")
}

@Composable
private fun SignalsStep() {
    StepTitle(
        "Мягкие сигналы",
        "Не больше трёх коротких сигналов за вечер и один утром. Никаких блокировок — решаешь ты.",
    )
    SectionCard {
        Bullets(
            listOf(
                "За 20 минут до своего времени — закругляемся",
                "Начало подготовки ко сну",
                "Минута на выгрузку мыслей перед отбоем",
                "Утром — оценка самочувствия в одно касание",
            ),
        )
    }
    Hint("Для сигналов нужно разрешение на уведомления.")
}
