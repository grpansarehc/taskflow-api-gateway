package com.taskflow.api_gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

@Component
public class JwtUtils {

    // You must add this key to Gateway's application.properties
    @Value("${app.jwtSecret}")
    private String jwtSecret;

    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public void validateJwtToken(String token) {
        Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token);
    }

    public String getUserId(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build()
                .parseClaimsJws(token).getBody().getSubject(); // or .get("id", String.class);
    }

    public String getEmail(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build()
                .parseClaimsJws(token).getBody().get("email", String.class);
    }
}