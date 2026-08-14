package com.rukavina.gymbuddy.domain.model

/**
 * Local bookkeeping for the future sync engine (Phase 4): whether this
 * device's copy of a row still needs to be pushed to the server.
 *
 * Purely local state - it is never sent to a server and never received
 * from one. A server response can only ever change it indirectly, by
 * causing local code (written in Phase 4) to set it.
 */
enum class SyncState {
    SYNCED,
    PENDING,
    CONFLICTED
}
