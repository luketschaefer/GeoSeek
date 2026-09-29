package com.geoseek.profile

import com.geoseek.domain.profile.ProfileRepository
import com.geoseek.profile.data.RoomProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileModule {
    @Binds
    abstract fun bindProfileRepository(impl: RoomProfileRepository): ProfileRepository
}
