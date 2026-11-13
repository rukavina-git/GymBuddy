package com.rukavina.gymbuddy.ui.you.licences

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.ui.design.BackTopBar
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun load(context: Context): LicencePage =
    context.resources.openRawResource(R.raw.aboutlibraries).bufferedReader().use { parseLicences(it.readText()) }

@Composable
private fun rememberPage(): LicencePage? {
    val context = LocalContext.current
    val page by produceState<LicencePage?>(null) { value = withContext(Dispatchers.IO) { load(context) } }
    return page
}

/**
 * You › About › Open-source licences: every library and bundled asset
 * with its licence. Tapping a licence opens its text ([LicenceTextScreen]).
 */
@Composable
fun LicencesScreen(onBack: () -> Unit, onOpenLicence: (String) -> Unit) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    val page = rememberPage()
    Surface(Modifier.fillMaxSize(), color = cs.background) {
        Column(Modifier.fillMaxSize()) {
            BackTopBar(onBack = onBack, title = stringResource(R.string.you_licences))
            val p = page ?: return@Column
            val labels = p.licences.associate { it.id to it.label }
            LazyColumn(contentPadding = PaddingValues(start = d.insetScreen, end = d.insetScreen, bottom = d.group)) {
                item(key = "t-title") { SectionTitle(stringResource(R.string.you_licences_table)) }
                items(p.rows, key = { "r-" + it.name + it.licenceIds }) { row ->
                    TableRow(row.name) {
                        row.licenceIds.forEach { id ->
                            Text(
                                labels.getValue(id),
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.primary,
                                textAlign = TextAlign.End,
                                modifier = Modifier.clickable { onOpenLicence(id) }.padding(vertical = d.stackTight / 2),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** One licence's full text, or its link when the report has no text (Google's SDK terms). */
@Composable
fun LicenceTextScreen(id: String, onBack: () -> Unit) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    val licence = rememberPage()?.licences?.firstOrNull { it.id == id }
    Surface(Modifier.fillMaxSize(), color = cs.background) {
        Column(Modifier.fillMaxSize()) {
            BackTopBar(onBack = onBack, title = licence?.name)
            val text = licence?.text ?: return@Column
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(start = d.insetScreen, end = d.insetScreen, bottom = d.group),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    val d = GymBuddyTheme.dimens
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = d.group, bottom = d.stackTight))
}

@Composable
private fun TableRow(name: String, licences: @Composable () -> Unit) {
    val d = GymBuddyTheme.dimens
    Column {
        Row(
            Modifier.fillMaxWidth().padding(vertical = d.stackTight / 2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.stackTight),
        ) {
            Text(name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Column(Modifier.weight(0.6f), horizontalAlignment = Alignment.End) { licences() }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
