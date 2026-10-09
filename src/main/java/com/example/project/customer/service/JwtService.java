package com.example.project.customer.service;

import com.example.project.customer.entity.Customer;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration:86400000}") long expirationMs
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret cannot be null or empty");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret key must be at least 256 bits (32 bytes)");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    /**
     * Generates a HinchMart JWT for the specified customer.
     * Contains minimal essential claims: sub, customerId, uid, role, iat, exp.
     * Excludes sensitive PII such as phone, email, name, etc.
     */
    public String generateToken(Customer customer) {
        if (customer == null) {
            throw new IllegalArgumentException("Customer cannot be null when generating JWT");
        }
        return generateToken(customer.getCustomerId(), customer.getFirebaseUid(), customer.getRole());
    }

    /**
     * Generates a HinchMart JWT with explicit customer parameters.
     */
    public String generateToken(Integer customerId, String uid, String role) {
        long now = System.currentTimeMillis();
        String resolvedRole = (role != null && !role.isBlank()) ? role : "CUSTOMER";
        String subject = (customerId != null) ? String.valueOf(customerId) : (uid != null ? uid : "unknown");

        return Jwts.builder()
                .subject(subject)
                .claim("customerId", customerId)
                .claim("uid", uid)
                .claim("role", resolvedRole)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validates the token's signature, structure, and expiration.
     */
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Claims claims = extractClaims(token);
            return !isTokenExpired(claims);
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT token validation failed: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Extracts all claims payload from the token after verifying its signature.
     */
    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts the customer ID from the token payload.
     */
    public Integer extractCustomerId(String token) {
        Claims claims = extractClaims(token);
        Object custId = claims.get("customerId");
        if (custId instanceof Number num) {
            return num.intValue();
        }
        if (custId != null) {
            try {
                return Integer.parseInt(custId.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        String sub = claims.getSubject();
        if (sub != null) {
            try {
                return Integer.parseInt(sub);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    /**
     * Extracts the Firebase UID from the token payload.
     */
    public String extractUid(String token) {
        Claims claims = extractClaims(token);
        Object uid = claims.get("uid");
        return uid != null ? uid.toString() : null;
    }

    /**
     * Extracts the customer's role from the token payload.
     */
    public String extractRole(String token) {
        Claims claims = extractClaims(token);
        Object role = claims.get("role");
        return role != null ? role.toString() : "CUSTOMER";
    }

    /**
     * Checks if the token has expired.
     */
    public boolean isTokenExpired(String token) {
        try {
            return isTokenExpired(extractClaims(token));
        } catch (JwtException | IllegalArgumentException ex) {
            return true;
        }
    }

    private boolean isTokenExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration != null && expiration.before(new Date());
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
