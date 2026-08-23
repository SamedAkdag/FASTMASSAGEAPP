package com.aistudio.pingring.pgrng.ui.theme

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
    primary = CrimsonBright,
    onPrimary = Color.White,
    primaryContainer = CrimsonDark,
    onPrimaryContainer = CrimsonLight,
    secondary = Color(0xFF94A3B8),
    onSecondary = SlateDark,
    secondaryContainer = SlateMedium,
    onSecondaryContainer = Color(0xFFF1F5F9),
    tertiary = StatusInfo,
    onTertiary = Color.White,
    background = SlateDarker,
    onBackground = TextOnDark,
    surface = SlateMedium,
    onSurface = TextOnDark,
    surfaceVariant = SlateBorderDark,
    onSurfaceVariant = TextSecondaryOnDark,
    outline = SlateBorderDark,
    error = CrimsonBright,
    onError = Color.White
  )

private val LightColorScheme =
  lightColorScheme(
    primary = CrimsonPrimary,
    onPrimary = Color.White,
    primaryContainer = CrimsonLight,
    onPrimaryContainer = CrimsonDark,
    secondary = SlateMedium,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1F5F9),
    onSecondaryContainer = SlateDark,
    tertiary = StatusInfo,
    onTertiary = Color.White,
    background = SlateLight,
    onBackground = TextPrimary,
    surface = SlateCard,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF8FAFC),
    onSurfaceVariant = TextSecondary,
    outline = SlateBorder,
    error = CrimsonPrimary,
    onError = Color.White
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
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
