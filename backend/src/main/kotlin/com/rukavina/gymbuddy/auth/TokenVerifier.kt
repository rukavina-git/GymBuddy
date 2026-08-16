package com.rukavina.gymbuddy.auth

/** Identity extracted from a verified bearer token. */
data class VerifiedIdentity(
    val uid: String,
    val email: String?,
    val name: String?,
    val pictureUrl: String?,
)

/** Thrown for any rejected token - missing, malformed, expired, wrong issuer/audience, bad signature. */
class TokenVerificationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Verifies a bearer token and extracts the caller's identity. The
 * production implementation (FirebaseTokenVerifier) delegates entirely
 * to the Firebase Admin SDK's verifyIdToken - JWKS fetching/caching,
 * signature verification, and issuer/audience/expiry checks are not
 * reimplemented here. Tests substitute a local, non-network
 * implementation behind this same interface so the rest of the
 * authentication filter chain runs unmodified.
 */
interface TokenVerifier {
    fun verify(idToken: String): VerifiedIdentity
}
