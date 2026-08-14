package com.rukavina.gymbuddy.testutil

import com.rukavina.gymbuddy.domain.id.IdGenerator

/**
 * Deterministic IdGenerator for tests. Hands out ids from a fixed
 * sequence (looping if exhausted) and records how many were issued, so
 * a test can assert both the resulting id and how many were minted.
 */
class FixedIdGenerator(
    private val ids: List<String> = listOf("generated-id")
) : IdGenerator {

    constructor(vararg ids: String) : this(ids.toList())

    var callCount: Int = 0
        private set

    override fun newId(): String {
        val id = ids[callCount % ids.size]
        callCount++
        return id
    }
}
