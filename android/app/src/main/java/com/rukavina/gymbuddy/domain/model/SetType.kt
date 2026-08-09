package com.rukavina.gymbuddy.domain.model

/**
 * Role a set plays within an exercise, used to distinguish sets that
 * should be excluded from or weighted differently in volume calculations.
 */
enum class SetType {
    WARMUP,   // excluded from volume calculations later
    WORKING,  // the default
    DROP,     // performed immediately after a working set, reduced load
    FAILURE   // taken to muscular failure
}
