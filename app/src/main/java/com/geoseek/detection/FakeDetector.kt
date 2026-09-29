package com.geoseek.detection

import com.geoseek.domain.detection.Detection

/**
 * Scriptable detector for tests and emulators without a useful camera feed. Returns [script]
 * frames in order (looping if [loop]), or whatever was last passed to [emit] once the script is
 * exhausted. Thread-safe.
 */
class FakeDetector(
    script: List<List<Detection>> = emptyList(),
    private val loop: Boolean = false,
) : ObjectDetector {
    private val lock = Any()
    private val script = script.toList()
    private var index = 0
    private var override: List<Detection> = emptyList()
    private var closed = false

    override val name: String = "Fake (scripted)"

    fun emit(detections: List<Detection>) = synchronized(lock) { override = detections }

    /** Script-only variant used by tests; the frame argument is ignored. */
    fun nextFrame(): List<Detection> =
        synchronized(lock) {
            check(!closed) { "FakeDetector is closed" }
            when {
                index < script.size -> {
                    script[index++]
                }

                loop && script.isNotEmpty() -> {
                    index = 1
                    script[0]
                }

                else -> {
                    override
                }
            }
        }

    override suspend fun detect(frame: CameraFrame): List<Detection> = nextFrame()

    override fun close() = synchronized(lock) { closed = true }
}
