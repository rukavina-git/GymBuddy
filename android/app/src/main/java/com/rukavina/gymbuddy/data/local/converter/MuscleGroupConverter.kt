package com.rukavina.gymbuddy.data.local.converter

import androidx.room.TypeConverter
import com.rukavina.gymbuddy.data.model.MuscleGroup

/**
 * Type converter for storing MuscleGroup lists in Room database.
 * Converts List<MuscleGroup> to/from comma-separated string.
 */
class MuscleGroupConverter {
    /**
     * Convert comma-separated string to List<MuscleGroup>.
     * @param value Comma-separated muscle groups (e.g., "CHEST,BACK,ARMS")
     * @return List of MuscleGroup enums
     */
    @TypeConverter
    fun fromString(value: String?): List<MuscleGroup> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(",").mapNotNull { muscleString ->
            try {
                MuscleGroup.valueOf(muscleString.trim())
            } catch (e: IllegalArgumentException) {
                null // Skip invalid values
            }
        }
    }

    /**
     * Convert List<MuscleGroup> to comma-separated string.
     * @param muscles List of MuscleGroup enums
     * @return Comma-separated string (e.g., "CHEST,BACK,ARMS")
     */
    @TypeConverter
    fun toString(muscles: List<MuscleGroup>?): String {
        return muscles?.joinToString(",") { it.name } ?: ""
    }
}
