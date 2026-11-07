package com.rukavina.gymbuddy.ui.you

import org.junit.Assert.assertEquals
import org.junit.Test

class BodyWheelTest {

    @Test
    fun `inches split into feet and inches, rounded and held in range`() {
        assertEquals(5 to 9, BodyWheel.feetInches(68.9f))
        assertEquals(6 to 0, BodyWheel.feetInches(71.6f))
        assertEquals(2 to 0, BodyWheel.feetInches(10f))
        assertEquals(8 to 11, BodyWheel.feetInches(200f))
        assertEquals(69, BodyWheel.totalInches(5, 9))
    }

    @Test
    fun `weight splits into whole and tenth and joins back inside the range`() {
        assertEquals(81 to 5, BodyWheel.split(81.46f))
        assertEquals(82 to 0, BodyWheel.split(81.96f))
        val kg = 20f..700f
        assertEquals(81.5f, BodyWheel.join(81, 5, kg), 0.0001f)
        assertEquals(700f, BodyWheel.join(700, 9, kg), 0.0001f)
        assertEquals(20f, BodyWheel.join(19, 0, kg), 0.0001f)
        assertEquals(44..1543, BodyWheel.wholes(44.09f..1543.24f))
    }
}
