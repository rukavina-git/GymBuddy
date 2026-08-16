package com.rukavina.gymbuddy.auth

import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.Base64

/**
 * Initialises the Firebase Admin SDK from FIREBASE_CREDENTIALS_B64 - the
 * base64-encoded service account JSON, supplied only as an env var
 * (Keychain locally, the host environment in production). There is no
 * key file on disk anywhere; see backend/README.md for how to supply it.
 *
 * This is a @Bean, not a lazily-resolved property: Spring instantiates
 * singleton beans during context refresh, before the app starts serving
 * requests, so a missing or invalid credential fails application startup
 * loudly (Spring Boot's "APPLICATION FAILED TO START" banner) rather
 * than surfacing as a 401/500 on the first real request.
 *
 * Excluded under the "test" profile: tests substitute a local
 * TokenVerifier (see auth/testsupport) that never touches Firebase, so
 * there is nothing here for them to configure.
 */
@Configuration
@Profile("!test")
class FirebaseAdminConfig {

    private val logger = LoggerFactory.getLogger(FirebaseAdminConfig::class.java)

    @Bean
    fun firebaseApp(@Value("\${FIREBASE_CREDENTIALS_B64:}") credentialsB64: String): FirebaseApp {
        if (credentialsB64.isBlank()) {
            throw IllegalStateException(
                "FIREBASE_CREDENTIALS_B64 is not set. Provide the base64-encoded Firebase " +
                    "service account JSON via that environment variable - see backend/README.md " +
                    "for how to supply it locally (macOS Keychain) and in production."
            )
        }

        val decoded = try {
            Base64.getDecoder().decode(credentialsB64)
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException("FIREBASE_CREDENTIALS_B64 is not valid base64.", e)
        }

        val credentials = try {
            GoogleCredentials.fromStream(ByteArrayInputStream(decoded))
        } catch (e: IOException) {
            throw IllegalStateException(
                "FIREBASE_CREDENTIALS_B64 did not decode to a valid Firebase service account JSON.", e
            )
        }

        // FirebaseOptions doesn't derive projectId from the credentials on its
        // own - it must be set explicitly, or FirebaseApp.getOptions().projectId
        // (used for logging below, and by anything else that reads it) is null.
        val projectId = (credentials as? ServiceAccountCredentials)?.projectId
        val options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .apply { if (projectId != null) setProjectId(projectId) }
            .build()
        val app = if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
        } else {
            FirebaseApp.getInstance()
        }
        logger.info("Firebase Admin SDK initialised for project '{}'", app.options.projectId)
        return app
    }

    @Bean
    fun firebaseAuth(firebaseApp: FirebaseApp): FirebaseAuth = FirebaseAuth.getInstance(firebaseApp)
}
