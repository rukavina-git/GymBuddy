package com.rukavina.gymbuddy.data.repository

import com.rukavina.gymbuddy.domain.model.UserProfile
import kotlinx.coroutines.flow.first
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The health-data consent from Create account (Plan 7 step 04). The box
 * is ticked before the account exists, so SignInViewModel parks the time in preferences
 * until the account's file is open and any guest merge has run, then
 * lands on that account's profile (decision 2A of Plan 6 step 05:
 * recorded, not enforced).
 */
@Singleton
class ConsentRecorder @Inject constructor(
    private val preferences: AppPreferencesRepository,
    private val profiles: UserProfileRepository,
    private val clock: Clock
) {

    /** The user didn't go through with it (merge Cancel). */
    suspend fun clear() = preferences.setPendingConsentAt(null)

    /**
     * Writes a pending consent onto [uid]'s profile, creating a bare
     * profile if the account has none yet. Does nothing unless [uid]'s
     * file is the open one.
     */
    suspend fun applyTo(uid: String) {
        val at = preferences.pendingConsentAt.first() ?: return
        if (profiles.isGuest || profiles.ownerUid != uid) return
        profiles.updateProfile(uid, { UserProfile(uid = uid, name = "", email = "", joinedDate = clock.millis()) }) {
            it.copy(healthDataConsentVersion = CONSENT_VERSION, healthDataConsentAt = at)
        }
        clear()
    }

    companion object {
        /** Wording version of the consent box. Final text comes with the privacy pass (roadmap 4.6). */
        const val CONSENT_VERSION = "2026-11-03"
    }
}
