package com.rukavina.gymbuddy.ui.components

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.math.abs

@Composable
fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    label: String = "",
    itemHeight: Dp = 50.dp
) {
    val items = range.toList()
    val visibleItemsCount = 5
    val startIndex = maxOf(0, items.indexOf(value) - visibleItemsCount / 2)

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .map { (index, offset) ->
                val offsetInItems = offset / itemHeightPx
                val currentIndex = index + if (offsetInItems > 0.5f) 1 else 0
                items.getOrNull(currentIndex) ?: value
            }
            .distinctUntilChanged()
            .collect { onValueChange(it) }
    }

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight * visibleItemsCount)
                .fadingEdges()
        ) {
            items(visibleItemsCount / 2) {
                Box(modifier = Modifier.height(itemHeight))
            }

            items(items.size) { index ->
                val item = items[index]
                NumberPickerItem(
                    value = item,
                    label = label,
                    itemHeight = itemHeight,
                    listState = listState,
                    itemIndex = index,
                    itemHeightPx = itemHeightPx
                )
            }

            items(visibleItemsCount / 2) {
                Box(modifier = Modifier.height(itemHeight))
            }
        }

        // Selection indicators
        HorizontalDivider(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = -itemHeight / 2),
            color = MaterialTheme.colorScheme.primary
        )
        HorizontalDivider(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = itemHeight / 2),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun DecimalNumberPicker(
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    step: Float = 0.1f,
    modifier: Modifier = Modifier,
    label: String = "",
    itemHeight: Dp = 50.dp
) {
    val items = generateSequence(range.start) { it + step }
        .takeWhile { it <= range.endInclusive }
        .map { (it * 10).toInt() / 10f }
        .toList()

    val visibleItemsCount = 5
    val currentIndex = items.indexOfFirst { abs(it - value) < step / 2 }.coerceAtLeast(0)
    val startIndex = maxOf(0, currentIndex - visibleItemsCount / 2)

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .map { (index, offset) ->
                val offsetInItems = offset / itemHeightPx
                val currentIdx = index + if (offsetInItems > 0.5f) 1 else 0
                items.getOrNull(currentIdx) ?: value
            }
            .distinctUntilChanged()
            .collect { onValueChange(it) }
    }

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight * visibleItemsCount)
                .fadingEdges()
        ) {
            items(visibleItemsCount / 2) {
                Box(modifier = Modifier.height(itemHeight))
            }

            items(items.size) { index ->
                val item = items[index]
                DecimalNumberPickerItem(
                    value = item,
                    label = label,
                    itemHeight = itemHeight,
                    listState = listState,
                    itemIndex = index,
                    itemHeightPx = itemHeightPx
                )
            }

            items(visibleItemsCount / 2) {
                Box(modifier = Modifier.height(itemHeight))
            }
        }

        // Selection indicators
        HorizontalDivider(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = -itemHeight / 2),
            color = MaterialTheme.colorScheme.primary
        )
        HorizontalDivider(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = itemHeight / 2),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun NumberPickerItem(
    value: Int,
    label: String,
    itemHeight: Dp,
    listState: androidx.compose.foundation.lazy.LazyListState,
    itemIndex: Int,
    itemHeightPx: Float
) {
    val density = LocalDensity.current
    val layoutInfo = listState.layoutInfo

    val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == itemIndex + 2 }
    val delta = itemInfo?.let {
        val itemCenter = it.offset + it.size / 2f
        val containerCenter = layoutInfo.viewportEndOffset / 2f
        abs(itemCenter - containerCenter) / itemHeightPx
    } ?: 1f

    val alpha = (1f - delta.coerceIn(0f, 1f) * 0.7f).coerceIn(0.3f, 1f)
    val scale = (1f - delta.coerceIn(0f, 1f) * 0.3f).coerceIn(0.7f, 1f)

    Box(
        modifier = Modifier
            .height(itemHeight)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (label.isNotEmpty()) "$value $label" else value.toString(),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = (24 * scale).sp),
            color = LocalContentColor.current.copy(alpha = alpha),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DecimalNumberPickerItem(
    value: Float,
    label: String,
    itemHeight: Dp,
    listState: androidx.compose.foundation.lazy.LazyListState,
    itemIndex: Int,
    itemHeightPx: Float
) {
    val density = LocalDensity.current
    val layoutInfo = listState.layoutInfo

    val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == itemIndex + 2 }
    val delta = itemInfo?.let {
        val itemCenter = it.offset + it.size / 2f
        val containerCenter = layoutInfo.viewportEndOffset / 2f
        abs(itemCenter - containerCenter) / itemHeightPx
    } ?: 1f

    val alpha = (1f - delta.coerceIn(0f, 1f) * 0.7f).coerceIn(0.3f, 1f)
    val scale = (1f - delta.coerceIn(0f, 1f) * 0.3f).coerceIn(0.7f, 1f)

    Box(
        modifier = Modifier
            .height(itemHeight)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (label.isNotEmpty()) "%.1f %s".format(value, label) else "%.1f".format(value),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = (24 * scale).sp),
            color = LocalContentColor.current.copy(alpha = alpha),
            textAlign = TextAlign.Center
        )
    }
}

private fun Modifier.fadingEdges(): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val fadeHeight = size.height * 0.2f

        // Top fade
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black),
                startY = 0f,
                endY = fadeHeight
            ),
            blendMode = BlendMode.DstIn
        )

        // Bottom fade
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Black, Color.Transparent),
                startY = size.height - fadeHeight,
                endY = size.height
            ),
            blendMode = BlendMode.DstIn
        )
    }
