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

    /** Plan 6 step 05. Set by onboarding and You (Plan 7); kept by every other save. */
    val experienceLevel: ExperienceLevel? = null,

    /** Preset avatar id, e.g. "cat" (no photos in v1). Null: the default avatar. */
    val avatarId: String? = null,

    val trainingDays: TrainingDays? = null,

    /** Health-data consent record from account creation (decision 2A: recorded, not enforced). */
    val healthDataConsentVersion: String? = null,
    val healthDataConsentAt: Long? = null,

    /** The StarterPlan the user follows (decision 2A: synced with the profile). Soft reference. */
    val activePlanId: String? = null,

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