package com.example.Authentication.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service // Marks this class as a Spring service component
public class ReferenceTokenService {
    // Stores the mapping of reference tokens to JWTs
    private final Map<String, String> referenceTokenStore = new ConcurrentHashMap<>();
    // Stores the expiration time of each reference token
    private final Map<String, Long> expirationStore = new ConcurrentHashMap<>();
    // Defines the token expiration time (1 hour in milliseconds)
    @Value("${referenceToken.expiration:31536000000}")
    private long EXPIRATION_TIME_MILLIS; // 1 hour

    /**
     * Generates a new reference token for the provided JWT and stores it with an expiration time.
     *
     * @param jwt The JWT for which the reference token is to be generated.
     * @return The generated reference token.
     */
    public String generateReferenceToken(String jwt) {
        String referenceToken = UUID.randomUUID().toString(); // Creates a unique reference token
        long expirationTime = System.currentTimeMillis() + EXPIRATION_TIME_MILLIS; // Calculates expiration time

        referenceTokenStore.put(referenceToken, jwt); // Stores the reference token and associated JWT
        expirationStore.put(referenceToken, expirationTime); // Stores the expiration time

        return referenceToken; // Returns the generated reference token
    }

    /**
     * Retrieves the JWT associated with the given reference token, if the token is still valid.
     *
     * @param referenceToken The reference token to lookup.
     * @return The associated JWT if the reference token is valid; null if the token is expired or not found.
     */
    public String getJwtFromReferenceToken(String referenceToken) {
        return referenceTokenStore.computeIfPresent(referenceToken, (key, jwt) -> {
            if (System.currentTimeMillis() > expirationStore.get(key)) { // Checks if the token has expired
                invalidateReferenceToken(key); // Invalidates the expired token
                return null; // Returns null since the token is expired
            }
            return jwt; // Returns the associated JWT if the token is valid
        });
    }

    /**
     * Invalidates the given reference token by removing it from the stores.
     *
     * @param referenceToken The reference token to invalidate.
     */
    public void invalidateReferenceToken(String referenceToken) {
        referenceTokenStore.remove(referenceToken); // Removes the reference token from the store
        expirationStore.remove(referenceToken); // Removes the expiration time from the store
    }

    /**
     * Scheduled task that cleans up expired reference tokens.
     * This method runs every 30 minutes and removes tokens that have passed their expiration time.
     */
    @Scheduled(fixedRate = 30 * 60 * 1000) // Run cleanup every 30 minutes
    public void cleanupExpiredTokens() {
        long currentTime = System.currentTimeMillis(); // Gets the current time
        expirationStore.entrySet().removeIf(entry -> {
            if (currentTime > entry.getValue()) { // Checks if the token has expired
                referenceTokenStore.remove(entry.getKey()); // Removes the expired reference token
                return true; // Indicates that the token should be removed from the expiration store
            }
            return false; // Keeps the token in the expiration store if it is still valid
        });
    }

}
