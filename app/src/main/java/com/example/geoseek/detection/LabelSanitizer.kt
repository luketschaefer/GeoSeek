package com.example.geoseek.detection

/**
 * Filters out abstract, generic, or non-object vision labels produced by ML Kit.
 *
 * ML Kit's base Image Labeling model output (`com.google.mlkit.vision.label`) includes structural tags
 * (such as *"Product"*, *"Material"*, *"Rectangle"*, *"Line"*, *"Font"*). This component sanitizes frame labels
 * so that live UI feedback (`"Camera sees: ..."`) only displays clean, real-world object descriptors.
 */
class LabelSanitizer {
    private val suppressedLabels = setOf(
        "product", "material", "line", "font", "rectangle", "parallel", "graphics",
        "technology", "electronic device", "indoor", "outdoor", "logo", "brand",
        "snapshot", "sleeve", "component", "design", "pattern", "circle", "triangle",
        "wood", "plastic", "metal", "paper", "text", "symbol", "number", "shade",
    )

    /**
     * Determines whether a vision label string represents a meaningful object.
     *
     * @param labelText Raw label text string.
     * @return True if the label is a valid object descriptor and not in the suppression list.
     */
    fun isMeaningfulLabel(labelText: String): Boolean {
        val normalized = labelText.trim().lowercase()
        return (normalized !in suppressedLabels) && (normalized.length > 2)
    }

    /**
     * Extracts the highest-confidence meaningful object label from a list of raw frame labels.
     *
     * @param labels List of [RawLabel]s produced by the vision analyzer.
     * @return Best user-facing label string, or null if no meaningful object label is present.
     */
    fun extractVisibleLabel(labels: List<RawLabel>): String? {
        return labels
            .filter { (text, _) -> isMeaningfulLabel(text) }
            .maxByOrNull { (_, confidence) -> confidence }
            ?.text
    }
}
