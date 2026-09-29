package com.geoseek.domain.catalog

/** Thrown when the bundled catalog cannot be used. The app treats this as fatal at startup. */
sealed class CatalogException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {
    /** The JSON could not be decoded at all (syntax error, wrong types, unknown keys, bad enum). */
    class Malformed(
        cause: Throwable,
    ) : CatalogException("Catalog JSON is malformed: ${cause.message}", cause)

    /** The JSON decoded but broke one or more content rules. All problems are listed at once. */
    class Invalid(
        val errors: List<String>,
    ) : CatalogException("Catalog is invalid (${errors.size} problem(s)):\n" + errors.joinToString("\n") { " - $it" })
}
