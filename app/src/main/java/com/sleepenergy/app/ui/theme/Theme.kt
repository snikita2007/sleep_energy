package com.sleepenergy.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Ночная палитра: глубокий синий фон и тёплый янтарь, как свет лампы.
// Тема только тёмная — приложение открывают вечером, яркий экран перед сном мешает.
// Динамические цвета Android выключены сознательно.
private val NightColors = darkColorScheme(
    primary = Color(0xFFBCC3FF),
    onPrimary = Color(0xFF1B2266),
    primaryContainer = Color(0xFF30398A),
    onPrimaryContainer = Color(0xFFDFE0FF),
    secondary = Color(0xFFC4C5DD),
    onSecondary = Color(0xFF2D2F42),
    secondaryContainer = Color(0xFF3B3E57),
    onSecondaryContainer = Color(0xFFE0E1F9),
    tertiary = Color(0xFFF6C26B),
    onTertiary = Color(0xFF432C00),
    tertiaryContainer = Color(0xFF5E4100),
    onTertiaryContainer = Color(0xFFFFDDB1),
    background = Color(0xFF0E1122),
    onBackground = Color(0xFFE3E2F0),
    surface = Color(0xFF0E1122),
    onSurface = Color(0xFFE3E2F0),
    surfaceVariant = Color(0xFF2A2E45),
    onSurfaceVariant = Color(0xFFC5C6DB),
    surfaceContainerLowest = Color(0xFF090B18),
    surfaceContainerLow = Color(0xFF14182E),
    surfaceContainer = Color(0xFF191D36),
    surfaceContainerHigh = Color(0xFF212640),
    surfaceContainerHighest = Color(0xFF2A2F4B),
    outline = Color(0xFF8F90A6),
    outlineVariant = Color(0xFF3E4260),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val SleepShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val SleepTypography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun SleepEnergyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NightColors,
        typography = SleepTypography,
        shapes = SleepShapes,
        content = content,
    )
}
