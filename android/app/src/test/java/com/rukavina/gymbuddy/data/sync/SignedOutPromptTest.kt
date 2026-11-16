package com.rukavina.gymbuddy.data.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Plan 9 step 04a: when the signed-out dialog opens by itself. */
class SignedOutPromptTest {

    private class MemoryStore : SignedOutPrompt.Store {
        override var continues = 0
        override var lastContinueAt = 0L
    }

    private var now = 1_000_000L
    private val store = MemoryStore()
    private val prompt = SignedOutPrompt(store) { now }
    private val hour = 60 * 60 * 1000L

    @Test
    fun `the first time it always shows`() = assertTrue(prompt.shouldShow())

    @Test
    fun `after Continue it waits 48 hours`() {
        prompt.onContinue()
        now += 47 * hour
        assertFalse(prompt.shouldShow())
        now += hour
        assertTrue(prompt.shouldShow())
    }

    @Test
    fun `after three Continues it never shows again`() {
        repeat(3) {
            assertTrue(prompt.shouldShow())
            prompt.onContinue()
            now += 48 * hour
        }
        now += 1000 * hour
        assertFalse(prompt.shouldShow())
    }

    @Test
    fun `a sign-in starts over`() {
        repeat(3) { prompt.onContinue() }
        prompt.reset()
        assertTrue(prompt.shouldShow())
    }
}
