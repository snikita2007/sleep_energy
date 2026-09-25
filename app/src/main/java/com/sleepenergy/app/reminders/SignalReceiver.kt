package com.sleepenergy.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sleepenergy.app.data.AppDatabase
import com.sleepenergy.app.data.NightEntity
import com.sleepenergy.app.data.isDone
import com.sleepenergy.app.data.planner
import com.sleepenergy.app.data.toLocalDateTime
import com.sleepenergy.app.data.withSignalSent
import com.sleepenergy.app.domain.SignalKind
import com.sleepenergy.app.domain.SignalPlanner
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Срабатывание сигнала. Показываем уведомление, только если оно ещё к месту:
 * человек не лёг, выгрузка не записана, утро не отмечено, будильник не опоздал.
 * В любом случае ставим следующий будильник.
 */
class SignalReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(Signals.EXTRA_KIND)
            ?.let { name -> SignalKind.entries.firstOrNull { it.name == name } }
            ?: return
        val nightEpochDay = intent.getLongExtra(Signals.EXTRA_NIGHT, -1L)
        val scheduledAt = intent.getLongExtra(Signals.EXTRA_AT, -1L)
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.get(context)
                val settings = db.settingsDao().get()
                if (settings != null && settings.onboarded && nightEpochDay >= 0 && scheduledAt > 0) {
                    val night = db.nightDao().get(nightEpochDay) ?: NightEntity(nightEpochDay)
                    val verdict = SignalPlanner.verdict(scheduledAt.toLocalDateTime(), LocalDateTime.now())
                    // EARLY: будильник доставлен уже с данными следующего срабатывания
                    // (перепланирование успело обновить PendingIntent) — не трогаем ничего.
                    if (verdict != SignalPlanner.Verdict.EARLY) {
                        if (verdict == SignalPlanner.Verdict.DUE && !night.isDone(kind)) {
                            val plan = settings.planner(listOf(night)).plan(LocalDate.ofEpochDay(nightEpochDay))
                            Signals.notify(context, kind, plan)
                        }
                        // Сигнал этой ночи отработал (или уже не к месту) — второй раз не ставим.
                        db.nightDao().upsert(night.withSignalSent(kind))
                    }
                }
                Signals.rescheduleAll(context, db)
            } finally {
                result.finish()
            }
        }
    }
}
