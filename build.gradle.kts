plugins {
    id("com.android.application") version libs.versions.agp apply false
    id("com.android.library") version libs.versions.agp apply false
    id("org.jetbrains.kotlin.android") version libs.versions.kotlin apply false
    id("com.google.dagger.hilt.android") version libs.versions.daggerHilt apply false
    id("com.google.devtools.ksp") version libs.versions.ksp apply false
    alias(libs.plugins.compose.compiler) apply false
    id("com.google.gms.google-services") version "4.4.3" apply false
}