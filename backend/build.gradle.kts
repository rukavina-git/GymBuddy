plugins {
    id("org.springframework.boot") version "4.1.2"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.4.21"
    kotlin("plugin.spring") version "2.4.21"
    kotlin("plugin.jpa") version "2.4.21"
    jacoco
    id("org.owasp.dependencycheck") version "13.0.0"
    // Licence report + gate (Apache 2.0), Plan 9 step 01, ADR 0018.
    id("com.github.jk1.dependency-license-report") version "3.1.5"
}

group = "com.rukavina"
version = "0.0.1-SNAPSHOT"

// JDK 21: this build is independent of android/, which targets 17.
// jvmToolchain makes the target explicit no matter which JDK launches
// Gradle itself.
kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

repositories {
    mavenCentral()
}

// Security pins on top of the Spring Boot BOM (dependency graph,
// GitHub Advisory DB, 2026-11-15). Drop each once the BOM or its
// owner (firebase-admin pulls both) ships the fixed version itself.
// GHSA-ccrj-gw8r-854m: OpenTelemetry API <= 1.66.0 (Boot 4.1.2 pins 1.62.0).
extra["opentelemetry.version"] = "1.68.0"
// Boot 4.1 BOM pins Kotlin 2.3.x; 2.4.20+ fixes CVE-2026-53914 (build
// cache deserialization). The kotlin plugins above set the same version.
extra["kotlin.version"] = "2.4.21"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    // Boot 4 moved Flyway auto-config into its own module.
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation(libs.springdoc.webmvc.ui)
    implementation(libs.firebase.admin)
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    // Parses the committed api/openapi.yaml for OpenApiSpecDriftTest.
    // Version comes from the Spring Boot BOM (io.spring.dependency-management),
    // same as jackson-module-kotlin above - no need to pin one here.
    testImplementation("tools.jackson.dataformat:jackson-dataformat-yaml")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    constraints {
        implementation("com.google.guava:guava:33.8.0-jre") {
            because("GHSA-xxph-c9ww-hj94: Guava <= 33.7.1 (via firebase-admin)")
        }
        implementation("org.webjars:swagger-ui:5.35.0") {
            because("springdoc 3.2.0 ships 5.34.0, whose DOMPurify has CVE-2026-116802..4")
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    // Kept from Testcontainers 1.21 (Boot 3), harmless on 2.x: 1.21 shipped a docker-java
    // client that defaults to API 1.32, which Docker Engine 29+ rejects outright
    // (empty 400 stub from /info) instead of negotiating down; its floor is 1.40.
    // docker-java's shaded DefaultDockerClientConfig only honors the literal env
    // var key "api.version" (verified against its CONFIG_KEYS bytecode) - not
    // API_VERSION or the Docker CLI's DOCKER_API_VERSION - to pin a version and
    // skip negotiation.
    environment("api.version", "1.44")
}

// JaCoCo coverage reporting for the test task, mirroring android/app's
// setup: XML at the path SonarQube expects, plus HTML, no CSV.
//
// toolVersion is left unset deliberately, same reasoning as android/app:
// Gradle 9.9's bundled default already supports this module's JDK 21
// class files (jvmToolchain(21) above), and pinning a guessed version
// risks a dependency that doesn't resolve. Bump explicitly if a
// specific version is ever needed.
//
// Unlike android/app there is no Hilt/data-binding/R-class/Compose-UI
// bucket to exclude here - this module has no compile-time code
// generation at all (no kapt/ksp, no annotation processors that emit
// classes). The only exclusion category that actually applies is
// configuration classes: every @Configuration class in this codebase
// happens to be named *Config (see auth/SecurityConfig,
// auth/FirebaseAdminConfig, config/OpenApiConfig, config/TransactionConfig),
// plus the @SpringBootApplication entry point, which is bootstrap
// wiring with no logic of its own.
val jacocoExcludes = listOf(
    "com/rukavina/gymbuddy/**/*Config.class",
    "com/rukavina/gymbuddy/**/*Config$*.class",
    "com/rukavina/gymbuddy/GymBuddyApplication.class",
)

val NVD_MIRROR = "https://dependency-check.github.io/DependencyCheck_Builder/nvd_cache/nvdcve-{0}.json.gz"

// OWASP dependency-check - see android/app/build.gradle.kts for the full
// rationale (same setup, mirrored here). Run via
// `./gradlew dependencyCheckAnalyze` (this module has no subprojects, so
// unlike android there's no :app to target).
dependencyCheck {
    formats = listOf("HTML", "SARIF")
    data.directory = "$rootDir/dependency-check-data"
    // CVE data comes from the dependency-check project's own NVD mirror
    // (rebuilt daily on GitHub Pages), not the NVD API: the API returned
    // 503 on every run on 2026-11-15, and a cold sync through it needs an
    // API key and many minutes. With datafeedUrl set, no key is needed.
    nvd.datafeedUrl = NVD_MIRROR
    // Only what ships in the jar: skip the Kotlin compiler/tooling
    // classpaths (test ones are skipped by default). Build tooling is
    // covered by the dependency graph (dependency-graph.yml).
    // scanConfigurations = runtimeClasspath scanned 0 deps here.
    skipConfigurations = listOf(
        "kotlinCompilerClasspath",
        "kotlinBuildToolsApiClasspath",
        "kotlinAbiValidationCompatClasspath",
        "kotlinCompilerPluginClasspath",
        "kotlinNativeCompilerPluginClasspath",
        "kotlinKlibCommonizerClasspath",
        "developmentOnly",
        "testAndDevelopmentOnly",
    )
    suppressionFile = "$rootDir/config/dependency-check-suppressions.xml"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml"))
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/test/html"))
        csv.required.set(false)
    }
    classDirectories.setFrom(
        classDirectories.files.map { fileTree(it) { exclude(jacocoExcludes) } },
    )
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

// Serves the real backend for the Android sync engine's end-to-end
// tests, with test-key token verification instead of Firebase. See
// src/test/kotlin/e2e/gymbuddy/E2eBackendApplication.kt.
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootTestRun") {
    mainClass.set("e2e.gymbuddy.E2eBackendApplicationKt")
}

// Plan 9 step 01 (ADR 0018): runtime libraries and their licences.
// `check` fails on a licence not in config/allowed-licenses.json. The
// few weak-copyleft libraries Spring Boot needs (Hibernate, Logback,
// Jakarta APIs...) are allowed by exact module name only: the backend
// runs on our server and is never distributed. AGPL never.
licenseReport {
    configurations = arrayOf("runtimeClasspath")
    filters = arrayOf(com.github.jk1.license.filter.LicenseBundleNormalizer())
    renderers = arrayOf(com.github.jk1.license.render.JsonReportRenderer("licences.json"))
    allowedLicensesFile = file("config/allowed-licenses.json")
}

tasks.check {
    dependsOn(tasks.named("checkLicense"))
}
