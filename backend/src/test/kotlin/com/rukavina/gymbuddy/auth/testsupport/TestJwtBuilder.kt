package com.rukavina.gymbuddy.auth.testsupport

import com.fasterxml.jackson.databind.ObjectMapper
import java.security.PrivateKey
import java.security.Signature
import java.time.Instant
import java.util.Base64

/**
 * Hand-crafts signed JWTs for tests - no Firebase call, no third-party
 * JWT library, just JDK crypto - so FirebaseAuthenticationFilterTest can
 * exercise every rejection path (expired, wrong issuer, wrong audience,
 * malformed, bad signature) deterministically and offline.
 */
class TestJwtBuilder {
    companion object {
        const val DEFAULT_ISSUER = "https://securetoken.google.com/gymbuddy-test"
        const val DEFAULT_AUDIENCE = "gymbuddy-test"
        const val DEFAULT_SUBJECT = "test-uid-0001"
    }

    private val objectMapper = ObjectMapper()
    private var issuer: String = DEFAULT_ISSUER
    private var audience: String = DEFAULT_AUDIENCE
    private var subject: String = DEFAULT_SUBJECT
    private var issuedAt: Instant = Instant.now()
    private var expiresAt: Instant = Instant.now().plusSeconds(3600)
    private var extraClaims: MutableMap<String, Any?> = mutableMapOf()
    private var privateKey: PrivateKey = TestKeys.privateKey

    fun issuer(value: String) = apply { issuer = value }
    fun audience(value: String) = apply { audience = value }
    fun subject(value: String) = apply { subject = value }
    fun expiresAt(value: Instant) = apply { expiresAt = value }
    fun issuedAt(value: Instant) = apply { issuedAt = value }
    fun claim(key: String, value: Any?) = apply { extraClaims[key] = value }
    fun signedWith(key: PrivateKey) = apply { privateKey = key }

    fun build(): String {
        val header = mapOf("alg" to "RS256", "typ" to "JWT")
        val payload = mutableMapOf<String, Any?>(
            "iss" to issuer,
            "aud" to audience,
            "sub" to subject,
            "iat" to issuedAt.epochSecond,
            "exp" to expiresAt.epochSecond,
        )
        payload.putAll(extraClaims)

        val encodedHeader = encode(objectMapper.writeValueAsBytes(header))
        val encodedPayload = encode(objectMapper.writeValueAsBytes(payload))
        val signingInput = "$encodedHeader.$encodedPayload"
        val signature = Signature.getInstance("SHA256withRSA").apply {
            initSign(privateKey)
            update(signingInput.toByteArray(Charsets.UTF_8))
        }.sign()

        return "$signingInput.${encode(signature)}"
    }

    private fun encode(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}
