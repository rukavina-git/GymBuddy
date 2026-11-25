package com.rukavina.gymbuddy.feedback

import com.rukavina.gymbuddy.auth.AdminAccess
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import java.util.Optional

/** Plain unit tests: the admin guard's list rules and the ntfy Click header. */
class AdminAccessAndNotifierTest {

    private fun signedIn(uid: String) = UsernamePasswordAuthenticationToken(uid, null, emptyList())
    private val anonymous = AnonymousAuthenticationToken("key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"))

    @Test
    fun `an unset or empty admin list lets nobody in`() {
        for (raw in listOf("", " ", " , ")) {
            val access = AdminAccess(raw)
            assertFalse(access.isAdmin(signedIn("admin-1")), "list '$raw'")
            assertFalse(access.isAdmin(signedIn("")), "list '$raw', empty uid")
        }
    }

    @Test
    fun `only listed uids are admins, never the anonymous principal`() {
        val access = AdminAccess("admin-1, admin-2")
        assertTrue(access.isAdmin(signedIn("admin-2")))
        assertFalse(access.isAdmin(signedIn("admin-3")))
        assertFalse(access.isAdmin(null))
        assertFalse(access.isAdmin(anonymous))
        assertFalse(AdminAccess("anonymousUser").isAdmin(anonymous))
    }

    @Test
    fun `the ping links to the admin page only when ADMIN_URL is set`() {
        val withUrl = FeedbackNotifier("topic", "https://ntfy.example/", "https://api-test.example/admin/")
        assertEquals(Optional.of("https://api-test.example/admin/"), withUrl.buildRequest().headers().firstValue("Click"))
        assertEquals("https://ntfy.example/topic", withUrl.buildRequest().uri().toString())

        val without = FeedbackNotifier("topic", "https://ntfy.example", "")
        assertEquals(Optional.empty<String>(), without.buildRequest().headers().firstValue("Click"))
    }
}
