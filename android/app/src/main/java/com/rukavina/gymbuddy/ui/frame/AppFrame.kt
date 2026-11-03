package com.rukavina.gymbuddy.ui.frame

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.ui.design.AppNavBar
import com.rukavina.gymbuddy.ui.design.LiveBar
import com.rukavina.gymbuddy.ui.design.NavTab
import com.rukavina.gymbuddy.ui.theme.Durations
import com.rukavina.gymbuddy.ui.theme.Easings
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import com.rukavina.gymbuddy.ui.theme.motionTween

/**
 * The tabbed shell (Figma 85:9890): content, then the live bar above
 * the nav bar while a workout runs. [fullScreen] hides both (the
 * running workout).
 */
@Composable
fun AppFrame(
    selectedTab: NavTab,
    onSelectTab: (NavTab) -> Unit,
    fullScreen: Boolean,
    liveBar: LiveBarState?,
    onLiveBarClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (!fullScreen) {
                Column {
                    LiveBarSlot(liveBar, onLiveBarClick)
                    AppNavBar(selected = selectedTab, onSelect = onSelectTab)
                }
            }
        },
        content = content,
    )
}

@Composable
private fun LiveBarSlot(state: LiveBarState?, onClick: () -> Unit) {
    // Keep the last text on screen while the bar slides away.
    var last by remember { mutableStateOf(state) }
    if (state != null) last = state
    val d = GymBuddyTheme.dimens
    AnimatedVisibility(
        visible = state != null,
        // Stand-in for the container transform from ⌄ (motion.md #10, 7.6).
        enter = expandVertically(motionTween(Durations.LONG2, Easings.Emphasized), expandFrom = Alignment.Bottom) +
            fadeIn(motionTween(Durations.LONG2, Easings.Emphasized)),
        // motion.md #12: slides down behind the nav bar.
        exit = slideOutVertically(motionTween(Durations.MEDIUM3, Easings.EmphasizedAccelerate)) { it } +
            fadeOut(motionTween(Durations.MEDIUM3, Easings.EmphasizedAccelerate)),
    ) {
        val bar = last ?: return@AnimatedVisibility
        LiveBar(
            title = bar.title,
            subtitle = subtitleFor(bar),
            subtitleKey = bar.setNumber to bar.allDone,
            progress = bar.progress,
            onClick = onClick,
            modifier = Modifier.padding(start = d.insetScreen, end = d.insetScreen, bottom = d.group),
        )
    }
}

@Composable
private fun subtitleFor(bar: LiveBarState): String {
    val elapsed = formatElapsed(bar.elapsedSeconds)
    val set = when {
        bar.setNumber != null -> stringResource(R.string.live_bar_set, bar.setNumber, bar.setCount)
        bar.allDone -> stringResource(R.string.live_bar_all_done)
        else -> null
    }
    return if (set == null) elapsed else "$set · $elapsed"
}
