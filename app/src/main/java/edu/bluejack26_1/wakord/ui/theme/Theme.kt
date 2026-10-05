package edu.bluejack26_1.wakord.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private fun WakColors.toColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        secondary = peach,
        onSecondary = onPrimary,
        background = bg,
        onBackground = text,
        surface = s1,
        onSurface = text,
        surfaceVariant = s3,
        onSurfaceVariant = text2,
        surfaceContainerLowest = bg,
        surfaceContainerLow = s1,
        surfaceContainer = s2,
        surfaceContainerHigh = s3,
        surfaceContainerHighest = s4,
        outline = outline,
        outlineVariant = s5,
        error = error,
        onError = onError,
    )
}

@Composable
fun WAKordTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkWakColors else LightWakColors
    CompositionLocalProvider(LocalWakColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(),
            typography = WakTypography,
            content = content,
        )
    }
}

object WakTheme {
    val colors: WakColors
        @Composable
        @ReadOnlyComposable
        get() = LocalWakColors.current
}
