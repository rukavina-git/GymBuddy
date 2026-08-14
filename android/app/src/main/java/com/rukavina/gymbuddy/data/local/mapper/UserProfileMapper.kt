package com.rukavina.gymbuddy.data.local.mapper

import com.rukavina.gymbuddy.data.local.entity.UserProfileEntity
import com.rukavina.gymbuddy.domain.model.UserProfile

/**
 * Mapper to convert between UserProfile domain model and Room entity.
 */
object UserProfileMapper {

    /**
     * Convert domain UserProfile to Room entity.
     */
    fun toEntity(userProfile: UserProfile): UserProfileEntity = UserProfileEntity(
        uid = userProfile.uid,
        name = userProfile.name,
        email = userProfile.email,
        profileImageUrl = userProfile.profileImageUrl,
        birthDate = userProfile.birthDate,
        weight = userProfile.weight,
        height = userProfile.height,
        gender = userProfile.gender,
        fitnessGoal = userProfile.fitnessGoal,
        activityLevel = userProfile.activityLevel,
        targetWeight = userProfile.targetWeight,
        joinedDate = userProfile.joinedDate,
        bio = userProfile.bio,
        updatedAt = userProfile.updatedAt,
        deletedAt = userProfile.deletedAt,
        revision = userProfile.revision,
        syncState = userProfile.syncState
    )

    /**
     * Convert Room entity to domain UserProfile.
     */
    fun toDomain(entity: UserProfileEntity): UserProfile = UserProfile(
        uid = entity.uid,
        name = entity.name,
        email = entity.email,
        profileImageUrl = entity.profileImageUrl,
        birthDate = entity.birthDate,
        weight = entity.weight,
        height = entity.height,
        gender = entity.gender,
        fitnessGoal = entity.fitnessGoal,
        activityLevel = entity.activityLevel,
        targetWeight = entity.targetWeight,
        joinedDate = entity.joinedDate,
        bio = entity.bio,
        updatedAt = entity.updatedAt,
        deletedAt = entity.deletedAt,
        revision = entity.revision,
        syncState = entity.syncState
    )
}