package com.emeal.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;

@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${app.jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:86400000}")
    private int jwtExpirationMs;

    private SecretKey key() {
        byte[] keyBytes = null;
        if (jwtSecret != null && jwtSecret.matches("^[0-9a-fA-F]+$") && jwtSecret.length() >= 64) {
            try {
                int len = jwtSecret.length();
                keyBytes = new byte[len / 2];
                for (int i = 0; i < len; i += 2) {
                    keyBytes[i / 2] = (byte) ((Character.digit(jwtSecret.charAt(i), 16) << 4)
                            + Character.digit(jwtSecret.charAt(i + 1), 16));
                }
            } catch (Exception ignored) {
            }
        }

        if (keyBytes == null || keyBytes.length < 32) {
            try {
                keyBytes = Decoders.BASE64.decode(jwtSecret);
            } catch (Exception ignored) {
            }
        }

        if (keyBytes == null || keyBytes.length < 32) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                keyBytes = md.digest((jwtSecret != null ? jwtSecret : "fallback-default-secret-key-employee-meal").getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                logger.error("Failed to digest JWT secret with SHA-256: {}", e.getMessage());
            }
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateJwtToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key())
                .compact();
    }

    public String getUsernameFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser().verifyWith(key()).build().parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}
