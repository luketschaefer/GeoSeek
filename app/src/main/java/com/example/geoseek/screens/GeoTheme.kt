// Colors, text styles, rarity colors, and the per-mode scenes that steer the pixel world behind every screen.
package com.example.geoseek.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.geoseek.models.Rarity

object GeoColors {
    val Night = Color(0xFF070A1F)
    val Text = Color(0xFFFFFFFF)
    val TextSoft = Color(0xFFD7DCF4)
    val TextMuted = Color(0xFF98A2CC)
    val Outline = Color(0xFF0A0A12)

    /** The yellow a menu item turns when you press it, straight out of a title screen. */
    val Highlight = Color(0xFFFFE45C)

    val Panel = Color(0xD8192157)
    val PanelDeep = Color(0xEB0F1440)
    val PanelBorder = Color(0xFF4A5BC0)
    val PanelEdge = Color(0xFF080B26)
    val Slot = Color(0xC0283A8F)
    val SlotBorder = Color(0xFF6577DA)

    val Danger = Color(0xFFFF5D5D)
    val Success = Color(0xFF7BEA6E)
    val Gold = Color(0xFFFFD34D)
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
