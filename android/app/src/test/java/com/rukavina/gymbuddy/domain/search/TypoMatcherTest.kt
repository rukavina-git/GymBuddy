package com.rukavina.gymbuddy.domain.search

import com.rukavina.gymbuddy.domain.search.TypoMatcher.Match
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TypoMatcherTest {

    @Test
    fun emptyOrBlankMatchesEverything() {
        assertEquals(Match.Exact, TypoMatcher.match("", "Barbell Bench Press"))
        assertEquals(Match.Exact, TypoMatcher.match("   ", "Push-Up"))
    }

    @Test
    fun substringIsExactAndIgnoresCase() {
        assertEquals(Match.Exact, TypoMatcher.match("BENCH", "Barbell Bench Press"))
        assertEquals(Match.Exact, TypoMatcher.match("bell ben", "Barbell Bench Press"))
    }

    @Test
    fun oneTypoPerWordMatches() {
        assertEquals(Match.Typo, TypoMatcher.match("bnch", "Barbell Bench Press")) // missing letter
        assertEquals(Match.Typo, TypoMatcher.match("bemch", "Dumbbell Bench Press")) // wrong letter
        // A swap is 2 edits on the word, but one from its start ("benhc" → "benc").
        assertEquals(Match.Typo, TypoMatcher.match("benhc press", "Close-Grip Bench Press"))
        assertEquals(Match.Typo, TypoMatcher.match("squta", "Back Squat"))
    }

    @Test
    fun twoTyposOrShortWordsDoNotMatch() {
        assertNull(TypoMatcher.match("bxxch", "Barbell Bench Press"))
        assertNull(TypoMatcher.match("bx", "Barbell Bench Press"))
        assertNull(TypoMatcher.match("bench curl", "Barbell Bench Press"))
    }

    @Test
    fun accentsAreIgnored() {
        assertEquals(Match.Exact, TypoMatcher.match("presse", "Développé Presse"))
        assertEquals(Match.Exact, TypoMatcher.match("développé", "Developpe couché"))
        assertEquals(Match.Typo, TypoMatcher.match("couhe", "Développé couché"))
    }

    @Test
    fun withinOne() {
        assertTrue(TypoMatcher.withinOne("bench", "bench"))
        assertTrue(TypoMatcher.withinOne("bnch", "bench"))
        assertTrue(TypoMatcher.withinOne("benchx", "bench"))
        assertTrue(TypoMatcher.withinOne("bunch", "bench"))
        assertFalse(TypoMatcher.withinOne("bnc", "bench"))
        assertFalse(TypoMatcher.withinOne("ebnch", "bench"))
    }
}
