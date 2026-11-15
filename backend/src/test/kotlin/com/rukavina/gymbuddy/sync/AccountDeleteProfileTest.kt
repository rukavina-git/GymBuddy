package com.rukavina.gymbuddy.sync

import com.rukavina.gymbuddy.auth.FirebaseUserDeleter
import com.rukavina.gymbuddy.auth.testsupport.TestSecurityConfig
import com.rukavina.gymbuddy.testsupport.AbstractPostgresIntegrationTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.context.WebApplicationContext

/**
 * The "account-delete" profile as delete-account.sh starts it: the uid
 * from ACCOUNT_DELETE_UID, the runner deletes during startup, and
 * application-account-delete.yml keeps the web server off.
 */
@SpringBootTest(properties = ["ACCOUNT_DELETE_UID=$PROFILE_UID"])
@ActiveProfiles("account-delete")
@Import(TestSecurityConfig::class)
class AccountDeleteProfileTest : AbstractPostgresIntegrationTest() {

    @Autowired
    private lateinit var context: ApplicationContext

    @MockitoBean
    private lateinit var firebaseUserDeleter: FirebaseUserDeleter

    @Test
    fun `startup deletes the configured uid and starts no web server`() {
        verify(firebaseUserDeleter).deleteUser(PROFILE_UID)
        assertFalse(context is WebApplicationContext, "account-delete must not start a web server")
    }
}

private const val PROFILE_UID = "profileCheckUid42"
