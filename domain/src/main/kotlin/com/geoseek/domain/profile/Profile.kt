package com.geoseek.domain.profile

/** The local player's profile. There is exactly one on a device. */
data class Profile(
    val displayName: String,
    val totalXp: Long,
) {
    val level: LevelProgress get() = LevelCurve.progress(totalXp)

    companion object {
        const val DEFAULT_DISPLAY_NAME = "Seeker"
    }
}
