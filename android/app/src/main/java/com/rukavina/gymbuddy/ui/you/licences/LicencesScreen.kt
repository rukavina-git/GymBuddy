package com.rukavina.gymbuddy.ui.you.licences

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.ui.design.BackTopBar
import com.rukavina.gymbuddy.ui.design.ListRow
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads the report the build bundles as res/raw/aboutlibraries.json. */
@Composable
private fun rememberLicences(): List<LicenceEntry>? {
    val context = LocalContext.current
    val entries by produceState<List<LicenceEntry>?>(null) { value = withContext(Dispatchers.IO) { load(context) } }
    return entries
}

private fun load(context: Context): List<LicenceEntry> =
    context.resources.openRawResource(R.raw.aboutlibraries).bufferedReader().use { parseLicences(it.readText()) }

/** You › About › Open-source licences: every library and bundled asset. */
@Composable
fun LicencesScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val d = GymBuddyTheme.dimens
    val entries = rememberLicences()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            BackTopBar(onBack = onBack, title = stringResource(R.string.you_licences))
            LazyColumn(
                contentPadding = PaddingValues(start = d.insetScreen, end = d.insetScreen, bottom = d.group),
                verticalArrangement = Arrangement.spacedBy(d.stackTight),
            ) {
                items(entries.orEmpty(), key = { it.id }) { entry ->
                    ListRow(
                        title = entry.name,
                        subtitle = entry.licenceNames.joinToString(),
                        onClick = { onOpen(entry.id) },
                    )
                }
            }
        }
    }
}

/** One entry: name, version, licence names and full texts. */
@Composable
fun LicenceDetailScreen(id: String, onBack: () -> Unit) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    val entry = rememberLicences()?.firstOrNull { it.id == id }
    Surface(Modifier.fillMaxSize(), color = cs.background) {
        Column(Modifier.fillMaxSize()) {
            BackTopBar(onBack = onBack, title = entry?.name)
            if (entry == null) return@Column
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(start = d.insetScreen, end = d.insetScreen, bottom = d.group),
                verticalArrangement = Arrangement.spacedBy(d.stackTight),
            ) {
                val meta = listOfNotNull(entry.version, entry.licenceNames.joinToString().ifEmpty { null })
                Text(meta.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                entry.website?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant) }
                entry.texts.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}
