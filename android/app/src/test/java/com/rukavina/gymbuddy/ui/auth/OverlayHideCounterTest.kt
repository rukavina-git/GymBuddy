package com.rukavina.gymbuddy.ui.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayHideCounterTest {
    private val calls = mutableListOf<Pair<String, Boolean>>()
    private val counter = OverlayHideCounter<String> { key, hide -> calls += key to hide }

    @Test
    fun `first holder hides, last one shows again`() {
        counter.acquire("w")
        counter.acquire("w")
        counter.release("w")
        assertEquals(listOf("w" to true), calls)
        counter.release("w")
        assertEquals(listOf("w" to true, "w" to false), calls)
    }

    @Test
    fun `windows are counted apart`() {
        counter.acquire("a")
        counter.acquire("b")
        counter.release("a")
        assertEquals(listOf("a" to true, "b" to true, "a" to false), calls)
    }

    @Test
    fun `extra release does nothing`() {
        counter.release("w")
        counter.acquire("w")
        counter.release("w")
        counter.release("w")
        assertEquals(listOf("w" to true, "w" to false), calls)
    }
}
