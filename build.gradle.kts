import dev.detekt.gradle.Detekt

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.lint) apply false
    // Declaring KGP here pins the Kotlin version that AGP's built-in Kotlin support uses.
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.spotless)
}

val ktlintVersion = libs.versions.ktlint.get()
val composeRules = "io.nlopez.compose.rules:ktlint:${libs.versions.composeRules.get()}"

spotless {
    // Explicit source roots: a root-level "**/*.kt" glob walks build/ dirs while other tasks write them.
    kotlin {
        target("app/src/**/*.kt", "domain/src/**/*.kt")
        ktlint(ktlintVersion).customRuleSets(listOf(composeRules))
    }
    kotlinGradle {
        target("*.gradle.kts", "app/*.gradle.kts", "domain/*.gradle.kts")
        ktlint(ktlintVersion)
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("config/detekt/detekt.yml"))
    source.setFrom(
        files(
            "app/src/main/java",
            "app/src/test/java",
            "domain/src/main/kotlin",
            "domain/src/test/kotlin",
        ),
    )
    parallel = true
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = "17"
    reports {
        html.required.set(true)
        sarif.required.set(true)
    }
}
