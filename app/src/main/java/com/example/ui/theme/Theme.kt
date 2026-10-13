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

private val DarkColorScheme =
  darkColorScheme(
    primary = PurpleGrey40,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF090D16),
    surface = Color(0xFF151F32),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = KorvixPrimary,
    secondary = KorvixSecondary,
    tertiary = Pink40,
    background = KorvixBackground,
    surface = KorvixSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = KorvixTextDark,
    onSurface = KorvixTextDark,
    outline = KorvixBorderAccent
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemDark.value,
  // Set dynamic color to false by default to ensure KORVIX branding is intact
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
