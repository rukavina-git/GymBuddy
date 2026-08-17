package com.rukavina.gymbuddy.utils

import com.rukavina.gymbuddy.domain.model.PreferredUnits
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConverterTest {

    @Test
    fun `weightToDisplayUnit shows two decimals for imperial`() {
        assertEquals("22.05", UnitConverter.weightToDisplayUnit(10f, PreferredUnits.IMPERIAL))
    }

    @Test
    fun `weightToDisplayUnit trims trailing zeros`() {
        assertEquals("20", UnitConverter.weightToDisplayUnit(20f, PreferredUnits.METRIC))
        assertEquals("20.5", UnitConverter.weightToDisplayUnit(20.5f, PreferredUnits.METRIC))
    }

    @Test
    fun `heightToDisplayUnit shows decimals and trims trailing zeros`() {
        assertEquals("180", UnitConverter.heightToDisplayUnit(180f, PreferredUnits.METRIC))
        assertEquals("70.87", UnitConverter.heightToDisplayUnit(180f, PreferredUnits.IMPERIAL))
    }

    @Test
    fun `display formatting never mutates the stored value - metric to imperial to metric round trip`() {
        val storedKg = 10f

        val displayedLbs = UnitConverter.weightToDisplayUnit(storedKg, PreferredUnits.IMPERIAL)
        assertEquals("22.05", displayedLbs)

        // Switching back to metric re-derives from the original stored kg,
        // never from the rounded display string - so it must land exactly
        // on the original value with no drift.
        val displayedKgAgain = UnitConverter.weightToDisplayUnit(storedKg, PreferredUnits.METRIC)
        assertEquals("10", displayedKgAgain)
    }

    @Test
    fun `weightToMetric does not round imperial input before converting to kg`() {
        val parsed = UnitConverter.weightToMetric("22.05", PreferredUnits.IMPERIAL)
        assertEquals(22.05f / 2.20462f, parsed!!, 0.0001f)
    }

    @Test
    fun `heightToMetric does not round imperial input before converting to cm`() {
        val parsed = UnitConverter.heightToMetric("70.87", PreferredUnits.IMPERIAL)
        assertEquals(70.87f * 2.54f, parsed!!, 0.0001f)
    }

    // The round trip that matters in practice: a user typing into an
    // IMPERIAL-mode field (ActiveWorkoutViewModel.saveWorkout, the Edit*
    // Screens) goes through weightToMetric/heightToMetric for storage, then
    // back through weightToDisplayUnit/heightToDisplayUnit to redisplay -
    // never through kgToLbs/lbsToKg directly. If those two conversions
    // don't invert each other, the user sees their own input silently
    // drift on every save/reload.

    @Test
    fun `weight typed in imperial round-trips back to the same displayed value after storage`() {
        val typedLbs = "185"

        val storedKg = UnitConverter.weightToMetric(typedLbs, PreferredUnits.IMPERIAL)
        val redisplayedLbs = UnitConverter.weightToDisplayUnit(storedKg, PreferredUnits.IMPERIAL)

        assertEquals(typedLbs, redisplayedLbs)
    }

    @Test
    fun `weight typed in metric round-trips back to the same displayed value after storage`() {
        val typedKg = "82.5"

        val storedKg = UnitConverter.weightToMetric(typedKg, PreferredUnits.METRIC)
        val redisplayedKg = UnitConverter.weightToDisplayUnit(storedKg, PreferredUnits.METRIC)

        assertEquals(typedKg, redisplayedKg)
    }

    @Test
    fun `height typed in imperial round-trips back to the same displayed value after storage`() {
        val typedInches = "70.87"

        val storedCm = UnitConverter.heightToMetric(typedInches, PreferredUnits.IMPERIAL)
        val redisplayedInches = UnitConverter.heightToDisplayUnit(storedCm, PreferredUnits.IMPERIAL)

        assertEquals(typedInches, redisplayedInches)
    }

    @Test
    fun `a value stored in one unit and redisplayed in the other converts, not just reformats`() {
        // 100 kg stored, then the user flips their preference to imperial.
        val storedKg = 100f

        val displayedLbs = UnitConverter.weightToDisplayUnit(storedKg, PreferredUnits.IMPERIAL)

        assertEquals("220.46", displayedLbs)
    }
}
