package com.example.Authentication.Middleware;

import com.example.Authentication.Components.UserPrinciple;
import com.example.Authentication.Interface.UserContextInterface;
import com.example.common.Model.UserDetails1;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class AuthorizationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(AuthorizationFilter.class);

    private static final String[] PUBLIC_PATHS = {
            "/v1/auth/login",
            "/v1/auth/register",
            "/v1/home/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api/register",
            "/api/weather/**"
    };

    @Autowired
    private UserContextInterface userContext;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath();
        logger.debug("Processing request for path: {}", path);

        // Skip authentication for public endpoints
        AntPathMatcher pathMatcher = new AntPathMatcher();
        for (String publicPath : PUBLIC_PATHS) {
            if (pathMatcher.match(publicPath, path)) {
                logger.debug("Public endpoint accessed, skipping authentication: {}", path);
                filterChain.doFilter(request, response);
                return;
            }
        }

        // Validate and extract user
        String username = userContext.validateAndExtractUser(request, response);
        if (username == null) {
            logger.warn("No valid JWT token found or user validation failed");
            userContext.sendUnauthorizedResponse(response, "Authorization failed: Invalid or missing token");
            return;
        }

        // Extract additional JWT claims
        Long userId = userContext.extractUserId(request);
        String fullName = userContext.extractFullname(request);
        String statusStr = userContext.extractStatus(request);
        String verificationStatusStr = userContext.extractVerificationStatus(request);

        // Convert string status to enums
        UserDetails1.UserStatus status;
        UserDetails1.VerificationStatus verificationStatus;
        try {
            status = UserDetails1.UserStatus.valueOf(statusStr);
            verificationStatus = UserDetails1.VerificationStatus.valueOf(verificationStatusStr);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid status or verificationStatus: {}, {}", statusStr, verificationStatusStr);
            userContext.sendUnauthorizedResponse(response, "Authorization failed: Invalid user status or verification status");
            return;
        }

        // Check account status
        if (status != UserDetails1.UserStatus.Active || verificationStatus != UserDetails1.VerificationStatus.Verified) {
            logger.warn("Account is not Active or Verified for user: {}", username);
            userContext.sendUnauthorizedResponse(response, "Authorization failed: Account is not Active or Verified");
            return;
        }

        // Set authentication if not already set
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserPrinciple userPrinciple = new UserPrinciple(userId, username, fullName, status, verificationStatus);
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userPrinciple, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authToken);
                logger.info("Successful authentication for user: {} with status: {} and verification: {}",
                        username, status, verificationStatus);
            } catch (Exception e) {
                logger.error("Error setting authentication for user: {} - {}", username, e.getMessage(), e);
                userContext.sendUnauthorizedResponse(response, "Authorization failed: " + e.getMessage());
                return;
            }
        }

        // Set UserPrinciple in request scope
        UserPrinciple userPrinciple = new UserPrinciple(userId, username, fullName, status, verificationStatus);
        request.setAttribute("user", userPrinciple);
        logger.debug("Set UserPrinciple in request scope: {}", userPrinciple);

        filterChain.doFilter(request, response);
    }
}