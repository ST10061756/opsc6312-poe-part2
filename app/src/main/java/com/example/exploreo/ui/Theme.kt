package com.example.exploreo.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Surface

private val Gold = Color(0xFFFFD700)
private val RichGold = Color(0xFFFFC107)
private val JetBlack = Color(0xFF000000)
private val Charcoal = Color(0xFF121212)

private val DarkColors = darkColorScheme(
    primary = Gold,
    secondary = RichGold,
    background = Charcoal,
    surface = Charcoal,
    onPrimary = JetBlack,
    onBackground = Color(0xFFEFEFEF),
)

private val LightColors = lightColorScheme(
    primary = RichGold,
    secondary = Gold,
    background = Color(0xFFF5F5F5),
    surface = Color(0xFFFFFFFF),
    onPrimary = JetBlack,
    onBackground = Color(0xFF101010),
)

@Composable
fun GradientBackground(content: @Composable () -> Unit) {
    val gradient = Brush.verticalGradient(colors = listOf(Gold, Charcoal))
    Surface(color = Color.Transparent) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
        ) {
            content()
        }
    }
}

@Composable
fun ExploreoTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (useDarkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors) {
        GradientBackground { content() }
    }
}


