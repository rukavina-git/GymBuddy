package com.rukavina.gymbuddy.auth

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

/**
 * Baseline security posture: everything requires authentication by
 * default, with a small explicit allowlist. Token verification itself
 * (Firebase) is not wired up yet - until it is, every non-allowlisted
 * request simply has no way to authenticate, which is the correct
 * fail-closed behavior for a skeleton.
 */
@Configuration
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            authorizeHttpRequests {
                authorize("/v1/health", permitAll)
                authorize("/v3/api-docs/**", permitAll)
                authorize("/swagger-ui/**", permitAll)
                authorize("/swagger-ui.html", permitAll)
                authorize(anyRequest, authenticated)
            }
            // Stateless bearer-token API: no cookie-based session, so no
            // CSRF exposure, and no server-side session to maintain.
            csrf { disable() }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            httpBasic { disable() }
            formLogin { disable() }
        }
        return http.build()
    }
}
