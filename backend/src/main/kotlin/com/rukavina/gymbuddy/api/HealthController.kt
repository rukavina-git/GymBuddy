package com.rukavina.gymbuddy.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Liveness only, per api/openapi.yaml: answers "is the process alive",
 * not "is everything working". Must never touch the database.
 */
@RestController
class HealthController {

    @GetMapping("/v1/health")
    fun health(): HealthStatus = HealthStatus()
}
