package com.example.Authentication.Components;

import com.example.Authentication.Interface.UserContextInterface;
import com.example.Authentication.UTIL.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Component
public class UserContext implements UserContextInterface {
    private static final Logger logger = LoggerFactory.getLogger(UserContext.class);

    @Autowired
    private JwtUtil jwtUtil;

    // Extract JWT token from Authorization header
    public String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        return (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer "))
                ? bearerToken.substring(7)
                : null;
    }

    // Validate token and extract username
    @Override
    public String validateAndExtractUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String token = extractToken(request);
        if (token == null) {
            logger.warn("Missing JWT token in Authorization header");
            sendUnauthorizedResponse(response, "Authorization failed: Missing token");
            return null;
        }

        try {
            String username = jwtUtil.extractUsername(token);
            if (username == null || !jwtUtil.validateToken(token)) {
                logger.warn("Invalid or expired JWT token");
                sendUnauthorizedResponse(response, "Authorization failed: Invalid or expired token");
                return null;
            }
            logger.debug("Successfully validated token for user: {}", username);
            return username;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            logger.warn("JWT token expired: {}", e.getMessage());
            sendUnauthorizedResponse(response, "Authorization failed: Token expired");
            return null;
        } catch (io.jsonwebtoken.JwtException e) {
            logger.warn("Invalid JWT token: {}", e.getMessage());
            sendUnauthorizedResponse(response, "Authorization failed: Invalid token");
            return null;
        } catch (Exception e) {
            logger.error("Unexpected error during token validation: {}", e.getMessage(), e);
            sendUnauthorizedResponse(response, "Authorization failed: " + e.getMessage());
            return null;
        }
    }

    // Send unauthorized response
    public void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("text/plain");
        response.getWriter().write(message);
    }
    // Generic JWT value extraction
    public <T> T extractJWTValue(HttpServletRequest request, String key, Class<T> type) {
        String token = extractToken(request);
        if (token == null) {
            logger.warn("No token found for extracting claim: {}", key);
            return null;
        }
        try {
            T value = jwtUtil.extractJWTValue(token, key, type);
            logger.debug("Extracted {}: {}", key, value);
            return value;
        } catch (Exception e) {
            logger.warn("Failed to extract {}: {}", key, e.getMessage());
            return null;
        }
    }

    @Override
    public String extractFullname(HttpServletRequest request) {
        return extractJWTValue(request, "fullName", String.class);
    }

    @Override
    public Long extractUserId(HttpServletRequest request) {
        return extractJWTValue(request, "userId", Long.class);
    }

    @Override
    public String extractStatus(HttpServletRequest request) {
        return extractJWTValue(request, "status", String.class);
    }

    @Override
    public String extractVerificationStatus(HttpServletRequest request) {
        return extractJWTValue(request, "verificationStatus", String.class);
    }
}