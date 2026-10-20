package com.example.geoseek.data

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Full 2 x 2 x 2 combinatorial coverage of AuthStore.signUp.
 *
 * Username is always valid ("Explorer"). Independent factors are:
 * password length {valid, too short}, confirmation {matches, differs},
 * and username availability {available, taken}.
 *
 * Each case uses isolated local Android preferences, never phone account data.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class AuthStoreSignUpTest {
    private lateinit var application: Application
    private lateinit var preferences: SharedPreferences
    private lateinit var store: AuthStore

    /** Starts each case with empty local account storage. */
    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication()
        preferences = application.getSharedPreferences("geoseek_auth", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        store = AuthStore(application)
    }

    /** Clears test accounts after each case, including failed assertions. */
    @After
    fun tearDown() {
        if (::preferences.isInitialized) preferences.edit().clear().commit()
    }

    /** C1: valid password x matching confirmation x available username succeeds. */
    @Test
    fun C1ValidPasswordMatchingConfirmationAvailableUsername() {
        assertSignUpCase("secret", "secret", usernameTaken = false, expectedFailure = null)
    }

    /** C2: valid password x matching confirmation x taken username reports duplication. */
    @Test
    fun C2ValidPasswordMatchingConfirmationTakenUsername() {
        assertSignUpCase("secret", "secret", usernameTaken = true,
            expectedFailure = AuthResult.Failure("That explorer name is taken", AuthField.Username))
    }

    /** C3: valid password x mismatching confirmation x available username reports confirmation. */
    @Test
    fun C3ValidPasswordDifferentConfirmationAvailableUsername() {
        assertSignUpCase("secret", "different", usernameTaken = false,
            expectedFailure = AuthResult.Failure("Passwords don't match", AuthField.Confirm))
    }

    /** C4: confirmation is checked before username duplication when both are invalid. */
    @Test
    fun C4ValidPasswordDifferentConfirmationTakenUsername() {
        assertSignUpCase("secret", "different", usernameTaken = true,
            expectedFailure = AuthResult.Failure("Passwords don't match", AuthField.Confirm))
    }

    /** C5: short password x matching confirmation x available username reports password length. */
    @Test
    fun C5ShortPasswordMatchingConfirmationAvailableUsername() {
        assertSignUpCase("short", "short", usernameTaken = false,
            expectedFailure = AuthResult.Failure("Password needs at least 6 characters", AuthField.Password))
    }

    /** C6: password length is checked before username duplication. */
    @Test
    fun C6ShortPasswordMatchingConfirmationTakenUsername() {
        assertSignUpCase("short", "short", usernameTaken = true,
            expectedFailure = AuthResult.Failure("Password needs at least 6 characters", AuthField.Password))
    }

    /** C7: password length is checked before mismatching confirmation. */
    @Test
    fun C7ShortPasswordDifferentConfirmationAvailableUsername() {
        assertSignUpCase("short", "different", usernameTaken = false,
            expectedFailure = AuthResult.Failure("Password needs at least 6 characters", AuthField.Password))
    }

    /** C8: password length takes priority when all three factors are unfavorable. */
    @Test
    fun C8ShortPasswordDifferentConfirmationTakenUsername() {
        assertSignUpCase("short", "different", usernameTaken = true,
            expectedFailure = AuthResult.Failure("Password needs at least 6 characters", AuthField.Password))
    }

    /**
     * Sets up username availability, invokes the real sign-up method, and checks its effects.
     * Failures must preserve account storage and leave the session signed out.
     * Success must persist the returned player as the current session.
     */
    private fun assertSignUpCase(
        password: String,
        confirmation: String,
        usernameTaken: Boolean,
        expectedFailure: AuthResult.Failure?,
    ) {
        if (usernameTaken) {
            assertTrue(store.signUp("Explorer", "original1", "original1") is AuthResult.Success)
            store.signOut()
        }
        assertNull(store.currentPlayer())
        val before = preferences.all.toMap()

        val result = store.signUp("Explorer", password, confirmation)

        if (expectedFailure == null) {
            assertTrue("Expected successful registration but received $result", result is AuthResult.Success)
            val player = (result as AuthResult.Success).player
            assertEquals("Explorer", player.username)
            assertEquals(player, AuthStore(application).currentPlayer())
        } else {
            assertEquals(expectedFailure, result)
            assertEquals("Rejected registration must not modify storage", before, preferences.all)
            assertNull(store.currentPlayer())
        }
    }
}
