package com.rukavina.gymbuddy.sync

import com.rukavina.gymbuddy.auth.FirebaseUserDeleter
import com.rukavina.gymbuddy.auth.testsupport.TestJwtBuilder
import com.rukavina.gymbuddy.auth.testsupport.TestSecurityConfig
import com.rukavina.gymbuddy.testsupport.AbstractPostgresIntegrationTest
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.willThrow
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * A database failure during account deletion must never reach Firebase
 * - dedicated Spring context (AccountDataDeleter mocked to throw) so
 * AccountDeletionTest's happy-path assertions keep using the real
 * deletion path. See AccountDeletionService's header for why the two
 * steps are ordered this way.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestSecurityConfig::class)
class AccountDeletionFailureTest : AbstractPostgresIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var accountDataDeleter: AccountDataDeleter

    @MockitoBean
    private lateinit var firebaseUserDeleter: FirebaseUserDeleter

    private fun bearerToken(uid: String): String = TestJwtBuilder().subject(uid).build()

    @Test
    fun `a database failure during deletion does not delete the Firebase user`() {
        val uid = "delete-db-failure-${System.nanoTime()}"
        willThrow(RuntimeException("simulated database outage")).given(accountDataDeleter).deleteAllDataFor(uid)

        mockMvc.perform(delete("/v1/account").header("Authorization", "Bearer ${bearerToken(uid)}"))
            .andExpect(status().isInternalServerError)

        verify(firebaseUserDeleter, never()).deleteUser(uid)
    }
}
