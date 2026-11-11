package com.rukavina.gymbuddy.ui.template

import com.rukavina.gymbuddy.domain.model.EntitySource
import com.rukavina.gymbuddy.domain.model.StarterPlan
import com.rukavina.gymbuddy.domain.model.WorkoutSession
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import com.rukavina.gymbuddy.domain.usecase.template.TemplateStats

/** A template as the lists show it (Home, Start sheet, Workouts tab). */
data class TemplateLine(
    val template: WorkoutTemplate,
    /** When it was last done (epoch ms); null = never. */
    val lastDone: Long?,
    /** "about N min"; null until there is data. */
    val averageMinutes: Int?,
    /** Live plan this template is a day of, with its 1-based day. */
    val plan: StarterPlan? = null,
    val planDay: Int? = null,
) {
    val id: String get() = template.id
    val title: String get() = template.title
    val exerciseCount: Int get() = template.templateExercises.size
    val isStarter: Boolean get() = template.source == EntitySource.DEFAULT
}

/** Builds [TemplateLine]s for one set of sessions and plans. */
class TemplateLines(private val sessions: List<WorkoutSession>, private val plans: List<StarterPlan>) {
    val lastDone: Map<String, Long> = TemplateStats.lastDone(sessions)

    fun line(template: WorkoutTemplate): TemplateLine {
        val day = TemplateStats.planDay(template.id, plans)
        return TemplateLine(
            template = template,
            lastDone = lastDone[template.id],
            averageMinutes = TemplateStats.averageMinutes(template.id, sessions),
            plan = day?.first,
            planDay = day?.second,
        )
    }

    /**
     * Starter templates in plan order (live plans first, by plan order,
     * then by day), then any starter in no plan, by title.
     */
    fun starterOrder(templates: List<WorkoutTemplate>): List<WorkoutTemplate> {
        val rank = plans.filter { !it.deprecated }
            .flatMap { it.templateIds }
            .withIndex()
            .associate { (i, id) -> id to i }
        return templates.sortedWith(
            compareBy<WorkoutTemplate> { rank[it.id] ?: Int.MAX_VALUE }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
        )
    }
}
