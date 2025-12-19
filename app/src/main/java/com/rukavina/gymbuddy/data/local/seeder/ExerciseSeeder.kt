package com.rukavina.gymbuddy.data.local.seeder

import android.content.Context
import android.util.Log
import com.rukavina.gymbuddy.data.local.dao.ExerciseDao
import com.rukavina.gymbuddy.data.local.dao.ExerciseVersionDao
import com.rukavina.gymbuddy.data.local.entity.ExerciseEntity
import com.rukavina.gymbuddy.data.local.entity.ExerciseVersionEntity
import com.rukavina.gymbuddy.data.model.DifficultyLevel
import com.rukavina.gymbuddy.data.model.Equipment
import com.rukavina.gymbuddy.data.model.ExerciseCategory
import com.rukavina.gymbuddy.data.model.ExerciseType
import com.rukavina.gymbuddy.data.model.MuscleGroup
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

/**
 * Handles seeding and version management of default exercises.
 * Loads exercises from bundled JSON asset and updates when version changes.
 */
class ExerciseSeeder @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val versionDao: ExerciseVersionDao
) {
    companion object {
        private const val TAG = "ExerciseSeeder"
        private const val DEFAULT_EXERCISES_FILE = "default_exercises.json"
    }

    /**
     * Check if default exercises need to be loaded or updated.
     * Compares bundled version with stored version.
     *
     * @param context Application context for accessing assets
     * @return true if seeding occurred, false if already up to date
     */
    suspend fun seedIfNeeded(context: Context): Boolean {
        try {
            // Load bundled exercise data
            val json = loadJsonFromAssets(context)
            val bundledVersion = json.getInt("version")

            // Check current version in database
            val currentVersion = versionDao.getCurrentVersion()?.version ?: 0

            Log.d(TAG, "Bundled version: $bundledVersion, Current version: $currentVersion")

            if (bundledVersion > currentVersion) {
                Log.i(TAG, "Updating default exercises from v$currentVersion to v$bundledVersion")
                seedDefaultExercises(json)
                versionDao.setVersion(ExerciseVersionEntity(version = bundledVersion))
                return true
            } else {
                Log.d(TAG, "Default exercises are up to date (v$currentVersion)")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error seeding exercises", e)
            throw e
        }
    }

    /**
     * Force reload of default exercises regardless of version.
     * Useful for testing or manual refresh.
     */
    suspend fun forceReseed(context: Context) {
        val json = loadJsonFromAssets(context)
        seedDefaultExercises(json)
        val version = json.getInt("version")
        versionDao.setVersion(ExerciseVersionEntity(version = version))
        Log.i(TAG, "Forced reseed of default exercises (v$version)")
    }

    /**
     * Load and parse the default exercises JSON from assets.
     */
    private fun loadJsonFromAssets(context: Context): JSONObject {
        val jsonString = context.assets.open(DEFAULT_EXERCISES_FILE).bufferedReader().use {
            it.readText()
        }
        return JSONObject(jsonString)
    }

    /**
     * Parse and insert default exercises into the database.
     * Deletes existing default exercises first to ensure clean update.
     */
    private suspend fun seedDefaultExercises(json: JSONObject) {
        // Delete old default exercises
        exerciseDao.deleteAllDefaultExercises()

        // Parse exercises from JSON
        val exercisesJson = json.getJSONArray("exercises")
        val exercises = mutableListOf<ExerciseEntity>()

        for (i in 0 until exercisesJson.length()) {
            val exerciseJson = exercisesJson.getJSONObject(i)
            val exercise = parseExercise(exerciseJson)
            exercises.add(exercise)
        }

        // Bulk insert all exercises
        exerciseDao.insertExercises(exercises)
        Log.i(TAG, "Seeded ${exercises.size} default exercises")
    }

    /**
     * Parse a single exercise from JSON to ExerciseEntity.
     */
    private fun parseExercise(json: JSONObject): ExerciseEntity {
        return ExerciseEntity(
            id = json.getString("id"),
            name = json.getString("name"),
            primaryMuscles = parseEnumList<MuscleGroup>(json.getJSONArray("primaryMuscles")),
            secondaryMuscles = parseEnumList<MuscleGroup>(json.getJSONArray("secondaryMuscles")),
            description = json.optString("description").takeIf { it.isNotEmpty() },
            instructions = parseStringList(json.optJSONArray("instructions")),
            difficulty = DifficultyLevel.valueOf(json.getString("difficulty")),
            equipmentNeeded = parseEnumList<Equipment>(json.getJSONArray("equipmentNeeded")),
            category = ExerciseCategory.valueOf(json.getString("category")),
            exerciseType = ExerciseType.valueOf(json.getString("exerciseType")),
            videoUrl = json.optString("videoUrl").takeIf { it.isNotEmpty() },
            thumbnailUrl = json.optString("thumbnailUrl").takeIf { it.isNotEmpty() },
            isCustom = json.getBoolean("isCustom"),
            createdBy = json.optString("createdBy").takeIf { it.isNotEmpty() }
        )
    }

    /**
     * Parse a JSON array of strings to a List<String>.
     */
    private fun parseStringList(jsonArray: JSONArray?): List<String> {
        if (jsonArray == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.getString(i))
        }
        return list
    }

    /**
     * Parse a JSON array of enum names to a List of enum values.
     */
    private inline fun <reified T : Enum<T>> parseEnumList(jsonArray: JSONArray): List<T> {
        val list = mutableListOf<T>()
        for (i in 0 until jsonArray.length()) {
            val enumName = jsonArray.getString(i)
            try {
                val enumValue = enumValueOf<T>(enumName)
                list.add(enumValue)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Unknown enum value: $enumName for ${T::class.simpleName}")
            }
        }
        return list
    }
}
