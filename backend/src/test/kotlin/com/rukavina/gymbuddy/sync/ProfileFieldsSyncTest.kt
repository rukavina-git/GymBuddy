package com.rukavina.gymbuddy.sync

import com.fasterxml.jackson.databind.ObjectMapper
import com.rukavina.gymbuddy.api.dto.PullResponseDto
import com.rukavina.gymbuddy.api.dto.PushRequestDto
import com.rukavina.gymbuddy.api.dto.PushResponseDto
import com.rukavina.gymbuddy.auth.testsupport.TestJwtBuilder
import com.rukavina.gymbuddy.auth.testsupport.TestSecurityConfig
import com.rukavina.gymbuddy.domain.ExperienceLevel
import com.rukavina.gymbuddy.domain.SyncStatus
import com.rukavina.gymbuddy.domain.TrainingDays
import com.rukavina.gymbuddy.testsupport.AbstractPostgresIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** Plan 6 step 05: experience, avatar, training days and the consent record on the profile. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestSecurityConfig::class)
class ProfileFieldsSyncTest : AbstractPostgresIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private fun newUid(label: String) = "$label-${System.nanoTime()}"

    private fun bearerToken(uid: String): String = TestJwtBuilder().subject(uid).build()

    private fun push(uid: String, request: PushRequestDto): PushResponseDto {
        val body = mockMvc.perform(
            post("/v1/sync/push")
                .header("Authorization", "Bearer ${bearerToken(uid)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isOk).andReturn().response.contentAsString
        return objectMapper.readValue(body, PushResponseDto::class.java)
    }

    private fun pull(uid: String): PullResponseDto {
        val body = mockMvc.perform(get("/v1/sync/pull").header("Authorization", "Bearer ${bearerToken(uid)}"))
            .andExpect(status().isOk).andReturn().response.contentAsString
        return objectMapper.readValue(body, PullResponseDto::class.java)
    }

    @Test
    fun `new profile fields push and come back on pull`() {
        val uid = newUid("fields")
        val profile = SyncTestFixtures.userProfile().copy(
            experienceLevel = ExperienceLevel.SOME,
            avatarId = "red-panda",
            trainingDays = TrainingDays.FOUR,
            healthDataConsentVersion = "2026-11",
            healthDataConsentAt = 1_762_000_000_000,
        )

        assertEquals(SyncStatus.APPLIED, push(uid, PushRequestDto(userProfile = profile)).results.single().status)

        val pulled = pull(uid).userProfile!!
        assertEquals(ExperienceLevel.SOME, pulled.experienceLevel)
        assertEquals("red-panda", pulled.avatarId)
        assertEquals(TrainingDays.FOUR, pulled.trainingDays)
        assertEquals("2026-11", pulled.healthDataConsentVersion)
        assertEquals(1_762_000_000_000, pulled.healthDataConsentAt)
    }

    @Test
    fun `fields left null stay null`() {
        val uid = newUid("fields-null")
        push(uid, PushRequestDto(userProfile = SyncTestFixtures.userProfile()))

        val pulled = pull(uid).userProfile!!
        assertNull(pulled.experienceLevel)
        assertNull(pulled.avatarId)
        assertNull(pulled.trainingDays)
        assertNull(pulled.healthDataConsentVersion)
        assertNull(pulled.healthDataConsentAt)
    }

    @Test
    fun `a malformed avatar id or an over-long consent version is INVALID`() {
        val uid = newUid("fields-invalid")
        val badAvatar = push(uid, PushRequestDto(userProfile = SyncTestFixtures.userProfile().copy(avatarId = "Cat Photo.png")))
        assertEquals(SyncStatus.INVALID, badAvatar.results.single().status)

        val longVersion = push(uid, PushRequestDto(userProfile = SyncTestFixtures.userProfile().copy(healthDataConsentVersion = "x".repeat(33))))
        assertEquals(SyncStatus.INVALID, longVersion.results.single().status)
        assertNull(pull(uid).userProfile?.avatarId)
    }

    @Test
    fun `activePlanId pushes, comes back on pull and can be cleared`() {
        val uid = newUid("active-plan")
        val planId = "01a19f0f-dc18-7975-8b8f-61819042e23a"
        push(uid, PushRequestDto(userProfile = SyncTestFixtures.userProfile().copy(activePlanId = planId)))
        val pulled = pull(uid).userProfile!!
        assertEquals(planId, pulled.activePlanId)

        val cleared = push(uid, PushRequestDto(userProfile = SyncTestFixtures.userProfile().copy(revision = pulled.revision)))
        assertEquals(SyncStatus.APPLIED, cleared.results.single().status)
        assertNull(pull(uid).userProfile!!.activePlanId)
    }

    @Test
    fun `an activePlanId that is not a UUID is INVALID`() {
        val uid = newUid("active-plan-invalid")
        val result = push(uid, PushRequestDto(userProfile = SyncTestFixtures.userProfile().copy(activePlanId = "full-body")))
        assertEquals(SyncStatus.INVALID, result.results.single().status)
    }
}
