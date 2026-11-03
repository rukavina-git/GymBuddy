package com.rukavina.gymbuddy.data.local.seeder

import android.content.Context
import android.util.Log
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.data.local.db.DatabaseHolder
import kotlinx.coroutines.sync.withLock
import androidx.room.withTransaction
import com.rukavina.gymbuddy.data.local.dao.StarterPlanDao
import com.rukavina.gymbuddy.data.local.dao.WorkoutTemplateDao
import com.rukavina.gymbuddy.data.local.entity.StarterPlanEntity
import com.rukavina.gymbuddy.data.local.entity.StarterPlanTemplateEntity
import com.rukavina.gymbuddy.data.local.entity.TemplateExerciseEntity
import com.rukavina.gymbuddy.data.local.entity.TemplateVersionEntity
import com.rukavina.gymbuddy.data.local.entity.WorkoutTemplateEntity
import com.rukavina.gymbuddy.domain.model.EntitySource
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.TrainingDays
import org.json.JSONObject
import javax.inject.Inject

/**
 * Handles seeding and version management of default workout templates.
 * Loads templates from bundled JSON asset and updates when version changes.
 */
class WorkoutTemplateSeeder @Inject constructor(
    private val holder: DatabaseHolder
) {
    // Seeds whichever owner's file is current; the app runs it per new file.

    companion object {
        private const val TAG = "WorkoutTemplateSeeder"
        private const val DEFAULT_TEMPLATES_FILE = "default_workout_templates.json"
    }

    /**
     * Loads the bundled default templates on first install only - when no
     * library version is stored yet (fresh install, or after the
     * database was cleared). After that the server is authoritative:
     * SyncEngine replaces the library whenever /v1/reference/version
     * differs from the stored version, and stores the server's version.
     *
     * Comparing against the bundled version here instead would fight
     * the server whenever the app's bundle is ahead of it: this would
     * reseed on every cold start and the next sync would put the
     * server's library back (D-35).
     *
     * Exception: the guest's file never syncs, so the bundle is its only
     * source. A guest whose stored version is older than the bundle gets
     * the bundle (DEFAULT templates and plans replaced; custom templates
     * and overlays kept).
     *
     * @param context Application context for accessing assets
     * @return true if seeding occurred, false if a library was already loaded
     */
    suspend fun seedIfNeeded(context: Context): Boolean = holder.lock.withLock {
        // Under the switch lock, so the file and its owner can't change mid-seed.
        val db = holder.db
        val isGuest = holder.owner == null
        try {
            // Load bundled template data
            val json = loadJsonFromAssets(context)
            val bundledVersion = json.getInt("version")

            // Check current version in database
            val currentVersion = db.templateVersionDao().getCurrentVersion()?.version

            Log.d(TAG, "Bundled version: $bundledVersion, Current version: $currentVersion")

            if (currentVersion == null) {
                Log.i(TAG, "First install: seeding bundled default templates v$bundledVersion")
                seedAll(db, json, bundledVersion)
                return@withLock true
            } else if (isGuest && bundledVersion > currentVersion) {
                Log.i(TAG, "Guest file: refreshing default templates v$currentVersion -> v$bundledVersion")
                seedAll(db, json, bundledVersion, replaceDefaults = true)
                return@withLock true
            } else {
                Log.d(TAG, "Default templates already loaded (v$currentVersion); later updates come from the server")
                return@withLock false
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
        val version = json.getInt("version")
        seedAll(holder.db, json, version)
        Log.i(TAG, "Forced reseed of default templates (v$version)")
    }

    /** Templates, starter plans and the version land together, or not at all. */
    private suspend fun seedAll(db: AppDatabase, json: JSONObject, version: Int, replaceDefaults: Boolean = false) {
        db.withTransaction {
            // template_exercises go via ON DELETE CASCADE.
            if (replaceDefaults) db.syncDao().deleteDefaultTemplates()
            seedDefaultTemplates(db.workoutTemplateDao(), json)
            seedStarterPlans(db.starterPlanDao(), json)
            db.templateVersionDao().setVersion(TemplateVersionEntity(version = version))
        }
    }

    /** Plan 6 step 06: the bundle's "plans" replace whatever plans the file has. */
    private suspend fun seedStarterPlans(dao: StarterPlanDao, json: JSONObject) {
        dao.deleteAll()
        val plansJson = json.optJSONArray("plans") ?: return
        for (i in 0 until plansJson.length()) {
            val planJson = plansJson.getJSONObject(i)
            val planId = planJson.getString("id")
            dao.insertPlans(
                listOf(
                    StarterPlanEntity(
                        id = planId,
                        title = planJson.getString("title"),
                        description = planJson.optString("description").takeIf { it.isNotEmpty() },
                        suggestedFor = planJson.optString("suggestedFor").takeIf { it.isNotEmpty() }
                            ?.let { TrainingDays.valueOf(it) },
                        displayOrder = i,
                        deprecated = planJson.optBoolean("deprecated")
                    )
                )
            )
            val templateIds = planJson.getJSONArray("templateIds")
            dao.insertPlanTemplates(
                (0 until templateIds.length()).map {
                    StarterPlanTemplateEntity(planId = planId, orderIndex = it, templateId = templateIds.getString(it))
                }
            )
        }
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
    private suspend fun seedDefaultTemplates(workoutTemplateDao: WorkoutTemplateDao, json: JSONObject) {
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
                source = EntitySource.DEFAULT,
                ownerId = null,
                derivedFromId = null,
                deprecated = templateJson.optBoolean("deprecated")
            )

            val exercisesJson = templateJson.getJSONArray("exercises")
            val exercises = mutableListOf<TemplateExerciseEntity>()

            for (j in 0 until exercisesJson.length()) {
                val exerciseJson = exercisesJson.getJSONObject(j)
                exercises.add(
                    TemplateExerciseEntity(
                        id = exerciseJson.getString("id"),
                        templateId = templateId,
                        exerciseId = exerciseJson.getString("exerciseId"),
                        exerciseName = exerciseJson.getString("exerciseName"),
                        exerciseTrackingType = ExerciseTrackingType.valueOf(
                            exerciseJson.getString("exerciseTrackingType")
                        ),
                        plannedSets = exerciseJson.getInt("plannedSets"),
                        // Some entries (e.g. timed holds like Plank) carry
                        // plannedDurationSeconds instead of plannedReps.
                        plannedReps = if (exerciseJson.has("plannedReps")) {
                            exerciseJson.getInt("plannedReps")
                        } else {
                            null
                        },
                        orderIndex = exerciseJson.getInt("orderIndex"),
                        restSeconds = exerciseJson.optInt("restSeconds").takeIf { it > 0 },
                        plannedDurationSeconds = if (exerciseJson.has("plannedDurationSeconds")) {
                            exerciseJson.getInt("plannedDurationSeconds")
                        } else {
                            null
                        },
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
