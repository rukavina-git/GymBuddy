package com.rukavina.gymbuddy.domain.model

/**
 * Domain model representing a complete workout session.
 * Contains metadata about the workout session and a list of exercises performed during the session.
 *
 * Independent of persistence layer - can be mapped to Room entities or server DTOs.
 */
data class WorkoutSession(
    /**
     * Unique identifier for this workout session.
     * Use UUID string format for offline-first compatibility and server sync.
     */
    val id: String,

    /**
     * Date and time when the workout session started.
     * Stored as Unix timestamp (milliseconds since epoch) for easy serialization
     * and compatibility with both Room and server APIs.
     * Can be converted to/from LocalDateTime or Instant as needed in the UI layer.
     */
    val startedAt: Long,

    /**
     * Date and time when the workout session ended.
     * Null means the session is still in progress.
     */
    val endedAt: Long? = null,

    /**
     * Total duration of the workout session in seconds.
     * Stores precise duration to avoid losing seconds when converting to/from minutes.
     *
     * Deliberately not derived from endedAt - startedAt: elapsed wall-clock
     * time includes rest and idle time between sets, and the user may edit
     * the duration independently of when the session actually started/ended.
     */
    val durationSeconds: Int,

    /**
     * Title for the workout session.
     * Required field - should always have a value (e.g., template name or "Workout").
     */
    val title: String,

    /**
     * Per-session commentary, e.g. "felt strong today" or "shoulder was tight".
     */
    val notes: String? = null,

    /**
     * Reference to the WorkoutTemplate this session was started from, if any.
     * Points to WorkoutTemplate.id, but this is a soft reference, not a
     * foreign key: it must tolerate the referenced template being deleted.
     * Provenance only - use templateTitle for display.
     */
    val templateId: String? = null,

    /**
     * Snapshot of WorkoutTemplate.title at the time this session was
     * started. Written once and never updated - a later rename or deletion
     * of the template must not rewrite history. Null if this session
     * wasn't started from a template.
     */
    val templateTitle: String? = null,

    /**
     * List of exercises performed during this workout session.
     * Multiple performed exercises can reference the same Exercise template.
     * Order in the list represents the sequence in which exercises were performed.
     */
    val performedExercises: List<PerformedExercise>,

    /**
     * When this row last changed locally, in epoch ms. Set from the
     * injected Clock at write time - see Exercise.updatedAt.
     */
    val updatedAt: Long = 0L,

    /**
     * Tombstone: non-null means this session is deleted. A delete sets
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
