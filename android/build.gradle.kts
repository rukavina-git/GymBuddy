// OWASP dependency-check needs commons-lang3 3.18+ (its `Strings`
// class). AGP pulls 3.16 onto this root classpath; with the plugin only
// in :app, the two versions ended up in different classloaders and
// analysis died with IllegalAccessError. Loading the plugin here and
// forcing the version keeps one copy for everything.
buildscript {
    configurations.getByName("classpath") {
        resolutionStrategy {
            force("org.apache.commons:commons-lang3:3.20.0")
        }
    }
}

plugins {
    id("org.owasp.dependencycheck") version "13.0.0" apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}