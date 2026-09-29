package com.geoseek.detection

import com.geoseek.domain.detection.Detection

/**
 * Placeholder for server-side detection (e.g. a larger model for rare variants like
 * "supercar" vs "minivan"). It is intentionally NOT bound in DetectionModule.
 *
 * Why this calls OUR backend instead of Google Cloud Vision directly: any API key shipped in
 * an APK can be extracted in minutes (apktool/strings), then used to run up our bill or
 * exhaust quota. Keys belong on a server we control, which authenticates the player (Firebase
 * Auth ID token), rate-limits, and forwards to Cloud Vision. Never add a Cloud API key to this
 * app, BuildConfig, resources, or local.properties.
 */
class CloudVisionDetector(
    @Suppress("unused") private val backendBaseUrl: String,
) : ObjectDetector {
    override val name: String = "Cloud (not implemented)"

    override suspend fun detect(frame: CameraFrame): List<Detection> =
        throw DetectionException("CloudVisionDetector is a stub; see ROADMAP.md (Could: server detection)")

    override fun close() = Unit
}
