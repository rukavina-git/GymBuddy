package com.rukavina.gymbuddy.update

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdatePolicyTest {

    private fun offer(priority: Int, available: Boolean = true, allowed: Boolean = true, inProgress: Boolean = false) =
        UpdateOffer(available, inProgress, priority, versionCode = 2002, immediateAllowed = allowed)

    @Test
    fun `priority 0 (the default) leaves it to Play`() =
        assertEquals(UpdateAction.NONE, UpdatePolicy.decide(offer(0), null))

    @Test
    fun `priority 4 and 5 force the update`() {
        assertEquals(UpdateAction.FORCE, UpdatePolicy.decide(offer(4), null))
        assertEquals(UpdateAction.FORCE, UpdatePolicy.decide(offer(5), 2002))
    }

    @Test
    fun `priority 1 to 3 prompts once per version`() {
        assertEquals(UpdateAction.PROMPT, UpdatePolicy.decide(offer(3), null))
        assertEquals(UpdateAction.PROMPT, UpdatePolicy.decide(offer(1), 2001))
        assertEquals(UpdateAction.NONE, UpdatePolicy.decide(offer(3), 2002))
    }

    @Test
    fun `no update available does nothing`() =
        assertEquals(UpdateAction.NONE, UpdatePolicy.decide(offer(5, available = false), null))

    @Test
    fun `Play not allowing an immediate update does nothing`() =
        assertEquals(UpdateAction.NONE, UpdatePolicy.decide(offer(5, allowed = false), null))

    @Test
    fun `an update left half way is resumed`() =
        assertEquals(UpdateAction.FORCE, UpdatePolicy.decide(offer(0, available = false, inProgress = true), null))
}
