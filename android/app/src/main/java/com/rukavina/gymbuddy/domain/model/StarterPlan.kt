package com.rukavina.gymbuddy.domain.model

/**
 * A set of default templates to rotate through, in order (Plan 6 step
 * 06). Reference data: users pick one (UserProfile.activePlanId) but
 * don't create or edit plans (owner decision 1A).
 */
data class StarterPlan(
    val id: String,
    val title: String,
    val description: String?,
    /** Ordered ids of the plan's DEFAULT templates. */
    val templateIds: List<String>,
    /** The days-a-week bucket onboarding suggests this plan for. */
    val suggestedFor: TrainingDays?,
    val deprecated: Boolean
)
