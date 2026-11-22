package com.ahmedkh.banking.account.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenVerifierTest {

    private final JwtTokenVerifier verifier = new JwtTokenVerifier(TestJwtFactory.SECRET);

    @Test
    void verifiesClaimsAndRoles() {
        UUID authUserId = UUID.randomUUID();
        var claims = verifier.verify(TestJwtFactory.token(authUserId, "client@example.com", "CLIENT", "ADMIN"));

        assertThat(verifier.authUserId(claims)).isEqualTo(authUserId);
        assertThat(verifier.email(claims)).isEqualTo("client@example.com");
        assertThat(verifier.roles(claims)).containsExactly("CLIENT", "ADMIN");
    }

    @Test
    void rejectsInvalidSubject() {
        var claims = verifier.verify(TestJwtFactory.tokenWithSubject("not-a-uuid", "client@example.com", "CLIENT"));

        assertThatThrownBy(() -> verifier.authUserId(claims)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsMissingEmail() {
        var claims = verifier.verify(TestJwtFactory.tokenWithoutEmail(UUID.randomUUID(), "CLIENT"));

        assertThatThrownBy(() -> verifier.email(claims)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredToken() {
        String token = TestJwtFactory.expiredToken(UUID.randomUUID(), "client@example.com", "CLIENT");

        assertThatThrownBy(() -> verifier.verify(token)).isInstanceOf(JwtException.class);
    }
}
