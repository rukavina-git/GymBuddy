package com.rukavina.gymbuddy.ui.you

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.ui.design.WheelPicker
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import com.rukavina.gymbuddy.ui.design.keypadSeparator
import kotlin.math.roundToInt

/** F6 wheel maths: pure, so it is unit tested. */
internal object BodyWheel {
    const val INCHES_PER_FOOT = 12
    val FEET = 2..8

    /** Total inches → (ft, in), rounded to the inch, inside [FEET]. */
    fun feetInches(totalInches: Float): Pair<Int, Int> {
        val total = totalInches.roundToInt().coerceIn(FEET.first * INCHES_PER_FOOT, FEET.last * INCHES_PER_FOOT + 11)
        return total / INCHES_PER_FOOT to total % INCHES_PER_FOOT
    }

    fun totalInches(feet: Int, inches: Int): Int = feet * INCHES_PER_FOOT + inches

    /** 81.46 → (81, 5): whole and tenth of the value rounded to 0.1. */
    fun split(value: Float): Pair<Int, Int> {
        val tenths = (value * 10f).roundToInt()
        return tenths / 10 to tenths % 10
    }

    /** (81, 5) → 81.5, held inside [range]. */
    fun join(whole: Int, tenth: Int, range: ClosedFloatingPointRange<Float>): Float =
        ((whole * 10 + tenth) / 10f).coerceIn(range.start, range.endInclusive)

    /** Whole numbers the wheel offers for [range]. */
    fun wholes(range: ClosedFloatingPointRange<Float>): IntRange = range.start.toInt()..range.endInclusive.toInt()
}

/** Metric height: one cm wheel over [range]. */
@Composable
internal fun CmWheel(cm: Int, range: IntRange, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val unit = stringResource(R.string.unit_cm)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        WheelPicker(
            count = range.count(),
            selected = cm - range.first,
            onSelected = { onChange(range.first + it) },
            label = { (range.first + it).toString() },
            contentDescription = stringResource(R.string.profile_height),
            modifier = Modifier.width(WHEEL_WIDTH),
        )
        UnitText(unit)
    }
}

/** Imperial height: ft and in wheels. */
@Composable
internal fun FeetInchesWheels(totalInches: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val (ft, inch) = BodyWheel.feetInches(totalInches.toFloat())
    val feet = BodyWheel.FEET
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        WheelPicker(
            count = feet.count(),
            selected = ft - feet.first,
            onSelected = { onChange(BodyWheel.totalInches(feet.first + it, inch)) },
            label = { (feet.first + it).toString() },
            contentDescription = stringResource(R.string.wheel_feet),
            modifier = Modifier.width(WHEEL_WIDTH),
        )
        UnitText(stringResource(R.string.unit_ft))
        WheelPicker(
            count = BodyWheel.INCHES_PER_FOOT,
            selected = inch,
            onSelected = { onChange(BodyWheel.totalInches(ft, it)) },
            label = { it.toString() },
            contentDescription = stringResource(R.string.wheel_inches),
            modifier = Modifier.width(WHEEL_WIDTH),
        )
        UnitText(stringResource(R.string.unit_in))
    }
}

/** Weight: whole + tenth wheels in the display unit, held inside [range]. */
@Composable
internal fun WeightWheels(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    contentDescription: String,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val (whole, tenth) = BodyWheel.split(value)
    val wholes = BodyWheel.wholes(range)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        WheelPicker(
            count = wholes.count(),
            selected = whole - wholes.first,
            onSelected = { onChange(BodyWheel.join(wholes.first + it, tenth, range)) },
            label = { (wholes.first + it).toString() },
            contentDescription = contentDescription,
            modifier = Modifier.width(WHEEL_WIDTH),
        )
        Text(keypadSeparator().toString(), style = MaterialTheme.typography.headlineSmall)
        WheelPicker(
            count = 10,
            selected = tenth,
            onSelected = { onChange(BodyWheel.join(whole, it, range)) },
            label = { it.toString() },
            contentDescription = stringResource(R.string.wheel_tenths),
            modifier = Modifier.width(TENTH_WIDTH),
        )
        UnitText(unit)
    }
}

@Composable
private fun UnitText(unit: String) {
    Text(
        unit,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.width(UNIT_WIDTH).padding(start = GymBuddyTheme.dimens.stackTight),
    )
}

private val WHEEL_WIDTH = 88.dp
private val TENTH_WIDTH = 56.dp
private val UNIT_WIDTH = 48.dp
