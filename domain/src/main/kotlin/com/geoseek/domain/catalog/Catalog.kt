package com.geoseek.domain.catalog

/** Immutable, validated catalog. Objects are kept sorted by id so seeded selection is stable. */
class Catalog(
    objects: List<CatalogObject>,
) {
    val objects: List<CatalogObject> = objects.sortedBy { it.id.value }

    private val byId: Map<ObjectId, CatalogObject> = this.objects.associateBy { it.id }

    operator fun get(id: ObjectId): CatalogObject? = byId[id]

    fun require(id: ObjectId): CatalogObject = byId[id] ?: throw NoSuchElementException("Unknown catalog object '$id'")

    fun objectsIn(environment: Environment): List<CatalogObject> = objects.filter { environment in it.environments }
}
