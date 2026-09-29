package com.geoseek.catalog

import com.geoseek.domain.catalog.Catalog
import com.geoseek.domain.catalog.CatalogRepository
import com.geoseek.domain.hunt.RoundGenerator
import com.geoseek.domain.quests.DailyQuestSelector
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CatalogModule {
    @Binds
    abstract fun bindCatalogRepository(impl: AssetCatalogRepository): CatalogRepository

    companion object {
        @Provides
        @Singleton
        fun provideCatalog(repository: CatalogRepository): Catalog = repository.catalog()

        @Provides
        fun provideRoundGenerator(catalog: Catalog): RoundGenerator = RoundGenerator(catalog)

        @Provides
        fun provideDailyQuestSelector(catalog: Catalog): DailyQuestSelector = DailyQuestSelector(catalog)
    }
}
