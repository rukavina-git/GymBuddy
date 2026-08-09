package com.rukavina.gymbuddy.domain.id

/**
 * Generates identifiers for domain entities.
 *
 * Injected rather than called statically so use cases remain
 * deterministic under test.
 */
interface IdGenerator {
    fun newId(): String
}