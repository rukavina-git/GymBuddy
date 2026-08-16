package com.rukavina.gymbuddy.auth.testsupport

import com.fasterxml.jackson.databind.ObjectMapper
import com.rukavina.gymbuddy.auth.TokenVerificationException
import com.rukavina.gymbuddy.auth.TokenVerifier
import com.rukavina.gymbuddy.auth.VerifiedIdentity
import java.security.Signature
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.util.Base64

/**
 * Test-only stand-in for FirebaseTokenVerifier: verifies a JWT's
 * signature, issuer, audience and expiry against a fixed test key pair,
 * with no network call. Wired in behind the same TokenVerifier interface
 * FirebaseAuthenticationFilter depends on, so integration tests exercise
 * the real filter chain unmodified - only the verifier implementation
 * differs from production.
 */
class LocalJwtTokenVerifier(
    private val publicKey: RSAPublicKey = TestKeys.publicKey,
    private val expectedIssuer: String = TestJwtBuilder.DEFAULT_ISSUER,
    private val expectedAudience: String = TestJwtBuilder.DEFAULT_AUDIENCE,
) : TokenVerifier {

    private val objectMapper = ObjectMapper()

    override fun verify(idToken: String): VerifiedIdentity {
        val parts = idToken.split(".")
        if (parts.size != 3) {
            throw TokenVerificationException("Malformed JWT: expected 3 dot-separated segments, got ${parts.size}.")
        }
        val (headerB64, payloadB64, signatureB64) = parts
        val signingInput = "$headerB64.$payloadB64"

        val signatureValid = try {
            val signatureBytes = Base64.getUrlDecoder().decode(signatureB64)
            Signature.getInstance("SHA256withRSA").apply {
                initVerify(publicKey)
                update(signingInput.toByteArray(Charsets.UTF_8))
            }.verify(signatureBytes)
        } catch (e: IllegalArgumentException) {
            throw TokenVerificationException("Malformed JWT signature encoding.", e)
        }
        if (!signatureValid) {
            throw TokenVerificationException("JWT signature verification failed.")
        }

        val payloadJson = try {
            String(Base64.getUrlDecoder().decode(payloadB64), Charsets.UTF_8)
        } catch (e: IllegalArgumentException) {
            throw TokenVerificationException("Malformed JWT payload encoding.", e)
        }

        @Suppress("UNCHECKED_CAST")
        val claims = try {
            objectMapper.readValue(payloadJson, Map::class.java) as Map<String, Any?>
        } catch (e: Exception) {
            throw TokenVerificationException("Malformed JWT payload JSON.", e)
        }

        val issuer = claims["iss"] as? String
        if (issuer != expectedIssuer) {
            throw TokenVerificationException("Unexpected issuer: $issuer")
        }

        val audience = claims["aud"] as? String
        if (audience != expectedAudience) {
            throw TokenVerificationException("Unexpected audience: $audience")
        }

        val exp = (claims["exp"] as? Number)?.toLong()
            ?: throw TokenVerificationException("Missing exp claim.")
        if (Instant.ofEpochSecond(exp).isBefore(Instant.now())) {
            throw TokenVerificationException("Token expired.")
        }

        val uid = claims["sub"] as? String ?: throw TokenVerificationException("Missing sub claim.")
        return VerifiedIdentity(
            uid = uid,
            email = claims["email"] as? String,
            name = claims["name"] as? String,
            pictureUrl = claims["picture"] as? String,
        )
    }
}
