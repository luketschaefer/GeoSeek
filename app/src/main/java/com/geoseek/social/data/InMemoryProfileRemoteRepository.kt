package com.geoseek.social.data

import com.geoseek.domain.social.ProfileRemoteRepository
import com.geoseek.domain.social.RemoteProfile
import com.geoseek.domain.social.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** Offline stand-in. Nobody is signed in until Firebase Auth replaces this. */
@Singleton
class InMemoryProfileRemoteRepository
    @Inject
    constructor() : ProfileRemoteRepository {
        private val signedIn = MutableStateFlow<UserId?>(null)
        private val profiles = MutableStateFlow<Map<UserId, RemoteProfile>>(emptyMap())
        private val friendships = MutableStateFlow<Map<UserId, Set<UserId>>>(emptyMap())

        override fun currentUser(): Flow<UserId?> = signedIn.asStateFlow()

        override fun observeProfile(user: UserId): Flow<RemoteProfile?> = profiles.map { it[user] }

        override suspend fun publish(profile: RemoteProfile) {
            profiles.update { it + (profile.userId to profile) }
        }

        override fun observeFriends(user: UserId): Flow<List<RemoteProfile>> = combineFriends(user)

        private fun combineFriends(user: UserId): Flow<List<RemoteProfile>> =
            combine(
                friendships,
                profiles,
            ) { friends, all -> friends[user].orEmpty().mapNotNull { all[it] }.sortedBy { it.displayName } }

        /** Test/dev hook: simulate sign-in. */
        fun signInAs(user: UserId?) {
            signedIn.value = user
        }

        /** Test/dev hook: make two users friends (symmetric). */
        fun addFriendship(
            a: UserId,
            b: UserId,
        ) {
            friendships.update { it + (a to (it[a].orEmpty() + b)) + (b to (it[b].orEmpty() + a)) }
        }
    }
