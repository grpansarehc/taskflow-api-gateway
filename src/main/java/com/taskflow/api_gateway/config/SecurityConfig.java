package com.taskflow.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

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
                .cors(Customizer.withDefaults())
                .authorizeExchange(exchange -> exchange
                        // Public endpoints - no authentication required
                        .pathMatchers("/api/auth/**").permitAll()
                        .pathMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/webjars/**")
                        .permitAll()
                        .pathMatchers("/api/ws/**").permitAll()
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers("/actuator/health", "/actuator/gateway/**").permitAll()

                        // All other endpoints require authentication
                        .anyExchange().authenticated())
                // Configure OAuth2 Resource Server with JWT
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> {
                        }));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        corsConfig.setAllowedHeaders(Arrays.asList("*"));
        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        return source;
    }
}
