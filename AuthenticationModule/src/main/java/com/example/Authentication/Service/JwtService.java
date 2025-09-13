package com.example.Authentication.Service;

import com.example.Authentication.Interface.JwTService;
import com.example.Authentication.UTIL.JwtUtil;
import com.example.Authentication.dto.AuthResponseDTO;
import com.example.Authentication.repository.UserRepo;
import com.example.common.Model.UserDetails1;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class JwtService implements JwTService {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ReferenceTokenService referenceTokenService;

    @Autowired
    private UserRepo userRepo;

    @Override
    public ResponseEntity<?> generateAuthResponseForUser(UserDetails1 user) {
        try {
            // Validate user input
            if (user == null || user.getUserId() == null || user.getUsername() == null) {
                log.warn("Invalid user data provided for generating auth response: userId={}, username={}",
                        user != null ? user.getUserId() : null, user != null ? user.getUsername() : null);
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid user data"));
            }

            String status = user.getStatus() != null ? String.valueOf(user.getStatus()) : UserDetails1.UserStatus.Active.toString();
            String verificationStatus = user.getVerificationStatus() != null ? String.valueOf(user.getVerificationStatus()) : String.valueOf(UserDetails1.VerificationStatus.Verified);

            // Validate status and verificationStatus
            try {
                UserDetails1.UserStatus.valueOf(status);
                UserDetails1.VerificationStatus.valueOf(verificationStatus);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status or verificationStatus: {}, {}", status, verificationStatus);
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid user status or verification status"));
            }

            // Generate JWT token with required claims
            String jwtToken = jwtUtil.generateToken(
                    user.getUsername(),
                    user.getUserId(),
                    user.getFullname(),
                    status,
                    verificationStatus
            );

            // Generate refresh token
            String refreshToken = referenceTokenService.generateReferenceToken(jwtToken);

            // Update user status (remove redundant userId update)
            user.setStatus(UserDetails1.UserStatus.Active);
            userRepo.save(user);

            AuthResponseDTO authResponse = new AuthResponseDTO(jwtToken, refreshToken);
            log.info("Auth response generated successfully for user ID: {}", user.getUserId());
            return ResponseEntity.ok(authResponse);
        } catch (Exception e) {
            log.error("Error generating auth response for user ID: {} - {}",
                    user != null ? user.getUserId() : "null", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate auth response: " + e.getMessage()));
        }
    }
}