package com.geoseek.domain.catalog

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Decodes and validates catalog JSON. Decoding is strict (unknown keys fail) so that a typo
 * such as `"minConfidense"` is caught instead of silently falling back to a default.
 */
class CatalogParser(
    private val minObjectsPerEnvironment: Int = DEFAULT_MIN_OBJECTS_PER_ENVIRONMENT,
    /** When non-null, every accepted label must be in this set (the detector's label map). */
    private val knownLabels: Set<String>? = null,
) {
    private val json = Json { ignoreUnknownKeys = false }

    fun parse(text: String): Catalog {
        val dto = decode(text)
        val errors = validate(dto)
        if (errors.isNotEmpty()) throw CatalogException.Invalid(errors)
        return Catalog(dto.objects.map { it.toDomain() })
    }

    private fun decode(text: String): CatalogDto =
        try {
            json.decodeFromString<CatalogDto>(text)
        } catch (e: SerializationException) {
            throw CatalogException.Malformed(e)
        } catch (e: IllegalArgumentException) {
            throw CatalogException.Malformed(e)
        }

    private fun validate(dto: CatalogDto): List<String> =
        buildList {
            if (dto.version !=
                SUPPORTED_VERSION
            ) {
                add("Unsupported catalog version ${dto.version}; expected $SUPPORTED_VERSION")
            }
            if (dto.objects.isEmpty()) add("Catalog has no objects")

            dto.objects
                .groupingBy { it.id }
                .eachCount()
                .filterValues { it > 1 }
                .keys
                .forEach { add("Duplicate id '$it'") }

            dto.objects.forEachIndexed { index, obj -> addAll(validateObject(index, obj)) }

            if (dto.objects.isNotEmpty()) {
                Environment.entries.forEach { env ->
                    val count = dto.objects.count { env in it.environments }
                    if (count < minObjectsPerEnvironment) {
                        add("Environment $env has $count object(s); at least $minObjectsPerEnvironment required")
                    }
                }
            }
        }

    private fun validateObject(
        index: Int,
        obj: CatalogObjectDto,
    ): List<String> =
        buildList {
            val where = "objects[$index] ('${obj.id}')"
            if (!ID_PATTERN.matches(obj.id)) add("$where: id must match ${ID_PATTERN.pattern}")
            if (obj.name.isBlank()) add("$where: name is blank")
            if (obj.points <= 0) add("$where: points must be > 0, was ${obj.points}")
            if (obj.xp <= 0) add("$where: xp must be > 0, was ${obj.xp}")
            if (obj.environments.isEmpty()) add("$where: environments is empty")
            if (obj.environments.size != obj.environments.toSet().size) add("$where: environments has duplicates")
            if (obj.labels.isEmpty()) add("$where: labels is empty")
            if (obj.labels.any { it.isBlank() }) add("$where: labels contains a blank entry")
            if (obj.minConfidence <= 0f || obj.minConfidence > 1f) {
                add("$where: minConfidence must be in (0, 1], was ${obj.minConfidence}")
            }
            knownLabels?.let { known ->
                obj.labels.filter { it.isNotBlank() && it !in known }.forEach {
                    add("$where: label '$it' is not in the detector's label map")
                }
            }
        }

    companion object {
        const val SUPPORTED_VERSION = 1
        const val DEFAULT_MIN_OBJECTS_PER_ENVIRONMENT = 5
        private val ID_PATTERN = Regex("[a-z][a-z0-9_]*")
    }
}

@Serializable
private data class CatalogDto(
    val version: Int,
    val objects: List<CatalogObjectDto>,
)

@Serializable
private data class CatalogObjectDto(
    val id: String,
    val name: String,
    val rarity: Rarity,
    val points: Int,
    val xp: Int,
    val environments: List<Environment>,
    val labels: List<String>,
    val minConfidence: Float,
) {
    fun toDomain() =
        CatalogObject(
            id = ObjectId(id),
            name = name,
            rarity = rarity,
            points = points,
            xp = xp,
            environments = environments.toSet(),
            acceptedLabels = labels.toSet(),
            minConfidence = minConfidence,
        )
}
