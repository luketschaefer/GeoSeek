package com.geoseek.core.designsystem

import com.geoseek.R
import com.geoseek.domain.catalog.Environment

fun Environment.labelRes(): Int =
    when (this) {
        Environment.PARK -> R.string.env_park
        Environment.KITCHEN -> R.string.env_kitchen
        Environment.STREET -> R.string.env_street
        Environment.CAMPUS -> R.string.env_campus
    }
