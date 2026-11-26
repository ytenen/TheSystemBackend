package org.smorodin.infrastructure.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expirationMs;

    @PostConstruct
    public void validateConfiguration() {
        // Fail at startup rather than creating weak or non-expiring tokens.
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes");
        }
        if (expirationMs <= 0) {
            throw new IllegalStateException("JWT expiration must be positive");
        }
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(Math.addExact(now.getTime(), expirationMs));

        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Claims verifiedClaims(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        // The signature alone is not sufficient for authentication.
        if (claims.getExpiration() == null || claims.getSubject() == null
                || claims.getSubject().isBlank()) {
            throw new IllegalArgumentException("JWT must have a subject and expiration");
        }
        return claims;
    }

    public Long extractUserId(String token) {
        long userId = Long.parseLong(verifiedClaims(token).getSubject());
        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid JWT subject");
        }
        return userId;
    }

    public boolean validateToken(String token) {
        try {
            extractUserId(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        if (!(userDetails instanceof CustomUserDetails customUser)) {
            return false;
        }
        try {
            return extractUserId(token).equals(customUser.getUserId());
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }
}
