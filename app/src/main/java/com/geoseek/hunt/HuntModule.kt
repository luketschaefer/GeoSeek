package com.geoseek.hunt

import com.geoseek.domain.history.RoundHistoryRepository
import com.geoseek.hunt.data.RoomRoundHistoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class HuntModule {
    @Binds
    abstract fun bindRoundHistoryRepository(impl: RoomRoundHistoryRepository): RoundHistoryRepository
}
