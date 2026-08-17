package com.rukavina.gymbuddy.sync

import com.rukavina.gymbuddy.api.dto.UserProfileDto
import com.rukavina.gymbuddy.persistence.UserProfileRepository
import org.springframework.stereotype.Service

/** Read side of UserProfile for pull - the counterpart to ProfileSyncService's write side. */
@Service
class ProfilePullService(private val repository: UserProfileRepository) {
    fun find(uid: String): UserProfileDto? = repository.findByOwner(uid)
}
