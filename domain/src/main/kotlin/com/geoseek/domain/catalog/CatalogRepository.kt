package com.geoseek.domain.catalog

fun interface CatalogRepository {
    /** Returns the validated catalog, loading it on first access. Throws [CatalogException] on bad data. */
    fun catalog(): Catalog
}
