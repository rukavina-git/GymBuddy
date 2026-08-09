package com.rukavina.gymbuddy.domain.id

/** Deterministic generator for tests. Emits predictable, ordered ids. */
class FixedIdGenerator(private val prefix: String = "test") : IdGenerator {
    private var counter = 0
    override fun newId(): String = "$prefix-${counter++}"
}