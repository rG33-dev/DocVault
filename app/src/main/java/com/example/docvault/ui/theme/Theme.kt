package com.example.docvault.ui.theme

import android.app.Activity
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
    primary = DarkPrim,
    secondary = DarkSec,
    tertiary = DarkAccent,
    background = DarkBg,
    surface = DarkSurf,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = DarkOnSurf,
    onSurface = DarkOnSurf,
    surfaceVariant = Color(0xFF25234B),
    onSurfaceVariant = Color(0xFFB0B0C0)
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrim,
    secondary = LightSec,
    tertiary = LightAccent,
    background = LightBg,
    surface = LightSurf,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = LightOnSurf,
    onSurface = LightOnSurf,
    surfaceVariant = Color(0xFFE8EEFF),
    onSurfaceVariant = Color(0xFF546E7A)
)

@Composable
fun DocVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
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
