package com.rukavina.gymbuddy.ui.you.licences

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/** One library or bundled asset on the open-source licences page. */
data class LicenceEntry(
    val id: String,
    val name: String,
    val version: String?,
    val licenceNames: List<String>,
    val texts: List<String>,
    val website: String?,
)

// Shape of the build-time report (AboutLibraries plugin, Plan 9 step 01).
private data class Report(val libraries: List<ReportLibrary> = emptyList(), val licenses: Map<String, ReportLicence> = emptyMap())
private data class ReportLibrary(
    val uniqueId: String,
    val name: String,
    val artifactVersion: String? = null,
    val website: String? = null,
    val licenses: List<String> = emptyList(),
)
private data class ReportLicence(val name: String, val url: String? = null, val content: String? = null)

private val adapter = Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter(Report::class.java)

/**
 * Entries from the report, sorted by name. A licence without a stored
 * text falls back to its URL, so the page never shows a blank licence.
 */
fun parseLicences(json: String): List<LicenceEntry> {
    val report = adapter.fromJson(json) ?: return emptyList()
    return report.libraries
        .map { lib ->
            val licences = lib.licenses.mapNotNull { report.licenses[it] }.sortedBy { it.name }
            LicenceEntry(
                id = lib.uniqueId,
                name = lib.name,
                version = lib.artifactVersion?.takeIf { it.isNotBlank() },
                licenceNames = licences.map { it.name },
                texts = licences.mapNotNull { it.content?.takeIf(String::isNotBlank) ?: it.url },
                website = lib.website,
            )
        }
        .sortedBy { it.name.lowercase() }
}
