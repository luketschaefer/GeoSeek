package com.geoseek.domain.detection

/** One detector result for one frame. [confidence] is in [0, 1]. */
data class Detection(
    val label: String,
    val confidence: Float,
)
