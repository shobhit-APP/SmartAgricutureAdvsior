package com.example.Authentication.Service;

import com.example.Authentication.Interface.*;
import com.example.Authentication.Model.PasswordResetToken;
import com.example.Authentication.UTIL.validateNull;
import com.example.Authentication.enums.OtpPurpose;
import com.example.Authentication.repository.PasswordResetTokenRepository;
import com.example.Authentication.repository.UserRepo;
import com.example.common.Exception.AnyException;
import com.example.common.Model.UserDetails1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService, AuthHelper {
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final String VERIFICATION_LINK_TEMPLATE = "https://smartagriadvisior.com/v1/auth/verify";

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired
    private OtpService otpService;
    @Autowired
    private RedisService redisService;
    @Autowired
    private UserRepo userRepo;

    @Autowired
    private JwTService jwTService;
    @Autowired
    private UserService userService;

    @Autowired
    private validateNull validateNull;

    @Override
    public ResponseEntity<?> handleLoginRequest(String username, String phoneNumber, String email, String password) {
        logger.info("Login request: username={}, email={}, phone={}", username, email, phoneNumber);

        // Case 1: OTP Login (phone only, no password)
        if (validateNull.isNullOrEmpty(username) && validateNull.isNullOrEmpty(email) && !validateNull.isNullOrEmpty(phoneNumber) && validateNull.isNullOrEmpty(password)) {
            logger.info("OTP login flow for phone: {}", phoneNumber);
            return loginWithPhoneAndOtp(phoneNumber);
        }

        // Case 2: Missing credentials
        if (validateNull.isNullOrEmpty(password)
                || (validateNull.isNullOrEmpty(username) && validateNull.isNullOrEmpty(email) && validateNull.isNullOrEmpty(phoneNumber))) {
            logger.warn("Login failed: Missing credentials");
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Username, email, or phone number and password are required"));
        }

        // Case 3: Password-based login
        try {
            LoginMethod loginMethod = determineLoginMethod(username, email, phoneNumber);
            UserDetails1 user = authenticateUser(loginMethod, username, email, phoneNumber, password);

            if (user == null) {
                logger.warn("User not found for identifier: {}",
                        username != null ? username : (email != null ? email : phoneNumber));
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
            }

            if ("Blocked".equalsIgnoreCase(user.getStatus().name())) {
                logger.warn("Blocked user attempted login: userId={}", user.getUserId());
                redisService.addToBlockedUsers(user.getUserId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Account is blocked"));
            }

            if ("Deleted".equalsIgnoreCase(user.getStatus().name())) {
                logger.warn("Deleted account attempted login: userId={}", user.getUserId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Account is deleted"));
            }

            if ("Unverified".equalsIgnoreCase(user.getVerificationStatus().name())) {
                logger.info("Unverified user: userId={}. Triggering verification", user.getUserId());

                boolean isSent = userService.sendVerificationEmail(user.getUserId(), user.getUserEmail(), "login");

                if (!isSent) {
                    logger.error("Failed to send verification email to unverified user: {}", user.getUserId());
                } else {
                    logger.info("Verification email successfully sent to userId={}", user.getUserId());
                }
            }

            logger.info("Login successful for userId={}", user.getUserId());
            return jwTService.generateAuthResponseForUser(user);
        } catch (AnyException ae) {
            // If you throw AnyException with proper status+message elsewhere, preserve it
            logger.warn("Authentication error (custom): {}", ae.getMessage());
            return ResponseEntity.status(HttpStatus.valueOf(ae.getStatusCode())).body(Map.of("error", ae.getMessage()));
        } catch (Exception e) {
            logger.error("Authentication error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Authentication failed"));
        }
    }

    @Override
    public ResponseEntity<?> loginWithPhoneAndOtp(String phoneNumber) {
        try {
            if (validateNull.isNullOrEmpty(phoneNumber)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Phone number is required"));
            }
            UserDetails1 user = userRepo.findByContactNumber(phoneNumber);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
            }
            String otp = otpService.generateAndStoreOtp(phoneNumber, OtpPurpose.LOGIN);
            otpService.sendOtp(phoneNumber, otp);
            return ResponseEntity.ok(Map.of(
                    "message", "OTP sent to phone number",
                    "phoneNumber", maskPhoneNumber(phoneNumber),
                    "verificationUrl", "https://smartagriadvisior.com/api/verify-otp"));
        } catch (Exception e) {
            logger.error("Failed to send OTP for phone login: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to send OTP"));
        }
    }

    @Override
    public ResponseEntity<?> forgetPassword(String phoneNumber) {
        // Fixed validation: check for null/empty
        if (validateNull.isNullOrEmpty(phoneNumber)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Phone number is required"));
        }

        try {
            UserDetails1 user = userRepo.findByContactNumber(phoneNumber);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
            }

            String otp = otpService.generateAndStoreOtp(user.getUserEmail(), OtpPurpose.FORGOT_PASSWORD);
            otpService.sendOtp(phoneNumber, otp);
            return ResponseEntity.ok(Map.of(
                    "message", "OTP sent to phone number",
                    "phoneNumber", maskPhoneNumber(phoneNumber)));
        } catch (Exception e) {
            logger.error("Password reset request failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process password reset"));
        }
    }

    @Override
    public ResponseEntity<?> resetPassword(String phoneNumber, String newPassword) {
        try {
            // Validate input
            if (validateNull.isNullOrEmpty(phoneNumber) || validateNull.isNullOrEmpty(newPassword)) {
                logger.warn("Phone number or new password is missing");
                return ResponseEntity.badRequest().body(Map.of("error", "Phone number and new password are required"));
            }

            // Find user
            UserDetails1 user = userRepo.findByContactNumber(phoneNumber);
            if (user == null) {
                logger.warn("User not found for phone number: {}", phoneNumber);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
            }

            // Update password
            boolean isUpdated = userService.resetPassword(phoneNumber, newPassword);
            if (!isUpdated) {
                logger.error("Password reset failed for phone number: {}", phoneNumber);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to update password"));
            }

            logger.info("Password reset successful for userId={}", user.getUserId());
            return ResponseEntity.ok(Map.of("message", "Password updated successfully"));

        } catch (AnyException e) {
            logger.warn("Password reset validation error for phoneNumber={}: {}", phoneNumber, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error resetting password for phoneNumber={}: {}", phoneNumber, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to reset password"));
        }
    }

    @Override
    public ResponseEntity<?> verifyUser(String email, String token) {
        try {
            UserDetails1 user = userRepo.findByUserEmail(email).stream().findFirst().orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
            }
            if ("Verified".equalsIgnoreCase(String.valueOf(user.getVerificationStatus()))) {
                return ResponseEntity.ok(Map.of("message", "Account already verified"));
            }

            PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token);
            if (resetToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
            }

            if (!user.getUserId().equals(resetToken.getUserId())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
            }

            if (resetToken.getExpiryDate().isBefore(Instant.now())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Token expired"));
            }
            verifyUser(user);

            return ResponseEntity.ok(Map.of("message", "User verified successfully"));
        } catch (Exception e) {
            logger.error("User verification failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Verification failed"));
        }
    }

    @Transactional
    public void verifyUser(UserDetails1 user) {
        user.setVerificationStatus(UserDetails1.VerificationStatus.Verified);
        userRepo.save(user);
    }

    @Override
    public LoginMethod determineLoginMethod(String username, String email, String phoneNumber) {
        if (!validateNull.isNullOrEmpty(username))
            return LoginMethod.USERNAME;
        if (!validateNull.isNullOrEmpty(email))
            return LoginMethod.EMAIL;
        return LoginMethod.PHONE;
    }

    public String maskEmail(String email) {
        if (email == null || !email.contains("@"))
            return "****";
        String[] parts = email.split("@");
        String name = parts[0];
        String domain = parts[1];
        String maskedName = name.length() <= 2
                ? "*".repeat(name.length())
                : name.substring(0, 2) + "*".repeat(name.length() - 2);
        return maskedName + "@" + domain;
    }

    @Override
    public String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() <= 4)
            return "****";
        return "****" + phoneNumber.substring(phoneNumber.length() - 4);
    }

    @Override
    public UserDetails1 authenticateUser(LoginMethod method, String username, String email, String phoneNumber,
            String password) {
        String loginIdentifier = switch (method) {
            case USERNAME -> username;
            case EMAIL -> loginWithEmail(email);
            case PHONE -> loginWithPhone(phoneNumber);
        };
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginIdentifier, password));
        return findUser(method, username, email, phoneNumber);
    }



    @Override
    public String loginWithEmail(String email) {
        UserDetails1 user = userRepo.findByUserEmail(email).stream().findFirst().orElse(null);
        if (user == null)
            throw new AnyException(HttpStatus.NOT_FOUND.value(), "Invalid email or user not found");
        return user.getUsername();
    }

    @Override
    public String loginWithPhone(String phoneNumber) {
        UserDetails1 user = userRepo.findByContactNumber(phoneNumber);
        if (user == null)
            throw new AnyException(HttpStatus.NOT_FOUND.value(), "Invalid phone number or user not found");
        return user.getUsername();
    }

    @Override
    public UserDetails1 findUser(LoginMethod loginMethod, String username, String email, String phoneNumber) {

        return switch (loginMethod) {
            case USERNAME -> userRepo.findByUsername(username);
            case EMAIL -> userRepo.findByUserEmail(email).stream().findFirst().orElse(null);
            case PHONE -> userRepo.findByContactNumber(phoneNumber);
        };
    }

}
