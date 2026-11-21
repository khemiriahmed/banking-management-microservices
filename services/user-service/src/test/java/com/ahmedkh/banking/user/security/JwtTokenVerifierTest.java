package com.ahmedkh.banking.user.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenVerifierTest {

    private final JwtTokenVerifier verifier = new JwtTokenVerifier(TestJwtFactory.SECRET);

    @Test
    void shouldExtractIdentityFromSignedToken() {
        UUID authUserId = UUID.randomUUID();
        String token = TestJwtFactory.token(authUserId, "client@example.com", "CLIENT");

        Claims claims = verifier.verify(token);

        assertThat(verifier.authUserId(claims)).isEqualTo(authUserId);
        assertThat(verifier.email(claims)).isEqualTo("client@example.com");
        assertThat(verifier.roles(claims)).isEqualTo(List.of("CLIENT"));
    }

    @Test
    void shouldRejectExpiredToken() {
        String token = TestJwtFactory.expiredToken(UUID.randomUUID(), "client@example.com", "CLIENT");

        assertThatThrownBy(() -> verifier.verify(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenWithInvalidSubject() {
        String token = TestJwtFactory.tokenWithSubject("not-a-uuid", "client@example.com", "CLIENT");

        Claims claims = verifier.verify(token);

        assertThatThrownBy(() -> verifier.authUserId(claims)).isInstanceOf(JwtException.class);
    }
}
