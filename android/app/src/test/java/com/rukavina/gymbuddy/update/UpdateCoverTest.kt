package com.rukavina.gymbuddy.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCoverTest {
    @Test
    fun `covered at launch, gone once Play says nothing`() {
        val cover = UpdateCover()
        assertTrue(cover.covered)
        cover.onDecided(UpdateAction.NONE)
        assertFalse(cover.covered)
    }

    @Test
    fun `forced update stays covered, also when backing out closes the app`() {
        val cover = UpdateCover()
        cover.onDecided(UpdateAction.FORCE)
        cover.onTimeout()
        assertTrue(cover.covered)
        cover.onResult(closing = true)
        assertTrue(cover.covered)
    }

    @Test
    fun `offered update uncovers when you come back`() {
        val cover = UpdateCover()
        cover.onDecided(UpdateAction.PROMPT)
        assertTrue(cover.covered)
        cover.onResult(closing = false)
        assertFalse(cover.covered)
    }

    @Test
    fun `no answer in time shows the app`() {
        val cover = UpdateCover()
        cover.onTimeout()
        assertFalse(cover.covered)
    }

    @Test
    fun `a late forced answer still covers`() {
        val cover = UpdateCover()
        cover.onTimeout()
        cover.onDecided(UpdateAction.FORCE)
        assertTrue(cover.covered)
    }
}
