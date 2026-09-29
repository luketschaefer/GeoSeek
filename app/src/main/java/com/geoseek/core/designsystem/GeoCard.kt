package com.geoseek.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.geoseek.R
import com.geoseek.domain.catalog.Rarity

/**
 * The trading card used everywhere a catalog object is shown (collection grid, results, reveal).
 * [count] of 0 renders the card as locked (not yet collected).
 */
@Composable
fun GeoCard(
    name: String,
    rarity: Rarity,
    points: Int,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val rarityColor = GeoSeekTheme.rarityColors.of(rarity)
    val locked = count == 0
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    val border = BorderStroke(3.dp, if (locked) Color.Gray else rarityColor)
    val content: @Composable () -> Unit = {
        GeoCardContent(name = name, rarity = rarity, points = points, count = count, locked = locked)
    }
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.aspectRatio(CARD_ASPECT), colors = colors, border = border) {
            content()
        }
    } else {
        Card(modifier = modifier.aspectRatio(CARD_ASPECT), colors = colors, border = border) { content() }
    }
}

@Composable
private fun GeoCardContent(
    name: String,
    rarity: Rarity,
    points: Int,
    count: Int,
    locked: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        RarityBadge(rarity)
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (locked) Icons.Filled.Lock else Icons.Filled.Search,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (locked) Color.Gray else GeoSeekTheme.rarityColors.of(rarity),
            )
        }
        Text(
            text = if (locked) stringResource(R.string.card_locked_name) else name,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = pluralStringResource(R.plurals.card_points, points, points),
            style = MaterialTheme.typography.bodySmall,
        )
        if (count > 1) {
            Text(text = stringResource(R.string.card_count, count), style = MaterialTheme.typography.labelSmall)
        }
    }
}

private const val CARD_ASPECT = 0.7f

@Preview
@Composable
private fun GeoCardPreview() {
    GeoSeekTheme { GeoCard(name = "Helicopter", rarity = Rarity.LEGENDARY, points = 250, count = 2) }
}

@Preview
@Composable
private fun GeoCardLockedPreview() {
    GeoSeekTheme { GeoCard(name = "Cup", rarity = Rarity.COMMON, points = 10, count = 0) }
}
