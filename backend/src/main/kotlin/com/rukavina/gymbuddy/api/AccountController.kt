package com.rukavina.gymbuddy.api

import com.rukavina.gymbuddy.auth.currentUid
import com.rukavina.gymbuddy.sync.AccountDeletionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RestController

/**
 * DELETE /v1/account per api/openapi.yaml. Always the caller's own
 * account - uid comes from the security context, never a payload (see
 * auth/CurrentUser.kt) - so there is nothing to validate here beyond
 * authentication, which the filter chain already guarantees before this
 * method runs.
 */
@RestController
class AccountController(private val accountDeletionService: AccountDeletionService) {

    @DeleteMapping("/v1/account")
    fun deleteAccount(): ResponseEntity<Void> {
        accountDeletionService.deleteAccount(currentUid())
        return ResponseEntity.noContent().build()
    }
}
