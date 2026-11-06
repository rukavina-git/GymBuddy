package com.rukavina.gymbuddy.ui.you

import com.rukavina.gymbuddy.data.sync.LocalDataCounts
import com.rukavina.gymbuddy.ui.guest.countsText

/** "84 workouts, 6 templates, your exercises, notes and settings for rest." */
internal fun exportSummary(counts: LocalDataCounts?): String {
    val head = if (counts == null || counts.workouts == 0 && counts.templates == 0) {
        "Your profile"
    } else {
        countsText(counts.copy(exercises = 0))
    }
    return "$head, your exercises, notes and settings for rest."
}
