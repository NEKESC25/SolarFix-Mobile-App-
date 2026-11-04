package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SolarGold,
    onPrimary = Color(0xFF1E1300),
    primaryContainer = Color(0xFF6B4300),
    onPrimaryContainer = SolarGoldLight,

    secondary = CleanGreen,
    onSecondary = Color(0xFF002111),
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = CleanGreenLight,

    tertiary = SolarCyanGlow,
    onTertiary = Color(0xFF001F28),
    tertiaryContainer = Color(0xFF0E4C64),
    onTertiaryContainer = Color(0xFFA5F3FC),

    background = DarkBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkSurfaceBorder,
    outlineVariant = Color(0xFF334B73),
    error = AlertRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = SolarAmberDark,
    onPrimary = Color.White,
    primaryContainer = SolarAmberLight,
    onPrimaryContainer = Color(0xFF451A03),

    secondary = CleanGreenDark,
    onSecondary = Color.White,
    secondaryContainer = CleanGreenLight,
    onSecondaryContainer = Color(0xFF022C22),

    tertiary = TechCyanDark,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCFFAFE),
    onTertiaryContainer = Color(0xFF083344),

    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,
    outline = LightSurfaceBorder,
    error = AlertRed,
    onError = Color.White
)

@Composable
fun SolarfixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep Solarfix brand styling consistent across devices
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
