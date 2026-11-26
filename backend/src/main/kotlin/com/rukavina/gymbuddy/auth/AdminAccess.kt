package com.rukavina.gymbuddy.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.authorization.AuthorizationResult
import org.springframework.security.core.Authentication
import org.springframework.security.web.access.intercept.RequestAuthorizationContext
import org.springframework.stereotype.Component
import java.util.function.Supplier

/**
 * The one admin guard, for every route under /v1/admin/ (wired in
 * SecurityConfig, not in a controller). Admins are the Firebase uids in
 * env ADMIN_UIDS (comma list). Unset or empty = nobody. No token is
 * 401 (Spring sends anonymous denials to the entry point); a valid
 * token not on the list is 403. Guests carry Spring's anonymous token
 * whose principal is "anonymousUser": never read that as a uid.
 */
@Component
class AdminAccess(
    @Value("\${gymbuddy.admin.uids:}") rawUids: String,
) : AuthorizationManager<RequestAuthorizationContext> {

    private val uids: Set<String> = rawUids.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()

    fun isAdmin(authentication: Authentication?): Boolean {
        if (authentication == null || authentication is AnonymousAuthenticationToken) return false
        if (!authentication.isAuthenticated) return false
        val uid = authentication.principal as? String ?: return false
        return uid in uids
    }

    override fun authorize(
        authentication: Supplier<out Authentication?>,
        context: RequestAuthorizationContext,
    ): AuthorizationResult = AuthorizationDecision(isAdmin(authentication.get()))
}
