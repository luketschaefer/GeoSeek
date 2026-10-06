package com.example.geoseek.detection

/**
 * Sanitizes ML Kit frame labels by suppressing abstract and non-object generic tags.
 */
class LabelSanitizer {
    private val suppressedLabels = setOf(
        "product", "material", "line", "font", "rectangle", "parallel", "graphics",
        "technology", "electronic device", "indoor", "outdoor", "logo", "brand",
        "snapshot", "sleeve", "component", "design", "pattern", "circle", "triangle",
        "wood", "plastic", "metal", "paper", "text", "symbol", "number", "shade",
    )

    /**
     * Checks if a label string is a meaningful object label rather than a generic tag.
     */
    fun isMeaningfulLabel(labelText: String): Boolean {
        val normalized = labelText.trim().lowercase()
        return (normalized !in suppressedLabels) && (normalized.length > 2)
    }

    /**
     * Extracts the best user-facing label string from a list of raw frame labels.
     * Prefers higher-confidence meaningful object labels.
     */
    fun extractVisibleLabel(labels: List<RawLabel>): String? {
        return labels
            .filter { (text, _) -> isMeaningfulLabel(text) }
            .maxByOrNull { (_, confidence) -> confidence }
            ?.text
    }
}
