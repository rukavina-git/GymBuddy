package com.rukavina.gymbuddy.ui.you

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.ui.design.AppTextButton
import com.rukavina.gymbuddy.ui.design.BackTopBar
import com.rukavina.gymbuddy.ui.design.EmptyState
import com.rukavina.gymbuddy.ui.exercises.exerciseSubtitle
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme

/** You › Hidden exercises: unhide one or all. */
@Composable
fun HiddenExercisesScreen(onBack: () -> Unit, viewModel: HiddenExercisesViewModel = hiltViewModel()) {
    val hidden by viewModel.hiddenExercises.collectAsStateWithLifecycle(initialValue = null)
    HiddenList(
        title = stringResource(R.string.you_hidden_exercises),
        rows = hidden?.map { HiddenRow(it.id, it.name, exerciseSubtitle(it)) },
        emptyTitle = stringResource(R.string.you_hidden_exercises_empty),
        emptyBody = stringResource(R.string.you_hidden_exercises_empty_body),
        onBack = onBack,
        onUnhide = viewModel::unhideExercise,
        onUnhideAll = viewModel::unhideAllExercises,
    )
}

/** You › Hidden templates: unhide one. */
@Composable
fun HiddenTemplatesScreen(onBack: () -> Unit, viewModel: HiddenTemplatesViewModel = hiltViewModel()) {
    val hidden by viewModel.hiddenTemplates.collectAsStateWithLifecycle(initialValue = null)
    HiddenList(
        title = stringResource(R.string.you_hidden_templates),
        rows = hidden?.map { HiddenRow(it.id, it.title, null) },
        emptyTitle = stringResource(R.string.you_hidden_templates_empty),
        emptyBody = stringResource(R.string.you_hidden_templates_empty_body),
        onBack = onBack,
        onUnhide = viewModel::unhideTemplate,
        onUnhideAll = null,
    )
}

internal data class HiddenRow(val id: String, val title: String, val subtitle: String?)

@Composable
private fun HiddenList(
    title: String,
    rows: List<HiddenRow>?,
    emptyTitle: String,
    emptyBody: String,
    onBack: () -> Unit,
    onUnhide: (String) -> Unit,
    onUnhideAll: (() -> Unit)?,
) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxSize(), color = cs.background) {
        Column(Modifier.fillMaxSize()) {
            BackTopBar(onBack = onBack, title = title) {
                if (onUnhideAll != null && !rows.isNullOrEmpty()) AppTextButton(stringResource(R.string.you_unhide_all), onUnhideAll)
            }
            when {
                rows == null -> Unit
                rows.isEmpty() -> Box(Modifier.fillMaxSize().padding(horizontal = d.insetScreen), contentAlignment = Alignment.Center) {
                    EmptyState(glyph = AppIcons.Ui.Hide, title = emptyTitle, body = emptyBody)
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = d.insetScreen, end = d.insetScreen, top = d.stackTight, bottom = d.section),
                    verticalArrangement = Arrangement.spacedBy(d.stackTight),
                ) {
                    items(rows, key = { it.id }) { row ->
                        Surface(shape = MaterialTheme.shapes.medium, color = cs.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.heightIn(min = d.control).padding(start = d.insetCard, end = d.hairline, top = d.stackTight, bottom = d.stackTight),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(row.title, style = MaterialTheme.typography.titleSmall)
                                    if (!row.subtitle.isNullOrBlank()) {
                                        Text(row.subtitle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                                    }
                                }
                                AppTextButton(stringResource(R.string.you_unhide), { onUnhide(row.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}
