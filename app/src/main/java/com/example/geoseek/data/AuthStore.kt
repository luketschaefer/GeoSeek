// On-device accounts and the remembered session. Progress lives in ProgressStore.
// A player stays signed in until they sign out. Passwords are stored only as salted PBKDF2 hashes.
// This is local-only until a real backend exists.
package com.example.geoseek.data

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

sealed interface AuthResult {
    data class Success(val player: Player) : AuthResult
    data class Failure(val message: String, val field: AuthField? = null) : AuthResult
}

enum class AuthField { Username, Password, Confirm }

class AuthStore internal constructor(private val store: PlayerPrefs) {

    constructor(context: Context) : this(PlayerPrefs.from(context))

    private val prefs get() = store.prefs
    private fun key(username: String, field: String) = store.key(username, field)

    /** The remembered player, or null if nobody is signed in. */
    fun currentPlayer(): Player? = prefs.getString(KEY_SESSION, null)?.let(store::load)

    fun signUp(username: String, password: String, confirm: String): AuthResult {
        val name = username.trim()
        validateUsername(name)?.let { return AuthResult.Failure(it, AuthField.Username) }
        if (password.length < MIN_PASSWORD) {
            return AuthResult.Failure("Password needs at least $MIN_PASSWORD characters", AuthField.Password)
        }
        if (password != confirm) return AuthResult.Failure("Passwords don't match", AuthField.Confirm)
        if (store.exists(name)) {
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
        return AuthResult.Success(store.load(name)!!)
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
        return AuthResult.Success(store.load(name)!!)
    }

    fun signOut() {
        prefs.edit().remove(KEY_SESSION).apply()
    }

    private fun validateUsername(name: String): String? = when {
        name.length < 3 -> "Explorer name needs at least 3 characters"
        name.length > 20 -> "Explorer name can be at most 20 characters"
        !name.all { it.isLetterOrDigit() || it == '_' } -> "Use letters, numbers, and underscores only"
        else -> null
    }

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
