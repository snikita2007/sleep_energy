package com.sleepenergy.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.sleepenergy.app.ui.SleepRoot
import com.sleepenergy.app.ui.theme.SleepEnergyTheme

/** Куда открыть приложение из уведомления. */
object Route {
    const val EVENING = "evening"
    const val BRAIN_DUMP = "brain_dump"
    const val CHECKIN = "checkin"
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Тема всегда ночная — системные бары со светлыми иконками.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) viewModel.openRoute(intent?.getStringExtra(EXTRA_OPEN))
        setContent {
            LifecycleResumeEffect(Unit) {
                viewModel.onResumed()
                onPauseOrDispose { }
            }
            SleepEnergyTheme {
                // Surface задаёт цвет текста по умолчанию (onBackground) для всех экранов.
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SleepRoot(viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.openRoute(intent.getStringExtra(EXTRA_OPEN))
    }

    companion object {
        const val EXTRA_OPEN = "open"
    }
}
