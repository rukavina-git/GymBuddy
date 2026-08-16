package com.rukavina.gymbuddy.api

import com.rukavina.gymbuddy.api.dto.MeResponseDto
import com.rukavina.gymbuddy.auth.currentUid
import com.rukavina.gymbuddy.persistence.ReferenceVersionRepository
import com.rukavina.gymbuddy.persistence.UserProfileRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * GET /v1/me per api/openapi.yaml. The profile is guaranteed to exist
 * by the time this runs: FirebaseAuthenticationFilter provisions it
 * just-in-time before the request reaches here.
 */
@RestController
class MeController(
    private val userProfileRepository: UserProfileRepository,
    private val referenceVersionRepository: ReferenceVersionRepository,
) {

    @GetMapping("/v1/me")
    fun getMe(): MeResponseDto {
        val uid = currentUid()
        val profile = userProfileRepository.findByOwner(uid)
            ?: error("user_profile row for uid=$uid is missing despite just-in-time provisioning")
        val versions = referenceVersionRepository.get()
        return MeResponseDto(
            profile = profile,
            exerciseLibraryVersion = versions.exerciseLibraryVersion,
            templateLibraryVersion = versions.templateLibraryVersion,
            serverTime = System.currentTimeMillis(),
        )
    }
}
