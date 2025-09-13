package com.example.Authentication.UTIL;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Utility class for handling JSON Web Token (JWT) operations.
 * Provides methods for generating, validating, and extracting information from JWTs used for authentication.
 */
@Component
public class JwtUtil {

    private final String SECRET_KEY;

    @Value("${jwt.expiration:432000000}") // default 5 days
    private long JWT_EXPIRATION;

    /**
     * Constructs a JwtUtil instance and generates a secure secret key for JWT signing.
     * Uses HmacSHA256 algorithm to create the secret key, encoded in Base64.
     *
     * @throws RuntimeException if the HmacSHA256 algorithm is not available.
     */
    public JwtUtil() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA256");
            SecretKey sk = keyGenerator.generateKey();
            SECRET_KEY = Base64.getEncoder().encodeToString(sk.getEncoded());
            System.out.println("JWT Secret key generated successfully.");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves the signing key derived from the Base64-encoded secret key.
     *
     * @return A {@link Key} object used for signing and verifying JWTs.
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    /**
     * Generates a JWT for the specified user with custom claims.
     * The token includes the user's ID, full name, status, and verification status, and is signed with HmacSHA256.
     *
     * @param username           The username to set as the JWT subject.
     * @param userId            The user's ID to include in the claims.
     * @param fullName          The user's full name to include in the claims.
     * @param status            The user's status to include in the claims.
     * @param verificationStatus The user's verification status to include in the claims.
     * @return A JWT string containing the specified claims and expiration time.
     */
    public String generateToken(String username, Long userId, String fullName,
                                String status, String verificationStatus) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("fullName", fullName);
        claims.put("status", status);
        claims.put("verificationStatus", verificationStatus);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validates a JWT by checking its signature, subject, and expiration.
     * Ensures the token's subject matches the extracted username and the token is not expired.
     *
     * @param token The JWT string to validate.
     * @return {@code true} if the token is valid and not expired, {@code false} otherwise.
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaims(token);
            String extractedUsername = claims.getSubject();
            Date expirationDate = claims.getExpiration();
            String username = extractUsername(token);
            return extractedUsername.equals(username) && expirationDate.after(new Date());
        } catch (ExpiredJwtException e) {
            System.out.println("JWT Token expired: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.out.println("Invalid JWT Token: " + e.getMessage());
            return false;
        }
    }

    /**
     * Extracts the username (subject) from a JWT.
     *
     * @param token The JWT string to parse.
     * @return The username stored in the token's subject claim.
     * @throws JwtException if the token is invalid or cannot be parsed.
     */
    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Extracts a specific claim from a JWT and casts it to the specified type.
     *
     * @param token The JWT string to parse.
     * @param key   The key of the claim to extract (e.g., "userId", "fullName").
     * @param type  The expected type of the claim value.
     * @param <T>   The type of the claim value.
     * @return The claim value cast to the specified type.
     * @throws JwtException if the token is invalid or the claim cannot be retrieved.
     */
    public <T> T extractJWTValue(String token, String key, Class<T> type) {
        return getClaims(token).get(key, type);
    }

    /**
     * Parses a JWT and retrieves its claims.
     *
     * @param token The JWT string to parse.
     * @return A {@link Claims} object containing the token's claims.
     * @throws JwtException if the token is invalid, expired, or cannot be parsed.
     */
    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}