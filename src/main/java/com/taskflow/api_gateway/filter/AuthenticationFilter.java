package com.taskflow.api_gateway.filter;

import com.taskflow.api_gateway.util.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private JwtUtils jwtUtils;

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (request.getMethod().equals(HttpMethod.OPTIONS)) {
                return chain.filter(exchange);
            }

            String path = request.getURI().getPath();
            if (path.contains("/v3/api-docs") || path.contains("/swagger-ui") || path.contains("/webjars")) {
                return chain.filter(exchange);
            }

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Authorization Header");
            }

            String authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                authHeader = authHeader.substring(7);
            }

            try {
                // 2. Validate Token (Throws exception if invalid)
                jwtUtils.validateJwtToken(authHeader);

                // 3. Extract User Info
                String userId = jwtUtils.getUserId(authHeader);
                String email = jwtUtils.getEmail(authHeader);

                // 4. Mutate Request (Add Headers for Downstream Services)
                // Remove existing headers to prevent spoofing/duplication
                request = exchange.getRequest().mutate()
                        .headers(httpHeaders -> {
                            httpHeaders.remove("X-User-Id");
                            httpHeaders.remove("X-User-Email");
                        })
                        .header("X-User-Id", userId) // PMS will read this!
                        .header("X-User-Email", email)
                        .build();

                return chain.filter(exchange.mutate().request(request).build());

            } catch (Exception e) {
                System.err.println("Invalid Token: " + e.getMessage());
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Token");
            }
        };
    }

    public static class Config {
        // Configuration properties if needed
    }
}