package com.rukavina.gymbuddy.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * Production TokenVerifier. Delegates entirely to the Admin SDK, which
 * handles JWKS fetching/caching, signature verification, and
 * issuer/audience/expiry checks - none of that is reimplemented here.
 */
@Component
@Profile("!test")
class FirebaseTokenVerifier(private val firebaseAuth: FirebaseAuth) : TokenVerifier {

    override fun verify(idToken: String): VerifiedIdentity {
        val decoded = try {
            firebaseAuth.verifyIdToken(idToken)
        } catch (e: FirebaseAuthException) {
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
}
