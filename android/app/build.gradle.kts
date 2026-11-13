import java.util.Properties
import java.util.regex.Pattern

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.openapi.generator)
    alias(libs.plugins.aboutlibraries.android)
    jacoco
    id("org.owasp.dependencycheck") version "13.0.0"
}

kotlin {
    jvmToolchain(17)
}

// Exported Room schemas, one JSON per version. Committed: migration
// tests build old versions from them.
room {
    schemaDirectory("$projectDir/schemas")
}

// Output of openApiGenerate below.
val generatedClientDir = layout.buildDirectory.dir("generated/openapi")

// Release signing credentials, from android/keystore.properties
// (gitignored, never committed): storeFile, storePassword, keyAlias,
// keyPassword. When the file is absent (CI, a fresh clone) the release
// config stays empty: debug builds are unaffected, and a release build
// fails at signing validation instead of producing an unsigned APK.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

// App version: bump appVersion before each release. versionCode is derived
// (X*1_000_000 + Y*1_000 + Z), never typed; Play needs it higher on every
// upload. CI passes -Pgymbuddy.releaseTag=vX.Y.Z, which must equal
// "v$appVersion" or the build fails (the tag only checks, never sets).
val appVersion = "0.1.0"

val appVersionCode: Int = run {
    val match = Regex("""(\d{1,3})\.(\d{1,3})\.(\d{1,3})""").matchEntire(appVersion)
        ?: throw GradleException("appVersion must look like 1.2.3, got '$appVersion'")
    val (major, minor, patch) = match.destructured.toList().map { it.toInt() }
    major * 1_000_000 + minor * 1_000 + patch
}

findProperty("gymbuddy.releaseTag")?.toString()?.let { tag ->
    if (tag != "v$appVersion") {
        throw GradleException("Tag $tag doesn't match appVersion $appVersion in app/build.gradle.kts")
    }
}

android {
    namespace = "com.rukavina.gymbuddy"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.rukavina.gymbuddy"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersion
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Crash reporting, read by the manifest. Off in debug builds.
        manifestPlaceholders["crashlyticsEnabled"] = "true"
    }

    signingConfigs {
        create("release") {
            keystoreProperties.getProperty("storeFile")?.let { storeFile = file(it) }
            storePassword = keystoreProperties.getProperty("storePassword")
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")
        }
    }

    // One flavor per backend environment (ADR 0016). Each has its own
    // Firebase project: src/<flavor>/google-services.json (gitignored).
    // "qa", not "test": AGP rejects flavor names starting with "test".
    // For a local backend on the emulator, either flavor:
    // -Pgymbuddy.apiBaseUrl=http://10.0.2.2:8080/
    flavorDimensions += "env"
    productFlavors {
        create("qa") {
            dimension = "env"
            // Installs next to the prod app. Name: src/qa/res/values.
            applicationIdSuffix = ".qa"
            buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl("https://api-test.rukavina.app/")}\"")
        }
        create("prod") {
            dimension = "env"
            buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl("https://api.rukavina.app/")}\"")
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["crashlyticsEnabled"] = "false"
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            ndk {
                debugSymbolLevel = "FULL"
            }
            // Mapping upload needs Firebase credentials; local and CI
            // builds skip it. Pass -Pgymbuddy.uploadMapping=true to upload.
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                mappingFileUploadEnabled = findProperty("gymbuddy.uploadMapping") == "true"
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

    // CI gate (Plan 8.1): existing issues live in the baseline, only
    // new errors fail. Regenerate with ./gradlew updateLintBaseline.
    lint {
        baseline = file("lint-baseline.xml")
        abortOnError = true
        checkReleaseBuilds = false
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                // Sync end-to-end tests talk to a real backend and skip
                // themselves unless this is set - see
                // app/src/test/.../data/sync/e2e/E2eBackend.kt.
                it.systemProperty("gymbuddy.e2e.baseUrl", findProperty("e2eBaseUrl")?.toString() ?: "")
                // TokenParityTest reads the committed Figma tokens.
                it.systemProperty("gymbuddy.tokensJson", rootDir.resolve("../design/tokens.json").absolutePath)
                // IconParityTest checks design/icons + brand against res/drawable.
                it.systemProperty("gymbuddy.designDir", rootDir.resolve("../design").absolutePath)
                // Robolectric loads app classes through its own class
                // loader, which JaCoCo skips by default: every Robolectric
                // test then reports 0% coverage for the code it runs.
                it.extensions.configure<JacocoTaskExtension> {
                    isIncludeNoLocationClasses = true
                    excludes = listOf("jdk.internal.*")
                }
            }
        }
    }

    sourceSets {
        getByName("main") {
            kotlin.directories.add(generatedClientDir.get().dir("src/main/kotlin").asFile.path)
        }
        // MigrationTestHelper reads the exported schemas as assets.
        // Robolectric only sees the merged debug assets (test source set
        // assets are ignored), so they ship in debug APKs, not release.
        getByName("debug") {
            assets.directories.add("$projectDir/schemas")
        }
    }
}

// Prints the version without building or signing; with -Pgymbuddy.releaseTag
// it also checks the tag matches.
tasks.register("printReleaseVersion") {
    group = "release"
    description = "Prints versionName and versionCode (from appVersion)."
    val name = android.defaultConfig.versionName
    val code = android.defaultConfig.versionCode
    doLast { println("$name $code") }
}

fun apiBaseUrl(default: String): String = findProperty("gymbuddy.apiBaseUrl")?.toString() ?: default

// The Retrofit/Moshi client, generated from the committed spec at build
// time and never checked in. Same generator version (libs.versions.toml)
// and same options file as api/package.json's generate:kotlin, which CI
// compiles as a contract tripwire - so the app compiles exactly what CI
// verified.
openApiGenerate {
    generatorName.set("kotlin")
    inputSpec.set("$rootDir/../api/openapi.yaml")
    configFile.set("$rootDir/../api/kotlin-client-config.yaml")
    outputDir.set(generatedClientDir.get().asFile.path)
    generateApiTests.set(false)
    generateModelTests.set(false)
    generateApiDocumentation.set(false)
    generateModelDocumentation.set(false)
}

tasks.named("preBuild") {
    dependsOn("openApiGenerate")
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
    implementation(libs.firebase.crashlytics)

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

    // Sync: generated API client and its runtime
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.retrofit.converter.scalars)
    implementation(libs.moshi.kotlin)
    implementation(libs.moshi.adapters)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler)
    implementation(libs.lifecycle.process)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.room.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.work.testing)
    testImplementation(libs.lifecycle.runtime.testing)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(platform(libs.androidxComposeBom))
    testImplementation(libs.ui.test.junit)
    androidTestImplementation(libs.testExtJunit)
    androidTestImplementation(libs.espressoCore)
    androidTestImplementation(platform(libs.androidxComposeBom))
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
    "**/ui/design/**",
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
    "**/NavRoutes*.*",
    // openapi-generator output - verified by compiling, not by tests
    "**/data/remote/generated/**"
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
    dependsOn("testQaDebugUnitTest")
    group = "verification"
    description = "Generates JaCoCo XML (SonarQube) and HTML coverage reports from testQaDebugUnitTest."

    reports {
        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml"))
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/test/html"))
        csv.required.set(false)
    }

    classDirectories.setFrom(
        fileTree(layout.buildDirectory.dir("intermediates/javac/qaDebug/compileQaDebugJavaWithJavac/classes")) {
            exclude(jacocoExcludes)
        },
        fileTree(layout.buildDirectory.dir("intermediates/built_in_kotlinc/qaDebug/compileQaDebugKotlin/classes")) {
            exclude(jacocoExcludes)
        }
    )

    sourceDirectories.setFrom(files("src/main/java"))

    executionData.setFrom(
        fileTree(layout.buildDirectory.get()) {
            include("outputs/unit_test_code_coverage/qaDebugUnitTest/testQaDebugUnitTest.exec", "jacoco/testQaDebugUnitTest.exec")
        }
    )
}

// Plan 9 step 01 (ADR 0018): every runtime library with its licence,
// shown on the in-app licences page. The build fails on any licence
// outside the owner's list (CLAUDE.md); Google's SDK terms are allowed
// only for Google's own SDKs.
aboutLibraries {
    license {
        strictMode = com.mikepenz.aboutlibraries.plugin.StrictMode.FAIL
        allowedLicenses.addAll(
            "Apache-2.0", "MIT", "BSD-2-Clause", "BSD-3-Clause", "ISC",
            "OFL-1.1", "CC0-1.0", "CC-BY-4.0", "Unlicense",
            // The same licences with the project's own copyright line
            // (config/aboutlibraries/licenses), since MIT, BSD and OFL
            // must ship with it and the SPDX texts are blank templates.
            "MIT-calendar", "MIT-fluent-emoji", "BSD-3-Clause-protobuf", "OFL-1.1-figtree",
        )
        val googleSdks = listOf(
            "com.google.android.gms",
            "com.google.android.libraries.identity.googleid",
            "com.google.android.recaptcha",
            "com.google.android.play",
            "com.google.firebase",
        )
        allowedLicensesMap = mapOf(
            "ASDKL" to googleSdks, // Android SDK License
            "PCSDKToS" to googleSdks, // Play Core SDK Terms
            "7546fd760989f8b6ad8de53f6c981adb" to googleSdks, // Play Integrity API Terms
        )
    }
    collect {
        configPath = file("$rootDir/config/aboutlibraries")
    }
    library {
        // Shown instead through config/aboutlibraries/libraries entries
        // that carry the project's own licence text (copyright line).
        // Exact artifact names only, so nothing else can drop out.
        exclusionPatterns.addAll(
            Pattern.compile("com\\.kizitonwose\\.calendar:(compose|core|data)"),
            Pattern.compile("androidx\\.datastore:datastore-preferences-external-protobuf"),
        )
    }
    export {
        prettyPrint = true
        excludeFields.addAll("developers", "funding")
    }
}
