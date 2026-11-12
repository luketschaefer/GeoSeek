package com.example.geoseek.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Reads history without changing progress. ProgressStore writes it with the hunt's rewards. */
class HuntHistoryStore internal constructor(private val store: PlayerPrefs) {
    constructor(context: Context) : this(PlayerPrefs.from(context))

    fun load(username: String): HuntHistory {
        val saved = store.prefs.getString(store.key(username, "hunt_history"), null) ?: return HuntHistory()
        return try {
            val json = JSONObject(saved)
            if (json.getInt("version") != 1) return HuntHistory()
            val entries = json.getJSONArray("records")
            val records = (0 until minOf(entries.length(), HuntHistory.LIMIT)).mapNotNull { index ->
                try {
                    val entry = entries.getJSONObject(index)
                    HuntRecord(
                        id = entry.getString("id"),
                        environment = entry.getString("environment"),
                        finishedAtMillis = entry.getLong("finishedAt"),
                        targets = entry.getJSONArray("targets").strings(),
                        found = entry.getJSONArray("found").strings(),
                        score = entry.getInt("score"),
                        xpEarned = entry.getInt("xp"),
                        outcome = HuntOutcome.valueOf(entry.getString("outcome")),
                    )
                } catch (_: JSONException) {
                    null
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
            val stats = json.getJSONObject("stats")
            val counts = stats.getJSONObject("environmentCounts")
            HuntHistory(records, HuntStats(
                recordedHunts = stats.getInt("recordedHunts"),
                completed = stats.getInt("completed"),
                timedOut = stats.getInt("timedOut"),
                endedEarly = stats.getInt("endedEarly"),
                personalBest = stats.getInt("personalBest"),
                totalScore = stats.getLong("totalScore"),
                totalXp = stats.getLong("totalXp"),
                environmentCounts = counts.keys().asSequence().associateWith { counts.getInt(it) },
            ), json.getJSONArray("roundIds").strings().toSet())
        } catch (_: JSONException) {
            HuntHistory()
        }
    }

    internal fun write(editor: SharedPreferences.Editor, username: String, history: HuntHistory) {
        val records = JSONArray()
        history.records.forEach { record ->
            records.put(JSONObject()
                .put("id", record.id)
                .put("environment", record.environment)
                .put("finishedAt", record.finishedAtMillis)
                .put("targets", JSONArray(record.targets))
                .put("found", JSONArray(record.found))
                .put("score", record.score)
                .put("xp", record.xpEarned)
                .put("outcome", record.outcome.name))
        }
        val stats = history.stats
        val json = JSONObject()
            .put("version", 1)
            .put("records", records)
            .put("roundIds", JSONArray(history.recordedRoundIds.toList()))
            .put("stats", JSONObject()
                .put("recordedHunts", stats.recordedHunts)
                .put("completed", stats.completed)
                .put("timedOut", stats.timedOut)
                .put("endedEarly", stats.endedEarly)
                .put("personalBest", stats.personalBest)
                .put("totalScore", stats.totalScore)
                .put("totalXp", stats.totalXp)
                .put("environmentCounts", JSONObject(stats.environmentCounts)))
        editor.putString(store.key(username, "hunt_history"), json.toString())
    }

    private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }
}
