package com.rukavina.gymbuddy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.rukavina.gymbuddy.data.local.entity.TemplateExerciseEntity
import com.rukavina.gymbuddy.data.local.entity.WorkoutTemplateEntity
import com.rukavina.gymbuddy.data.local.entity.WorkoutTemplateWithExercises
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for WorkoutTemplate and TemplateExercise operations.
 * Handles queries for both tables due to their relationship.
 *
 * Follows the same pattern as WorkoutSessionDao for consistency.
 */
@Dao
interface WorkoutTemplateDao {
    /**
     * Get all workout templates with their exercises.
     * Ordered alphabetically by title for easy browsing.
     *
     * Returns a Flow for reactive updates - UI will automatically
     * update when templates are added, modified, or deleted.
     */
    @Transaction
    @Query("SELECT * FROM workout_templates ORDER BY title ASC")
    fun getAllTemplates(): Flow<List<WorkoutTemplateWithExercises>>

    /**
     * Get a single template by ID with its exercises.
     * Returns null if template not found.
     *
     * @param id The template ID to search for
     * @return The template with exercises, or null if not found
     */
    @Transaction
    @Query("SELECT * FROM workout_templates WHERE id = :id")
    suspend fun getTemplateById(id: String): WorkoutTemplateWithExercises?

    /**
     * Search templates by title (case-insensitive).
     * Useful for filtering or autocomplete functionality.
     *
     * @param query Search query (will be wrapped with % for LIKE query)
     * @return Flow of matching templates
     */
    @Transaction
    @Query("SELECT * FROM workout_templates WHERE title LIKE '%' || :query || '%' ORDER BY title ASC")
    fun searchTemplates(query: String): Flow<List<WorkoutTemplateWithExercises>>

    /**
     * Insert a new template.
     * Use with insertTemplateExercises for complete template creation.
     *
     * @param template The template metadata to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: WorkoutTemplateEntity)

    /**
     * Insert multiple template exercises.
     * Used when creating or updating a template with its exercises.
     *
     * @param exercises List of template exercises to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateExercises(exercises: List<TemplateExerciseEntity>)

    /**
     * Insert a single template exercise.
     * Useful for adding one exercise to an existing template.
     *
     * @param exercise The template exercise to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateExercise(exercise: TemplateExerciseEntity)

    /**
     * Update a template's metadata (title).
     *
     * @param template The updated template metadata
     */
    @Update
    suspend fun updateTemplate(template: WorkoutTemplateEntity)

    /**
     * Update a single template exercise.
     * Useful for modifying sets, reps, order, etc. of one exercise.
     *
     * @param exercise The updated template exercise
     */
    @Update
    suspend fun updateTemplateExercise(exercise: TemplateExerciseEntity)

    /**
     * Delete a template by ID.
     * Template exercises will be cascade deleted due to foreign key constraint.
     *
     * @param id The template ID to delete
     */
    @Query("DELETE FROM workout_templates WHERE id = :id")
    suspend fun deleteTemplate(id: String)

    /**
     * Delete all template exercises for a specific template.
     * Used when updating a template to replace all exercises.
     *
     * @param templateId The template ID whose exercises should be deleted
     */
    @Query("DELETE FROM template_exercises WHERE templateId = :templateId")
    suspend fun deleteTemplateExercisesByTemplateId(templateId: String)

    /**
     * Delete a specific template exercise by ID.
     * Useful for removing one exercise from a template.
     *
     * @param id The template exercise ID to delete
     */
    @Query("DELETE FROM template_exercises WHERE id = :id")
    suspend fun deleteTemplateExercise(id: String)

    /**
     * Delete all templates.
     * Useful for testing or clearing data.
     * All template exercises will be cascade deleted.
     */
    @Query("DELETE FROM workout_templates")
    suspend fun deleteAllTemplates()

    /**
     * Transaction to insert template with exercises atomically.
     * Ensures both template and exercises are inserted or neither is.
     *
     * @param template The template metadata
     * @param exercises The list of template exercises
     */
    @Transaction
    suspend fun insertTemplateWithExercises(
        template: WorkoutTemplateEntity,
        exercises: List<TemplateExerciseEntity>
    ) {
        insertTemplate(template)
        insertTemplateExercises(exercises)
    }

    /**
     * Transaction to update template with exercises atomically.
     * Replaces all template exercises with the new list.
     *
     * This is useful when user reorders exercises, adds/removes exercises,
     * or modifies the entire template structure.
     *
     * @param template The updated template metadata
     * @param exercises The new list of template exercises (replaces existing)
     */
    @Transaction
    suspend fun updateTemplateWithExercises(
        template: WorkoutTemplateEntity,
        exercises: List<TemplateExerciseEntity>
    ) {
        updateTemplate(template)
        deleteTemplateExercisesByTemplateId(template.id)
        insertTemplateExercises(exercises)
    }
}
