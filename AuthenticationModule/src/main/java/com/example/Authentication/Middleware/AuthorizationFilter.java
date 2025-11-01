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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * AuthorizationFilter is responsible for JWT validation and setting
 * the authenticated user in the Spring Security context.
 * <p>
 * It also populates the user's role as a Spring Security authority
 * so that @PreAuthorize annotations like isAuthenticated() and
 * hasRole('ADMIN') work correctly.
 */
@Component
public class AuthorizationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationFilter.class);

    /**
     * List of public endpoints that do not require authentication.
     */
    private static final String[] PUBLIC_PATHS = {
            "/v1/auth/login",
            "/v1/home/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/register",
            "/weather/**"
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

        // Validate JWT and extract username
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
        String roleStr = userContext.extractUserRole(request);

        // Convert strings to enums
        UserDetails1.UserStatus status;
        UserDetails1.VerificationStatus verificationStatus;
        UserDetails1.UserRole userRole;
        try {
            status = UserDetails1.UserStatus.valueOf(statusStr);
            verificationStatus = UserDetails1.VerificationStatus.valueOf(verificationStatusStr);
            userRole = UserDetails1.UserRole.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid status, verificationStatus, or role: {}, {}, {}", statusStr, verificationStatusStr, roleStr);
            userContext.sendUnauthorizedResponse(response, "Authorization failed: Invalid user status or verification status or Role");
            return;
        }

        // Check account status
        if (status != UserDetails1.UserStatus.Active || verificationStatus != UserDetails1.VerificationStatus.Verified) {
            logger.warn("Account is not Active or Verified for user: {}", username);
            userContext.sendUnauthorizedResponse(response, "Authorization failed: Account is not Active or Verified");
            return;
        }

        // Set authentication in SecurityContext if not already set
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserPrinciple userPrinciple = new UserPrinciple(userId, username, fullName, status, verificationStatus, userRole);

                // Set Spring Security authorities based on UserRole
                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + userRole.name())
                );

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userPrinciple, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authToken);

                logger.info("Successful authentication for user: {} with status: {} and verification: {}",
                        username, status, verificationStatus);
            } catch (Exception e) {
                logger.error("Error setting authentication for user: {} - {}", username, e.getMessage(), e);
                userContext.sendUnauthorizedResponse(response, "Authorization failed: " + e.getMessage());
                return;
            }
        }

        // Set UserPrinciple in request scope for controller use
        UserPrinciple userPrinciple = new UserPrinciple(userId, username, fullName, status, verificationStatus, userRole);
        request.setAttribute("user", userPrinciple);
        logger.debug("Set UserPrinciple in request scope: {}", userPrinciple);

        filterChain.doFilter(request, response);
    }
}
