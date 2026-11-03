package com.rukavina.gymbuddy.ui.design

import androidx.annotation.DrawableRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.FullShape
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme

private val TopBarHeight = 64.dp
private val ProgressPillHeight = 40.dp
private val ProgressIcon = 22.dp

/** Pushed screens: back on the left, optional title, icon actions right. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { if (title != null) Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = { AppIconButton(AppIcons.Ui.Back, stringResource(R.string.cd_back), onBack) },
        actions = actions,
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    )
}

/**
 * Tab roots: actions row, large title, optional muted subtitle. Static
 * here; the scroll collapse comes with the app frame (7.3).
 */
@Composable
fun LargeTitleTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val d = GymBuddyTheme.dimens
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = d.section, end = d.hairline, bottom = d.group),
        verticalArrangement = Arrangement.spacedBy(d.hairline),
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = d.target), horizontalArrangement = Arrangement.End, content = actions)
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(end = d.section).semantics { heading() },
        )
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Running workout: minimise, title + elapsed, progress pill (opens the
 * exercise list), ⋯. Elapsed is formatted by the caller.
 */
@Composable
fun WorkoutTopBar(
    title: String,
    elapsed: String,
    progress: String,
    onCollapse: () -> Unit,
    onProgress: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .heightIn(min = TopBarHeight)
            .padding(horizontal = d.hairline),
        horizontalArrangement = Arrangement.spacedBy(d.hairline),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconButton(AppIcons.Ui.ChevronDown, stringResource(R.string.cd_collapse_workout), onCollapse)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(elapsed, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
        }
        val interaction = remember { MutableInteractionSource() }
        val label = stringResource(R.string.cd_workout_progress)
        Surface(
            onClick = onProgress,
            shape = FullShape,
            color = cs.surfaceContainerHigh,
            interactionSource = interaction,
            modifier = Modifier.focusRing(interaction, FullShape).semantics { this.contentDescription = "$label $progress" },
        ) {
            Row(
                Modifier.heightIn(min = ProgressPillHeight).padding(horizontal = d.stack),
                horizontalArrangement = Arrangement.spacedBy(d.stackTight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(AppIcons.Ui.List), null, Modifier.size(ProgressIcon))
                Text(progress, style = MaterialTheme.typography.labelMedium)
            }
        }
        AppIconButton(AppIcons.Ui.More, stringResource(R.string.cd_more), onMore)
    }
}

/** The five tabs, in order. */
enum class NavTab(val icon: AppIcons.Tab, internal val label: Int) {
    Home(AppIcons.Tab.Home, R.string.tab_home),
    Workouts(AppIcons.Tab.Workouts, R.string.tab_workouts),
    Exercises(AppIcons.Tab.Exercises, R.string.tab_exercises),
    Progress(AppIcons.Tab.Progress, R.string.tab_progress),
    You(AppIcons.Tab.You, R.string.tab_you),
}

/** Which drawable a tab shows, and whether it takes a tint. */
internal data class NavIcon(@DrawableRes val res: Int, val tinted: Boolean)

/** Selected: filled, never tinted (cut-out detail). Otherwise outline, muted. */
internal fun navIcon(tab: NavTab, selected: Boolean) =
    if (selected) NavIcon(tab.icon.filled, tinted = false) else NavIcon(tab.icon.outline, tinted = true)

/**
 * Bottom bar (D14). Active tab: cobalt-tint pill, filled icon drawn
 * untinted, bold label; inactive: outline icon, muted label.
 */
@Composable
fun AppNavBar(
    selected: NavTab,
    onSelect: (NavTab) -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = NavigationBarDefaults.windowInsets,
) {
    val cs = MaterialTheme.colorScheme
    NavigationBar(
        modifier = modifier,
        containerColor = cs.surfaceContainer,
        tonalElevation = 0.dp,
        windowInsets = windowInsets,
    ) {
        NavTab.entries.forEach { tab ->
            val isSelected = tab == selected
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(tab) },
                icon = {
                    val icon = navIcon(tab, isSelected)
                    Icon(
                        painterResource(icon.res),
                        contentDescription = null,
                        tint = if (icon.tinted) cs.onSurfaceVariant else Color.Unspecified,
                    )
                },
                label = {
                    Text(
                        stringResource(tab.label),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else null,
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = GymBuddyTheme.colors.cobaltTint,
                    selectedTextColor = cs.onSurface,
                    unselectedTextColor = cs.onSurfaceVariant,
                ),
            )
        }
    }
}
