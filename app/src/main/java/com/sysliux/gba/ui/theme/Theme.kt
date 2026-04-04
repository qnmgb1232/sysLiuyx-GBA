package com.sysliux.gba.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val NeonRetroColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = NeonPink,
    tertiary = ElectricPurple,
    background = DarkPurpleBlack,
    surface = DarkPurpleBlack,
    onPrimary = DarkPurpleBlack,
    onSecondary = DarkPurpleBlack,
    onTertiary = DarkPurpleBlack,
    onBackground = LightPurpleWhite,
    onSurface = LightPurpleWhite,
)

@Composable
fun SysLiuxGbaTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = NeonRetroColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkPurpleBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
