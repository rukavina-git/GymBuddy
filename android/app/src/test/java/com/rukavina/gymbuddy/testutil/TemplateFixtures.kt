package com.rukavina.gymbuddy.testutil

import com.rukavina.gymbuddy.domain.model.EntitySource
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.domain.model.StarterPlan
import com.rukavina.gymbuddy.domain.model.TemplateExercise
import com.rukavina.gymbuddy.domain.model.WorkoutSession
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import com.rukavina.gymbuddy.ui.template.TemplateSources
import kotlinx.coroutines.flow.MutableStateFlow

/** Small builders for Home / Workouts / template page tests (Plan 7 05a). */
object TemplateFixtures {
    const val DAY = 86_400_000L

    fun template(id: String, title: String = id, source: EntitySource = EntitySource.CUSTOM, exercises: Int = 2) =
        WorkoutTemplate(
            id = id,
            title = title,
            source = source,
            templateExercises = (0 until exercises).map {
                TemplateExercise(
                    id = "$id-ex$it", exerciseId = "e$it", exerciseName = "Exercise $it",
                    exerciseTrackingType = ExerciseTrackingType.WEIGHT_REPS,
                    plannedSets = 3, plannedReps = 10, orderIndex = it, restSeconds = 90,
                )
            },
        )

    fun session(id: String, templateId: String?, endedAt: Long?, durationSeconds: Int = 3600, deletedAt: Long? = null) =
        WorkoutSession(
            id = id,
            startedAt = (endedAt ?: 0L) - durationSeconds * 1000L,
            endedAt = endedAt,
            durationSeconds = durationSeconds,
            title = id,
            templateId = templateId,
            performedExercises = emptyList(),
            deletedAt = deletedAt,
        )

    fun plan(id: String, vararg templateIds: String, deprecated: Boolean = false) =
        StarterPlan(id = id, title = "Plan $id", description = null, templateIds = templateIds.toList(), suggestedFor = null, deprecated = deprecated)
}

class FakeTemplateSources(
    templates: List<WorkoutTemplate> = emptyList(),
    sessions: List<WorkoutSession> = emptyList(),
    plans: List<StarterPlan> = emptyList(),
    activePlanId: String? = null,
) : TemplateSources {
    override val templates = MutableStateFlow(templates)
    override val sessions = MutableStateFlow(sessions)
    override val plans = MutableStateFlow(plans)
    override val activePlanId = MutableStateFlow(activePlanId)
    override val units = MutableStateFlow(PreferredUnits.METRIC)
}
