package com.sleepenergy.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepenergy.app.AppState
import com.sleepenergy.app.CheckinResult
import com.sleepenergy.app.MainViewModel
import com.sleepenergy.app.R
import com.sleepenergy.app.Route
import com.sleepenergy.app.RouteRequest
import com.sleepenergy.app.UiState
import com.sleepenergy.app.data.brainDumpItems

private enum class Tab(val title: String) {
    EVENING("Вечер"),
    PROGRESS("Прогресс"),
    SETTINGS("Настройки"),
}

/** Полноэкранные сценарии поверх вкладок. */
private enum class Overlay { BRAIN_DUMP, CHECKIN }

@Composable
fun SleepRoot(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val route by viewModel.route.collectAsStateWithLifecycle()
    val result by viewModel.checkinResult.collectAsStateWithLifecycle()

    when (val current = state) {
        AppState.Loading -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        AppState.Onboarding -> OnboardingScreen(onDone = viewModel::completeOnboarding)
        is AppState.Ready -> ReadyRoot(current.ui, viewModel, route, result)
    }
}

@Composable
private fun ReadyRoot(ui: UiState, viewModel: MainViewModel, route: RouteRequest?, result: CheckinResult?) {
    var tab by rememberSaveable { mutableStateOf(Tab.EVENING) }
    var overlay by remember { mutableStateOf<Overlay?>(null) }

    // Переход из уведомления — только по состоянию, посчитанному после нажатия:
    // иначе утром можно увидеть вчерашнее «отмечать нечего».
    LaunchedEffect(route, ui.now) {
        val request = route ?: return@LaunchedEffect
        if (ui.now < request.at) return@LaunchedEffect
        when (request.route) {
            Route.BRAIN_DUMP -> overlay = Overlay.BRAIN_DUMP
            Route.CHECKIN -> if (ui.checkin != null) overlay = Overlay.CHECKIN
        }
        tab = Tab.EVENING
        viewModel.consumeRoute()
    }

    BackHandler(enabled = result != null) { viewModel.dismissCheckinResult() }
    BackHandler(enabled = result == null && overlay != null) { overlay = null }

    val checkin = ui.checkin
    when {
        result != null -> CheckinResultScreen(
            result = result,
            streak = ui.streak,
            spareAvailable = ui.spareAvailable,
            onDone = viewModel::dismissCheckinResult,
        )

        overlay == Overlay.BRAIN_DUMP -> BrainDumpScreen(
            bedtime = ui.plan.bedtime,
            initial = ui.eveningRecord?.brainDumpItems.orEmpty(),
            onSave = { items, goToBed ->
                viewModel.saveBrainDump(items, goToBed)
                overlay = null
            },
            onClose = { overlay = null },
        )

        overlay == Overlay.CHECKIN && checkin != null -> MorningScreen(
            checkin = checkin,
            onSave = { rating, bedEstimate ->
                viewModel.saveCheckin(checkin.night, rating, bedEstimate)
                overlay = null
            },
            onClose = { overlay = null },
        )

        else -> Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Tab.entries.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { tab = item },
                            icon = { TabIcon(item) },
                            label = { Text(item.title) },
                        )
                    }
                }
            },
        ) { padding ->
            Box(
                Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding),
            ) {
                when (tab) {
                    Tab.EVENING -> EveningScreen(
                        ui = ui,
                        onOpenCheckin = { overlay = Overlay.CHECKIN },
                        onOpenBrainDump = { overlay = Overlay.BRAIN_DUMP },
                        onGoToBed = viewModel::goToBed,
                        onUndoBed = viewModel::undoGoToBed,
                        onWakeOverride = { viewModel.setWakeOverride(ui.eveningNight, it) },
                    )
                    Tab.PROGRESS -> ProgressScreen(ui)
                    Tab.SETTINGS -> SettingsScreen(ui, viewModel)
                }
            }
        }
    }
}

@Composable
private fun TabIcon(tab: Tab) {
    when (tab) {
        Tab.EVENING -> Icon(painterResource(R.drawable.ic_moon), contentDescription = null)
        Tab.PROGRESS -> Icon(Icons.Filled.DateRange, contentDescription = null)
        Tab.SETTINGS -> Icon(Icons.Filled.Settings, contentDescription = null)
    }
}
