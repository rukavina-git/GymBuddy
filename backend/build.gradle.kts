plugins {
    id("org.springframework.boot") version "3.5.3"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.1.21"
    kotlin("plugin.spring") version "2.1.21"
    kotlin("plugin.jpa") version "2.1.21"
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
