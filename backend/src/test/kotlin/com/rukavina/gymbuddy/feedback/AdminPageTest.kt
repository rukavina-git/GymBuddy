package com.rukavina.gymbuddy.feedback

import com.rukavina.gymbuddy.api.AdminPageController
import com.rukavina.gymbuddy.auth.testsupport.TestSecurityConfig
import com.rukavina.gymbuddy.testsupport.AbstractPostgresIntegrationTest
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * The /admin/ page: public static files and web config, while the data
 * API stays bearer-only. The shared test context has no Firebase web
 * config (admin off); the "on" config is tested on the controller directly
 * (a per-class property context would exhaust Postgres connections).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestSecurityConfig::class)
class AdminPageTest : AbstractPostgresIntegrationTest() {

    @Autowired private lateinit var mockMvc: MockMvc

    @Test
    fun `admin without slash redirects to the page`() {
        mockMvc.perform(get("/admin"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/admin/"))
    }

    @Test
    fun `admin slash serves the page without a token`() {
        mockMvc.perform(get("/admin/"))
            .andExpect(status().isOk)
            .andExpect(forwardedUrl("/admin/index.html"))
        mockMvc.perform(get("/admin/index.html"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Feedback")))
    }

    @Test
    fun `script and stylesheet load without a token`() {
        mockMvc.perform(get("/admin/admin.js"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("firebasejs/13.3.0")))
        mockMvc.perform(get("/admin/admin.css")).andExpect(status().isOk)
    }

    @Test
    fun `config is null when the Firebase web config is unset`() {
        mockMvc.perform(get("/admin/config.js"))
            .andExpect(status().isOk)
            .andExpect(header().string("Cache-Control", containsString("no-store")))
            .andExpect(content().contentTypeCompatibleWith("text/javascript"))
            .andExpect(content().string("window.ADMIN_CONFIG = null;\n"))
    }

    @Test
    fun `admin API still needs a token`() {
        mockMvc.perform(get("/v1/admin/feedback")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `config carries the three values when all are set`() {
        val body = AdminPageController("key-1", "proj.firebaseapp.com", "proj").config().body!!
        assertEquals(
            "window.ADMIN_CONFIG = {\"apiKey\":\"key-1\",\"authDomain\":\"proj.firebaseapp.com\",\"projectId\":\"proj\"};\n",
            body,
        )
    }

    @Test
    fun `config is null when any value is blank`() {
        val body = AdminPageController("key-1", " ", "proj").config().body!!
        assertEquals("window.ADMIN_CONFIG = null;\n", body)
    }

    @Test
    fun `config values cannot break out of the script`() {
        val body = AdminPageController("</script><script>alert(1)</script>", "a\"b\u2028", "p").config().body!!
        assertFalse(body.contains("<"), body)
        assertFalse(body.contains("\u2028"), body)
        assertTrue(body.contains("\\u003c/script>"), body)
        assertTrue(body.contains("a\\\"b"), body)
    }
}
