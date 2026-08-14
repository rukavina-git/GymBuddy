package com.rukavina.gymbuddy.testutil

import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import com.rukavina.gymbuddy.domain.repository.WorkoutTemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory WorkoutTemplateRepository fake. Records every create/update
 * call verbatim so tests can assert exactly what a use case persisted.
 */
class FakeWorkoutTemplateRepository(
    initialTemplates: List<WorkoutTemplate> = emptyList()
) : WorkoutTemplateRepository {

    private val templates = initialTemplates.associateBy { it.id }.toMutableMap()
    private val hidden = mutableSetOf<String>()
    val created = mutableListOf<WorkoutTemplate>()
    val updated = mutableListOf<WorkoutTemplate>()

    override fun getAllTemplates(): Flow<List<WorkoutTemplate>> =
        MutableStateFlow(templates.values.toList())

    override suspend fun getTemplateById(id: String): WorkoutTemplate? = templates[id]

    override fun searchTemplates(query: String): Flow<List<WorkoutTemplate>> =
        MutableStateFlow(templates.values.filter { it.title.contains(query, ignoreCase = true) })

    override suspend fun createTemplate(template: WorkoutTemplate) {
        templates[template.id] = template
        created.add(template)
    }

    override suspend fun updateTemplate(template: WorkoutTemplate) {
        templates[template.id] = template
        updated.add(template)
    }

    override suspend fun deleteTemplate(id: String) {
        templates.remove(id)
    }

    override suspend fun hideTemplate(id: String) {
        hidden.add(id)
    }

    override suspend fun unhideTemplate(id: String) {
        hidden.remove(id)
    }

    override fun getHiddenTemplates(): Flow<List<WorkoutTemplate>> =
        MutableStateFlow(templates.values.filter { it.id in hidden })
}
