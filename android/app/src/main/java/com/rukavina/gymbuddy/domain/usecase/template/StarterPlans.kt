package com.rukavina.gymbuddy.domain.usecase.template

import com.rukavina.gymbuddy.domain.model.ExperienceLevel
import com.rukavina.gymbuddy.domain.model.StarterPlan
import com.rukavina.gymbuddy.domain.model.TrainingDays
import com.rukavina.gymbuddy.domain.model.WorkoutSession

/** Pure plan rules (Plan 6 step 06). No storage, no Android. */
object StarterPlans {

    /**
     * The plan onboarding suggests: the one marked for [trainingDays].
     * "Not sure yet" (null) counts as 1–3 days (Full Body), or 4 days
     * (Upper/Lower) if the user isn't new to lifting. Null when no
     * live plan matches.
     */
    fun suggestPlan(
        plans: List<StarterPlan>,
        trainingDays: TrainingDays?,
        experience: ExperienceLevel?
    ): StarterPlan? {
        val bucket = trainingDays ?: when (experience) {
            ExperienceLevel.SOME, ExperienceLevel.EXPERIENCED -> TrainingDays.FOUR
            ExperienceLevel.NEW, null -> TrainingDays.ONE_TO_THREE
        }
        return plans.firstOrNull { !it.deprecated && it.suggestedFor == bucket }
    }

    /**
     * Owner decision 3A: the next workout is computed, never stored. It
     * is the plan template after the one used by the newest finished
     * session from this plan, wrapping at the end; with no such session,
     * the first. Templates [isAvailable] rejects (e.g. deprecated or
     * missing) are skipped. Null when none is available.
     */
    fun nextInPlan(
        plan: StarterPlan,
        sessions: List<WorkoutSession>,
        isAvailable: (templateId: String) -> Boolean = { true }
    ): String? {
        val ids = plan.templateIds
        if (ids.isEmpty()) return null
        val last = sessions
            .filter { it.deletedAt == null && it.templateId != null && it.templateId in ids }
            .maxByOrNull { it.endedAt ?: it.startedAt }
        val start = last?.let { ids.indexOf(it.templateId) + 1 } ?: 0
        return ids.indices
            .map { ids[(start + it) % ids.size] }
            .firstOrNull(isAvailable)
    }
}
