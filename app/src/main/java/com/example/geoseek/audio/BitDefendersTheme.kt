package com.example.geoseek.audio

import android.content.Context
import android.media.MediaPlayer
import com.example.geoseek.R

/**
 * Easter egg: plays BitDefenders' main menu music once through when the camera reads the word "BitDefenders".
 *
 * Scanning the word again while the track is playing doesn't restart it. Call from the main thread.
 */
object BitDefendersTheme {

    private var player: MediaPlayer? = null

    /** Whether [text] contains "BitDefenders", ignoring case, spaces, line breaks, and punctuation. */
    fun mentionedIn(text: String): Boolean =
        text.lowercase().filter { it.isLetterOrDigit() }.contains("bitdefenders")

    /** Starts the track unless it's already playing. */
    fun play(context: Context) {
        if (player != null) return
        player = MediaPlayer.create(context.applicationContext, R.raw.bitdefenders_main_menu)?.apply {
            setOnCompletionListener { stop() }
            start()
        }
    }

    /** Stops the track early, if it's playing. */
    fun stop() {
        player?.release()
        player = null
    }
}
