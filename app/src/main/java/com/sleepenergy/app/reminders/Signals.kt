package com.sleepenergy.app.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sleepenergy.app.MainActivity
import com.sleepenergy.app.R
import com.sleepenergy.app.Route
import com.sleepenergy.app.data.AppDatabase
import com.sleepenergy.app.data.isDone
import com.sleepenergy.app.data.night
import com.sleepenergy.app.data.planner
import com.sleepenergy.app.data.toEpochMillis
import com.sleepenergy.app.domain.EveningPlan
import com.sleepenergy.app.domain.RuText
import com.sleepenergy.app.domain.SignalKind
import com.sleepenergy.app.domain.SignalPlanner
import java.time.LocalDateTime

/**
 * Три вечерних сигнала и утренний. На каждый вид — один будильник на ближайшее
 * срабатывание; после срабатывания ставится следующий.
 *
 * Если пользователь включил «Будильники и напоминания» — будильник точный.
 * Иначе — окно ±5 минут вокруг нужного времени ([AlarmManager.setWindow]):
 * у обычного неточного будильника окно доходит до часа, а сигнал
 * «через 20 минут заканчиваем» через час бессмыслен.
 */
object Signals {
    const val EXTRA_KIND = "kind"
    const val EXTRA_NIGHT = "night"
    const val EXTRA_AT = "at"

    private const val ACTION_SIGNAL = "com.sleepenergy.app.action.SIGNAL"
    private const val CHANNEL_EVENING = "evening_signals"
    private const val CHANNEL_MORNING = "morning_checkin"
    private const val REQUEST_ALARM = 100
    private const val REQUEST_OPEN = 200
    private const val NOTIFICATION_ID = 10
    private const val WINDOW_MILLIS = 10 * 60_000L

    fun ensureChannels(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_EVENING, "Вечерние сигналы", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "Не больше трёх коротких сигналов за вечер" },
                NotificationChannel(CHANNEL_MORNING, "Утро", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "Оценка самочувствия в одно касание" },
            ),
        )
    }

    /** Перепланирует все сигналы. Вызывается после любых изменений — повторный вызов безопасен. */
    suspend fun rescheduleAll(context: Context, db: AppDatabase) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val settings = db.settingsDao().get()
        if (settings == null || !settings.onboarded) {
            SignalKind.entries.forEach { alarmManager.cancel(alarmIntent(context, it, null)) }
            return
        }
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val nights = db.nightDao().between(today.minusDays(1).toEpochDay(), today.plusDays(5).toEpochDay())
        val byNight = nights.associateBy { it.night }
        val planner = settings.planner(nights)
        val exact = canScheduleExact(context)
        for (kind in SignalKind.entries) {
            val next = SignalPlanner.next(kind, now, planner::plan) { night, k -> byNight[night]?.isDone(k) == true }
            val pending = alarmIntent(context, kind, next)
            when {
                next == null -> alarmManager.cancel(pending)
                exact -> alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    next.at.toEpochMillis(),
                    pending,
                )
                else -> alarmManager.setWindow(
                    AlarmManager.RTC_WAKEUP,
                    next.at.toEpochMillis() - WINDOW_MILLIS / 2,
                    WINDOW_MILLIS,
                    pending,
                )
            }
        }
    }

    /** Точные будильники: до Android 12 доступны всем, дальше — по спецдоступу. */
    fun canScheduleExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun canNotify(context: Context): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission") // проверено в canNotify
    fun notify(context: Context, kind: SignalKind, plan: EveningPlan) {
        if (!canNotify(context)) return
        ensureChannels(context)
        val text = RuText.signal(kind, plan)
        val open = PendingIntent.getActivity(
            context,
            REQUEST_OPEN + kind.ordinal,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN, routeFor(kind))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, if (kind.evening) CHANNEL_EVENING else CHANNEL_MORNING)
            .setSmallIcon(R.drawable.ic_moon)
            .setColor(context.getColor(R.color.amber))
            .setContentTitle(text.title)
            .setContentText(text.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text.text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID + kind.ordinal, notification)
    }

    private fun routeFor(kind: SignalKind): String = when (kind) {
        SignalKind.BRAIN_DUMP -> Route.BRAIN_DUMP
        SignalKind.MORNING -> Route.CHECKIN
        SignalKind.FINISH, SignalKind.WIND_DOWN -> Route.EVENING
    }

    private fun alarmIntent(context: Context, kind: SignalKind, planned: SignalPlanner.Planned?): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_ALARM + kind.ordinal,
            Intent(context, SignalReceiver::class.java)
                .setAction(ACTION_SIGNAL)
                .putExtra(EXTRA_KIND, kind.name)
                .putExtra(EXTRA_NIGHT, planned?.night?.toEpochDay() ?: -1L)
                .putExtra(EXTRA_AT, planned?.at?.toEpochMillis() ?: -1L),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
