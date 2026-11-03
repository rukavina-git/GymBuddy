package com.rukavina.gymbuddy.data.local.converter

import androidx.room.TypeConverter
import com.rukavina.gymbuddy.domain.model.ActivityLevel
import com.rukavina.gymbuddy.domain.model.ExperienceLevel
import com.rukavina.gymbuddy.domain.model.FitnessGoal
import com.rukavina.gymbuddy.domain.model.Gender
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.domain.model.TrainingDays

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
    fun fromExperienceLevel(value: ExperienceLevel?): String? = value?.name

    @TypeConverter
    fun toExperienceLevel(value: String?): ExperienceLevel? = value?.let { ExperienceLevel.valueOf(it) }

    @TypeConverter
    fun fromTrainingDays(value: TrainingDays?): String? = value?.name

    @TypeConverter
    fun toTrainingDays(value: String?): TrainingDays? = value?.let { TrainingDays.valueOf(it) }

    @TypeConverter
    fun fromPreferredUnits(value: PreferredUnits): String {
        return value.name
    }

    @TypeConverter
    fun toPreferredUnits(value: String): PreferredUnits {
        return PreferredUnits.valueOf(value)
    }
}
