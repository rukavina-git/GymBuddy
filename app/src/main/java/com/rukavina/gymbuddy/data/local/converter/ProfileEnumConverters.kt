package com.rukavina.gymbuddy.data.local.converter

import androidx.room.TypeConverter
import com.rukavina.gymbuddy.data.model.ActivityLevel
import com.rukavina.gymbuddy.data.model.FitnessGoal
import com.rukavina.gymbuddy.data.model.Gender
import com.rukavina.gymbuddy.data.model.PreferredUnits

class ProfileEnumConverters {

    @TypeConverter
    fun fromGender(value: Gender?): String? {
        return value?.name
    }

    @TypeConverter
    fun toGender(value: String?): Gender? {
        return value?.let { Gender.valueOf(it) }
    }

    @TypeConverter
    fun fromFitnessGoal(value: FitnessGoal?): String? {
        return value?.name
    }

    @TypeConverter
    fun toFitnessGoal(value: String?): FitnessGoal? {
        return value?.let { FitnessGoal.valueOf(it) }
    }

    @TypeConverter
    fun fromActivityLevel(value: ActivityLevel?): String? {
        return value?.name
    }

    @TypeConverter
    fun toActivityLevel(value: String?): ActivityLevel? {
        return value?.let { ActivityLevel.valueOf(it) }
    }

    @TypeConverter
    fun fromPreferredUnits(value: PreferredUnits): String {
        return value.name
    }

    @TypeConverter
    fun toPreferredUnits(value: String): PreferredUnits {
        return PreferredUnits.valueOf(value)
    }
}
