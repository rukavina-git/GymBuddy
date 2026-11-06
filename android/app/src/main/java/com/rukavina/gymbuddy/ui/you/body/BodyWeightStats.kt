package com.rukavina.gymbuddy.ui.you.body

import com.rukavina.gymbuddy.domain.model.BodyWeightEntry

/** Chart ranges on the Body weight page (R15-5). */
enum class WeightRange(val days: Long?) { Days30(30), Days90(90), Year(365), All(null) }

/** Pure maths for the Body weight page (7.10b). Entries come newest first. */
object BodyWeightStats {
    private const val DAY_MS = 86_400_000L
    const val CHANGE_DAYS = 30L

    /** Entries measured within [range] of [now]; All keeps every one. */
    fun inRange(entries: List<BodyWeightEntry>, range: WeightRange, now: Long): List<BodyWeightEntry> {
        val days = range.days ?: return entries
        val from = now - days * DAY_MS
        return entries.filter { it.measuredAt >= from }
    }

    /**
     * Newest weight minus the newest one at least 30 days older (kg).
     * Null when there is no entry that old yet.
     */
    fun change(entries: List<BodyWeightEntry>): Float? {
        val latest = entries.firstOrNull() ?: return null
        val before = entries.firstOrNull { it.measuredAt <= latest.measuredAt - CHANGE_DAYS * DAY_MS } ?: return null
        return latest.weightKg - before.weightKg
    }
}
