import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.services)
    alias(libs.plugins.openapi.generator)
    jacoco
    id("org.owasp.dependencycheck") version "13.0.0"
}

kotlin {
    jvmToolchain(17)
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

    signingConfigs {
        create("release") {
            keystoreProperties.getProperty("storeFile")?.let { storeFile = file(it) }
            storePassword = keystoreProperties.getProperty("storePassword")
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")
        }
    }

    buildTypes {
        debug {
            // 10.0.2.2 is the emulator's alias for the host machine, where
            // the backend runs locally. Override with -Pgymbuddy.apiBaseUrl.
            buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl("http://10.0.2.2:8080/")}\"")
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl("https://api.gymbuddy.app/")}\"")
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
            all {
                // Sync end-to-end tests talk to a real backend and skip
                // themselves unless this is set - see
                // app/src/test/.../data/sync/e2e/E2eBackend.kt.
                it.systemProperty("gymbuddy.e2e.baseUrl", findProperty("e2eBaseUrl")?.toString() ?: "")
            }
        }
    }

    sourceSets {
        getByName("main") {
            kotlin.directories.add(generatedClientDir.get().dir("src/main/kotlin").asFile.path)
        }
    }
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
