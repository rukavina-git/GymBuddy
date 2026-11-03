package com.rukavina.gymbuddy.domain.usecase.template

import com.rukavina.gymbuddy.domain.model.ExperienceLevel
import com.rukavina.gymbuddy.domain.model.StarterPlan
import com.rukavina.gymbuddy.domain.model.TrainingDays
import com.rukavina.gymbuddy.domain.model.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StarterPlansTest {

    private val fullBody = plan("full", TrainingDays.ONE_TO_THREE, "fa", "fb", "fc")
    private val upperLower = plan("ul", TrainingDays.FOUR, "ua", "la", "ub", "lb")
    private val ppl = plan("ppl", TrainingDays.FIVE_TO_SEVEN, "push", "pull", "legs")
    private val plans = listOf(fullBody, upperLower, ppl)

    @Test
    fun `suggestPlan follows the days a week, and experience only for not sure yet`() {
        val experiences = listOf(null) + ExperienceLevel.entries
        experiences.forEach { exp ->
            assertEquals("$exp", fullBody, StarterPlans.suggestPlan(plans, TrainingDays.ONE_TO_THREE, exp))
            assertEquals("$exp", upperLower, StarterPlans.suggestPlan(plans, TrainingDays.FOUR, exp))
            assertEquals("$exp", ppl, StarterPlans.suggestPlan(plans, TrainingDays.FIVE_TO_SEVEN, exp))
        }
        assertEquals(fullBody, StarterPlans.suggestPlan(plans, null, null))
        assertEquals(fullBody, StarterPlans.suggestPlan(plans, null, ExperienceLevel.NEW))
        assertEquals(upperLower, StarterPlans.suggestPlan(plans, null, ExperienceLevel.SOME))
        assertEquals(upperLower, StarterPlans.suggestPlan(plans, null, ExperienceLevel.EXPERIENCED))
    }

    @Test
    fun `suggestPlan skips deprecated plans`() {
        val old = upperLower.copy(id = "old", deprecated = true)
        assertEquals(upperLower, StarterPlans.suggestPlan(listOf(old, upperLower), TrainingDays.FOUR, null))
        assertNull(StarterPlans.suggestPlan(listOf(old), TrainingDays.FOUR, null))
    }

    @Test
    fun `nextInPlan starts at the first template with no sessions`() {
        assertEquals("ua", StarterPlans.nextInPlan(upperLower, emptyList()))
    }

    @Test
    fun `nextInPlan follows the newest session from the plan and wraps`() {
        assertEquals("ub", StarterPlans.nextInPlan(upperLower, listOf(session("ua", 1), session("la", 2))))
        assertEquals("ua", StarterPlans.nextInPlan(upperLower, listOf(session("ub", 1), session("lb", 2))))
    }

    @Test
    fun `nextInPlan ignores sessions outside the plan and deleted ones`() {
        val sessions = listOf(
            session("ua", 1),
            session("push", 5),
            session(null, 6),
            session("ub", 7, deleted = true)
        )
        assertEquals("la", StarterPlans.nextInPlan(upperLower, sessions))
    }

    @Test
    fun `nextInPlan skips unavailable templates`() {
        val available = setOf("ua", "ub", "lb")
        assertEquals("ub", StarterPlans.nextInPlan(upperLower, listOf(session("ua", 1))) { it in available })
        assertEquals("ua", StarterPlans.nextInPlan(upperLower, listOf(session("lb", 1))) { it in available })
        assertNull(StarterPlans.nextInPlan(upperLower, emptyList()) { false })
    }

    private fun plan(id: String, days: TrainingDays, vararg templateIds: String) =
        StarterPlan(id, id, null, templateIds.toList(), days, deprecated = false)

    private fun session(templateId: String?, endedAt: Long, deleted: Boolean = false) = WorkoutSession(
        id = "s-$templateId-$endedAt",
        startedAt = endedAt - 1,
        endedAt = endedAt,
        durationSeconds = 60,
        title = "t",
        templateId = templateId,
        performedExercises = emptyList(),
        deletedAt = if (deleted) endedAt else null
    )
}
