package com.geoseek.social

import com.geoseek.domain.social.ProfileRemoteRepository
import com.geoseek.domain.social.TradeRepository
import com.geoseek.social.data.InMemoryProfileRemoteRepository
import com.geoseek.social.data.InMemoryTradeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Swap these bindings for Firestore implementations when the backend lands. */
@Module
@InstallIn(SingletonComponent::class)
abstract class SocialModule {
    @Binds
    abstract fun bindTradeRepository(impl: InMemoryTradeRepository): TradeRepository

    @Binds
    abstract fun bindProfileRemoteRepository(impl: InMemoryProfileRemoteRepository): ProfileRemoteRepository
}
