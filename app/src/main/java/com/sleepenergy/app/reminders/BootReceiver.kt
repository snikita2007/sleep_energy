package com.sleepenergy.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sleepenergy.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Будильники не переживают перезагрузку, обновление и смену времени — ставим заново.
 * Так же — когда пользователь включил точные будильники.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Signals.rescheduleAll(context, AppDatabase.get(context))
            } finally {
                result.finish()
            }
        }
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            // AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED (API 31+).
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
        )
    }
}
