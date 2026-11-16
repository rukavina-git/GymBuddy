package com.rukavina.gymbuddy.auth

import com.google.firebase.ErrorCode
import com.google.firebase.auth.AuthErrorCode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseToken
import com.rukavina.gymbuddy.api.ErrorResponseWriter
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import tools.jackson.databind.json.JsonMapper

/**
 * Plan 9 step 04: tokens of deleted accounts stop working at once
 * (checkRevoked), and a Firebase outage is a 500, not a 401. The Admin
 * SDK is mocked; FirebaseTokenVerifier itself is off under "test".
 */
class FirebaseTokenVerifierTest {

    private val firebaseAuth = mock(FirebaseAuth::class.java)
    private val verifier = FirebaseTokenVerifier(firebaseAuth)

    private fun failure(code: AuthErrorCode?, error: ErrorCode = ErrorCode.INVALID_ARGUMENT) =
        FirebaseAuthException(error, "test", null, null, code)

    @Test
    fun `checks revocation, so a live token passes`() {
        val token = mock(FirebaseToken::class.java)
        given(token.uid).willReturn("liveUid1")
        given(firebaseAuth.verifyIdToken("t", true)).willReturn(token)

        assertEquals("liveUid1", verifier.verify("t").uid)
        verify(firebaseAuth, never()).verifyIdToken("t")
    }

    @Test
    fun `deleted, disabled or revoked account is a rejected token`() {
        listOf(AuthErrorCode.USER_NOT_FOUND, AuthErrorCode.USER_DISABLED, AuthErrorCode.REVOKED_ID_TOKEN).forEach { code ->
            willThrow(failure(code)).given(firebaseAuth).verifyIdToken("t", true)
            assertThrows<TokenVerificationException> { verifier.verify("t") }
        }
    }

    @Test
    fun `Firebase unreachable is unavailable, not rejected`() {
        willThrow(failure(null, ErrorCode.UNAVAILABLE)).given(firebaseAuth).verifyIdToken("t", true)
        assertThrows<TokenVerifierUnavailableException> { verifier.verify("t") }

        willThrow(failure(AuthErrorCode.CERTIFICATE_FETCH_FAILED)).given(firebaseAuth).verifyIdToken("t", true)
        assertThrows<TokenVerifierUnavailableException> { verifier.verify("t") }
    }

    @Test
    fun `filter answers 500 and stops when the token can't be checked`() {
        val provisioning = mock(UserProvisioningService::class.java)
        val filter = FirebaseAuthenticationFilter(
            { throw TokenVerifierUnavailableException("down") },
            provisioning,
            ErrorResponseWriter(JsonMapper()),
        )
        val chain = mock(FilterChain::class.java)
        val request = MockHttpServletRequest("POST", "/v1/sync/push").apply { addHeader("Authorization", "Bearer t") }
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, chain)

        assertEquals(500, response.status)
        assertEquals(true, response.contentAsString.contains("\"error\":\"INTERNAL\""))
        verifyNoInteractions(chain, provisioning)
    }
}
