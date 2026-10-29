package com.rukavina.gymbuddy.domain.sync

/** Records sync requests instead of running them. */
class RecordingSyncRequester(private val syncNowResult: SyncOutcome = SyncOutcome.SYNCED) : SyncRequester {
    val requested = mutableListOf<SyncReason>()
    val syncedNow = mutableListOf<SyncReason>()

    override fun requestSync(reason: SyncReason) {
        requested += reason
    }

    override suspend fun syncNow(reason: SyncReason): SyncOutcome {
        syncedNow += reason
        return syncNowResult
    }
}
