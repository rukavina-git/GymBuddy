package com.rukavina.gymbuddy.auth

import com.google.firebase.auth.AuthErrorCode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * Production TokenVerifier. Delegates entirely to the Admin SDK, which
 * handles JWKS fetching/caching, signature verification, and
 * issuer/audience/expiry checks - none of that is reimplemented here.
 *
 * checkRevoked = true (Plan 9 step 04): the SDK also looks the user up,
 * so a token of a deleted, disabled or revoked account is rejected at
 * once instead of working until it expires (up to 1 h). Without it a
 * phone still signed in could push after DELETE /v1/account and
 * recreate rows under the deleted uid. Costs one Firebase call per
 * request. If that lookup fails for a reason that says nothing about
 * the token (Firebase unreachable), it is a 500, not a 401: a 401
 * would make the app treat a Firebase outage as an expired session.
 */
@Component
@Profile("!test")
class FirebaseTokenVerifier(private val firebaseAuth: FirebaseAuth) : TokenVerifier {

    override fun verify(idToken: String): VerifiedIdentity {
        val decoded = try {
            firebaseAuth.verifyIdToken(idToken, true)
        } catch (e: FirebaseAuthException) {
            if (e.authErrorCode in UNAVAILABLE) {
                throw TokenVerifierUnavailableException("Firebase could not check the token (${e.authErrorCode ?: e.errorCode}).", e)
            }
            throw TokenVerificationException("Firebase rejected the token (${e.authErrorCode}).", e)
        } catch (e: IllegalArgumentException) {
            throw TokenVerificationException("Malformed bearer token.", e)
        }
        return VerifiedIdentity(
            uid = decoded.uid,
            email = decoded.email,
            name = decoded.name,
            pictureUrl = decoded.picture,
        )
    }

    private companion object {
        /** No auth code: the lookup failed for a non-token reason (network, Firebase 5xx, service account config). */
        val UNAVAILABLE = setOf(null, AuthErrorCode.CERTIFICATE_FETCH_FAILED)
    }
}
