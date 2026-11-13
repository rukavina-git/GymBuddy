package com.rukavina.gymbuddy.ui.workout

import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.ui.design.KeypadValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SetFieldsTest {

    private val set = WorkoutSet(id = "s", setNumber = 1, weight = "82.5", reps = "8", duration = "60")

    @Test
    fun fieldsFollowTheTrackingTypeInKeypadOrder() {
        assertEquals(listOf(SetField.Weight, SetField.Reps), SetFields.of(ExerciseTrackingType.WEIGHT_REPS))
        assertEquals(listOf(SetField.Distance, SetField.Duration), SetFields.of(ExerciseTrackingType.DISTANCE_DURATION))
        assertEquals(listOf(SetField.Duration), SetFields.of(ExerciseTrackingType.DURATION))
    }

    @Test
    fun stepsPerFieldAndUnit() {
        assertEquals("85", SetFields.stepped(set, SetField.Weight, true, PreferredUnits.METRIC)!!.weight)
        assertEquals("87.5", SetFields.stepped(set, SetField.Weight, true, PreferredUnits.IMPERIAL)!!.weight)
        assertEquals("7", SetFields.stepped(set, SetField.Reps, false, PreferredUnits.METRIC)!!.reps)
        assertEquals("65", SetFields.stepped(set, SetField.Duration, true, PreferredUnits.METRIC)!!.duration)
    }

    @Test
    fun boundsStopTheStepper() {
        val low = set.copy(weight = "0", reps = "1", duration = "5")
        assertNull(SetFields.stepped(low, SetField.Weight, false, PreferredUnits.METRIC))
        assertNull(SetFields.stepped(low, SetField.Reps, false, PreferredUnits.METRIC))
        assertNull(SetFields.stepped(low, SetField.Duration, false, PreferredUnits.METRIC))
        assertFalse(SetFields.canStep(low, SetField.Reps, plus = false))
        assertTrue(SetFields.canStep(low, SetField.Reps, plus = true))
        // Below the floor (typed 1 kg): − goes to the floor, not under it.
        assertEquals("0", SetFields.stepped(set.copy(weight = "1"), SetField.Weight, false, PreferredUnits.METRIC)!!.weight)
        assertNull(SetFields.stepped(set.copy(reps = "999"), SetField.Reps, true, PreferredUnits.METRIC))
    }

    @Test
    fun emptyCountsAsZero() {
        assertEquals("2.5", SetFields.stepped(set.copy(weight = ""), SetField.Weight, true, PreferredUnits.METRIC)!!.weight)
        assertEquals("1", SetFields.stepped(set.copy(reps = ""), SetField.Reps, true, PreferredUnits.METRIC)!!.reps)
    }

    @Test
    fun savableIgnoresWeightSoOldSetsAreKept() {
        assertTrue(SetFields.savable(ExerciseTrackingType.WEIGHT_REPS, set.copy(weight = "")))
        assertFalse(SetFields.savable(ExerciseTrackingType.WEIGHT_REPS, set.copy(weight = "60", reps = "0")))
    }

    @Test
    fun rulesBlockEmptyOrZero() {
        assertEquals(SetRule.AtLeastOneRep, SetFields.rule(SetField.Reps, "0"))
        assertEquals(SetRule.AtLeastOneRep, SetFields.rule(SetField.Reps, ""))
        assertEquals(SetRule.AtLeastOneSecond, SetFields.rule(SetField.Duration, ""))
        assertEquals(SetRule.OverZeroDistance, SetFields.rule(SetField.Distance, "0"))
        assertEquals(SetRule.OverZeroWeight, SetFields.rule(SetField.Weight, ""))
        assertEquals(SetRule.OverZeroWeight, SetFields.rule(SetField.Weight, "0"))
        assertNull(SetFields.rule(SetField.Weight, "0.5"))
        assertEquals(SetRule.OverZeroWeight, SetFields.rule(ExerciseTrackingType.WEIGHT_REPS, set.copy(weight = "0", reps = "0")))
        assertNull(SetFields.rule(ExerciseTrackingType.WEIGHT_REPS, set))
        assertEquals(SetRule.AtLeastOneRep, SetFields.rule(ExerciseTrackingType.REPS_ONLY, set.copy(reps = "")))
    }

    @Test
    fun formatTrimsAndUsesADot() {
        assertEquals("80", SetFields.format(80f))
        assertEquals("82.5", SetFields.format(82.5f))
        assertEquals("1.25", SetFields.format(1.25f))
        assertEquals("0", SetFields.format(0f))
    }

    @Test
    fun storedFormPerKind() {
        assertEquals("90", SetFields.stored(KeypadValue.Kind.Clock, "130"))
        assertEquals("", SetFields.stored(KeypadValue.Kind.Clock, ""))
        assertEquals("12", SetFields.stored(KeypadValue.Kind.Whole, "12"))
        assertEquals("", SetFields.stored(KeypadValue.Kind.Whole, ""))
        assertEquals("82.5", SetFields.stored(KeypadValue.Kind.Decimal, "82,5"))
        assertEquals("", SetFields.stored(KeypadValue.Kind.Decimal, ""))
    }
}
