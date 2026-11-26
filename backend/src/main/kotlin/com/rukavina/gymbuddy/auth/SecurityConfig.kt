package com.rukavina.gymbuddy.auth

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

/**
 * Everything requires authentication by default, with a small explicit
 * allowlist. FirebaseAuthenticationFilter verifies the bearer token and
 * populates the SecurityContext; RestAuthenticationEntryPoint turns a
 * missing/rejected authentication into the spec's 401 Error body instead
 * of Spring's default empty 403. RateLimitFilter runs after
 * authentication so it can key its token bucket on the uid.
 */
@Configuration
// Not in the account-delete one-off job (no web server, no HttpSecurity).
@ConditionalOnWebApplication
class SecurityConfig(
    private val firebaseAuthenticationFilter: FirebaseAuthenticationFilter,
    private val rateLimitFilter: RateLimitFilter,
    private val restAuthenticationEntryPoint: RestAuthenticationEntryPoint,
    private val restAccessDeniedHandler: RestAccessDeniedHandler,
    private val adminAccess: AdminAccess,
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            authorizeHttpRequests {
                authorize("/v1/health", permitAll)
                // Guests can send feedback (no account, no token); a valid
                // token still sets the uid. See FeedbackController.
                authorize(HttpMethod.POST, "/v1/feedback", permitAll)
                // The one admin guard for every admin route (AdminAccess).
                authorize("/v1/admin/**", adminAccess)
                // The admin page's static files and web config are public;
                // its data comes from /v1/admin/** above, bearer-only.
                authorize(HttpMethod.GET, "/admin", permitAll)
                authorize(HttpMethod.GET, "/admin/**", permitAll)
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
            exceptionHandling {
                authenticationEntryPoint = restAuthenticationEntryPoint
                accessDeniedHandler = restAccessDeniedHandler
            }
        }
        http.addFilterBefore(firebaseAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        http.addFilterAfter(rateLimitFilter, FirebaseAuthenticationFilter::class.java)
        return http.build()
    }
}
