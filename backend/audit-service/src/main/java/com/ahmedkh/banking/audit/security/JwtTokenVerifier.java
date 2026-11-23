package com.ahmedkh.banking.audit.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Component
public class JwtTokenVerifier {

    private final SecretKey key;

    public JwtTokenVerifier(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public Claims verify(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID authUserId(Claims claims) {
        try {
            return UUID.fromString(claims.getSubject());
        } catch (RuntimeException exception) {
            throw new JwtException("Invalid subject claim", exception);
        }
    }

    public String email(Claims claims) {
        String email = claims.get("email", String.class);
        if (email == null || email.isBlank()) {
            throw new JwtException("Missing email claim");
        }
        return email;
    }

    public List<String> roles(Claims claims) {
        Object value = claims.get("roles");
        if (!(value instanceof Collection<?> values)) {
            return List.of();
        }
        return values.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(role -> !role.isBlank())
                .toList();
    }
}
