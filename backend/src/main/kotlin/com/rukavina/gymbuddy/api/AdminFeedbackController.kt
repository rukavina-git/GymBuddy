package com.rukavina.gymbuddy.api

import com.rukavina.gymbuddy.api.dto.AdminFeedbackItemDto
import com.rukavina.gymbuddy.api.dto.AdminFeedbackPageDto
import com.rukavina.gymbuddy.api.dto.AdminFeedbackUpdateDto
import com.rukavina.gymbuddy.feedback.FeedbackAdminService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * GET/PATCH /v1/admin/feedback per api/openapi.yaml, for the owner's
 * admin page. Admin-only via AdminAccess in SecurityConfig: a request
 * that gets here is already an admin.
 */
@RestController
class AdminFeedbackController(private val service: FeedbackAdminService) {

    @GetMapping("/v1/admin/feedback")
    fun listFeedback(
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) limit: Int?,
    ): AdminFeedbackPageDto = service.list(status, cursor, limit)

    @PatchMapping("/v1/admin/feedback/{id}")
    fun updateFeedback(
        @PathVariable id: String,
        @RequestBody body: AdminFeedbackUpdateDto,
    ): AdminFeedbackItemDto = service.update(id, body)
}
