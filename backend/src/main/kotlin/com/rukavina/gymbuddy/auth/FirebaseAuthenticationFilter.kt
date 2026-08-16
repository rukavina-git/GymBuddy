package com.rukavina.gymbuddy.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

private const val BEARER_PREFIX = "Bearer "

/**
 * Verifies the bearer token on every request. Runs unconditionally
 * (including for /v1/health) but only ever sets the SecurityContext on
 * success; on any failure - missing header, malformed header, or a
 * token verifyIdToken rejects - it leaves the context unauthenticated
 * and lets the request continue down the chain. For a protected route
 * that means authorizeHttpRequests denies it and RestAuthenticationEntryPoint
 * produces the 401 Error body; for /v1/health (permitAll) it makes no
 * difference. This is what lets one filter serve every failure mode in
 * the spec without special-casing any of them.
 *
 * The uid goes into the SecurityContext as the Authentication principal
 * - not into a request attribute or header - so it is available
 * anywhere the SecurityContext is (controllers, later filters like rate
 * limiting) via the standard Spring Security API.
 */
@Component
class FirebaseAuthenticationFilter(
    private val tokenVerifier: TokenVerifier,
    private val userProvisioningService: UserProvisioningService,
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(FirebaseAuthenticationFilter::class.java)

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val header = request.getHeader(HttpHeaders.AUTHORIZATION)
        val token = header?.takeIf { it.startsWith(BEARER_PREFIX) }?.removePrefix(BEARER_PREFIX)?.trim()

        if (!token.isNullOrEmpty()) {
            try {
                val identity = tokenVerifier.verify(token)
                userProvisioningService.ensureProfile(identity)
                SecurityContextHolder.getContext().authentication =
                    UsernamePasswordAuthenticationToken(identity.uid, null, emptyList())
            } catch (e: TokenVerificationException) {
                log.debug("Rejected bearer token on {} {}: {}", request.method, request.requestURI, e.message)
            }
        }

        filterChain.doFilter(request, response)
    }
}
