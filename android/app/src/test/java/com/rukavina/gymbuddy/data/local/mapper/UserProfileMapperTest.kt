package com.rukavina.gymbuddy.data.local.mapper

import com.rukavina.gymbuddy.domain.model.ActivityLevel
import com.rukavina.gymbuddy.domain.model.FitnessGoal
import com.rukavina.gymbuddy.domain.model.Gender
import com.rukavina.gymbuddy.domain.model.SyncState
import com.rukavina.gymbuddy.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserProfileMapperTest {

    private fun fullProfile() = UserProfile(
        uid = "user-1",
        name = "Alex",
        email = "alex@example.com",
        profileImageUrl = "https://example.com/photo.jpg",
        birthDate = 500_000_000_000L,
        weight = 82.5f,
        height = 180f,
        gender = Gender.FEMALE,
        fitnessGoal = FitnessGoal.BUILD_MUSCLE,
        activityLevel = ActivityLevel.VERY_ACTIVE,
        targetWeight = 78f,
        joinedDate = 1_700_000_000_000L,
        bio = "Lifts things",
        updatedAt = 1_700_003_600_000L,
        deletedAt = 1_700_090_000_000L,
        revision = 2,
        syncState = SyncState.CONFLICTED
    )

    @Test
    fun `round trips a full profile through toEntity and toDomain unchanged`() {
        val original = fullProfile()

        val result = UserProfileMapper.toDomain(UserProfileMapper.toEntity(original))

        assertEquals(original, result)
    }

    @Test
    fun `round trips a profile with every nullable field absent`() {
        val original = UserProfile(
            uid = "user-2",
            name = "Sam",
            email = "sam@example.com",
            profileImageUrl = null,
            birthDate = null,
            weight = null,
            height = null,
            gender = null,
            fitnessGoal = null,
            activityLevel = null,
            targetWeight = null,
            joinedDate = 1_700_000_000_000L,
            bio = null
        )

        val result = UserProfileMapper.toDomain(UserProfileMapper.toEntity(original))

        assertEquals(original, result)
        assertNull(result.profileImageUrl)
        assertNull(result.gender)
        assertNull(result.deletedAt)
    }

    @Test
    fun `preserves sync metadata and soft-deleted tombstone state across the round trip`() {
        val original = fullProfile()

        val result = UserProfileMapper.toDomain(UserProfileMapper.toEntity(original))

        assertEquals(original.updatedAt, result.updatedAt)
        assertEquals(original.deletedAt, result.deletedAt)
        assertEquals(original.revision, result.revision)
        assertEquals(original.syncState, result.syncState)
    }
}
