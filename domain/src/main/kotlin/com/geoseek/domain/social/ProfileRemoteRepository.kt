package com.geoseek.domain.social

import kotlinx.coroutines.flow.Flow

/**
 * Public profiles and friends. In-memory today; Firebase Auth (for [currentUser]) + Firestore
 * (`profiles/{uid}`, `profiles/{uid}/friends/{uid}`) later.
 */
interface ProfileRemoteRepository {
    /** Signed-in user, or null when signed out / offline-only. */
    fun currentUser(): Flow<UserId?>

    fun observeProfile(user: UserId): Flow<RemoteProfile?>

    suspend fun publish(profile: RemoteProfile)

    fun observeFriends(user: UserId): Flow<List<RemoteProfile>>
}
