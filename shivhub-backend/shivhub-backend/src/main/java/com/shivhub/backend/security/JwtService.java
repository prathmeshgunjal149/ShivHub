package com.shivhub.backend.security;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.shivhub.backend.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/*
 * JwtService
 *
 * This class is responsible for:
 *
 * 1. Generating JWT token
 * 2. Extracting information from JWT
 * 3. Validating JWT token
 *
 * JWT = JSON Web Token
 *
 * Login successful:
 *
 * Email + Password
 *       ↓
 * JwtService
 *       ↓
 * JWT Token
 *       ↓
 * Frontend
 *
 * Later, frontend sends this token with protected APIs.
 */

@Service
public class JwtService {

    /*
     * Secret key used to sign JWT tokens.
     *
     * This value will come from application.properties.
     *
     * IMPORTANT:
     * In production, never hard-code the secret key.
     */
    @Value("${jwt.secret:ShivHubLocalDevJwtSecretChangeBeforeProduction2026Secure}")
    private String secretKey;

    /*
     * JWT validity period.
     *
     * Value comes from application.properties.
     *
     * We will keep the token valid for 24 hours.
     */
    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;


    /*
     * Create signing key from secret string.
     */
    private Key getSigningKey() {

        return Keys.hmacShaKeyFor(
                secretKey.getBytes(StandardCharsets.UTF_8)
        );
    }


    /*
     * Generate JWT token for a user.
     *
     * Token contains:
     *
     * - User email
     * - User ID
     * - User role
     * - Issue time
     * - Expiration time
     */
    public String generateToken(User user) {

        Date now = new Date();

        Date expirationDate =
                new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()

                // User email
                .subject(user.getEmail())

                /*
                 * Add User ID to JWT claims.
                 */
                .claim("userId", user.getId())

                /*
                 * Add role to JWT claims.
                 */
                .claim("role", user.getRole().name())

                /*
                 * Token creation time.
                 */
                .issuedAt(now)

                /*
                 * Token expiration time.
                 */
                .expiration(expirationDate)

                /*
                 * Sign token using secret key.
                 */
                .signWith(getSigningKey())

                /*
                 * Build final token.
                 */
                .compact();
    }


    /*
     * Extract email from JWT token.
     *
     * The email is stored in the JWT subject.
     */
    public String extractEmail(String token) {

        return getClaims(token).getSubject();
    }


    /*
     * Extract all claims from token.
     */
    private Claims getClaims(String token) {

        return Jwts.parser()

                /*
                 * Verify token using our secret key.
                 */
                .verifyWith(
                        (javax.crypto.SecretKey) getSigningKey()
                )

                .build()

                .parseSignedClaims(token)

                .getPayload();
    }


    /*
     * Check whether JWT token is valid.
     *
     * Token is valid when:
     *
     * 1. Email matches the user
     * 2. Token is not expired
     */
    public boolean isTokenValid(
            String token,
            String email) {

        try {

            String tokenEmail = extractEmail(token);

            return tokenEmail.equals(email)
                    && !isTokenExpired(token);

        } catch (Exception exception) {

            return false;
        }
    }


    /*
     * Check whether token is expired.
     */
    private boolean isTokenExpired(String token) {

        Date expiration =
                getClaims(token).getExpiration();

        return expiration.before(new Date());
    }
}
