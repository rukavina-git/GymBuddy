plugins {
    id("org.springframework.boot") version "3.5.3"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.1.21"
    kotlin("plugin.spring") version "2.1.21"
    kotlin("plugin.jpa") version "2.1.21"
    jacoco
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

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")
    implementation("com.google.firebase:firebase-admin:9.9.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    // Parses the committed api/openapi.yaml for OpenApiSpecDriftTest.
    // Version comes from the Spring Boot BOM (io.spring.dependency-management),
    // same as jackson-module-kotlin above - no need to pin one here.
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
    // Testcontainers 1.21.2 (pinned by the Spring Boot BOM) ships a docker-java
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
// Gradle 9.7's bundled default already supports this module's JDK 21
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
