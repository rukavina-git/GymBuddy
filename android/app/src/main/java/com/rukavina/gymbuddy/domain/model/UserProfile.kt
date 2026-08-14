package com.rukavina.gymbuddy.domain.model

data class UserProfile(
    val uid: String,
    val name: String,
    val email: String,
    val profileImageUrl: String? = null,
    val birthDate: Long? = null,
    val weight: Float? = null,
    val height: Float? = null,
    val gender: Gender? = null,
    val fitnessGoal: FitnessGoal? = null,
    val activityLevel: ActivityLevel? = null,
    val targetWeight: Float? = null,
    val joinedDate: Long,
    val bio: String? = null,

    /**
     * When this row last changed locally, in epoch ms. Set from the
     * injected Clock at write time - see Exercise.updatedAt.
     */
    val updatedAt: Long = 0L,

    /**
     * Tombstone: non-null means this profile is deleted. A delete sets
     * this and bumps updatedAt instead of removing the row.
     */
    val deletedAt: Long? = null,

    /**
     * Server-assigned revision number. Stays 0 until a backend exists.
     */
    val revision: Int = 0,

    /**
     * Local-only bookkeeping for the future sync engine - see SyncState.
     */
    val syncState: SyncState = SyncState.PENDING
)