package com.geoseek.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.geoseek.domain.catalog.Rarity

/** Rarity palette. Read via `GeoSeekTheme.rarityColors`, never hard-code rarity colors in features. */
@Immutable
data class RarityColors(
    val common: Color,
    val uncommon: Color,
    val rare: Color,
    val epic: Color,
    val legendary: Color,
) {
    fun of(rarity: Rarity): Color =
        when (rarity) {
            Rarity.COMMON -> common
            Rarity.UNCOMMON -> uncommon
            Rarity.RARE -> rare
            Rarity.EPIC -> epic
            Rarity.LEGENDARY -> legendary
        }
}

internal val LightRarityColors =
    RarityColors(
        common = Color(0xFF757575),
        uncommon = Color(0xFF2E7D32),
        rare = Color(0xFF1565C0),
        epic = Color(0xFF6A1B9A),
        legendary = Color(0xFFEF6C00),
    )

internal val DarkRarityColors =
    RarityColors(
        common = Color(0xFFBDBDBD),
        uncommon = Color(0xFF81C784),
        rare = Color(0xFF64B5F6),
        epic = Color(0xFFCE93D8),
        legendary = Color(0xFFFFB74D),
    )

val LocalRarityColors = staticCompositionLocalOf { LightRarityColors }
