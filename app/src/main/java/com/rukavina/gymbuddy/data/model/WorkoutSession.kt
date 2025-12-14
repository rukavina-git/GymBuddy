package com.rukavina.gymbuddy.data.model

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
     * Date and time when the workout session was performed.
     * Stored as Unix timestamp (milliseconds since epoch) for easy serialization
     * and compatibility with both Room and server APIs.
     * Can be converted to/from LocalDateTime or Instant as needed in the UI layer.
     */
    val date: Long,

    /**
     * Total duration of the workout session in minutes.
     * Useful for tracking workout length and analyzing training volume.
     */
    val durationMinutes: Int,

    /**
     * List of exercises performed during this workout session.
     * Multiple performed exercises can reference the same Exercise template.
     * Order in the list represents the sequence in which exercises were performed.
     */
    val performedExercises: List<PerformedExercise>
)
