package com.rukavina.gymbuddy.ui.you.licences

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/** Content of the open-source licences page: a table, then each licence once. */
data class LicencePage(val rows: List<LibraryRow>, val licences: List<LicenceText>)

/** One table row: a library or bundled asset and its licence ids. */
data class LibraryRow(val name: String, val licenceIds: List<String>)

/** One distinct licence. [label] is short for the table; [text] is the full text or, if missing, its URL. */
data class LicenceText(val id: String, val label: String, val name: String, val text: String?)

// Shape of the build-time report (AboutLibraries plugin, Plan 9 step 01).
private data class Report(val libraries: List<ReportLibrary> = emptyList(), val licenses: Map<String, ReportLicence> = emptyMap())
private data class ReportLibrary(val uniqueId: String, val name: String, val licenses: List<String> = emptyList())
private data class ReportLicence(val name: String, val url: String? = null, val content: String? = null, val spdxId: String? = null)

// SPDX ids short enough for the table. Google's SDK terms carry made-up
// ids (ASDKL, PCSDKToS), so they show a shortened name instead.
private val shortIds = setOf(
    "Apache-2.0", "MIT", "BSD-2-Clause", "BSD-3-Clause", "ISC", "OFL-1.1", "CC0-1.0", "CC-BY-4.0", "Unlicense",
)

private fun label(l: ReportLicence): String =
    l.spdxId?.takeIf { it in shortIds } ?: l.name.replace("Software Development Kit", "SDK")

private val adapter = Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter(Report::class.java)

/**
 * Rows sorted by name, each name and licence pair once (several
 * artifacts of one library share a name). Licences ordered by how many
 * rows use them; only licences some row uses are kept.
 */
fun parseLicences(json: String): LicencePage {
    val report = adapter.fromJson(json) ?: return LicencePage(emptyList(), emptyList())
    val rows = report.libraries
        .map { lib -> LibraryRow(lib.name, lib.licenses.filter { it in report.licenses }.sorted()) }
        .filter { it.licenceIds.isNotEmpty() }
        .distinct()
        .sortedBy { it.name.lowercase() }
    val uses = rows.flatMap { it.licenceIds }.groupingBy { it }.eachCount()
    val licences = uses.keys
        .map { id ->
            val l = report.licenses.getValue(id)
            LicenceText(id, label(l), l.name, l.content?.takeIf(String::isNotBlank) ?: l.url)
        }
        .sortedWith(compareByDescending<LicenceText> { uses.getValue(it.id) }.thenBy { it.name })
    return LicencePage(rows, licences)
}
