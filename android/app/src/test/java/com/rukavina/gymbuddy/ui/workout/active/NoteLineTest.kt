package com.rukavina.gymbuddy.ui.workout.active

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Owner rule for the one note line (R13-13/14). */
class NoteLineTest {

    @Test
    fun planNoteWinsAndYourNoteWaitsBehindTheIcon() {
        assertEquals(NoteLine("Pause 1 s", icon = true, dot = true), noteLine(" Pause 1 s ", "Notch 3"))
        assertEquals(NoteLine("Pause 1 s", icon = true, dot = false), noteLine("Pause 1 s", null))
    }

    @Test
    fun onlyYourNoteIsTheLine() {
        assertEquals(NoteLine("Notch 3", icon = false, dot = false), noteLine("  ", "Notch 3"))
    }

    @Test
    fun neitherShowsNothing() {
        assertNull(noteLine(null, " "))
    }
}
