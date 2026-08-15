package com.rukavina.gymbuddy.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Springdoc generates its spec from this plus the annotated controllers
 * and serves it at /v3/api-docs. Matching it against the hand-authored
 * api/openapi.yaml is Group J - not attempted here.
 */
@Configuration
class OpenApiConfig {

    @Bean
    fun gymBuddyOpenApi(): OpenAPI =
        OpenAPI().info(
            Info()
                .title("GymBuddy API")
                .version("1.0.0")
        )
}
