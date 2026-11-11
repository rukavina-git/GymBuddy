package com.rukavina.gymbuddy.ui.you.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** F3b: the inline profile name only saves a trimmed, valid, changed name. */
class NameToSaveTest {
    @Test fun trimsAndSavesChangedName() = assertEquals("Ivan", nameToSave("  Ivan ", "Karlo"))
    @Test fun blankReverts() = assertNull(nameToSave("   ", "Karlo"))
    @Test fun unchangedSavesNothing() = assertNull(nameToSave(" Karlo ", "Karlo"))
    @Test fun tooLongSavesNothing() = assertNull(nameToSave("x".repeat(ProfileViewModel.MAX_NAME + 1), "Karlo"))
    @Test fun maxLengthSaves() = assertEquals("x".repeat(ProfileViewModel.MAX_NAME), nameToSave("x".repeat(ProfileViewModel.MAX_NAME), "Karlo"))
}
