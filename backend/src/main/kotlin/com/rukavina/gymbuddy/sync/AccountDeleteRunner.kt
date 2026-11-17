package com.rukavina.gymbuddy.sync

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * Deletes one account from the command line, for privacy@ requests from
 * people without the app (Plan 9 step 03). Runs the same
 * AccountDeletionService as DELETE /v1/account, so the two can't drift.
 *
 * Only under the "account-delete" profile, which also turns the web
 * server off (application-account-delete.yml); main() exits once this
 * returns. Started by docker/server/delete-account.sh with the uid in
 * ACCOUNT_DELETE_UID. Throwing fails startup, so the process exits
 * non-zero and the script reports the failure.
 */
@Component
@Profile("account-delete")
class AccountDeleteRunner(
    private val accountDeletionService: AccountDeletionService,
    @Value("\${gymbuddy.account-delete.uid:}") private val uid: String,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(AccountDeleteRunner::class.java)

    override fun run(args: ApplicationArguments) {
        require(UID.matches(uid)) { "ACCOUNT_DELETE_UID is not a Firebase uid: '$uid'" }
        val result = accountDeletionService.deleteAccount(uid)
        log.info("ACCOUNT_DELETE_DONE uid={} rows={} firebaseUserDeleted={}", uid, result.rows, result.firebaseUserDeleted)
        check(result.firebaseUserDeleted) {
            "Data deleted ($uid, ${result.rows} rows), but the Firebase user was not. " +
                "Delete it in the Firebase console (Authentication → Users)."
        }
    }

    companion object {
        /** Firebase uids are 1-128 chars; ours are alphanumeric. */
        val UID = Regex("^[A-Za-z0-9]{1,128}$")
    }
}
