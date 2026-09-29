package com.geoseek.collector

import com.geoseek.collector.data.RoomCollectionRepository
import com.geoseek.domain.catalog.Catalog
import com.geoseek.domain.collector.AcquireCardUseCase
import com.geoseek.domain.collector.CollectionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

@Module
@InstallIn(SingletonComponent::class)
abstract class CollectorModule {
    @Binds
    abstract fun bindCollectionRepository(impl: RoomCollectionRepository): CollectionRepository

    companion object {
        @Provides
        fun provideAcquireCard(
            catalog: Catalog,
            repository: CollectionRepository,
            clock: Clock,
        ) = AcquireCardUseCase(catalog, repository, clock)
    }
}
