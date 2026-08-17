package com.rukavina.gymbuddy.sync

import com.fasterxml.jackson.databind.ObjectMapper
import com.rukavina.gymbuddy.api.dto.PushRequestDto
import com.rukavina.gymbuddy.api.dto.PushResponseDto
import com.rukavina.gymbuddy.auth.FirebaseUserDeleter
import com.rukavina.gymbuddy.auth.testsupport.TestJwtBuilder
import com.rukavina.gymbuddy.auth.testsupport.TestSecurityConfig
import com.rukavina.gymbuddy.testsupport.AbstractPostgresIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * End-to-end coverage of DELETE /v1/account against a real Postgres
 * (Testcontainers). FirebaseUserDeleter is mocked - the point here is
 * the database side and the ordering contract, not the Admin SDK itself
 * (FirebaseAdminUserDeleter is excluded under the "test" profile
 * regardless - see its class doc). The database-failure path lives in
 * AccountDeletionFailureTest, in its own Spring context, since it needs
 * AccountDataDeleter mocked too and this class deliberately uses the
 * real one so the deletion assertions below mean something.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestSecurityConfig::class)
class AccountDeletionTest : AbstractPostgresIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @MockitoBean
    private lateinit var firebaseUserDeleter: FirebaseUserDeleter

    private fun newUid(label: String) = "$label-${System.nanoTime()}"

    private fun bearerToken(uid: String): String = TestJwtBuilder().subject(uid).build()

    private fun push(uid: String, request: PushRequestDto): PushResponseDto {
        val body = mockMvc.perform(
            post("/v1/sync/push")
                .header("Authorization", "Bearer ${bearerToken(uid)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andReturn().response.contentAsString
        return objectMapper.readValue(body, PushResponseDto::class.java)
    }

    private fun pushFullSetOf(uid: String): Triple<String, String, String> {
        val session = SyncTestFixtures.workoutSession()
        val exercise = SyncTestFixtures.exercise()
        val template = SyncTestFixtures.workoutTemplate()
        push(
            uid,
            PushRequestDto(
                workoutSessions = listOf(session),
                exercises = listOf(exercise),
                workoutTemplates = listOf(template),
                userExerciseStates = listOf(SyncTestFixtures.userExerciseState(exerciseId = exercise.id)),
                userTemplateStates = listOf(SyncTestFixtures.userTemplateState(templateId = template.id)),
                userProfile = SyncTestFixtures.userProfile(),
            ),
        )
        return Triple(session.id, exercise.id, template.id)
    }

    private fun rowCounts(uid: String): Map<String, Int> = mapOf(
        "workout_sessions" to count("workout_sessions", "owner_id", uid),
        "performed_exercises" to jdbcTemplate.queryForObject(
            "SELECT count(*) FROM performed_exercises pe JOIN workout_sessions s ON s.id = pe.workout_session_id WHERE s.owner_id = :uid",
            mapOf("uid" to uid),
            Int::class.java,
        )!!,
        "exercises" to count("exercises", "owner_id", uid),
        "workout_templates" to count("workout_templates", "owner_id", uid),
        "template_exercises" to jdbcTemplate.queryForObject(
            "SELECT count(*) FROM template_exercises te JOIN workout_templates t ON t.id = te.template_id WHERE t.owner_id = :uid",
            mapOf("uid" to uid),
            Int::class.java,
        )!!,
        "user_exercise_state" to count("user_exercise_state", "owner_id", uid),
        "user_template_state" to count("user_template_state", "owner_id", uid),
        "user_profile" to count("user_profile", "uid", uid),
        "change_log" to count("change_log", "user_id", uid),
    )

    private fun count(table: String, column: String, uid: String): Int =
        jdbcTemplate.queryForObject("SELECT count(*) FROM $table WHERE $column = :uid", mapOf("uid" to uid), Int::class.java)!!

    @Test
    fun `account deletion removes every row across every table for that user and leaves another user's data untouched`() {
        val target = newUid("delete-me")
        val other = newUid("keep-me")
        pushFullSetOf(target)
        pushFullSetOf(other)

        val beforeOther = rowCounts(other)
        assertTrue(beforeOther.values.all { it > 0 }, "precondition: the other user has real data in every table")

        mockMvc.perform(delete("/v1/account").header("Authorization", "Bearer ${bearerToken(target)}"))
            .andExpect(status().isNoContent)

        val afterTarget = rowCounts(target)
        assertTrue(afterTarget.values.all { it == 0 }, "expected every table empty for the deleted user, got: $afterTarget")

        val afterOther = rowCounts(other)
        assertEquals(beforeOther, afterOther, "another user's data must be completely untouched")
    }

    @Test
    fun `the Firebase user is deleted with the caller's own uid`() {
        val uid = newUid("firebase-delete")
        pushFullSetOf(uid)

        mockMvc.perform(delete("/v1/account").header("Authorization", "Bearer ${bearerToken(uid)}"))
            .andExpect(status().isNoContent)

        verify(firebaseUserDeleter).deleteUser(uid)
    }

    private fun assertTrue(condition: Boolean, message: String) {
        org.junit.jupiter.api.Assertions.assertTrue(condition, message)
    }
}
