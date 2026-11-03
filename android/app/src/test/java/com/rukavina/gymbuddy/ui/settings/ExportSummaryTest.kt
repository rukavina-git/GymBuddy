package com.rukavina.gymbuddy.ui.settings

import com.rukavina.gymbuddy.data.sync.LocalDataCounts
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportSummaryTest {

    private val tail = ", your exercises, notes and settings for rest."

    @Test
    fun noCountsYet() = assertEquals("Your profile$tail", exportSummary(null))

    @Test
    fun nothingButExercises() =
        assertEquals("Your profile$tail", exportSummary(LocalDataCounts(0, 0, 4)))

    @Test
    fun workoutsAndTemplatesExerciseCountLeftOut() =
        assertEquals("84 workouts and 6 templates$tail", exportSummary(LocalDataCounts(84, 6, 3)))
}
