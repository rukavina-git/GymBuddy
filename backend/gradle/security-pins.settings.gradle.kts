// Security floors for transitive libraries (Dependabot, 2026-11-17).
//
// Raises a vulnerable version to a fixed one in every configuration,
// including the plugin classpaths (AGP, openapi-generator, Kotlin,
// OWASP...), which no BOM or version catalog reaches. Only raises,
// never lowers: a newer version requested elsewhere wins.
//
// Identical copies: android/gradle/ and backend/gradle/ (the backend
// Docker build only sees backend/). Change both. Drop a floor once
// the plugins that pull it in ship a fixed version themselves.

fun versionParts(v: String): List<Int> =
    v.split('.', '-').takeWhile { it.isNotEmpty() && it.all(Char::isDigit) }.map { it.toInt() }

fun lessThan(a: String, b: String): Boolean {
    val x = versionParts(a)
    val y = versionParts(b)
    for (i in 0 until maxOf(x.size, y.size)) {
        val d = x.getOrElse(i) { 0 } - y.getOrElse(i) { 0 }
        if (d != 0) return d < 0
    }
    return false
}

/** Fixed version for [group]:[name] at [v], or null if it's fine. */
fun securityFloor(group: String, name: String, v: String): String? {
    fun below(min: String, to: String = min) = if (lessThan(v, min)) to else null
    return when {
        group == "org.bouncycastle" && name.endsWith("-jdk18on") -> below("1.85")
        group == "com.google.guava" && name == "guava" ->
            below("33.7.2", if (v.endsWith("-android")) "33.8.0-android" else "33.8.0-jre")
        group == "org.jsoup" && name == "jsoup" -> below("1.23.2")
        group == "org.json" && name == "json" -> below("20260719")
        group == "com.github.jknack" && name == "handlebars" -> below("4.5.2")
        group == "org.bitbucket.b_c" && name == "jose4j" -> below("0.9.6")
        group == "org.jdom" && name == "jdom2" -> below("2.0.6.1")
        group == "org.apache.commons" && name == "commons-lang3" -> below("3.18.0", "3.20.0")
        group == "org.apache.httpcomponents" && name == "httpclient" -> below("4.5.13", "4.5.14")
        // Fixed lines: 2.21.7+ and 2.22.3+. Stay on the requested line.
        group == "com.fasterxml.jackson.core" && (name == "jackson-core" || name == "jackson-databind") ->
            when {
                lessThan(v, "2.21.7") -> "2.21.7"
                !lessThan(v, "2.22.0") && lessThan(v, "2.22.3") -> "2.22.4"
                else -> null
            }
        else -> null
    }
}

fun ResolutionStrategy.applySecurityFloors() = eachDependency {
    val v = requested.version ?: return@eachDependency
    securityFloor(requested.group, requested.name, v)?.let {
        useVersion(it)
        because("security floor (gradle/security-pins.settings.gradle.kts)")
    }
}

gradle.allprojects {
    buildscript.configurations.configureEach { resolutionStrategy.applySecurityFloors() }
    configurations.configureEach { resolutionStrategy.applySecurityFloors() }
}
