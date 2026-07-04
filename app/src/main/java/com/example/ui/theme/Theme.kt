package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = CyberCyan,
    secondary = CyberPink,
    tertiary = CyberPurple,
    background = CyberBlack,
    surface = CyberDarkCard,
    onBackground = CyberWhite,
    onSurface = CyberWhite
  )

private val LightColorScheme = DarkColorScheme // Always use cyber dark for this game!

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme for the cyber neon aesthetic!
  dynamicColor: Boolean = false, // Force disable dynamic colors to preserve our hand-picked neon styling
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
