plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.services)
    jacoco
    id("org.owasp.dependencycheck") version "13.0.0"
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "com.rukavina.gymbuddy"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.rukavina.gymbuddy"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 5
        versionName = "0.0.2-SNAPSHOT"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            ndk {
                debugSymbolLevel = "FULL"
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.androidxCoreKtx)
    implementation(libs.lifecycleKtx)
    implementation(libs.activity.compose)

    // Compose
    implementation(platform(libs.androidxComposeBom))
    implementation(libs.composeUi)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.composeMaterial)
    implementation(libs.composeLiveData)

    // Material
    implementation(libs.androidxMaterial)
    implementation(libs.androidx.material3)
    implementation(libs.material3)
    implementation(libs.material.icons.extended)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.daggerHilt)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.daggerHiltCompiler)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)

    // Google Sign-In (Credential Manager)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Other
    implementation(libs.coil.compose)
    implementation(libs.coil.okhttp)
    implementation(libs.datastore.preferences)
    implementation(libs.kizitonwose.calendar.compose)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.room.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.testExtJunit)
    androidTestImplementation(libs.espressoCore)
    androidTestImplementation(libs.ui.test.junit)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}

val jacocoExcludes = listOf(
    // Android build-generated
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    // Hilt/Dagger generated
    "**/Hilt_*.class",
    "**/Hilt_*$*.class",
    "**/*_Factory.class",
    "**/*_Factory$*.class",
    "**/*_MembersInjector.class",
    "**/*_Impl.class",
    "**/*_Impl$*.class",
    "**/*Module*.class",
    "**/*Module*$*.class",
    // Data binding
    "**/databinding/**",
    "**/*Binding.class",
    "**/*Binding$*.class",
    "**/BR.class",
    "**/ui/**/*Screen*.*",
    "**/ui/**/*Dialog*.*",
    "**/ui/**/*Card*.*",
    "**/ui/**/*BottomSheet*.*",
    "**/ui/components/**",
    "**/*PasswordTextField*.*",
    "**/*ProfileTextField*.*",
    "**/*SettingsComponents*.*",
    "**/*TemplateExerciseListItem*.*",
    "**/*ExerciseFilterContent*.*",
    "**/*ExerciseFormAdvanced*.*",
    "**/*ExerciseFormDetails*.*",
    "**/*ExerciseFormRequiredFields*.*",
    "**/ui/theme/**",
    "**/*MainActivity*.*",
    "**/*Application*.*",
    "**/data/local/converter/**",
    "**/data/local/seeder/**",
    "**/NavRoutes*.*"
)

buildscript {
    configurations.getByName("classpath") {
        resolutionStrategy {
            force("org.apache.commons:commons-lang3:3.20.0")
        }
    }
}

dependencyCheck {
    formats = listOf("HTML", "SARIF")
    data.directory = "$rootDir/dependency-check-data"
    System.getenv("NVD_API_KEY")?.takeIf { it.isNotBlank() }?.let { nvd.apiKey = it }
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    group = "verification"
    description = "Generates JaCoCo XML (SonarQube) and HTML coverage reports from testDebugUnitTest."

    reports {
        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml"))
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/test/html"))
        csv.required.set(false)
    }

    classDirectories.setFrom(
        fileTree(layout.buildDirectory.dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes")) {
            exclude(jacocoExcludes)
        },
        fileTree(layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")) {
            exclude(jacocoExcludes)
        }
    )

    sourceDirectories.setFrom(files("src/main/java"))

    executionData.setFrom(
        fileTree(layout.buildDirectory.get()) {
            include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec", "jacoco/testDebugUnitTest.exec")
        }
    )
}
