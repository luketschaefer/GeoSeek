package com.geoseek.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight

private val Base = Typography()

internal val GeoSeekTypography =
    Typography(
        displaySmall = Base.displaySmall.copy(fontWeight = FontWeight.Bold),
        headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = Base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
