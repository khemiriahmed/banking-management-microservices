package com.ahmedkh.banking.auth;

import com.ahmedkh.banking.auth.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenProviderTest {

    private static final String SECRET =
            "YmFua2luZy1hdXRoLXRlc3Qtc2VjcmV0LWtleS0wMTIzNDU2Nzg5LWxvY2Fs";

    @Test
    void generatedTokenContainsSubjectEmailAndRoles() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 3600L);
        UUID userId = UUID.randomUUID();

        String token = provider.generateToken(userId, "client@example.com", List.of("CLIENT", "ADMIN"));
        Claims claims = provider.parseClaims(token);

        assertEquals(userId.toString(), claims.getSubject());
        assertEquals("client@example.com", claims.get("email", String.class));
        assertEquals(List.of("CLIENT", "ADMIN"), claims.get("roles", List.class));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, -1L);
        String token = provider.generateToken(UUID.randomUUID(), "client@example.com", List.of("CLIENT"));

        assertThrows(JwtException.class, () -> provider.parseClaims(token));
    }
}
