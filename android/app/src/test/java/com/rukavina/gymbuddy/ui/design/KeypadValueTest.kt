package com.rukavina.gymbuddy.ui.design

import com.rukavina.gymbuddy.ui.design.KeypadValue.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeypadValueTest {

    private fun type(kind: Kind, vararg keys: KeypadInput, separator: Char = '.'): String =
        keys.fold("") { text, key -> KeypadValue.apply(text, key, kind, separator) }

    private fun d(n: Int) = KeypadInput.Digit(n)

    @Test
    fun parseAcceptsDotAndComma() {
        assertEquals(12.5f, KeypadValue.parse("12.5"))
        assertEquals(12.5f, KeypadValue.parse("12,5"))
        assertEquals(80f, KeypadValue.parse("80"))
        assertNull(KeypadValue.parse(""))
        assertNull(KeypadValue.parse("."))
    }

    @Test
    fun decimalUsesTheGivenSeparatorOnceAndLimitsDecimals() {
        assertEquals("22,5", type(Kind.Decimal, d(2), d(2), KeypadInput.Decimal, d(5), separator = ','))
        assertEquals("0.5", type(Kind.Decimal, KeypadInput.Decimal, d(5)))
        assertEquals("1.25", type(Kind.Decimal, d(1), KeypadInput.Decimal, KeypadInput.Decimal, d(2), d(5), d(9)))
    }

    @Test
    fun wholeIgnoresDecimalAndLeadingZeroAndCapsLength() {
        assertEquals("12", type(Kind.Whole, d(1), KeypadInput.Decimal, d(2)))
        assertEquals("7", type(Kind.Whole, d(0), d(7)))
        assertEquals("1234", type(Kind.Whole, d(1), d(2), d(3), d(4), d(5)))
    }

    @Test
    fun backspaceAndButtonsKeepOrDropText() {
        assertEquals("1", type(Kind.Whole, d(1), d(2), KeypadInput.Backspace))
        assertEquals("", type(Kind.Whole, KeypadInput.Backspace))
        assertEquals("3", type(Kind.Whole, d(3), KeypadInput.Next, KeypadInput.Cancel))
    }

    @Test
    fun clockTypesFromTheRight() {
        val digits = type(Kind.Clock, d(1), d(3), d(0))
        assertEquals("130", digits)
        assertEquals("1:30", KeypadValue.clockText(digits))
        assertEquals(90, KeypadValue.parseClock(digits))
        assertEquals("0:05", KeypadValue.clockText("5"))
        assertEquals("0:00", KeypadValue.clockText(""))
        assertEquals("", type(Kind.Clock, d(0)))
        assertNull(KeypadValue.parseClock(""))
        // 90 typed means 90 s.
        assertEquals(90, KeypadValue.parseClock("90"))
    }

    @Test
    fun clockDigitsRoundTrip() {
        assertEquals("200", KeypadValue.clockDigits(120))
        assertEquals("", KeypadValue.clockDigits(null))
        assertEquals(120, KeypadValue.parseClock(KeypadValue.clockDigits(120)))
    }
}
