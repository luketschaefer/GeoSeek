package com.geoseek.ar

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ArModule {
    @Binds
    abstract fun bindArAvailabilityChecker(impl: ArCoreAvailabilityChecker): ArAvailabilityChecker
}
