package com.geoseek.domain.catalog

/**
 * Hunt environments. Adding one requires catalog objects for it (the parser enforces a
 * minimum pool size) and a display name/icon in the app's environment picker.
 */
enum class Environment {
    PARK,
    KITCHEN,
    STREET,
    CAMPUS,
}
