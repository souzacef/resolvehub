package com.resolvehub.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final int MIN_HS256_KEY_BYTES = 32;

    private final SecretKey signingKey;
    private final long expirationSeconds;

    public JwtService(
            @Value("${resolvehub.security.jwt.secret}") String secret,
            @Value("${resolvehub.security.jwt.expiration-seconds:3600}") long expirationSeconds
    ) {
        this.signingKey = buildSigningKey(secret);
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(ResolveHubUserPrincipal principal) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(principal.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .claim("uid", principal.getUserId().toString())
                .claim("oid", principal.getOrganizationId().toString())
                .claim("role", principal.getRole().name())
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public UUID extractOrganizationId(String token) {
        String organizationId = extractAllClaims(token).get("oid", String.class);
        return UUID.fromString(organizationId);
    }

    public boolean isTokenValid(String token, ResolveHubUserPrincipal principal) {
        String username = extractUsername(token);
        return username.equals(principal.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        Date expiration = extractAllClaims(token).getExpiration();
        return expiration.before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey buildSigningKey(String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length < MIN_HS256_KEY_BYTES) {
            throw new IllegalStateException(
                    "JWT secret must be at least 32 bytes for HS256 when using plain text UTF-8 secrets."
            );
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }
}
