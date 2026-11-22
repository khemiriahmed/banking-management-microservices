package com.ahmedkh.banking.user.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public final class TestJwtFactory {

    public static final String SECRET =
            "YmFua2luZy1hdXRoLXRlc3Qtc2VjcmV0LWtleS0wMTIzNDU2Nzg5LWxvY2Fs";

    private TestJwtFactory() {
    }

    public static String token(UUID authUserId, String email, String... roles) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(authUserId.toString())
                .claim("email", email)
                .claim("roles", List.of(roles))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public static String tokenWithSubject(String subject, String email, String... roles) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .claim("email", email)
                .claim("roles", List.of(roles))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public static String expiredToken(UUID authUserId, String email, String... roles) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        Instant now = Instant.now().minusSeconds(7200);
        return Jwts.builder()
                .subject(authUserId.toString())
                .claim("email", email)
                .claim("roles", List.of(roles))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(60)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
}
