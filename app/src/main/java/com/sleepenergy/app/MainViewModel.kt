package com.sleepenergy.app

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sleepenergy.app.data.AppDatabase
import com.sleepenergy.app.data.BedSource
import com.sleepenergy.app.data.NightEntity
import com.sleepenergy.app.data.SettingsEntity
import com.sleepenergy.app.data.brainDumpItems
import com.sleepenergy.app.data.deviationMinutes
import com.sleepenergy.app.data.firstNight
import com.sleepenergy.app.data.night
import com.sleepenergy.app.data.outcome
import com.sleepenergy.app.data.planner
import com.sleepenergy.app.data.toEpochMillis
import com.sleepenergy.app.data.toLocalDateTime
import com.sleepenergy.app.data.week
import com.sleepenergy.app.domain.CsvExport
import com.sleepenergy.app.domain.EveningPlan
import com.sleepenergy.app.domain.NightMath
import com.sleepenergy.app.domain.Outcome
import com.sleepenergy.app.domain.Progress
import com.sleepenergy.app.domain.ShiftMath
import com.sleepenergy.app.domain.SignalKind
import com.sleepenergy.app.domain.Stage
import com.sleepenergy.app.domain.StreakMath
import com.sleepenergy.app.domain.TimeFormat
import com.sleepenergy.app.domain.WakeSchedule
import com.sleepenergy.app.export.CsvShare
import com.sleepenergy.app.reminders.Signals
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface AppState {
    data object Loading : AppState
    data object Onboarding : AppState
    data class Ready(val ui: UiState) : AppState
}

data class UiState(
    val now: LocalDateTime,
    val settings: SettingsEntity,
    val phase: NightMath.Phase,
    /** Ночь предстоящего (или идущего) вечера — к ней относятся план и кнопки. */
    val eveningNight: LocalDate,
    val plan: EveningPlan,
    /** План той же ночи без постепенного сдвига — куда ведём. */
    val goalPlan: EveningPlan,
    val eveningRecord: NightEntity?,
    val stage: Stage,
    /** Нажал «Ложусь», ещё ночь. */
    val sleeping: Boolean,
    val checkin: CheckinUi?,
    /** Дела на сегодня из вчерашней выгрузки мыслей. */
    val todayList: List<String>,
    val streak: StreakMath.Streak,
    val spareAvailable: Boolean,
    val week: Progress.Week,
    val history: List<NightRow>,
    val lastClosedNight: LocalDate,
)

data class CheckinUi(val night: LocalDate, val plan: EveningPlan, val record: NightEntity?)

data class NightRow(
    val night: LocalDate,
    val target: LocalDateTime?,
    val bedAt: LocalDateTime?,
    val deviation: Long?,
    val outcome: Outcome,
    val rating: Int?,
)

data class CheckinResult(
    val rating: Int,
    val deviation: Long?,
    val tonightBedtime: LocalDateTime,
    val shifted: Boolean,
    val goalReached: Boolean,
    val todayList: List<String>,
)

/** Запрос открыть экран из уведомления; [at] — чтобы дождаться состояния, посчитанного после него. */
data class RouteRequest(val route: String, val at: LocalDateTime)

data class OnboardingAnswers(
    val weekdayWake: Int,
    val weekendWake: Int,
    val sleepNeedMinutes: Int,
    val currentBedMinutes: Int,
    val ownTimeMinutes: Int,
    val envChecklistMask: Int,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.get(application)
    private val settingsDao = db.settingsDao()
    private val nightDao = db.nightDao()

    /** Тикает раз в минуту — этапы вечера сменяются без перезапуска экрана. */
    private val clock = MutableStateFlow(LocalDateTime.now())

    val state: StateFlow<AppState> = combine(
        settingsDao.observe(),
        nightDao.observeFrom(LocalDate.now().minusDays(HISTORY_DAYS).toEpochDay()),
        clock,
    ) { settings, nights, now ->
        if (settings == null || !settings.onboarded) AppState.Onboarding
        else AppState.Ready(buildUi(settings, nights, now))
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppState.Loading)

    private val _route = MutableStateFlow<RouteRequest?>(null)
    /** Куда открыть приложение после нажатия на уведомление. */
    val route: StateFlow<RouteRequest?> = _route.asStateFlow()

    private val _checkinResult = MutableStateFlow<CheckinResult?>(null)
    val checkinResult: StateFlow<CheckinResult?> = _checkinResult.asStateFlow()

    init {
        Signals.ensureChannels(application)
        viewModelScope.launch {
            while (true) {
                val now = LocalDateTime.now()
                delay((60 - now.second) * 1_000L - now.nano / 1_000_000)
                clock.value = LocalDateTime.now()
            }
        }
    }

    fun onResumed() {
        clock.value = LocalDateTime.now()
        reschedule()
    }

    fun openRoute(route: String?) {
        if (route == null) return
        // Уведомление могло прийти в давно открытое приложение: пересчитываем состояние
        // на «сейчас», а экран решает, что открыть, только по свежему состоянию.
        val now = LocalDateTime.now()
        _route.value = RouteRequest(route, now)
        clock.value = now
    }

    fun consumeRoute() {
        _route.value = null
    }

    fun completeOnboarding(answers: OnboardingAnswers) {
        viewModelScope.launch {
            val week = WakeSchedule.weekdaysAndWeekend(answers.weekdayWake, answers.weekendWake)
            val now = LocalDateTime.now()
            val todayWake = now.toLocalDate().atStartOfDay()
                .plusMinutes(WakeSchedule.wakeMinutesFor(now.toLocalDate(), week, null).toLong())
            // Первая ночь — ближайший вечер, который приложение сможет вести.
            val firstNight = NightMath.eveningNight(NightMath.phase(now, todayWake))
            settingsDao.upsert(
                SettingsEntity(
                    onboarded = true,
                    firstNightEpochDay = firstNight.toEpochDay(),
                    wakeTimesCsv = WakeSchedule.encode(week),
                    sleepNeedMinutes = answers.sleepNeedMinutes,
                    ownTimeMinutes = answers.ownTimeMinutes,
                    shiftOffsetMinutes = ShiftMath.initialOffset(
                        answers.currentBedMinutes,
                        WakeSchedule.typicalWake(week),
                        answers.sleepNeedMinutes,
                    ),
                    lastShiftEpochDay = firstNight.minusDays(1).toEpochDay(),
                    baselineBedMinutes = answers.currentBedMinutes,
                    envChecklistMask = answers.envChecklistMask,
                ),
            )
            rescheduleNow()
        }
    }

    fun setWakeTime(dayIndex: Int, minutes: Int) = updateSettings { s ->
        s.copy(wakeTimesCsv = WakeSchedule.encode(s.week.toMutableList().also { it[dayIndex] = minutes }))
    }

    fun setAllWakeTimes(minutes: Int) = updateSettings {
        it.copy(wakeTimesCsv = WakeSchedule.encode(List(WakeSchedule.DAYS) { minutes }))
    }

    fun setSleepNeed(minutes: Int) = updateSettings { it.copy(sleepNeedMinutes = minutes) }

    fun setOwnTime(minutes: Int) = updateSettings { it.copy(ownTimeMinutes = minutes) }

    fun setEnvChecklist(mask: Int) = updateSettings { it.copy(envChecklistMask = mask) }

    /** «Я сейчас ложусь в…» — сдвиг начинается заново от нового ответа. */
    fun rebaseline(currentBedMinutes: Int) {
        viewModelScope.launch {
            val settings = settingsDao.get() ?: return@launch
            val night = eveningNightNow(settings)
            settingsDao.upsert(
                settings.copy(
                    baselineBedMinutes = currentBedMinutes,
                    shiftOffsetMinutes = ShiftMath.initialOffset(
                        currentBedMinutes,
                        WakeSchedule.typicalWake(settings.week),
                        settings.sleepNeedMinutes,
                    ),
                    lastShiftEpochDay = night.minusDays(1).toEpochDay(),
                ),
            )
            rescheduleNow()
        }
    }

    /** «Завтра подъём в…» — только для одной ночи; null возвращает обычное расписание. */
    fun setWakeOverride(night: LocalDate, minutes: Int?) {
        viewModelScope.launch {
            nightDao.upsert(record(night).copy(wakeOverrideMinutes = minutes))
            rescheduleNow()
        }
    }

    fun goToBed() {
        viewModelScope.launch {
            val settings = settingsDao.get() ?: return@launch
            val now = LocalDateTime.now()
            recordBed(settings, eveningNightNow(settings, now), now, BedSource.EVENING)
            rescheduleNow()
        }
    }

    fun undoGoToBed() {
        viewModelScope.launch {
            val settings = settingsDao.get() ?: return@launch
            val night = eveningNightNow(settings)
            val record = nightDao.get(night.toEpochDay()) ?: return@launch
            nightDao.upsert(
                record.copy(bedAt = null, bedSource = BedSource.NONE, targetBedAt = null, wakeMinutes = null),
            )
            rescheduleNow()
        }
    }

    fun saveBrainDump(items: List<String>, goToBed: Boolean) {
        viewModelScope.launch {
            val settings = settingsDao.get() ?: return@launch
            val now = LocalDateTime.now()
            val night = eveningNightNow(settings, now)
            val text = items.map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n").ifEmpty { null }
            nightDao.upsert(record(night).copy(brainDump = text))
            if (goToBed) recordBed(settings, night, now, BedSource.EVENING)
            rescheduleNow()
        }
    }

    /**
     * Утренняя отметка: оценка в одно касание (и отбой, если вечером его не отметили).
     * Здесь же — единственное место, где шагает постепенный сдвиг: цель не меняется посреди вечера.
     */
    fun saveCheckin(night: LocalDate, rating: Int, bedEstimate: LocalDateTime?) {
        viewModelScope.launch {
            val settings = settingsDao.get() ?: return@launch
            if (bedEstimate != null && record(night).bedAt == null) {
                recordBed(settings, night, bedEstimate, BedSource.MORNING)
            }
            val record = record(night).copy(rating = rating, checkinAt = LocalDateTime.now().toEpochMillis())
            nightDao.upsert(record)

            val recent = nightDao.between(night.minusDays(2).toEpochDay(), night.toEpochDay())
                .associateBy { it.night }
            val outcomes = (0L until ShiftMath.WINDOW_NIGHTS).map {
                recent[night.minusDays(it)]?.outcome ?: Outcome.UNKNOWN
            }
            val newOffset = ShiftMath.nextOffset(
                settings.shiftOffsetMinutes,
                LocalDate.ofEpochDay(settings.lastShiftEpochDay),
                night,
                outcomes,
            )
            val current = if (newOffset != null) {
                settings.copy(shiftOffsetMinutes = newOffset, lastShiftEpochDay = night.toEpochDay())
                    .also { settingsDao.upsert(it) }
            } else {
                settings
            }

            val tonight = night.plusDays(1)
            val tonightPlan = current.planner(listOfNotNull(nightDao.get(tonight.toEpochDay()))).plan(tonight)
            _checkinResult.value = CheckinResult(
                rating = rating,
                deviation = record.deviationMinutes,
                tonightBedtime = tonightPlan.bedtime,
                shifted = newOffset != null,
                goalReached = newOffset == 0,
                todayList = record.brainDumpItems,
            )
            rescheduleNow()
        }
    }

    fun dismissCheckinResult() {
        _checkinResult.value = null
    }

    fun exportCsv(onReady: (Intent) -> Unit) {
        viewModelScope.launch {
            val settings = settingsDao.get() ?: return@launch
            val lastNight = (state.value as? AppState.Ready)?.ui?.lastClosedNight
                ?: LocalDate.now().minusDays(1)
            val csv = CsvExport.build(CsvShare.rows(settings, nightDao.all(), lastNight))
            val uri = CsvShare.write(getApplication(), csv, "sleep_energy_${LocalDate.now()}.csv")
            onReady(CsvShare.shareIntent(uri))
        }
    }

    /** Пробный сигнал: проверить, что уведомления доходят. Нажатие откроет выгрузку мыслей. */
    fun sendTestSignal() {
        val ui = (state.value as? AppState.Ready)?.ui ?: return
        Signals.notify(getApplication(), SignalKind.BRAIN_DUMP, ui.plan)
    }

    private fun updateSettings(transform: (SettingsEntity) -> SettingsEntity) {
        viewModelScope.launch {
            val current = settingsDao.get() ?: return@launch
            settingsDao.upsert(transform(current))
            rescheduleNow()
        }
    }

    private suspend fun record(night: LocalDate): NightEntity =
        nightDao.get(night.toEpochDay()) ?: NightEntity(night.toEpochDay())

    /** Записывает отбой вместе со снимком цели и подъёма — метрики не «плывут» после смены настроек. */
    private suspend fun recordBed(settings: SettingsEntity, night: LocalDate, at: LocalDateTime, source: Int) {
        val record = record(night)
        val plan = settings.planner(listOf(record)).plan(night)
        nightDao.upsert(
            record.copy(
                bedAt = at.toEpochMillis(),
                bedSource = source,
                targetBedAt = plan.bedtime.toEpochMillis(),
                wakeMinutes = TimeFormat.minutesOfDay(plan.wake),
            ),
        )
    }

    private suspend fun eveningNightNow(
        settings: SettingsEntity,
        now: LocalDateTime = LocalDateTime.now(),
    ): LocalDate {
        val today = now.toLocalDate()
        val nights = nightDao.between(today.minusDays(1).toEpochDay(), today.plusDays(1).toEpochDay())
        val phase = NightMath.phase(now, settings.planner(nights).plan(today).wake)
        return NightMath.eveningNight(phase)
    }

    private fun reschedule() {
        viewModelScope.launch { rescheduleNow() }
    }

    private suspend fun rescheduleNow() {
        withContext(Dispatchers.IO) { Signals.rescheduleAll(getApplication(), db) }
    }

    private fun buildUi(settings: SettingsEntity, nights: List<NightEntity>, now: LocalDateTime): UiState {
        val byNight = nights.associateBy { it.night }
        val planner = settings.planner(nights)
        val today = now.toLocalDate()
        val firstNight = settings.firstNight
        val phase = NightMath.phase(now, planner.plan(today).wake)
        val eveningNight = NightMath.eveningNight(phase)
        val plan = planner.plan(eveningNight)
        val eveningRecord = byNight[eveningNight]

        val openCheckin = NightMath.checkinNight(now, phase, planner::plan)
        val checkin = openCheckin
            ?.takeIf { it >= firstNight && byNight[it]?.rating == null }
            ?.let { CheckinUi(it, planner.plan(it), byNight[it]) }

        // Открытые ночи без данных — ещё не срыв: идущий вечер и ночь, которую можно отметить утром.
        val lastNight = phase.night
        val neutralFrom = listOfNotNull((phase as? NightMath.Evening)?.night, openCheckin).minOrNull()
            ?: lastNight.plusDays(1)
        val outcomes = nights.associate { it.night to it.outcome }
        val streak = StreakMath.compute(outcomes, firstNight, lastNight, neutralFrom)
        val spare = StreakMath.spareAvailable(outcomes, firstNight, lastNight, neutralFrom, eveningNight)

        val sleeping = phase is NightMath.Evening && eveningRecord?.bedAt != null
        val lastClosedNight = if (phase is NightMath.Evening && !sleeping) phase.night.minusDays(1) else phase.night
        val stats = nights.associate { it.night to Progress.NightStat(it.outcome, it.rating) }
        val history = (0L until HISTORY_ROWS)
            .map { lastClosedNight.minusDays(it) }
            .filter { it >= firstNight }
            .map { night ->
                val r = byNight[night]
                NightRow(
                    night = night,
                    target = r?.targetBedAt?.toLocalDateTime(),
                    bedAt = r?.bedAt?.toLocalDateTime(),
                    deviation = r?.deviationMinutes,
                    outcome = r?.outcome ?: Outcome.UNKNOWN,
                    rating = r?.rating,
                )
            }

        return UiState(
            now = now,
            settings = settings,
            phase = phase,
            eveningNight = eveningNight,
            plan = plan,
            goalPlan = planner.goalPlan(eveningNight),
            eveningRecord = eveningRecord,
            stage = plan.stageAt(now),
            sleeping = sleeping,
            checkin = checkin,
            todayList = byNight[today]?.brainDumpItems.orEmpty(),
            streak = streak,
            spareAvailable = spare,
            week = Progress.week(stats, firstNight, lastClosedNight),
            history = history,
            lastClosedNight = lastClosedNight,
        )
    }

    private companion object {
        const val HISTORY_DAYS = 60L
        const val HISTORY_ROWS = 14L
    }
}
