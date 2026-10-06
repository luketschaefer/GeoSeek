// On-device accounts, the remembered session, and each player's progress (XP, cards, hunts, daily quest).
// A player stays signed in until they sign out. Passwords are stored only as salted PBKDF2 hashes.
// This is local-only until a real backend exists.
package com.example.geoseek.screens

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.example.geoseek.managers.XPManager
import com.example.geoseek.models.GameObject
import java.security.SecureRandom
import java.util.Calendar
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class Player(
    val username: String,
    val joinedAtMillis: Long,
    val xp: Int,
    val huntsPlayed: Int,
    val bestScore: Int,
    val cards: Set<String>,
    val findsToday: Int,
    val questClaimedToday: Boolean,
)

data class LevelInfo(val level: Int, val intoLevel: Int, val needed: Int) {
    val progress: Float get() = intoLevel / needed.toFloat()
}

/** Level progress, using the same curve as XPManager (a new level every 100 XP). */
val Player.levelInfo: LevelInfo
    get() {
        val manager = XPManager().apply { addXp(xp) }
        return LevelInfo(manager.level, xp % 100, 100)
    }

sealed interface AuthResult {
    data class Success(val player: Player) : AuthResult
    data class Failure(val message: String, val field: AuthField? = null) : AuthResult
}

enum class AuthField { Username, Password, Confirm }

class AuthStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("geoseek_auth", Context.MODE_PRIVATE)

    /** The remembered player, or null if nobody is signed in. */
    fun currentPlayer(): Player? = prefs.getString(KEY_SESSION, null)?.let(::loadPlayer)

    fun signUp(username: String, password: String, confirm: String): AuthResult {
        val name = username.trim()
        validateUsername(name)?.let { return AuthResult.Failure(it, AuthField.Username) }
        if (password.length < MIN_PASSWORD) {
            return AuthResult.Failure("Password needs at least $MIN_PASSWORD characters", AuthField.Password)
        }
        if (password != confirm) return AuthResult.Failure("Passwords don't match", AuthField.Confirm)
        if (prefs.contains(key(name, "hash"))) {
            return AuthResult.Failure("That explorer name is taken", AuthField.Username)
        }

        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(key(name, "display"), name)
            .putString(key(name, "salt"), salt.encode())
            .putString(key(name, "hash"), hash(password, salt).encode())
            .putLong(key(name, "joined"), System.currentTimeMillis())
            .putString(KEY_SESSION, name.lowercase())
            .apply()
        return AuthResult.Success(loadPlayer(name)!!)
    }

    fun logIn(username: String, password: String): AuthResult {
        val name = username.trim()
        if (name.isEmpty()) return AuthResult.Failure("Enter your explorer name", AuthField.Username)
        if (password.isEmpty()) return AuthResult.Failure("Enter your password", AuthField.Password)

        val salt = prefs.getString(key(name, "salt"), null)?.decode()
        val stored = prefs.getString(key(name, "hash"), null)?.decode()
        if (salt == null || stored == null || !hash(password, salt).contentEquals(stored)) {
            return AuthResult.Failure("Wrong explorer name or password", AuthField.Password)
        }
        prefs.edit().putString(KEY_SESSION, name.lowercase()).apply()
        return AuthResult.Success(loadPlayer(name)!!)
    }

    fun signOut() {
        prefs.edit().remove(KEY_SESSION).apply()
    }

    /**
     * Records a finished hunt: the score counts toward the best score, the XP is banked,
     * every found object joins the collection, and each find counts toward the daily quest.
     */
    fun recordHunt(player: Player, score: Int, xpEarned: Int, found: List<GameObject>): Player {
        val name = player.username
        prefs.edit()
            .putInt(key(name, "hunts"), player.huntsPlayed + 1)
            .putInt(key(name, "best"), maxOf(player.bestScore, score))
            .putInt(key(name, "xp"), player.xp + xpEarned)
            .putStringSet(key(name, "cards"), player.cards + found.map { it.name })
            .putInt(key(name, "finds.${today()}"), player.findsToday + found.size)
            .apply()
        return loadPlayer(name) ?: player
    }

    /** Adds a card. A card is worth its points in XP, but only the first time it's found. */
    fun collect(player: Player, obj: GameObject): Player {
        val name = player.username
        val isNew = obj.name !in player.cards
        prefs.edit()
            .putStringSet(key(name, "cards"), player.cards + obj.name)
            .putInt(key(name, "xp"), player.xp + if (isNew) obj.points else 0)
            .putInt(key(name, "finds.${today()}"), player.findsToday + 1)
            .apply()
        return loadPlayer(name) ?: player
    }

    fun claimQuest(player: Player, rewardXp: Int): Player {
        if (player.questClaimedToday) return player
        val name = player.username
        prefs.edit()
            .putInt(key(name, "xp"), player.xp + rewardXp)
            .putBoolean(key(name, "quest.${today()}"), true)
            .apply()
        return loadPlayer(name) ?: player
    }

    private fun loadPlayer(username: String): Player? {
        val display = prefs.getString(key(username, "display"), null) ?: return null
        return Player(
            username = display,
            joinedAtMillis = prefs.getLong(key(username, "joined"), 0L),
            xp = prefs.getInt(key(username, "xp"), 0),
            huntsPlayed = prefs.getInt(key(username, "hunts"), 0),
            bestScore = prefs.getInt(key(username, "best"), 0),
            cards = prefs.getStringSet(key(username, "cards"), emptySet())!!.toSet(),
            findsToday = prefs.getInt(key(username, "finds.${today()}"), 0),
            questClaimedToday = prefs.getBoolean(key(username, "quest.${today()}"), false),
        )
    }

    private fun validateUsername(name: String): String? = when {
        name.length < 3 -> "Explorer name needs at least 3 characters"
        name.length > 20 -> "Explorer name can be at most 20 characters"
        !name.all { it.isLetterOrDigit() || it == '_' } -> "Use letters, numbers, and underscores only"
        else -> null
    }

    private fun key(username: String, field: String) = "user.${username.lowercase()}.$field"

    private fun hash(password: String, salt: ByteArray): ByteArray {
        // PBKDF2WithHmacSHA1 is the strongest PBKDF2 variant available on API 24.
        val spec = PBEKeySpec(password.toCharArray(), salt, 12_000, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
    }

    private fun ByteArray.encode() = Base64.encodeToString(this, Base64.NO_WRAP)
    private fun String.decode() = Base64.decode(this, Base64.NO_WRAP)

    private companion object {
        const val KEY_SESSION = "session.user"
        const val MIN_PASSWORD = 6
    }
}

/** Today's date as a number, used to key daily progress. */
fun today(): Int = Calendar.getInstance().let { it.get(Calendar.YEAR) * 1000 + it.get(Calendar.DAY_OF_YEAR) }
