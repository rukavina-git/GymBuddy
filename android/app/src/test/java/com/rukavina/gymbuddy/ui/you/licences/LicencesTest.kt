package com.rukavina.gymbuddy.ui.you.licences

import org.junit.Assert.assertEquals
import org.junit.Test

class LicencesTest {

    private val json = """
        {
          "libraries": [
            {"uniqueId": "asset:figtree", "name": "Figtree", "licenses": ["OFL-1.1"]},
            {"uniqueId": "a:core", "name": "core", "licenses": ["Apache-2.0"]},
            {"uniqueId": "a:activity", "name": "Activity", "licenses": ["Apache-2.0"]},
            {"uniqueId": "a:activity-ktx", "name": "Activity", "licenses": ["Apache-2.0"]},
            {"uniqueId": "g:auth", "name": "Firebase Auth", "licenses": ["ASDKL"]},
            {"uniqueId": "x:unknown", "name": "Mystery", "licenses": ["NOPE"]}
          ],
          "licenses": {
            "OFL-1.1": {"name": "SIL Open Font License 1.1", "url": "https://spdx.org/licenses/OFL-1.1.html",
                        "content": "SIL OPEN FONT LICENSE", "spdxId": "OFL-1.1"},
            "Apache-2.0": {"name": "Apache License 2.0", "url": "https://spdx.org/licenses/Apache-2.0.html",
                           "spdxId": "Apache-2.0"},
            "ASDKL": {"name": "Android Software Development Kit License", "url": "https://developer.android.com/studio/terms.html",
                      "spdxId": "ASDKL"}
          }
        }
    """.trimIndent()

    @Test
    fun rowsSortedByNameEachOnceAndOnlyKnownLicences() {
        val rows = parseLicences(json).rows
        assertEquals(listOf("Activity", "core", "Figtree", "Firebase Auth"), rows.map { it.name })
        assertEquals(listOf("Apache-2.0"), rows.first().licenceIds)
    }

    @Test
    fun eachLicenceOnceMostUsedFirst() {
        assertEquals(listOf("Apache-2.0", "ASDKL", "OFL-1.1"), parseLicences(json).licences.map { it.id })
    }

    @Test
    fun labelIsAShortSpdxIdOrShortenedNameAndTextFallsBackToTheUrl() {
        val licences = parseLicences(json).licences.associateBy { it.id }
        assertEquals("OFL-1.1", licences.getValue("OFL-1.1").label)
        assertEquals("Android SDK License", licences.getValue("ASDKL").label)
        assertEquals("SIL OPEN FONT LICENSE", licences.getValue("OFL-1.1").text)
        assertEquals("https://spdx.org/licenses/Apache-2.0.html", licences.getValue("Apache-2.0").text)
    }

    @Test
    fun reflowJoinsWrappedLinesButKeepsParagraphs() {
        val text = "MIT License\n\nCopyright (c) Someone.\n\nPermission is hereby granted,\n  free of charge.\n"
        assertEquals("MIT License\n\nCopyright (c) Someone.\n\nPermission is hereby granted, free of charge.", reflow(text))
    }
}
