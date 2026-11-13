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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.ui.design.BackTopBar
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun load(context: Context): LicencePage =
    context.resources.openRawResource(R.raw.aboutlibraries).bufferedReader().use { parseLicences(it.readText()) }

/**
 * You › About › Open-source licences. A table of every library and
 * bundled asset with its licence; tapping a licence jumps to its text,
 * which appears once below the table.
 */
@Composable
fun LicencesScreen(onBack: () -> Unit) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    val page by produceState<LicencePage?>(null) { value = withContext(Dispatchers.IO) { load(context) } }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    Surface(Modifier.fillMaxSize(), color = cs.background) {
        Column(Modifier.fillMaxSize()) {
            BackTopBar(onBack = onBack, title = stringResource(R.string.you_licences))
            val p = page ?: return@Column
            val labels = p.licences.associate { it.id to it.label }
            // Row 0 is the table title, then the rows, then the licences title.
            val firstLicenceIndex = 1 + p.rows.size + 1
            fun jumpTo(id: String) {
                val i = p.licences.indexOfFirst { it.id == id }
                if (i >= 0) scope.launch { listState.animateScrollToItem(firstLicenceIndex + i) }
            }
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = d.insetScreen, end = d.insetScreen, bottom = d.group),
            ) {
                item(key = "t-title") { SectionTitle(stringResource(R.string.you_licences_table)) }
                items(p.rows, key = { "r-" + it.name + it.licenceIds }) { row ->
                    TableRow(row.name) {
                        row.licenceIds.forEach { id ->
                            Text(
                                labels.getValue(id),
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.primary,
                                textAlign = TextAlign.End,
                                modifier = Modifier.clickable { jumpTo(id) }.padding(vertical = d.stackTight / 2),
                            )
                        }
                    }
                }
                item(key = "l-title") { SectionTitle(stringResource(R.string.you_licences_texts)) }
                items(p.licences, key = { "l-" + it.id }) { licence ->
                    Column(Modifier.padding(bottom = d.group), verticalArrangement = Arrangement.spacedBy(d.stackTight)) {
                        Text(licence.name, style = MaterialTheme.typography.titleSmall)
                        licence.text?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant) }
                    }
                }
            }
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
