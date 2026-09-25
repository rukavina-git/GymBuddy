package com.rukavina.gymbuddy.domain.sync

/** Records sync requests instead of running them. */
class RecordingSyncRequester(private val syncNowResult: Boolean = true) : SyncRequester {
    val requested = mutableListOf<SyncReason>()
    val syncedNow = mutableListOf<SyncReason>()

    override fun requestSync(reason: SyncReason) {
        requested += reason
    }

    override suspend fun syncNow(reason: SyncReason): Boolean {
        syncedNow += reason
        return syncNowResult
    }
}
