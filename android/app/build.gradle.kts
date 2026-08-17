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

// OWASP dependency-check, applied here (not at the android/ root) because
// this is the only module with a real resolved dependency graph - the
// root project just declares plugin versions. Run via
// `./gradlew :app:dependencyCheckAnalyze`.
//
// This replaces a previous setup that ran the standalone dependency-check
// CLI action against the checked-out source tree, which found jar/aar
// files nowhere near a Gradle project's source layout and reported
// "Dependencies Scanned: 1". This plugin runs inside the build and reads
// the actual resolved configurations, so it sees the real graph -
// hundreds of dependencies, not one.
dependencyCheck {
    // autoUpdate defaults to true - left unset rather than forced, so the
    // NVD sync actually happens instead of silently scanning against
    // whatever is (or isn't) in data.directory. dependency-check-core's
    // own freshness check (nvd.validForHours, default 4h) already skips
    // redundant re-downloads on a warm cache, so there's no need to gate
    // this on a CI cache hit as well - see _owasp.yml.
    formats = listOf("HTML", "SARIF")
    // Module-root-relative (rootDir here is android/, the root project
    // dir, not app/) so CI can cache exactly this path across runs - see
    // _owasp.yml's "Cache NVD database" step, which must agree with this.
    data.directory = "$rootDir/dependency-check-data"
    // Read from an env var (set by _owasp.yml from the NVD_API_KEY
    // secret) rather than a Gradle property, so a local `./gradlew
    // dependencyCheckAnalyze` picks up a key from the shell too.
    //
    // Only assign when a real key exists: nvd.apiKey defaults to an empty
    // string, not null, so assigning `null` here wouldn't clear it back
    // to "no key" - it would leave the empty-string default in place.
    // That distinction turns out not to matter in practice, though:
    // verified locally (no key vs. a well-formed-but-fake key) that
    // dependency-check-gradle 13.0.0 does NOT fall back to unauthenticated
    // NVD access when no key is configured - it fails immediately with
    // "Invalid API Key, length of 0", vs. a real rejection message
    // ("Invalid API Key: 0000-****-0000") once a key is actually present.
    // An upstream issue independently confirms the unauthenticated path is
    // effectively non-functional at this version too - it doesn't fail
    // fast, it grinds through NIST's rate limit until the download breaks
    // partway through (github.com/dependency-check/DependencyCheck#8298).
    // Bottom line: NVD_API_KEY is not an optional speed optimization for
    // this workflow, it is required for it to complete at all.
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
