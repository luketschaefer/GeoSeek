package com.geoseek.detection

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * Unscoped on purpose: each ViewModel gets its own detector and closes it in onCleared.
 * To try the FakeDetector on an emulator, temporarily bind it here (it is not a DI default).
 */
@Module
@InstallIn(ViewModelComponent::class)
abstract class DetectionModule {
    @Binds
    abstract fun bindObjectDetector(impl: MlKitLabelDetector): ObjectDetector
}
