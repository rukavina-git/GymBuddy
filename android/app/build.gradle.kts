plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.services)
    jacoco
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
    // Robolectric provides a Context for the ViewModel/repository tests
    // that need one - Room's in-memory builder and AppPreferencesRepository's
    // DataStore both require a real Context, not something fakeable without
    // a mocking framework or a production-code refactor. See
    // src/test/resources/robolectric.properties for the pinned SDK level.
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.testExtJunit)
    androidTestImplementation(libs.espressoCore)
    androidTestImplementation(libs.ui.test.junit)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}

// JaCoCo coverage reporting for testDebugUnitTest. Instrumented-test
// coverage (androidTest) is not wired up yet - unit tests only for now.
//
// toolVersion is left unset deliberately: Gradle 9.7's bundled default
// already supports Java 17 class files (this module targets 17 via
// jvmToolchain), and pinning a guessed version risks a dependency that
// doesn't resolve. Bump explicitly if a specific version is ever needed.
//
// Class directories point at AGP 9's built-in Kotlin compilation output
// (build/intermediates/built_in_kotlinc/<variant>/compile<Variant>Kotlin/classes),
// not the classic build/tmp/kotlin-classes/<variant> path from the
// standalone Kotlin Gradle plugin - this project applies no
// org.jetbrains.kotlin.android plugin, so that classic path is never
// produced here. This is an AGP-version-specific intermediate layout,
// not a stable public API, and may move again on a future AGP upgrade.
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
    // Room type converters - one-liners asserting an enum has a name;
    // seeders - proven correct on every fresh install, not by a unit test;
    // navigation route constants - a plain `object` of string literals,
    // nothing to assert beyond "the string is the string". None of these
    // are unit-tested by design, same rationale as the Compose UI excludes
    // above. Kept in lockstep with
    // .github/actions/coverage-exclusions/action.yml's android_patterns -
    // see that file for why this matters.
    "**/data/local/converter/**",
    "**/data/local/seeder/**",
    "**/NavRoutes*.*"
)

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
