package com.geoseek.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.geoseek.R
import com.geoseek.domain.catalog.Rarity

@Composable
fun RarityBadge(
    rarity: Rarity,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(rarity.labelRes()),
        style = MaterialTheme.typography.labelMedium,
        color = Color.White,
        modifier =
            modifier
                .background(GeoSeekTheme.rarityColors.of(rarity), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 2.dp),
    )
}

fun Rarity.labelRes(): Int =
    when (this) {
        Rarity.COMMON -> R.string.rarity_common
        Rarity.UNCOMMON -> R.string.rarity_uncommon
        Rarity.RARE -> R.string.rarity_rare
        Rarity.EPIC -> R.string.rarity_epic
        Rarity.LEGENDARY -> R.string.rarity_legendary
    }
