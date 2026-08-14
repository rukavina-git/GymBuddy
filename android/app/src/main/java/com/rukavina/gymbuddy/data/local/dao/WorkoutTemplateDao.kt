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
 *
 * Hidden/favorite state lives in user_template_state, not on the
 * workout_templates row itself - see UserTemplateStateEntity. List/search
 * queries below LEFT JOIN that table only to exclude hidden templates -
 * the primary query still projects exactly WorkoutTemplateEntity's own
 * columns (via `wt.*`), so the @Relation-based exercise fetch on
 * WorkoutTemplateWithExercises is unaffected by the join.
 *
 * Every read query also excludes deletedAt IS NOT NULL rows (tombstoned
 * CUSTOM templates), including the detail lookup by id - see ExerciseDao
 * for why deleted rows are excluded even there, unlike hidden/deprecated.
 */
@Dao
interface WorkoutTemplateDao {
    /**
     * Get all visible workout templates with their exercises.
     * Excludes hidden templates. Ordered alphabetically by title for easy browsing.
     *
     * Returns a Flow for reactive updates - UI will automatically
     * update when templates are added, modified, or deleted.
     */
    @Transaction
    @Query(
        """
        SELECT wt.* FROM workout_templates wt
        LEFT JOIN user_template_state s ON s.templateId = wt.id
        WHERE COALESCE(s.isHidden, 0) = 0 AND wt.deprecated = 0 AND wt.deletedAt IS NULL
        ORDER BY wt.title ASC
        """
    )
    fun getAllTemplates(): Flow<List<WorkoutTemplateWithExercises>>

    /**
     * Get all workout templates including hidden ones.
     * Useful for settings/management screens. Still excludes deleted ones.
     */
    @Transaction
    @Query("SELECT * FROM workout_templates WHERE deprecated = 0 AND deletedAt IS NULL ORDER BY title ASC")
    fun getAllTemplatesIncludingHidden(): Flow<List<WorkoutTemplateWithExercises>>

    /**
     * Get a single template by ID with its exercises.
     * Not filtered by hidden or deprecated - a detail lookup must resolve
     * regardless of either status - but excludes deleted rows.
     *
     * @param id The template ID to search for
     * @return The template with exercises, or null if not found
     */
    @Transaction
    @Query("SELECT * FROM workout_templates WHERE id = :id AND deletedAt IS NULL")
    suspend fun getTemplateById(id: String): WorkoutTemplateWithExercises?

    /**
     * Search visible templates by title (case-insensitive).
     * Excludes hidden templates.
     * Useful for filtering or autocomplete functionality.
     *
     * @param query Search query (will be wrapped with % for LIKE query)
     * @return Flow of matching templates
     */
    @Transaction
    @Query(
        """
        SELECT wt.* FROM workout_templates wt
        LEFT JOIN user_template_state s ON s.templateId = wt.id
        WHERE COALESCE(s.isHidden, 0) = 0 AND wt.title LIKE '%' || :query || '%' AND wt.deprecated = 0 AND wt.deletedAt IS NULL
        ORDER BY wt.title ASC
        """
    )
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
     * Tombstone a template by ID instead of removing the row, so a future
     * sync engine has something to push. Since this is an UPDATE rather
     * than a DELETE, the cascade-delete foreign key on template_exercises
     * does not fire - its rows stay in place, governed by the now-
     * tombstoned parent, exactly like the "child governed by parent" rule
     * for other aggregates.
     *
     * @param id The template ID to delete
     */
    @Query("UPDATE workout_templates SET deletedAt = :deletedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun deleteTemplate(id: String, deletedAt: Long, updatedAt: Long)

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
     * Get all hidden templates. Requires an overlay row marking the
     * template hidden, so orphaned overlay rows (template no longer
     * exists) never appear here. Excludes deleted templates.
     * Useful for a "restore hidden templates" feature.
     */
    @Transaction
    @Query(
        """
        SELECT wt.* FROM workout_templates wt
        INNER JOIN user_template_state s ON s.templateId = wt.id
        WHERE s.isHidden = 1 AND wt.deprecated = 0 AND wt.deletedAt IS NULL
        ORDER BY wt.title ASC
        """
    )
    fun getHiddenTemplates(): Flow<List<WorkoutTemplateWithExercises>>

    /**
     * Get default templates only.
     * Useful for management screens.
     */
    @Transaction
    @Query("SELECT * FROM workout_templates WHERE source = 'DEFAULT' AND deprecated = 0 AND deletedAt IS NULL ORDER BY title ASC")
    fun getDefaultTemplates(): Flow<List<WorkoutTemplateWithExercises>>

    /**
     * Get custom (user-created) templates only. Excludes hidden templates.
     */
    @Transaction
    @Query(
        """
        SELECT wt.* FROM workout_templates wt
        LEFT JOIN user_template_state s ON s.templateId = wt.id
        WHERE wt.source = 'CUSTOM' AND COALESCE(s.isHidden, 0) = 0 AND wt.deprecated = 0 AND wt.deletedAt IS NULL
        ORDER BY wt.title ASC
        """
    )
    fun getCustomTemplates(): Flow<List<WorkoutTemplateWithExercises>>

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
