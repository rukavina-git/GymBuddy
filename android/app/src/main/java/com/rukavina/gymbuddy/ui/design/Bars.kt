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
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import kotlin.math.roundToInt
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.FullShape
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme

private val TopBarHeight = 64.dp
private val ProgressPillHeight = 40.dp
private val ProgressIcon = 22.dp

/**
 * Pushed screens: back on the left, optional title, icon actions right.
 * [close]: × instead of ← (editor and picker, R12-4/R12-6).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    close: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { if (title != null) Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (close) AppIconButton(AppIcons.Ui.Close, stringResource(R.string.cd_close), onBack)
            else AppIconButton(AppIcons.Ui.Back, stringResource(R.string.cd_back), onBack)
        },
        actions = actions,
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    )
}

/**
 * Scroll state for [LargeTitleTopBar] (motion.md #16). Scrolling up
 * collapses the title first; it grows back only once the content is at
 * the top again (exit until collapsed). Hook [nestedScrollConnection]
 * to the scrolling content with `Modifier.nestedScroll`.
 */
@Stable
class LargeTitleState {
    internal var limit by mutableFloatStateOf(0f)
    /** 0 expanded … [limit] (negative px) collapsed. */
    internal var offset by mutableFloatStateOf(0f)

    /** 0 = big title, 1 = small bar. */
    val collapsedFraction: Float
        get() = if (limit == 0f || offset == 0f) 0f else (offset / limit).coerceIn(0f, 1f)

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
            if (available.y < 0f) consume(available.y) else Offset.Zero

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
            if (available.y > 0f) consume(available.y) else Offset.Zero
    }

    private fun consume(dy: Float): Offset {
        val old = offset
        offset = (offset + dy).coerceIn(limit, 0f)
        return Offset(0f, offset - old)
    }
}

@Composable
fun rememberLargeTitleState(): LargeTitleState = remember { LargeTitleState() }

/**
 * Tab roots: actions row, large title, optional muted subtitle. With a
 * [state], the big title shrinks away as content scrolls and a small
 * title fades into the actions row on a raised background (Figma
 * 85:10257). TalkBack sees one heading: the big title.
 */
@Composable
fun LargeTitleTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    state: LargeTitleState? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val d = GymBuddyTheme.dimens
    val raised = MaterialTheme.colorScheme.surfaceContainerHigh
    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val f = state?.collapsedFraction ?: 0f
                if (f > 0f) drawRect(raised.copy(alpha = f))
            }
            .statusBarsPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = TopBarHeight).padding(end = d.hairline),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = d.section, end = d.stackTight)
                    .graphicsLayer { alpha = state?.collapsedFraction ?: 0f }
                    .clearAndSetSemantics {},
            )
            actions()
        }
        Column(
            modifier = Modifier
                .clipToBounds()
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val full = placeable.height
                    val shown = if (state == null) {
                        full
                    } else {
                        if (state.limit != -full.toFloat()) {
                            state.limit = -full.toFloat()
                            state.offset = state.offset.coerceAtLeast(state.limit)
                        }
                        (full + state.offset).roundToInt().coerceIn(0, full)
                    }
                    layout(placeable.width, shown) { placeable.place(0, shown - full) }
                }
                .graphicsLayer { alpha = 1f - (state?.collapsedFraction ?: 0f) }
                .padding(start = d.section, end = d.section, bottom = d.group),
            verticalArrangement = Arrangement.spacedBy(d.hairline),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() },
            )
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * Running workout: minimise, title + elapsed, progress pill (opens the
 * exercise list; hidden when [onProgress] is null), ⋯. Elapsed is
 * formatted by the caller.
 */
@Composable
fun WorkoutTopBar(
    title: String,
    elapsed: String,
    progress: String,
    onCollapse: () -> Unit,
    onProgress: (() -> Unit)?,
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
        if (onProgress != null) {
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
        }
        AppIconButton(AppIcons.Ui.More, stringResource(R.string.cd_more), onMore)
    }
}

/** "Exercises" overflows a fifth of a 360dp bar from about here. */
const val NAV_ICONS_ONLY_FONT_SCALE = 1.3f

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
 * untinted, bold label; inactive: outline icon, muted label. From
 * [NAV_ICONS_ONLY_FONT_SCALE] the labels no longer fit: icons only,
 * named for TalkBack, long-press shows the name (Figma 85:9892).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavBar(
    selected: NavTab,
    onSelect: (NavTab) -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = NavigationBarDefaults.windowInsets,
) {
    val cs = MaterialTheme.colorScheme
    val iconsOnly = LocalDensity.current.fontScale >= NAV_ICONS_ONLY_FONT_SCALE
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
                    val name = stringResource(tab.label)
                    val drawn = @Composable {
                        Icon(
                            painterResource(icon.res),
                            contentDescription = if (iconsOnly) name else null,
                            tint = if (icon.tinted) cs.onSurfaceVariant else Color.Unspecified,
                        )
                    }
                    if (iconsOnly) {
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            tooltip = { PlainTooltip { Text(name) } },
                            state = rememberTooltipState(),
                        ) { drawn() }
                    } else {
                        drawn()
                    }
                },
                label = if (iconsOnly) {
                    null
                } else {
                    {
                        Text(
                            stringResource(tab.label),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else null,
                        )
                    }
                },
                alwaysShowLabel = !iconsOnly,
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = GymBuddyTheme.colors.cobaltTint,
                    selectedTextColor = cs.onSurface,
                    unselectedTextColor = cs.onSurfaceVariant,
                ),
            )
        }
    }
}
