package com.rukavina.gymbuddy.ui.design

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.rememberTextMeasurer
import com.rukavina.gymbuddy.ui.theme.TABULAR_FIGURES
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.Durations
import com.rukavina.gymbuddy.ui.theme.FullShape
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import com.rukavina.gymbuddy.ui.theme.Haptic
import com.rukavina.gymbuddy.ui.theme.motionTween
import com.rukavina.gymbuddy.ui.theme.rememberHaptics
import kotlinx.coroutines.delay

/** Raw sizes from "01 System" (DESIGN.md §4). */
private val SetMark = 28.dp
private val RestIcon = 16.dp
private val StepperWidth = 64.dp
/** Panels narrower than this use the smaller number style (360dp phones). */
private val WideNumberMinWidth = 176.dp

enum class SetState { Done, Upcoming }

/**
 * One set in the workout list. Done: surface fill, lime check. Upcoming:
 * outlined, muted value. Min 48 (D13), radius 12. Swipe comes in 7.6.
 */
@Composable
fun SetRow(
    setLabel: String,
    value: String,
    state: SetState,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    val shape = MaterialTheme.shapes.small
    val done = state == SetState.Done
    val stateText = stringResource(if (done) R.string.state_set_done else R.string.state_set_upcoming)
    Surface(
        shape = shape,
        color = if (done) cs.surfaceContainer else Color.Transparent,
        border = if (done) null else BorderStroke(d.strokeDivider, cs.outline),
        modifier = modifier.semantics(mergeDescendants = true) { stateDescription = stateText },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = d.rowSet)
                .padding(horizontal = d.stack, vertical = d.stackTight),
            horizontalArrangement = Arrangement.spacedBy(d.group),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val markModifier = Modifier.size(SetMark)
            if (done) {
                Surface(shape = FullShape, color = GymBuddyTheme.colors.limeTint, modifier = markModifier) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(painterResource(AppIcons.Ui.Check), null, Modifier.size(d.iconSmall), tint = cs.primary)
                    }
                }
            } else {
                Box(markModifier.border(d.strokeOutline, cs.outline, FullShape))
            }
            Text(setLabel, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                color = if (done) cs.onSurface else cs.onSurfaceVariant,
            )
        }
    }
}

/**
 * Rest between sets: always shown, one quiet muted line (DESIGN.md §1).
 * Figma draws the value in text colour; DESIGN.md wins (owner, 02b).
 */
@Composable
fun RestLine(value: String, modifier: Modifier = Modifier) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val d = GymBuddyTheme.dimens
    Row(
        modifier.heightIn(min = d.target).semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(d.stackTight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(AppIcons.Ui.Timer), null, Modifier.size(RestIcon), tint = muted)
        Text(value, style = MaterialTheme.typography.bodySmall, color = muted)
    }
}

enum class StepperSign { Minus, Plus }

/**
 * − / + key, 64×56, radius 16. One tick haptic on the first press; held,
 * it repeats and speeds up (motion.md #3b), with one reject when it stops
 * at the floor or ceiling ([enabled] turns false). [contentDescription]
 * defaults to "Decrease"/"Increase".
 */
@Composable
fun StepperKey(
    sign: StepperSign,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    onHoldChange: (Boolean) -> Unit = {},
) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    val shape = MaterialTheme.shapes.medium
    val haptics = rememberHaptics()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val click by rememberUpdatedState(onClick)
    val canRepeat by rememberUpdatedState(enabled)
    val holdChange by rememberUpdatedState(onHoldChange)
    // Set while a hold repeated, so the release doesn't add one more step.
    var repeated by remember { mutableStateOf(false) }
    LaunchedEffect(pressed) {
        if (!pressed) {
            holdChange(false)
            return@LaunchedEffect
        }
        repeated = false
        delay(HOLD_START_MS)
        holdChange(true)
        haptics(Haptic.StepperTick)
        var interval = HOLD_FIRST_MS
        var steps = 0
        while (true) {
            if (!canRepeat) {
                haptics(Haptic.Reject)
                break
            }
            repeated = true
            click()
            steps++
            if (steps % HOLD_TICK_EVERY == 0) haptics(Haptic.StepperTick)
            delay(interval)
            interval = (interval * HOLD_SPEEDUP).toLong().coerceAtLeast(HOLD_FASTEST_MS)
        }
    }
    val icon = if (sign == StepperSign.Minus) AppIcons.Ui.Remove else AppIcons.Ui.Add
    val cd = contentDescription
        ?: stringResource(if (sign == StepperSign.Minus) R.string.cd_decrease else R.string.cd_increase)
    Surface(
        onClick = {
            if (repeated) {
                repeated = false
            } else {
                haptics(Haptic.StepperTick)
                onClick()
            }
        },
        enabled = enabled,
        shape = shape,
        color = if (pressed) cs.surfaceContainerHigh else cs.surfaceContainerHighest,
        contentColor = if (enabled) cs.onSurface else GymBuddyTheme.colors.disabled,
        interactionSource = interaction,
        modifier = modifier
            .pressScale(interaction)
            .focusRing(interaction, shape)
            .semantics { role = Role.Button },
    ) {
        Box(Modifier.widthIn(min = StepperWidth).height(d.target), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), cd, Modifier.size(d.icon))
        }
    }
}

/** Hold to repeat: raw ms (motion.md #3b has no token). */
private const val HOLD_START_MS = 400L
private const val HOLD_FIRST_MS = 150L
// F20: 50 ms ran 0 -> 167.5 kg in 4.5 s; 110 ms keeps a long hold readable.
private const val HOLD_FASTEST_MS = 110L
private const val HOLD_SPEEDUP = 0.9
/** One haptic tick every this many repeated steps. */
private const val HOLD_TICK_EVERY = 5

/**
 * Weight or reps on the set card. Tapping the number opens the keypad
 * ([onClick]); [editing] draws the lime line and shows [step] ("± 2.5 kg").
 * The number rolls up on + and down on − (motion.md #3), not while held.
 * [minusDescription]/[plusDescription] name the value for TalkBack
 * ("Decrease weight").
 */
@Composable
fun ValuePanel(
    number: String,
    unit: String,
    editing: Boolean,
    onClick: () -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    step: String? = null,
    minusEnabled: Boolean = true,
    plusEnabled: Boolean = true,
    minusDescription: String? = null,
    plusDescription: String? = null,
) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    val shape = MaterialTheme.shapes.medium
    var held by remember { mutableStateOf(false) }
    Surface(
        shape = shape,
        color = cs.surfaceContainerHigh,
        border = if (editing) BorderStroke(d.strokeEmphasis, cs.primary) else null,
        modifier = modifier,
    ) {
        BoxWithConstraints {
            // Big digits only where "102.5 kg" fits; 360dp phones get the smaller style.
            val numberStyle = if (maxWidth >= WideNumberMinWidth) {
                MaterialTheme.typography.displayMedium
            } else {
                MaterialTheme.typography.headlineLarge.copy(fontFeatureSettings = TABULAR_FIGURES)
            }
            val unitStyle = MaterialTheme.typography.titleSmall
            Column(
                Modifier.padding(d.stack),
                verticalArrangement = Arrangement.spacedBy(d.stackTight),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = d.target)
                        .selectable(selected = editing, onClick = onClick, role = Role.Button),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // The number box is at least as wide as "00.0", right-aligned, so
                    // the unit stays put while the number changes ("17.5" -> "20", 8 -> 10).
                    Box(Modifier.widthIn(min = numberSlot(numberStyle)), contentAlignment = Alignment.CenterEnd) {
                        AnimatedContent(
                            targetState = number,
                            transitionSpec = {
                                // F20: no size animation and no clip, or the leading digit
                                // gets cut while the width changes ("87.5" read "37.5").
                                val noResize = SizeTransform(clip = false) { _, _ -> snap() }
                                if (held) {
                                    (EnterTransition.None togetherWith ExitTransition.None) using noResize
                                } else {
                                    val up = (initialState.toFloatOrNull() ?: 0f) <= (targetState.toFloatOrNull() ?: 0f)
                                    val tween = motionTween<IntOffset>(Durations.SHORT4)
                                    ((slideInVertically(tween) { if (up) it else -it } + fadeIn(motionTween(Durations.SHORT4))) togetherWith
                                        (slideOutVertically(tween) { if (up) -it else it } + fadeOut(motionTween(Durations.SHORT4)))) using noResize
                                }
                            },
                            contentAlignment = Alignment.CenterEnd,
                            label = "odometer",
                        ) { value ->
                            Text(value, style = numberStyle, maxLines = 1, softWrap = false)
                        }
                    }
                    // AnimatedContent passes no baseline up, so alignByBaseline put the unit
                    // on its edge. Line the baselines up by measuring both styles instead.
                    Text(
                        unit,
                        style = unitStyle,
                        color = cs.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier
                            .padding(start = d.hairline)
                            .baselineOffset(numberStyle, unitStyle),
                    )
                }
                if (editing && step != null) {
                    Text(step, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.stackTight)) {
                    StepperKey(
                        StepperSign.Minus, onMinus, enabled = minusEnabled,
                        contentDescription = minusDescription, onHoldChange = { held = it },
                        modifier = Modifier.weight(1f),
                    )
                    StepperKey(
                        StepperSign.Plus, onPlus, enabled = plusEnabled,
                        contentDescription = plusDescription, onHoldChange = { held = it },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * Moves the unit down so its baseline sits on the number's, both centred
 * in the row: half the difference of the two "top to baseline" gaps
 * shifted by half the height difference.
 */
@Composable
private fun Modifier.baselineOffset(numberStyle: TextStyle, unitStyle: TextStyle): Modifier {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val offset = remember(numberStyle, unitStyle, density) {
        val n = measurer.measure("0", numberStyle)
        val u = measurer.measure("0", unitStyle)
        val dy = baselineShift(n.firstBaseline, n.size.height.toFloat(), u.firstBaseline, u.size.height.toFloat())
        with(density) { dy.toDp() }
    }
    return this.offset(y = offset)
}

/**
 * How far to move a vertically centred unit so its baseline meets the
 * centred number's: (number baseline - its centre) - (unit baseline - its centre).
 */
internal fun baselineShift(numberBaseline: Float, numberHeight: Float, unitBaseline: Float, unitHeight: Float): Float =
    (numberBaseline - numberHeight / 2f) - (unitBaseline - unitHeight / 2f)

/** Width of [NUMBER_SLOT] in [style]: the number box never gets narrower. */
@Composable
private fun numberSlot(style: TextStyle): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(style, density) {
        with(density) { measurer.measure(NUMBER_SLOT, style).size.width.toDp() }
    }
}

/** Covers 0–999.5 kg and any reps, so the box never resizes mid-hold (F20). */
private const val NUMBER_SLOT = "000.0"


/** Two panels of two 48dp keys each need this much; narrower cards stack them. */
private val SideBySideMinWidth = 264.dp

/**
 * The current set: title, optional muted [hint] on the right ("Tap a
 * number to type"), and one [ValuePanel] per value, cobalt ring ("you
 * are here"), radius 20. Panels sit side by side, or stacked when the
 * card is too narrow for two pairs of − / + keys. Each panel lambda gets
 * the modifier to put on its ValuePanel.
 */
@Composable
fun SetCard(
    title: String,
    panels: List<@Composable (Modifier) -> Unit>,
    modifier: Modifier = Modifier,
    hint: String? = null,
    titleContent: @Composable (String) -> Unit = { Text(it, style = MaterialTheme.typography.titleMedium) },
) {
    val d = GymBuddyTheme.dimens
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(d.strokeEmphasis, GymBuddyTheme.colors.cobalt),
        modifier = modifier,
    ) {
        Column(Modifier.padding(d.insetCard), verticalArrangement = Arrangement.spacedBy(d.stack)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { titleContent(title) }
                if (hint != null) {
                    Text(hint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (panels.size > 1 && maxWidth < SideBySideMinWidth) {
                    Column(verticalArrangement = Arrangement.spacedBy(d.stackTight)) {
                        panels.forEach { it(Modifier.fillMaxWidth()) }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(d.stackTight)) {
                        panels.forEach { it(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}
