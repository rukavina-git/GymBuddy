package com.rukavina.gymbuddy.auth

import org.springframework.security.core.context.SecurityContextHolder

/**
 * The only sanctioned way to get "who is calling" into a controller:
 * always the SecurityContext, which FirebaseAuthenticationFilter
 * populated from a verified token - never a client-supplied field.
 * Controllers on a protected route can call this unconditionally: an
 * unauthenticated request never reaches them (RestAuthenticationEntryPoint
 * short-circuits it first).
 */
fun currentUid(): String =
    SecurityContextHolder.getContext().authentication?.principal as? String
        ?: error("currentUid() called without an authenticated principal in the SecurityContext")
