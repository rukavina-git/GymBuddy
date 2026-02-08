package com.rukavina.gymbuddy.domain.repository

import com.rukavina.gymbuddy.data.model.WorkoutTemplate
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for WorkoutTemplate operations.
 * Abstracts the data source (local Room database, future remote API).
 * Domain layer depends on this interface, not concrete implementations.
 *
 * Follows the same pattern as WorkoutSessionRepository for consistency.
 */
interface WorkoutTemplateRepository {
    /**
     * Get all workout templates as a Flow for reactive updates.
     * UI will automatically update when templates are added, modified, or deleted.
     * Ordered alphabetically by title.
     *
     * @return Flow of template list
     */
    fun getAllTemplates(): Flow<List<WorkoutTemplate>>

    /**
     * Get a single template by ID with all exercises.
     * Returns null if template not found.
     *
     * @param id The template ID to search for
     * @return The template with exercises, or null if not found
     */
    suspend fun getTemplateById(id: String): WorkoutTemplate?

    /**
     * Search templates by title (case-insensitive).
     * Useful for filtering or autocomplete functionality.
     *
     * @param query Search query string
     * @return Flow of matching templates
     */
    fun searchTemplates(query: String): Flow<List<WorkoutTemplate>>

    /**
     * Create a new workout template.
     * IDs should be generated (UUID) before calling this.
     *
     * @param template The template to create
     */
    suspend fun createTemplate(template: WorkoutTemplate)

    /**
     * Update an existing template.
     * Replaces the entire template including all exercises.
     *
     * @param template The updated template
     */
    suspend fun updateTemplate(template: WorkoutTemplate)

    /**
     * Delete a template by ID.
     * Also deletes all associated template exercises (cascade).
     *
     * @param id The template ID to delete
     */
    suspend fun deleteTemplate(id: String)

    /**
     * Hide a template by ID.
     * Hidden templates won't appear in the main list.
     *
     * @param id The template ID to hide
     */
    suspend fun hideTemplate(id: String)

    /**
     * Unhide a template by ID.
     * Makes the template visible again.
     *
     * @param id The template ID to unhide
     */
    suspend fun unhideTemplate(id: String)

    /**
     * Get all hidden templates.
     *
     * @return Flow of hidden templates
     */
    fun getHiddenTemplates(): Flow<List<WorkoutTemplate>>
}
