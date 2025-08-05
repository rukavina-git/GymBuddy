import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.compose.compiler)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.rukavina.gymbuddy"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.rukavina.gymbuddy"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 3
        versionName = "0.0.2-SNAPSHOT"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidxCoreKtx)
    implementation(libs.lifecycleKtx)
    implementation(libs.activity.compose)
    implementation(platform(libs.androidxComposeBom))
    implementation(libs.composeUi)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.daggerHilt)
    kapt(libs.daggerHiltCompiler)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.navigation.compose)
    implementation(libs.activity.compose)
    implementation(libs.composeUi)
    implementation(libs.androidxMaterial)
    implementation(libs.composeMaterial)
    implementation(libs.composeLiveData)
    implementation(libs.navigation.compose)
    implementation(libs.composeMaterial)
    implementation(libs.material3)
    implementation(libs.material.icons.extended)
    implementation(libs.fragment.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.testExtJunit)
    androidTestImplementation(libs.espressoCore)
    androidTestImplementation(platform(libs.androidxComposeBom))
    androidTestImplementation(libs.ui.test.junit)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}

kapt {
    correctErrorTypes = true
}