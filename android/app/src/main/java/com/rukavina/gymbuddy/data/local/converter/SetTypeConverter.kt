package com.rukavina.gymbuddy.data.local.converter

import androidx.room.TypeConverter
import com.rukavina.gymbuddy.domain.model.SetType

/**
 * Type converter for storing SetType in Room database.
 */
class SetTypeConverter {
    @TypeConverter
    fun fromSetType(value: SetType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toSetType(value: String?): SetType? {
        return value?.let {
            try {
                SetType.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}
