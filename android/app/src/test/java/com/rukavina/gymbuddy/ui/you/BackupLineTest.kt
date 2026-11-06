package com.rukavina.gymbuddy.ui.you

import org.junit.Assert.assertEquals
import org.junit.Test

/** The You card's backup line (R15-2, R15-12) and dialog text joins. */
class BackupLineTest {

    private val now = 10 * BACKUP_FAILED_AFTER_MS

    @Test
    fun `nothing waiting is backed up, with the last sync if known`() {
        assertEquals(BackupLine.BackedUp(null), backupLine(0, null, null, now))
        assertEquals(BackupLine.BackedUp(now - 1_000), backupLine(0, null, now - 1_000, now))
    }

    @Test
    fun `changes under a day old are waiting`() {
        assertEquals(BackupLine.Waiting(3), backupLine(3, now - BACKUP_FAILED_AFTER_MS, now - 5, now))
    }

    @Test
    fun `a change older than a day means the backup failed`() {
        assertEquals(BackupLine.Failed(2), backupLine(2, now - BACKUP_FAILED_AFTER_MS - 1, null, now))
    }

    @Test
    fun `lines join with commas and a final and`() {
        assertEquals("", joinLines(emptyList()))
        assertEquals("1 workout session", joinLines(listOf("1 workout session")))
        assertEquals("a, b and c", joinLines(listOf("a", "b", "c")))
    }
}
