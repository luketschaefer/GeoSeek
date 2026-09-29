package com.geoseek.catalog

import android.content.Context
import com.geoseek.domain.catalog.Catalog
import com.geoseek.domain.catalog.CatalogParser
import com.geoseek.domain.catalog.CatalogRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads `assets/catalog.json` once. Parsing is strict and throws CatalogException on bad data;
 * GeoSeekApplication forces the load at startup so a broken catalog crashes immediately with a
 * readable list of problems instead of failing later mid-hunt.
 */
@Singleton
class AssetCatalogRepository
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : CatalogRepository {
        private val catalog: Catalog by lazy {
            val text =
                context.assets
                    .open(ASSET_PATH)
                    .bufferedReader()
                    .use { it.readText() }
            CatalogParser().parse(text)
        }

        override fun catalog(): Catalog = catalog

        companion object {
            const val ASSET_PATH = "catalog.json"
        }
    }
