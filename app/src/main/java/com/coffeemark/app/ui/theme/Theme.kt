package com.coffeemark.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = CoffeeBrown,
    onPrimary = OnCoffee,
    primaryContainer = CoffeeLight,
    onPrimaryContainer = OnCream,
    secondary = CoffeeBrownVariant,
    onSecondary = OnCoffee,
    tertiary = Caramel,
    onTertiary = OnCoffee,
    background = Cream,
    onBackground = OnCream,
    surface = CreamWhite,
    onSurface = OnCream,
    surfaceVariant = CreamWarm,
    onSurfaceVariant = OnCreamMuted,
    outline = CoffeeLight,
    outlineVariant = Color(0xFFD7CCC8),
    error = Error,
    onError = OnCoffee
)

private val DarkColorScheme = darkColorScheme(
    primary = CoffeeLight,
    onPrimary = DarkBackground,
    primaryContainer = CoffeeBrown,
    onPrimaryContainer = OnCoffee,
    secondary = CoffeeBrownVariant,
    onSecondary = OnCoffee,
    tertiary = Caramel,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = OnDarkSurface,
    surface = DarkSurface,
    onSurface = OnDarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkSurfaceMuted,
    outline = CoffeeBrownVariant,
    outlineVariant = DarkSurfaceVariant,
    error = Color(0xFFCF6679),
    onError = DarkBackground
)

@Composable
fun CoffeeMarkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CoffeeMarkTypography,
        content = content
    )
}
