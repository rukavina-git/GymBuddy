package com.rukavina.gymbuddy.auth

import com.rukavina.gymbuddy.persistence.UserProfileRepository
import org.springframework.stereotype.Service

/**
 * Just-in-time provisioning: on first sight of a uid with no
 * user_profile row, create one from the token's claims. Tolerates
 * missing claims - some providers supply no display name, and some
 * (e.g. Sign in with Apple's private relay) supply an email that isn't
 * the user's real address but is still a valid, usable one; a provider
 * that supplies no email at all still needs a non-null placeholder,
 * since the column is NOT NULL.
 *
 * Race safety is delegated to UserProfileRepository.insertIfAbsent's
 * ON CONFLICT DO NOTHING - not enforced here.
 */
@Service
class UserProvisioningService(private val userProfileRepository: UserProfileRepository) {

    fun ensureProfile(identity: VerifiedIdentity) {
        val name = identity.name?.trim()?.takeIf { it.isNotEmpty() }
            ?: identity.email?.substringBefore("@")?.trim()?.takeIf { it.isNotEmpty() }
            ?: "GymBuddy User"
        val email = identity.email?.trim()?.takeIf { it.isNotEmpty() }
            ?: "${identity.uid}@no-email.gymbuddy.local"

        userProfileRepository.insertIfAbsent(
            uid = identity.uid,
            name = name,
            email = email,
            profileImageUrl = identity.pictureUrl,
        )
    }
}
