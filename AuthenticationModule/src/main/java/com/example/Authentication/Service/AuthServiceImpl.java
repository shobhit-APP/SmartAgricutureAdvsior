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

/**
 * Service class implementing authentication and verification logic for users.
 * Handles login via username, email, or phone number (with password or OTP), password resets,
 * and user verification using email tokens. Integrates with Spring Security for authentication,
 * OTP services for phone-based login, and Redis for managing blocked users.
 */
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
    private EmailServiceInterface emailService;

    @Autowired
    private JwTService jwTService;
    @Autowired
    private UserService userService;

    @Autowired
    private validateNull validateNull;

    /**
     * Handles user login requests using username, email, phone number, or OTP-based authentication.
     * Supports password-based login and OTP-based login for phone numbers, with checks for blocked,
     * deleted, or unverified accounts.
     *
     * @param username    the username for login (optional)
     * @param phoneNumber the phone number for login (optional)
     * @param email       the email for login (optional)
     * @param password    the password for authentication (optional for OTP login)
     * @return a {@link ResponseEntity} containing authentication response (JWT token) or error details
     * @throws AnyException if authentication fails due to invalid credentials or user status
     */
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

                boolean isSent = userService.sendVerificationEmail(user.getUserId(), user.getUserEmail(),user.getContactNumber(), "login");

                if (!isSent) {
                    logger.error("Failed to send verification email to unverified user: {}", user.getUserId());
                } else {
                    logger.info("Verification email successfully sent to userId={}", user.getUserId());
                }
            }

            logger.info("Login successful for userId={}", user.getUserId());
            return jwTService.generateAuthResponseForUser(user);
        } catch (AnyException ae) {
            logger.warn("Authentication error (custom): {}", ae.getMessage());
            return ResponseEntity.status(HttpStatus.valueOf(ae.getStatusCode())).body(Map.of("error", ae.getMessage()));
        } catch (Exception e) {
            logger.error("Authentication error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Authentication failed"));
        }
    }

    /**
     * Initiates OTP-based login by sending an OTP to the provided phone number.
     *
     * @param phoneNumber the phone number to send the OTP to
     * @return a {@link ResponseEntity} containing a success message, masked phone number, and verification URL
     * @throws AnyException if the phone number is invalid or OTP sending fails
     */
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

    /**
     * Initiates a password reset by sending an OTP to the provided phone number.
     *
     * @param email the phone number associated with the user account
     * @return a {@link ResponseEntity} containing a success message and masked phone number
     * @throws AnyException if the phone number is invalid or OTP sending fails
     */
    @Override
    public ResponseEntity<?> forgetPassword(String email) {
        if (validateNull.isNullOrEmpty(email)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Phone number is required"));
        }

        try {
            UserDetails1 user = userRepo.findByUserEmail(email);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
            }

            String otp = otpService.generateAndStoreOtp(user.getUserEmail(), OtpPurpose.FORGOT_PASSWORD);
            emailService.sendOtp(email, otp);
            return ResponseEntity.ok(Map.of(
                    "message", "OTP sent to email",
                    "phoneNumber",maskEmail(email)));
        } catch (Exception e) {
            logger.error("Password reset request failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process password reset"));
        }
    }

    /**
     * Resets the user's password using the provided phone number and new password.
     *
     * @param email the email associated with the user account
     * @param newPassword the new password to set
     * @return a {@link ResponseEntity} containing a success message or error details
     * @throws AnyException if the phone number or password is invalid, or the reset fails
     */
    @Override
    public ResponseEntity<?> resetPassword(String email, String newPassword) {
        try {
            if (validateNull.isNullOrEmpty(email) || validateNull.isNullOrEmpty(newPassword)) {
                logger.warn("email or new password is missing");
                return ResponseEntity.badRequest().body(Map.of("error", "email and new password are required"));
            }

            UserDetails1 user = userRepo.findByUserEmail(email);
            if (user == null) {
                logger.warn("User not found for email: {}", email);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
            }

            boolean isUpdated = userService.resetPassword(email, newPassword,user);
            if (!isUpdated) {
                logger.error("Password reset failed for email: {}", email);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to update password"));
            }

            logger.info("Password reset successful for userId={}", user.getUserId());
            return ResponseEntity.ok(Map.of("message", "Password updated successfully"));

        } catch (AnyException e) {
            logger.warn("Password reset validation error for email={}: {}", email, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error resetting password for email={}: {}", email, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to reset password"));
        }
    }

    /**
     * Verifies a user account using the provided email and token.
     *
     * @param email the email address of the user to verify
     * @param token the verification token
     * @return a {@link ResponseEntity} containing a success message or error details
     * @throws AnyException if the email, token, or verification process is invalid
     */
    @Override
    public ResponseEntity<?> verifyUser(String email, String token, String otp) {
        try {
            UserDetails1 user = userRepo.findByUserEmail(email);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found"));
            }

            if ("Verified".equalsIgnoreCase(String.valueOf(user.getVerificationStatus()))) {
                return ResponseEntity.ok(Map.of("message", "Account already verified"));
            }

            boolean verified = false;

            if (otp != null && !otp.isBlank()) {
                boolean otpVerified = otpService.verifyOtp(email, otp, OtpPurpose.REGISTRATION);
                if (otpVerified) {
                    verified = true;
                }
            }

            if (token != null && !token.isBlank()) {
                PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token);
                if (resetToken == null) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("error", "Invalid token"));
                }
                if (!user.getUserId().equals(resetToken.getUserId())) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("error", "Token does not belong to this user"));
                }
                if (resetToken.getExpiryDate().isBefore(Instant.now())) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Token expired"));
                }
                verified = true;
            }

            if (!verified) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid or expired OTP/token"));
            }

            verifyUser(user);
            return ResponseEntity.ok(Map.of("message", "User verified successfully"));
        } catch (Exception e) {
            logger.error("User verification failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Verification failed"));
        }
    }

    /**
     * Updates the user's verification status to verified and saves it to the database.
     * This method is transactional to ensure data consistency.
     *
     * @param user the {@link UserDetails1} object to verify
     */
    @Transactional
    public void verifyUser(UserDetails1 user) {
        user.setVerificationStatus(UserDetails1.VerificationStatus.Verified);
        userRepo.save(user);
    }

    /**
     * Determines the login method based on provided credentials.
     *
     * @param username    the username (optional)
     * @param email       the email (optional)
     * @param phoneNumber the phone number (optional)
     * @return the {@link LoginMethod} (USERNAME, EMAIL, or PHONE) based on the provided input
     */
    @Override
    public LoginMethod determineLoginMethod(String username, String email, String phoneNumber) {
        if (!validateNull.isNullOrEmpty(username))
            return LoginMethod.USERNAME;
        if (!validateNull.isNullOrEmpty(email))
            return LoginMethod.EMAIL;
        return LoginMethod.PHONE;
    }

    /**
     * Masks an email address to protect sensitive information.
     *
     * @param email the email address to mask
     * @return the masked email address, or "****" if invalid
     */
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

    /**
     * Masks a phone number to protect sensitive information, showing only the last four digits.
     *
     * @param phoneNumber the phone number to mask
     * @return the masked phone number, or "****" if invalid
     */
    @Override
    public String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() <= 4)
            return "****";
        return "****" + phoneNumber.substring(phoneNumber.length() - 4);
    }

    /**
     * Authenticates a user using the specified login method and credentials.
     *
     * @param method       the {@link LoginMethod} (USERNAME, EMAIL, or PHONE)
     * @param username     the username (optional)
     * @param email        the email (optional)
     * @param phoneNumber  the phone number (optional)
     * @param password     the password for authentication
     * @return the authenticated {@link UserDetails1} object, or null if authentication fails
     * @throws AnyException if the user is not found or authentication fails
     */
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

    /**
     * Retrieves the username associated with the provided email for login purposes.
     *
     * @param email the email address to look up
     * @return the username associated with the email
     * @throws AnyException if the email is invalid or the user is not found
     */
    @Override
    public String loginWithEmail(String email) {
        UserDetails1 user = userRepo.findByUserEmail(email);
        if (user == null)
            throw new AnyException(HttpStatus.NOT_FOUND.value(), "Invalid email or user not found");
        return user.getUsername();
    }

    /**
     * Retrieves the username associated with the provided phone number for login purposes.
     *
     * @param phoneNumber the phone number to look up
     * @return the username associated with the phone number
     * @throws AnyException if the phone number is invalid or the user is not found
     */
    @Override
    public String loginWithPhone(String phoneNumber) {
        UserDetails1 user = userRepo.findByContactNumber(phoneNumber);
        if (user == null)
            throw new AnyException(HttpStatus.NOT_FOUND.value(), "Invalid phone number or user not found");
        return user.getUsername();
    }

    /**
     * Finds a user based on the specified login method and credentials.
     *
     * @param loginMethod  the {@link LoginMethod} (USERNAME, EMAIL, or PHONE)
     * @param username     the username (optional)
     * @param email        the email (optional)
     * @param phoneNumber  the phone number (optional)
     * @return the {@link UserDetails1} object if found, or null otherwise
     */
    @Override
    public UserDetails1 findUser(LoginMethod loginMethod, String username, String email, String phoneNumber) {
        return switch (loginMethod) {
            case USERNAME -> userRepo.findByUsername(username);
            case EMAIL -> userRepo.findByUserEmail(email);
            case PHONE -> userRepo.findByContactNumber(phoneNumber);
        };
    }
}