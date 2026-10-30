package com.rukavina.gymbuddy.sync

import com.rukavina.gymbuddy.api.dto.MutationResultDto
import com.rukavina.gymbuddy.api.dto.PullResponseDto
import com.rukavina.gymbuddy.domain.EntityType
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

/**
 * Sync volume counters. Conflicts are `gymbuddy.sync.push.results` with
 * status=CONFLICT; a 410 resync is `gymbuddy.sync.cursor.expired`.
 * Registered in the actuator's MeterRegistry only - no endpoint beyond
 * what actuator already exposes (health).
 */
@Component
class SyncMetrics(private val registry: MeterRegistry) {

    fun recordPush(results: List<MutationResultDto>) {
        results.forEach {
            registry.counter(PUSH_RESULTS, "entityType", it.entityType.name, "status", it.status.name).increment()
        }
    }

    fun recordPull(response: PullResponseDto) {
        pulled(EntityType.WORKOUT_SESSION, response.workoutSessions.size)
        pulled(EntityType.EXERCISE, response.exercises.size)
        pulled(EntityType.WORKOUT_TEMPLATE, response.workoutTemplates.size)
        pulled(EntityType.USER_EXERCISE_STATE, response.userExerciseStates.size)
        pulled(EntityType.USER_TEMPLATE_STATE, response.userTemplateStates.size)
        pulled(EntityType.USER_PROFILE, if (response.userProfile != null) 1 else 0)
    }

    fun recordCursorExpired() {
        registry.counter(CURSOR_EXPIRED).increment()
    }

    private fun pulled(type: EntityType, count: Int) {
        if (count > 0) registry.counter(PULL_ENTITIES, "entityType", type.name).increment(count.toDouble())
    }

    companion object {
        const val PUSH_RESULTS = "gymbuddy.sync.push.results"
        const val PULL_ENTITIES = "gymbuddy.sync.pull.entities"
        const val CURSOR_EXPIRED = "gymbuddy.sync.cursor.expired"
    }
}
