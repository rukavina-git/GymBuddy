package com.rukavina.gymbuddy.auth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.KeyPairGenerator
import java.util.Base64

/**
 * Pure unit test - no Spring context. firebaseApp() is a plain function
 * of its @Value-injected String parameter, so failure modes (unset,
 * not base64, not JSON, not a service account) are exercised directly
 * without needing to fake an environment variable or start the app.
 */
class FirebaseAdminConfigTest {

    private val config = FirebaseAdminConfig()

    @Test
    fun `blank credentials fails fast with a clear message`() {
        val ex = assertThrows(IllegalStateException::class.java) { config.firebaseApp("") }
        assertTrue(ex.message!!.contains("FIREBASE_CREDENTIALS_B64 is not set"), ex.message)
    }

    @Test
    fun `invalid base64 fails fast with a clear message`() {
        val ex = assertThrows(IllegalStateException::class.java) { config.firebaseApp("not-valid-base64!!!") }
        assertTrue(ex.message!!.contains("not valid base64"), ex.message)
    }

    @Test
    fun `base64 of garbage bytes fails fast with a clear message`() {
        val garbage = Base64.getEncoder().encodeToString(byteArrayOf(0x00, 0x01, 0x02, 0x03))
        val ex = assertThrows(IllegalStateException::class.java) { config.firebaseApp(garbage) }
        assertTrue(ex.message!!.contains("did not decode to a valid Firebase service account JSON"), ex.message)
    }

    @Test
    fun `base64 of valid JSON that is not a service account fails fast`() {
        val notAServiceAccount = Base64.getEncoder().encodeToString("""{"hello":"world"}""".toByteArray())
        val ex = assertThrows(IllegalStateException::class.java) { config.firebaseApp(notAServiceAccount) }
        assertTrue(ex.message!!.contains("did not decode to a valid Firebase service account JSON"), ex.message)
    }

    @Test
    fun `valid service account JSON initialises FirebaseApp`() {
        val projectId = "gymbuddy-valid-test"
        val b64 = Base64.getEncoder().encodeToString(syntheticServiceAccountJson(projectId).toByteArray())

        val app = config.firebaseApp(b64)
        try {
            assertEquals(projectId, app.options.projectId)
        } finally {
            app.delete()
        }
    }

    /** A syntactically valid, self-signed service account JSON - never registered with Google, only used to exercise parsing. */
    private fun syntheticServiceAccountJson(projectId: String): String {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val pkcs8Base64 = Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(keyPair.private.encoded)
        val pem = "-----BEGIN PRIVATE KEY-----\n$pkcs8Base64\n-----END PRIVATE KEY-----\n".replace("\n", "\\n")
        return """
            {
              "type": "service_account",
              "project_id": "$projectId",
              "private_key_id": "test-key-id",
              "private_key": "$pem",
              "client_email": "test@$projectId.iam.gserviceaccount.com",
              "client_id": "123456789",
              "auth_uri": "https://accounts.google.com/o/oauth2/auth",
              "token_uri": "https://oauth2.googleapis.com/token",
              "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
              "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/test%40$projectId.iam.gserviceaccount.com"
            }
        """.trimIndent()
    }
}
