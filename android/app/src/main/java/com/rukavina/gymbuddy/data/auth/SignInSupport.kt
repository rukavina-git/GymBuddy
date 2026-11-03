package com.rukavina.gymbuddy.data.auth

import com.rukavina.gymbuddy.data.local.db.DatabaseHolder
import com.rukavina.gymbuddy.data.sync.DataOwnerStore
import javax.inject.Inject

/** What the sign-in screens need to know about the data on this phone (ADR 0014). */
interface SignInSupport {
    /** The account whose data is open, if any (a dead session keeps it). */
    val ownerEmail: String?

    /** Changes in the open account's file that haven't backed up yet; 0 for a guest. */
    suspend fun pendingChanges(): Int
}

class DefaultSignInSupport @Inject constructor(
    private val holder: DatabaseHolder,
    private val owners: DataOwnerStore
) : SignInSupport {
    override val ownerEmail: String? get() = owners.email

    override suspend fun pendingChanges(): Int =
        if (holder.owner == null) 0 else holder.db.syncDao().outboxCount()
}
