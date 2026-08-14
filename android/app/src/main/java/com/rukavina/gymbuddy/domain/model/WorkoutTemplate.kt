package com.rukavina.gymbuddy.domain.model

/**
 * Domain model representing a workout template.
 * Templates are blueprints that can be used to quickly start workout sessions.
 * Unlike WorkoutSession, templates only contain planned exercises without actual weight data.
 *
 * Use case: User creates a "Push Day" template with exercises like Bench Press (4x8),
 * Overhead Press (3x10), etc. When starting a workout, they can use this template
 * and fill in the actual weights used during the session.
 *
 * Independent of persistence layer - can be mapped to Room entities or server DTOs.
 */
data class WorkoutTemplate(
    /**
     * Unique identifier for this template.
     * Use UUID string format for offline-first compatibility and server sync.
     */
    val id: String,

    /**
     * User-defined title for the template.
     * Examples: "Push Day", "Pull Day", "Leg Day", "Full Body", "Upper Body Strength"
     */
    val title: String,

    /**
     * List of exercises planned in this template with their target sets/reps.
     * Each exercise includes planning details (sets, reps, rest time) but no weight.
     * Order in the list represents the planned sequence of exercises.
     */
    val templateExercises: List<TemplateExercise>,

    /**
     * Whether this is a default/bundled (or future server-provided)
     * template, or one the user created. Default templates cannot be
     * edited or deleted, only hidden.
     */
    val source: EntitySource = EntitySource.CUSTOM,

    /**
     * User ID of the creator, for CUSTOM templates. Null for DEFAULT rows.
     */
    val ownerId: String? = null,

    /**
     * Id of the DEFAULT template this one was duplicated from, for a
     * future "duplicate and edit" action on a default entry. Not used yet.
     */
    val derivedFromId: String? = null,

    /**
     * Whether this template has been superseded and should no longer
     * appear in lists, search, or filters. Existing references to it
     * remain resolvable. See docs/adr/0002-deprecation-over-deletion.md.
     */
    val deprecated: Boolean = false,

    /**
     * When this row last changed locally, in epoch ms. Set from the
     * injected Clock at write time - see Exercise.updatedAt.
     */
    val updatedAt: Long = 0L,

    /**
     * Tombstone: non-null means this template is deleted. A delete sets
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
