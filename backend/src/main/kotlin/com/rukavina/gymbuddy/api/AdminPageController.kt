package com.rukavina.gymbuddy.api

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody
import tools.jackson.databind.json.JsonMapper

/**
 * The owner's feedback admin page (static files in static/admin/) and
 * its Firebase web config. Not part of the API contract (@Hidden): the
 * page is a client of the /v1/admin/ API, which stays bearer-only. The config
 * holds only public Firebase web values; any of them unset = the page
 * shows "admin off".
 */
@Hidden
@Controller
class AdminPageController(
    @Value("\${gymbuddy.admin.firebase.api-key:}") private val apiKey: String,
    @Value("\${gymbuddy.admin.firebase.auth-domain:}") private val authDomain: String,
    @Value("\${gymbuddy.admin.firebase.project-id:}") private val projectId: String,
) {

    private val json = JsonMapper.builder().build()

    @GetMapping("/admin")
    fun adminNoSlash(): String = "redirect:/admin/"

    @GetMapping("/admin/")
    fun adminPage(): String = "forward:/admin/index.html"

    @GetMapping("/admin/config.js", produces = ["text/javascript"])
    @ResponseBody
    fun config(): ResponseEntity<String> {
        val values = listOf(apiKey, authDomain, projectId).map { it.trim() }
        val config = if (values.any { it.isEmpty() }) {
            "null"
        } else {
            json.writeValueAsString(mapOf("apiKey" to values[0], "authDomain" to values[1], "projectId" to values[2]))
                // JSON is valid JS; also keep "</script>" or U+2028 from ever mattering.
                .replace("<", "\\u003c")
                .replace(" ", "\\u2028")
                .replace(" ", "\\u2029")
        }
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .contentType(MediaType.parseMediaType("text/javascript"))
            .body("window.ADMIN_CONFIG = $config;\n")
    }
}
