package com.taskflow.api_gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Authentication Filter for API Gateway
 * Extracts user information from Keycloak JWT and forwards to downstream services
 */
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // Skip authentication for OPTIONS requests (CORS preflight)
            if (request.getMethod().equals(HttpMethod.OPTIONS)) {
                return chain.filter(exchange);
            }

            // Skip authentication for public endpoints (handled by SecurityConfig)
            String path = request.getURI().getPath();
            if (path.contains("/v3/api-docs") || path.contains("/swagger-ui") || path.contains("/webjars")) {
                return chain.filter(exchange);
            }

            // Extract JWT from security context (already validated by Spring Security)
            return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> securityContext.getAuthentication())
                .filter(authentication -> authentication instanceof JwtAuthenticationToken)
                .map(authentication -> (JwtAuthenticationToken) authentication)
                .map(jwtAuth -> jwtAuth.getToken())
                .flatMap(jwt -> {
                    // Extract user information from Keycloak JWT claims
                    String userId = extractUserId(jwt);
                    String email = extractEmail(jwt);
                    
                    // Mutate request to add user headers for downstream services
                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                        .headers(httpHeaders -> {
                            // Remove existing headers to prevent spoofing
                            httpHeaders.remove("X-User-Id");
                            httpHeaders.remove("X-User-Email");
                        })
                        .header("X-User-Id", userId)
                        .header("X-User-Email", email)
                        .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                })
                .switchIfEmpty(chain.filter(exchange)); // Continue if no JWT (public endpoints)
        };
    }

    /**
     * Extract user ID from Keycloak JWT
     * Keycloak uses 'sub' claim for user ID
     */
    private String extractUserId(Jwt jwt) {
        // Try to get 'sub' claim (Keycloak user ID)
        String sub = jwt.getSubject();
        if (sub != null) {
            return sub;
        }
        
        // Fallback to 'userId' claim if custom mapper is configured
        Object userIdClaim = jwt.getClaim("userId");
        if (userIdClaim != null) {
            return userIdClaim.toString();
        }
        
        return "unknown";
    }

    /**
     * Extract email from Keycloak JWT
     */
    private String extractEmail(Jwt jwt) {
        Object emailClaim = jwt.getClaim("email");
        if (emailClaim != null) {
            return emailClaim.toString();
        }
        
        // Fallback to preferred_username (usually email in Keycloak)
        Object preferredUsername = jwt.getClaim("preferred_username");
        if (preferredUsername != null) {
            return preferredUsername.toString();
        }
        
        return "unknown";
    }

    public static class Config {
        // Configuration properties if needed
    }
}
