package com.rukavina.gymbuddy.ui.you.licences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LicencesTest {

    private val json = """
        {
          "libraries": [
            {"uniqueId": "asset:figtree", "artifactVersion": "032dfa7", "name": "Figtree",
             "website": "https://github.com/erikdkennedy/figtree", "licenses": ["OFL-1.1"]},
            {"uniqueId": "a:core", "artifactVersion": "", "name": "android core", "licenses": ["Apache-2.0"]}
          ],
          "licenses": {
            "OFL-1.1": {"name": "SIL Open Font License 1.1", "url": "https://spdx.org/licenses/OFL-1.1.html",
                        "content": "SIL OPEN FONT LICENSE", "spdxId": "OFL-1.1", "hash": "OFL-1.1"},
            "Apache-2.0": {"name": "Apache License 2.0", "url": "https://spdx.org/licenses/Apache-2.0.html",
                           "spdxId": "Apache-2.0", "hash": "Apache-2.0"}
          }
        }
    """.trimIndent()

    @Test
    fun sortedByNameIgnoringCase() {
        assertEquals(listOf("android core", "Figtree"), parseLicences(json).map { it.name })
    }

    @Test
    fun carriesLicenceNameAndText() {
        val figtree = parseLicences(json).single { it.id == "asset:figtree" }
        assertEquals("032dfa7", figtree.version)
        assertEquals(listOf("SIL Open Font License 1.1"), figtree.licenceNames)
        assertEquals(listOf("SIL OPEN FONT LICENSE"), figtree.texts)
        assertEquals("https://github.com/erikdkennedy/figtree", figtree.website)
    }

    @Test
    fun missingTextFallsBackToTheLicenceUrlAndBlankVersionIsNull() {
        val core = parseLicences(json).single { it.id == "a:core" }
        assertNull(core.version)
        assertEquals(listOf("https://spdx.org/licenses/Apache-2.0.html"), core.texts)
    }
}
