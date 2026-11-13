// Offline quest time: wall time seeds it once; elapsed time advances it afterwards.
package com.example.geoseek.data

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.provider.Settings
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Keeps device clock edits and travel from changing the active quest day.
 *
 * The checkpoint survives app restarts. A reboot starts elapsed time over, so resume from the last
 * saved quest time instead of trusting the new wall clock. Time while powered off (and between the
 * last checkpoint and the first read after reboot) cannot be measured reliably offline and is not
 * credited. The first reference still depends on the device date; this is not a server clock.
 */
internal class DailyQuestClock(
    private val prefs: SharedPreferences,
    private val wallTimeMillis: () -> Long,
    private val elapsedRealtime: () -> Long,
    private val bootCount: () -> Int,
) {
    fun today(): Int = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT).let {
        it.timeInMillis = nowMillis()
        it.get(Calendar.YEAR) * 1000 + it.get(Calendar.DAY_OF_YEAR)
    }

    internal fun nowMillis(): Long = synchronized(prefs) {
        val elapsed = elapsedRealtime()
        val boot = bootCount()
        val previousElapsed = prefs.getLong(KEY_ELAPSED, elapsed)
        val now = when {
            !prefs.contains(KEY_TIME) -> wallTimeMillis()
            prefs.getInt(KEY_BOOT, boot) == boot && elapsed >= previousElapsed ->
                prefs.getLong(KEY_TIME, 0L) + elapsed - previousElapsed
            else -> prefs.getLong(KEY_TIME, 0L)
        }
        prefs.edit()
            .putLong(KEY_TIME, now)
            .putLong(KEY_ELAPSED, elapsed)
            .putInt(KEY_BOOT, boot)
            .apply()
        now
    }

    companion object {
        private const val KEY_TIME = "quest_clock.time"
        private const val KEY_ELAPSED = "quest_clock.elapsed"
        private const val KEY_BOOT = "quest_clock.boot"

        fun from(context: Context, prefs: SharedPreferences) = DailyQuestClock(
            prefs,
            System::currentTimeMillis,
            SystemClock::elapsedRealtime,
            {
                try {
                    Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
                } catch (_: SecurityException) {
                    // If boot count is unavailable, a lower elapsed value still detects a restart.
                    -1
                }
            },
        )
    }
}
