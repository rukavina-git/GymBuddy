package com.rukavina.gymbuddy.data.local.seeder

import android.content.Context
import android.util.Log
import com.rukavina.gymbuddy.data.local.dao.TemplateVersionDao
import com.rukavina.gymbuddy.data.local.dao.WorkoutTemplateDao
import com.rukavina.gymbuddy.data.local.entity.TemplateExerciseEntity
import com.rukavina.gymbuddy.data.local.entity.TemplateVersionEntity
import com.rukavina.gymbuddy.data.local.entity.WorkoutTemplateEntity
import org.json.JSONObject
import javax.inject.Inject

/**
 * Handles seeding and version management of default workout templates.
 * Loads templates from bundled JSON asset and updates when version changes.
 */
class WorkoutTemplateSeeder @Inject constructor(
    private val workoutTemplateDao: WorkoutTemplateDao,
    private val versionDao: TemplateVersionDao
) {
    companion object {
        private const val TAG = "WorkoutTemplateSeeder"
        private const val DEFAULT_TEMPLATES_FILE = "default_workout_templates.json"
    }

    /**
     * Check if default templates need to be loaded or updated.
     * Compares bundled version with stored version.
     *
     * @param context Application context for accessing assets
     * @return true if seeding occurred, false if already up to date
     */
    suspend fun seedIfNeeded(context: Context): Boolean {
        try {
            // Load bundled template data
            val json = loadJsonFromAssets(context)
            val bundledVersion = json.getInt("version")

            // Check current version in database
            val currentVersion = versionDao.getCurrentVersion()?.version ?: 0

            Log.d(TAG, "Bundled version: $bundledVersion, Current version: $currentVersion")

            if (bundledVersion > currentVersion) {
                Log.i(TAG, "Updating default templates from v$currentVersion to v$bundledVersion")
                seedDefaultTemplates(json)
                versionDao.setVersion(TemplateVersionEntity(version = bundledVersion))
                return true
            } else {
                Log.d(TAG, "Default templates are up to date (v$currentVersion)")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error seeding templates", e)
            throw e
        }
    }

    /**
     * Force reload of default templates regardless of version.
     * Useful for testing or manual refresh.
     */
    suspend fun forceReseed(context: Context) {
        val json = loadJsonFromAssets(context)
        seedDefaultTemplates(json)
        val version = json.getInt("version")
        versionDao.setVersion(TemplateVersionEntity(version = version))
        Log.i(TAG, "Forced reseed of default templates (v$version)")
    }

    /**
     * Load and parse the default templates JSON from assets.
     */
    private fun loadJsonFromAssets(context: Context): JSONObject {
        val jsonString = context.assets.open(DEFAULT_TEMPLATES_FILE).bufferedReader().use {
            it.readText()
        }
        return JSONObject(jsonString)
    }

    /**
     * Parse and insert default templates into the database.
     * Only inserts templates that don't already exist (by ID).
     */
    private suspend fun seedDefaultTemplates(json: JSONObject) {
        val templatesJson = json.getJSONArray("templates")

        for (i in 0 until templatesJson.length()) {
            val templateJson = templatesJson.getJSONObject(i)
            val templateId = templateJson.getString("id")

            // Check if template already exists (user might have modified it)
            val existingTemplate = workoutTemplateDao.getTemplateById(templateId)
            if (existingTemplate != null) {
                Log.d(TAG, "Template $templateId already exists, skipping")
                continue
            }

            // Parse and insert new template
            val template = WorkoutTemplateEntity(
                id = templateId,
                title = templateJson.getString("title"),
                isDefault = true, // Mark as default template
                isHidden = false
            )

            val exercisesJson = templateJson.getJSONArray("exercises")
            val exercises = mutableListOf<TemplateExerciseEntity>()

            for (j in 0 until exercisesJson.length()) {
                val exerciseJson = exercisesJson.getJSONObject(j)
                exercises.add(
                    TemplateExerciseEntity(
                        id = System.currentTimeMillis().toInt() + j, // Generate unique ID
                        templateId = templateId,
                        exerciseId = exerciseJson.getInt("exerciseId"),
                        plannedSets = exerciseJson.getInt("plannedSets"),
                        plannedReps = exerciseJson.getInt("plannedReps"),
                        orderIndex = exerciseJson.getInt("orderIndex"),
                        restSeconds = exerciseJson.optInt("restSeconds").takeIf { it > 0 },
                        notes = exerciseJson.optString("notes").takeIf { it.isNotEmpty() }
                    )
                )
            }

            workoutTemplateDao.insertTemplateWithExercises(template, exercises)
            Log.d(TAG, "Seeded template: ${template.title}")
        }

        Log.i(TAG, "Finished seeding default templates")
    }
}
