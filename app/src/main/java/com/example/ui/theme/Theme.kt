package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme =
    darkColorScheme(
        primary = Blue400,
        secondary = Emerald400,
        tertiary = Rose400,
        background = BackgroundDark,
        surface = SurfaceDark,
        surfaceVariant = SurfaceDark,
        onPrimary = BackgroundDark,
        onSecondary = BackgroundDark,
        onBackground = TextSlate200,
        onSurface = TextSlate200,
        onSurfaceVariant = TextSlate400,
        outline = BorderDark
    )

private val AmoledColorScheme =
    darkColorScheme(
        primary = Blue400,
        secondary = Emerald400,
        tertiary = Rose400,
        background = BackgroundAmoled,
        surface = SurfaceAmoled,
        surfaceVariant = SurfaceAmoled,
        onPrimary = BackgroundAmoled,
        onSecondary = BackgroundAmoled,
        onBackground = TextSlate200,
        onSurface = TextSlate200,
        onSurfaceVariant = TextSlate400,
        outline = BorderAmoled
    )

private val LightColorScheme =
    lightColorScheme(
        primary = Blue600,
        secondary = Emerald500,
        tertiary = Rose500,
        background = BackgroundLight,
        surface = SurfaceLight,
        surfaceVariant = SurfaceLight,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = TextBlack,
        onSurface = TextBlack,
        onSurfaceVariant = TextSlate700,
        outline = BorderLight
    )

@Composable
fun MyApplicationTheme(
  appThemeStr: String = "DARK", // "LIGHT", "DARK", "AMOLED"
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (appThemeStr != "LIGHT") dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      appThemeStr == "AMOLED" -> AmoledColorScheme
      appThemeStr == "LIGHT" -> LightColorScheme
      else -> DarkColorScheme
    }

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      val isDark = appThemeStr != "LIGHT"
      WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
      WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
    }
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
