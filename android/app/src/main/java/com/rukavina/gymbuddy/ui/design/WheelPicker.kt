package com.rukavina.gymbuddy.ui.design

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import com.rukavina.gymbuddy.ui.theme.Haptic
import com.rukavina.gymbuddy.ui.theme.rememberHaptics
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/** Rows shown; the middle one is the value. Odd, so there is a middle. */
private const val WHEEL_ROWS = 5

/**
 * F6: a snapping vertical wheel over [count] rows. The middle row is the
 * value, the rest are dim. Each new row ticks ([Haptic.StepperTick]) and
 * reports through [onSelected]. Tapping a row scrolls to it. If [selected]
 * changes from outside while the wheel is still, the wheel follows.
 */
@Composable
fun WheelPicker(
    count: Int,
    selected: Int,
    onSelected: (Int) -> Unit,
    label: (Int) -> String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    val row = d.rowSet
    val rowPx = with(LocalDensity.current) { row.toPx() }
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selected.coerceIn(0, (count - 1).coerceAtLeast(0)))
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    val report by rememberUpdatedState(onSelected)
    // Top padding of two rows puts item i in the middle when it is first visible.
    val centre by remember(count) {
        derivedStateOf {
            val i = state.firstVisibleItemIndex + if (state.firstVisibleItemScrollOffset > rowPx / 2) 1 else 0
            i.coerceIn(0, (count - 1).coerceAtLeast(0))
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { centre }.distinctUntilChanged().drop(1).collect {
            haptics(Haptic.StepperTick)
            report(it)
        }
    }
    LaunchedEffect(selected) {
        if (!state.isScrollInProgress && centre != selected && selected in 0 until count) state.scrollToItem(selected)
    }
    Box(
        modifier
            .height(row * WHEEL_ROWS)
            .semantics {
                this.contentDescription = contentDescription
                stateDescription = label(centre)
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = cs.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().height(row),
        ) {}
        LazyColumn(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(state),
            contentPadding = PaddingValues(vertical = row * (WHEEL_ROWS / 2)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(count) { i ->
                val isCentre = i == centre
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(row)
                        .clickable(interactionSource = null, indication = null) { scope.launch { state.animateScrollToItem(i) } },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label(i),
                        style = if (isCentre) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                        color = if (isCentre) cs.onSurface else cs.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
