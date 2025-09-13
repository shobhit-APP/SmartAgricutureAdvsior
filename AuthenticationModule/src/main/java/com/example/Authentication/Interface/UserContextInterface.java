package com.example.Authentication.Interface;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Interface for handling user context operations, including JWT validation and claim extraction.
 */
public interface UserContextInterface {

    /**
     * Validates the JWT token in the request and extracts the username.
     *
     * @param request  the HTTP request containing the JWT token
     * @param response the HTTP response to send unauthorized messages
     * @return the username extracted from the token, or null if invalid
     * @throws IOException if an I/O error occurs while writing to the response
     */
    String validateAndExtractUser(HttpServletRequest request, HttpServletResponse response) throws IOException;

    /**
     * Extracts the user ID from the JWT token.
     *
     * @param request the HTTP request containing the JWT token
     * @return the user ID, or null if not found
     */
    Long extractUserId(HttpServletRequest request);

    /**
     * Extracts the full name from the JWT token.
     *
     * @param request the HTTP request containing the JWT token
     * @return the full name, or null if not found
     */
    String extractFullname(HttpServletRequest request);

    /**
     * Extracts the user status from the JWT token.
     *
     * @param request the HTTP request containing the JWT token
     * @return the status as a string, or null if not found
     */
    String extractStatus(HttpServletRequest request);

    /**
     * Extracts the verification status from the JWT token.
     *
     * @param request the HTTP request containing the JWT token
     * @return the verification status as a string, or null if not found
     */
    String extractVerificationStatus(HttpServletRequest request);

    /**
     * Sends an unauthorized response with the specified message.
     *
     * @param response the HTTP response to write to
     * @param message  the error message to send
     * @throws IOException if an I/O error occurs while writing to the response
     */
    void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException;
}