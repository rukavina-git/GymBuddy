package com.rukavina.gymbuddy.data.local.converter

import androidx.room.TypeConverter
import com.rukavina.gymbuddy.domain.model.DifficultyLevel
import com.rukavina.gymbuddy.domain.model.EntitySource
import com.rukavina.gymbuddy.domain.model.Equipment
import com.rukavina.gymbuddy.domain.model.ExerciseCategory
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.ExerciseType
import com.rukavina.gymbuddy.domain.model.SyncState

/**
 * Type converters for Exercise-related enums and lists.
 * Used by Room to store complex types in the database.
 */
class ExerciseConverters {

    // Equipment List Converters
    @TypeConverter
    fun fromEquipmentString(value: String?): List<Equipment> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(",").mapNotNull { equipmentString ->
            try {
                Equipment.valueOf(equipmentString.trim())
            } catch (e: IllegalArgumentException) {
                null // Skip invalid values
            }
        }
    }

    @TypeConverter
    fun toEquipmentString(equipment: List<Equipment>?): String {
        return equipment?.joinToString(",") { it.name } ?: ""
    }

    // String List Converters (for instructions)
    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        // Use a delimiter that won't appear in normal text
        return value.split("|||").map { it.trim() }
    }

    @TypeConverter
    fun toStringList(strings: List<String>?): String {
        return strings?.joinToString("|||") ?: ""
    }

    // DifficultyLevel Converters
    @TypeConverter
    fun fromDifficultyLevel(value: DifficultyLevel?): String? {
        return value?.name
    }

    @TypeConverter
    fun toDifficultyLevel(value: String?): DifficultyLevel? {
        return value?.let {
            try {
                DifficultyLevel.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }

    // ExerciseCategory Converters
    @TypeConverter
    fun fromExerciseCategory(value: ExerciseCategory?): String? {
        return value?.name
    }

    @TypeConverter
    fun toExerciseCategory(value: String?): ExerciseCategory? {
        return value?.let {
            try {
                ExerciseCategory.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }

    // ExerciseType Converters
    @TypeConverter
    fun fromExerciseType(value: ExerciseType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toExerciseType(value: String?): ExerciseType? {
        return value?.let {
            try {
                ExerciseType.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }

    // ExerciseTrackingType Converters
    @TypeConverter
    fun fromExerciseTrackingType(value: ExerciseTrackingType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toExerciseTrackingType(value: String?): ExerciseTrackingType? {
        return value?.let {
            try {
                ExerciseTrackingType.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }

    // EntitySource Converters (shared by Exercise and WorkoutTemplate)
    @TypeConverter
    fun fromEntitySource(value: EntitySource?): String? {
        return value?.name
    }

    @TypeConverter
    fun toEntitySource(value: String?): EntitySource? {
        return value?.let {
            try {
                EntitySource.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }

    // SyncState Converters (shared by every user-owned entity)
    @TypeConverter
    fun fromSyncState(value: SyncState?): String? {
        return value?.name
    }

    @TypeConverter
    fun toSyncState(value: String?): SyncState? {
        return value?.let {
            try {
                SyncState.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}
