package com.rukavina.gymbuddy.ui.workouts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.ui.design.AppIconButton
import com.rukavina.gymbuddy.ui.design.AppMenu
import com.rukavina.gymbuddy.ui.design.AppMenuItem
import com.rukavina.gymbuddy.ui.design.LargeTitleTopBar
import com.rukavina.gymbuddy.ui.design.SearchField
import com.rukavina.gymbuddy.ui.design.focusRing
import com.rukavina.gymbuddy.ui.design.rememberLargeTitleState
import com.rukavina.gymbuddy.ui.template.TemplateLine
import com.rukavina.gymbuddy.ui.template.aboutText
import com.rukavina.gymbuddy.ui.template.exerciseCountText
import com.rukavina.gymbuddy.ui.template.joinDot
import com.rukavina.gymbuddy.ui.template.shortDate
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import java.time.ZoneId

/**
 * Workouts tab (Figma 85:10177, scrolled 85:10257): large title with
 * + / search / ⋯, your templates, "+ New template", then starter plans.
 * A row opens the template page; starting happens there.
 */
@Composable
fun WorkoutsScreen(
    onOpenTemplate: (String) -> Unit,
    onNewTemplate: () -> Unit,
    onHiddenTemplates: () -> Unit,
    viewModel: WorkoutsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WorkoutsContent(state, viewModel::setQuery, onOpenTemplate, onNewTemplate, onHiddenTemplates)
}

@Composable
internal fun WorkoutsContent(
    state: WorkoutsUiState,
    onQuery: (String) -> Unit,
    onOpenTemplate: (String) -> Unit,
    onNewTemplate: () -> Unit,
    onHiddenTemplates: () -> Unit,
) {
    val d = GymBuddyTheme.dimens
    val titleState = rememberLargeTitleState()
    var searching by rememberSaveable { mutableStateOf(state.query.isNotEmpty()) }
    var menuOpen by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            LargeTitleTopBar(title = stringResource(R.string.workouts_title), state = titleState) {
                AppIconButton(AppIcons.Ui.Add, stringResource(R.string.new_template), onNewTemplate)
                AppIconButton(AppIcons.Ui.Search, stringResource(R.string.cd_search), {
                    searching = !searching
                    if (!searching) onQuery("")
                })
                Box {
                    AppIconButton(AppIcons.Ui.More, stringResource(R.string.cd_more), { menuOpen = true })
                    AppMenu(expanded = menuOpen, onDismiss = { menuOpen = false }) {
                        AppMenuItem(stringResource(R.string.hidden_templates), { menuOpen = false; onHiddenTemplates() }, AppIcons.Ui.Hide)
                    }
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().nestedScroll(titleState.nestedScrollConnection),
                contentPadding = PaddingValues(start = d.insetScreen, end = d.insetScreen, bottom = d.section),
                verticalArrangement = Arrangement.spacedBy(d.stackTight),
            ) {
                if (searching) {
                    item(key = "search") {
                        SearchField(
                            value = state.query,
                            onValueChange = onQuery,
                            placeholder = stringResource(R.string.search_templates),
                            modifier = Modifier.fillMaxWidth().padding(bottom = d.stackTight),
                        )
                    }
                }
                if (state.loaded && state.query.isNotBlank() && state.yours.isEmpty() && state.starters.isEmpty()) {
                    item(key = "none") {
                        Text(
                            stringResource(R.string.no_templates_found, state.query.trim()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(d.group),
                        )
                    }
                }
                if (state.yours.isNotEmpty() || state.query.isBlank()) {
                    item(key = "yours-header") { SectionHeader(stringResource(R.string.home_your_templates)) }
                }
                items(state.yours, key = { it.id }) { line ->
                    TemplateCard(line, state.zone, subtitle = joinDot(exerciseCountText(line.exerciseCount), aboutText(line.averageMinutes))) {
                        onOpenTemplate(line.id)
                    }
                }
                if (state.query.isBlank()) {
                    item(key = "new") { NewTemplateCard(onNewTemplate) }
                }
                if (state.starters.isNotEmpty()) {
                    item(key = "starters-header") { SectionHeader(stringResource(R.string.starter_plans)) }
                    items(state.starters, key = { it.id }) { line ->
                        TemplateCard(line, state.zone, subtitle = joinDot(exerciseCountText(line.exerciseCount), aboutText(line.averageMinutes))) {
                            onOpenTemplate(line.id)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(start = GymBuddyTheme.dimens.hairline, top = GymBuddyTheme.dimens.stackTight, bottom = GymBuddyTheme.dimens.hairline)
            .semantics { heading() },
    )
}

/** Figma 85:10177 row: title, "N exercises · about N min", date or "Never done", chevron. */
@Composable
private fun TemplateCard(line: TemplateLine, zone: ZoneId, subtitle: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    val shape = MaterialTheme.shapes.medium
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        shape = shape,
        color = cs.surfaceContainer,
        interactionSource = interaction,
        modifier = Modifier.fillMaxWidth().focusRing(interaction, shape),
    ) {
        Row(
            Modifier.heightIn(min = d.row).padding(start = d.group, end = d.stack, top = d.stack, bottom = d.stack),
            horizontalArrangement = Arrangement.spacedBy(d.stackTight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.hairline)) {
                Text(line.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(d.stackTight)) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(
                        line.lastDone?.let { shortDate(it, zone) } ?: stringResource(R.string.never_done),
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant,
                    )
                }
            }
            Icon(painterResource(AppIcons.Ui.ChevronRight), null, Modifier.size(d.iconSmall), tint = cs.onSurfaceVariant)
        }
    }
}

/** "+ New template": outlined card, lime + and label (Figma 85:10177). */
@Composable
private fun NewTemplateCard(onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    val shape = MaterialTheme.shapes.medium
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        shape = shape,
        color = cs.background,
        border = BorderStroke(d.strokeOutline, cs.outline),
        interactionSource = interaction,
        modifier = Modifier.fillMaxWidth().focusRing(interaction, shape),
    ) {
        Row(
            Modifier.heightIn(min = d.control).padding(horizontal = d.group),
            horizontalArrangement = Arrangement.spacedBy(d.stack),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(AppIcons.Ui.Add), null, Modifier.size(d.iconSmall), tint = cs.primary)
            Text(stringResource(R.string.new_template), style = MaterialTheme.typography.titleSmall, color = cs.primary)
        }
    }
}
