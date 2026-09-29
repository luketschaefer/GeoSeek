package com.geoseek.domain

import com.geoseek.domain.catalog.Catalog
import com.geoseek.domain.catalog.CatalogObject
import com.geoseek.domain.catalog.Environment
import com.geoseek.domain.catalog.ObjectId
import com.geoseek.domain.catalog.Rarity

fun testObject(
    id: String,
    rarity: Rarity = Rarity.COMMON,
    points: Int = 10,
    xp: Int = 5,
    environments: Set<Environment> = setOf(Environment.PARK),
    labels: Set<String> = setOf(id.replaceFirstChar { it.uppercase() }),
    minConfidence: Float = 0.7f,
) = CatalogObject(
    id = ObjectId(id),
    name = id.replaceFirstChar { it.uppercase() },
    rarity = rarity,
    points = points,
    xp = xp,
    environments = environments,
    acceptedLabels = labels,
    minConfidence = minConfidence,
)

/** Six objects per environment with a mix of rarities. */
val testCatalog: Catalog =
    Catalog(
        Environment.entries.flatMap { env ->
            Rarity.entries.mapIndexed { i, rarity ->
                testObject(
                    id = "${env.name.lowercase()}_$i",
                    rarity = rarity,
                    points = (i + 1) * 10,
                    xp = (i + 1) * 5,
                    environments = setOf(env),
                )
            } + testObject(id = "${env.name.lowercase()}_extra", environments = setOf(env))
        },
    )
