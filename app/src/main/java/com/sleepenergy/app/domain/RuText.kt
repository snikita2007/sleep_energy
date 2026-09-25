package com.sleepenergy.app.domain

import kotlin.math.abs

/** Русские формы слов и тексты сигналов. */
object RuText {
    fun plural(n: Long, one: String, few: String, many: String): String {
        val mod100 = abs(n) % 100
        val mod10 = mod100 % 10
        return when {
            mod100 in 11..14 -> many
            mod10 == 1L -> one
            mod10 in 2..4 -> few
            else -> many
        }
    }

    fun days(n: Int): String = "$n ${plural(n.toLong(), "день", "дня", "дней")}"

    fun steps(n: Int): String = "$n ${plural(n.toLong(), "шаг", "шага", "шагов")}"

    fun minutes(n: Long): String = "$n ${plural(n, "минута", "минуты", "минут")}"

    /** «1 ч 10 мин», «45 мин», «2 ч» — знак отбрасывается. */
    fun duration(minutes: Long): String {
        val total = abs(minutes)
        val hours = total / 60
        val rest = total % 60
        return when {
            hours == 0L -> "$rest мин"
            rest == 0L -> "$hours ч"
            else -> "$hours ч $rest мин"
        }
    }

    /** Длина своего времени для сигнала: «дальше час на себя». */
    fun ownTime(minutes: Int): String = when (minutes) {
        30 -> "полчаса"
        60 -> "час"
        90 -> "полтора часа"
        120 -> "два часа"
        else -> minutes(minutes.toLong())
    }

    data class SignalText(val title: String, val text: String)

    fun signal(kind: SignalKind, plan: EveningPlan): SignalText = when (kind) {
        SignalKind.FINISH -> SignalText(
            "Через ${minutes(EveningPlan.FINISH_LEAD_MINUTES)} заканчиваем дела",
            "Дальше ${ownTime(plan.ownTimeMinutes)} на себя — это часть плана",
        )
        SignalKind.WIND_DOWN -> SignalText(
            "Начинаем готовиться ко сну",
            "Душ, телефон на зарядку, свет потише",
        )
        SignalKind.BRAIN_DUMP -> SignalText(
            "Минута на выгрузку мыслей",
            "Запиши дела на завтра — и отбой в ${TimeFormat.hhmm(plan.bedtime)}",
        )
        SignalKind.MORNING -> SignalText(
            "Доброе утро! Как самочувствие?",
            "Одно касание — и готово",
        )
    }
}
