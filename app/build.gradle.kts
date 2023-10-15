plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.compose")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.rukavina.gymbuddy"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.rukavina.gymbuddy"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.0.1-SNAPSHOT"
        compileSdkPreview = "UpsideDownCakePrivacySandbox"
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
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidxCoreKtx)
    implementation(libs.lifecycleKtx)
    implementation(libs.activity.compose)
    implementation(platform(libs.androidxComposeBom))
    implementation(libs.composeUi)
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation(libs.daggerHilt)
    kapt(libs.daggerHiltCompiler)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)

    implementation ("androidx.navigation:navigation-compose:2.7.4")
    implementation ("androidx.activity:activity-compose:1.8.0")

    implementation(libs.composeUi)
    implementation(libs.androidxMaterial)
    implementation(libs.composeMaterial)
    implementation(libs.composeLiveData)
    implementation(libs.navigation.compose)
    implementation("androidx.compose.material3:material3")
    implementation (libs.material.icons.extended)
    implementation(libs.fragment.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.testExtJunit)
    androidTestImplementation(libs.espressoCore)
    androidTestImplementation(platform(libs.androidxComposeBom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

kapt {
    correctErrorTypes = true
}