package com.geoseek

import android.app.Application
import com.geoseek.domain.catalog.CatalogRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GeoSeekApplication : Application() {
    @Inject
    lateinit var catalogRepository: CatalogRepository

    override fun onCreate() {
        super.onCreate()
        // Fail fast: a bad catalog.json throws CatalogException here with every problem listed.
        catalogRepository.catalog()
    }
}
