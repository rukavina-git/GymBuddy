package com.rukavina.gymbuddy.api

import com.rukavina.gymbuddy.auth.testsupport.TestSecurityConfig
import com.rukavina.gymbuddy.testsupport.AbstractPostgresIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Swagger UI is served from the swagger-ui webjar, which is pinned over
 * springdoc's own version for a CVE fix (build.gradle.kts). Guards that
 * the pinned webjar still resolves (no auth needed).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestSecurityConfig::class)
class SwaggerUiTest : AbstractPostgresIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `swagger ui index and bundle are served`() {
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk)
        mockMvc.perform(get("/swagger-ui/swagger-ui-bundle.js")).andExpect(status().isOk)
    }
}
