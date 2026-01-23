package com.taskflow.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Security Configuration for API Gateway
 * Configures OAuth2 Resource Server for Keycloak JWT validation
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeExchange(exchange -> exchange
                // Public endpoints - no authentication required
                .pathMatchers("/api/auth/**").permitAll()
                .pathMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/webjars/**").permitAll()
                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .pathMatchers("/actuator/health", "/actuator/gateway/**").permitAll()
                
                // All other endpoints require authentication
                .anyExchange().authenticated()
            )
            // Configure OAuth2 Resource Server with JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {
                    // JWT decoder is auto-configured from application.properties
                    // spring.security.oauth2.resourceserver.jwt.issuer-uri
                    // spring.security.oauth2.resourceserver.jwt.jwk-set-uri
                })
            );

        return http.build();
    }
}
