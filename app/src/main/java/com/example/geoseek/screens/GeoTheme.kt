// Colors, text styles, rarity colors, light and dark color schemes, and the per-mode scenes that steer the pixel world behind every screen.
package com.example.geoseek.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.geoseek.models.Rarity

data class GeoColorScheme(
    val isDark: Boolean,
    val night: Color,
    val text: Color,
    val textSoft: Color,
    val textMuted: Color,
    val outline: Color,
    val highlight: Color,
    val panel: Color,
    val panelDeep: Color,
    val panelBorder: Color,
    val panelEdge: Color,
    val slot: Color,
    val slotBorder: Color,
    val danger: Color,
    val success: Color,
    val gold: Color,
)

val DarkGeoColorScheme = GeoColorScheme(
    isDark = true,
    night = Color(0xFF070A1F),
    text = Color(0xFFFFFFFF),
    textSoft = Color(0xFFD7DCF4),
    textMuted = Color(0xFF98A2CC),
    outline = Color(0xFF0A0A12),
    highlight = Color(0xFFFFE45C),
    panel = Color(0xD8192157),
    panelDeep = Color(0xEB0F1440),
    panelBorder = Color(0xFF4A5BC0),
    panelEdge = Color(0xFF080B26),
    slot = Color(0xC0283A8F),
    slotBorder = Color(0xFF6577DA),
    danger = Color(0xFFFF5D5D),
    success = Color(0xFF7BEA6E),
    gold = Color(0xFFFFD34D),
)

val LightGeoColorScheme = GeoColorScheme(
    isDark = false,
    night = Color(0xFFDCE5F7),
    text = Color(0xFF101530),
    textSoft = Color(0xFF2C3663),
    textMuted = Color(0xFF556299),
    outline = Color(0xFFFFFFFF),
    highlight = Color(0xFFD69900),
    panel = Color(0xEFF1F5FF),
    panelDeep = Color(0xF7E3EBFC),
    panelBorder = Color(0xFF7A8DEB),
    panelEdge = Color(0xFFB8C8FA),
    slot = Color(0xD8D9E3FF),
    slotBorder = Color(0xFF6B80E6),
    danger = Color(0xFFE03636),
    success = Color(0xFF2A9E1D),
    gold = Color(0xFFC48F00),
)

val LocalDarkMode = compositionLocalOf { true }
val LocalGeoColors = compositionLocalOf { DarkGeoColorScheme }

@Composable
fun GeoTheme(isDarkMode: Boolean = true, content: @Composable () -> Unit) {
    val scheme = if (isDarkMode) DarkGeoColorScheme else LightGeoColorScheme
    CompositionLocalProvider(
        LocalDarkMode provides isDarkMode,
        LocalGeoColors provides scheme,
        content = content,
    )
}

object GeoColors {
    val current: GeoColorScheme
        @Composable get() = LocalGeoColors.current

    val Night: Color
        @Composable get() = LocalGeoColors.current.night

    val Text: Color
        @Composable get() = LocalGeoColors.current.text

    val TextSoft: Color
        @Composable get() = LocalGeoColors.current.textSoft

    val TextMuted: Color
        @Composable get() = LocalGeoColors.current.textMuted

    val Outline: Color
        @Composable get() = LocalGeoColors.current.outline

    val Highlight: Color
        @Composable get() = LocalGeoColors.current.highlight

    val Panel: Color
        @Composable get() = LocalGeoColors.current.panel

    val PanelDeep: Color
        @Composable get() = LocalGeoColors.current.panelDeep

    val PanelBorder: Color
        @Composable get() = LocalGeoColors.current.panelBorder

    val PanelEdge: Color
        @Composable get() = LocalGeoColors.current.panelEdge

    val Slot: Color
        @Composable get() = LocalGeoColors.current.slot

    val SlotBorder: Color
        @Composable get() = LocalGeoColors.current.slotBorder

    val Danger: Color
        @Composable get() = LocalGeoColors.current.danger

    val Success: Color
        @Composable get() = LocalGeoColors.current.success

    val Gold: Color
        @Composable get() = LocalGeoColors.current.gold
}

fun rarityColor(rarity: Rarity): Color = when (rarity) {
    Rarity.COMMON -> Color(0xFFD0D3DC)
    Rarity.UNCOMMON -> Color(0xFF6BE070)
    Rarity.RARE -> Color(0xFF5AA2FF)
    Rarity.EPIC -> Color(0xFFC86BFF)
    Rarity.LEGENDARY -> Color(0xFFFF9628)
}

fun rarityLabel(rarity: Rarity): String =
    rarity.name.lowercase().replaceFirstChar { it.uppercase() }

object GeoType {
    private val Sans = FontFamily.SansSerif

    val Logo = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Black, fontSize = 56.sp, letterSpacing = 1.sp)
    val Menu = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Black, fontSize = 30.sp, letterSpacing = 0.5.sp)
    val Display = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Black, fontSize = 34.sp, letterSpacing = 0.sp, lineHeight = 38.sp)
    val Title = TextStyle(fontFamily = Sans, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
    val Heading = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    val Body = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 21.sp)
    val Label = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.3.sp)
    val Caption = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.2.sp)
    val Number = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Black, fontSize = 28.sp)
}

/** The biomes laid end to end around the world, in the same order as the menu. */
enum class Biome { Forest, Autumn, Snow, City, Coast }

/**
 * What the world should look like for a mode: which biome the camera travels to, the time of day
 * (null keeps the day/night dial turning), and which weather is out.
 */
data class Scene(
    val biome: Biome,
    val timeOfDay: Float?,
    val accent: Color,
    val roam: Boolean = false,
    val leaves: Float = 0f,
    val snow: Float = 0f,
    val aurora: Float = 0f,
    val fireflies: Float = 0f,
    val sparkles: Float = 0f,
)

object Scenes {
    /** Splash and sign in: the camera wanders the whole world while the sky turns. */
    val Title = Scene(Biome.Forest, null, Color(0xFF8BE04E), roam = true, aurora = 0.35f, fireflies = 0.7f)

    /** Home: a little village in the forest, days and nights rolling by. */
    val Home = Scene(Biome.Forest, null, Color(0xFF8BE04E), fireflies = 1f)

    /** Hunt: an autumn forest at sunset with leaves blowing through. */
    val Hunt = Scene(Biome.Autumn, 0.70f, Color(0xFFFF8A3D), leaves = 1f)

    /** Free look: snowy peaks under the aurora. */
    val Explore = Scene(Biome.Snow, 0.02f, Color(0xFF5CE1E6), snow = 1f, aurora = 1f)

    /** Collection: the city at night, windows lit, sparkles in the sky. */
    val Collection = Scene(Biome.City, 0.90f, Color(0xFFFFD34D), sparkles = 1f)

    /** Profile: a quiet morning on the coast. */
    val Profile = Scene(Biome.Coast, 0.33f, Color(0xFF6CB8FF))
}
