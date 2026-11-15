package com.rukavina.gymbuddy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The public name is "Gym Tracker" and lives in one string, brand_name
 * (Plan 9 step 02a). The old name collides with an EU trademark, so it
 * must never come back in anything a user can read.
 */
class BrandNameTest {

    private val oldName = Regex("""gym\s*buddy""", RegexOption.IGNORE_CASE)

    /** Unit tests run with the module (android/app) as working dir. */
    private val main = File("src/main")

    @Test
    fun noStringResourceNamesTheOldApp() {
        val hits = resourceFiles().flatMap { file ->
            stringValues(file.readText()).filter { oldName.containsMatchIn(it) }.map { "${file.path}: $it" }
        }
        assertTrue("old app name in strings:\n${hits.joinToString("\n")}", hits.isEmpty())
    }

    @Test
    fun brandNameIsTheOnlyLiteralName() {
        val strings = File(main, "res/values/strings.xml").readText()
        assertTrue(strings.contains("""<string name="brand_name">Gym Tracker</string>"""))
        assertTrue(strings.contains("""<string name="app_name">@string/brand_name</string>"""))
        // Every other mention goes through brand_name, so no string spells the name out.
        val literal = resourceFiles().flatMap { stringEntries(it.readText()) }
            .filter { (name, value) -> name != "brand_name" && value.contains("Gym Tracker") && !value.startsWith("Gym Tracker Test") }
        assertEquals(emptyList<Pair<String, String>>(), literal)
    }

    @Test
    fun noUserFacingKotlinLiteralNamesTheOldApp() {
        val literal = Regex("\"([^\"\\\\]|\\\\.)*\"")
        val hits = File(main, "java").walkTopDown().filter { it.extension == "kt" }.flatMap { file ->
            file.readLines().mapIndexedNotNull { i, line ->
                val code = line.substringBefore("//")
                if (code.trimStart().startsWith("*") || code.trimStart().startsWith("import ") || code.trimStart().startsWith("package ")) return@mapIndexedNotNull null
                literal.findAll(code).map { it.value }.firstOrNull { oldName.containsMatchIn(it) }?.let { "${file.name}:${i + 1}: $it" }
            }
        }.toList()
        assertTrue("old app name in Kotlin strings:\n${hits.joinToString("\n")}", hits.isEmpty())
    }

    private fun resourceFiles(): List<File> =
        File("src").walkTopDown()
            .filter { it.isFile && it.extension == "xml" && it.parentFile.name.startsWith("values") }
            .toList()

    private fun stringEntries(xml: String): List<Pair<String, String>> =
        Regex("""<string name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(xml).map { it.groupValues[1] to it.groupValues[2] }.toList()

    private fun stringValues(xml: String): List<String> = stringEntries(xml).map { it.second }
}
