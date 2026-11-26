package com.rukavina.gymbuddy.domain.search

/**
 * Other names people search for, keyed by built-in exercise id. Search
 * only: the exercise keeps its name. App-side on purpose (Plan 10 step
 * 05a): no Room column, no API field. Ids from default_exercises.json.
 */
object ExerciseAliases {

    private val byId: Map<String, List<String>> = mapOf(
        // Hip Thrust
        "019fe506-35c5-7e9d-9bff-db6cca316522" to listOf("Glute Bridge"),
        // Tricep Pushdown
        "019fe506-35b5-7334-bbaf-0cfabc29cfbd" to listOf("Rope Pushdown", "Tricep Rope"),
        // Cable Crossover
        "019fe506-35a7-777f-ac60-67574cbc06f7" to listOf("Cable Fly"),
    )

    /** Ids that have aliases (tests check they exist in the bundle). */
    val ids: Set<String> get() = byId.keys

    fun forId(id: String): List<String> = byId[id.lowercase()].orEmpty()

    /** Best match of [query] against [name] and the aliases of [id]. */
    fun match(query: String, id: String, name: String): TypoMatcher.Match? =
        (sequenceOf(name) + forId(id).asSequence())
            .mapNotNull { TypoMatcher.match(query, it) }
            .minOrNull()
}
