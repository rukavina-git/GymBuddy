package com.rukavina.gymbuddy.sync

import com.rukavina.gymbuddy.auth.FirebaseUserDeleter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

/**
 * DELETE /v1/account's orchestration. Order matters and is not
 * symmetric with anything else in this codebase: database first,
 * Firebase second, no rollback on the second step.
 *
 * If the database delete fails, the transaction rolls back and
 * Firebase is never called - the account keeps working, nothing is
 * half-deleted. If Firebase fails AFTER the database delete commits,
 * the data is correctly gone (the GDPR/App-Store-required guarantee)
 * but the Firebase Auth user still exists; rolling back the database
 * delete to "fix" that would mean a transient Firebase outage
 * resurrects data the caller was told is gone. So that failure is
 * logged loudly for manual cleanup instead, and the request still
 * succeeds - the account holder's data is genuinely deleted either way.
 */
@Service
class AccountDeletionService(
    private val accountDataDeleter: AccountDataDeleter,
    private val advisoryLock: AdvisoryLock,
    private val transactionTemplate: TransactionTemplate,
    private val firebaseUserDeleter: FirebaseUserDeleter,
) {
    private val log = LoggerFactory.getLogger(AccountDeletionService::class.java)

    fun deleteAccount(uid: String) {
        transactionTemplate.execute {
            advisoryLock.acquireForUser(uid)
            accountDataDeleter.deleteAllDataFor(uid)
        }

        try {
            firebaseUserDeleter.deleteUser(uid)
        } catch (e: Exception) {
            log.error(
                "ACCOUNT_DELETE_FIREBASE_FAILED uid={} - all synced data for this account has already " +
                    "been permanently deleted, but the Firebase Auth user could not be removed. " +
                    "MANUAL CLEANUP REQUIRED.",
                uid,
                e,
            )
        }
    }
}
