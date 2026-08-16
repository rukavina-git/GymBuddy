package com.rukavina.gymbuddy.api.dto

import com.rukavina.gymbuddy.domain.ActivityLevel
import com.rukavina.gymbuddy.domain.FitnessGoal
import com.rukavina.gymbuddy.domain.Gender

/** Mirrors the UserProfile schema in api/openapi.yaml. */
data class UserProfileDto(
    val uid: String,
    val name: String,
    val email: String,
    val profileImageUrl: String?,
    val birthDate: Long?,
    val weight: Float?,
    val height: Float?,
    val gender: Gender?,
    val fitnessGoal: FitnessGoal?,
    val activityLevel: ActivityLevel?,
    val targetWeight: Float?,
    val joinedDate: Long,
    val bio: String?,
    val updatedAt: Long,
    val deletedAt: Long?,
    val revision: Int,
)

/** Mirrors the MeResponse schema in api/openapi.yaml. */
data class MeResponseDto(
    val profile: UserProfileDto,
    val exerciseLibraryVersion: Int,
    val templateLibraryVersion: Int,
    val serverTime: Long,
)
