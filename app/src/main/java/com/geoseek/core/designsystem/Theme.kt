package com.geoseek.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private val LightColors =
    lightColorScheme(
        primary = Forest,
        secondary = Moss,
        tertiary = Amber,
        background = Sand,
        surface = Sand,
    )

private val DarkColors =
    darkColorScheme(
        primary = ForestLight,
        secondary = ForestLight,
        tertiary = Amber,
        background = Night,
        surface = Night,
    )

@Composable
fun GeoSeekTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalRarityColors provides if (darkTheme) DarkRarityColors else LightRarityColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = GeoSeekTypography,
            content = content,
        )
    }
}

object GeoSeekTheme {
    val rarityColors: RarityColors
        @Composable
        @ReadOnlyComposable
        get() = LocalRarityColors.current
}
