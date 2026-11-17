plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "gymbuddy-backend"

// Dependabot floors for transitive and plugin libraries.
apply(from = "gradle/security-pins.settings.gradle.kts")
