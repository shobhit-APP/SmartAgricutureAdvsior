package com.example.Authentication.Controller;

import com.example.Authentication.Components.UserPrinciple;
import com.example.Authentication.Interface.AuthHelper;
import com.example.Authentication.Interface.AuthService;
import com.example.Authentication.Interface.OtpService;
import com.example.Authentication.Service.ReferenceTokenService;
import com.example.Authentication.UTIL.JwtUtil;
import com.example.Authentication.UTIL.validateNull;
import com.example.common.Exception.AnyException;
import com.example.common.util.PasswordUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/v1/auth")
@Tag(name = "Authentication", description = "Authentication and authorization operations")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final OtpService otpService;
    private final JwtUtil jwtUtil;

    private final ReferenceTokenService referenceTokenService;
    @Autowired
    private validateNull validateNull;

    public AuthController(AuthService authService, OtpService otpService, JwtUtil jwtUtil, ReferenceTokenService referenceTokenService) {
        this.authService = authService;
        this.otpService = otpService;
        this.jwtUtil = jwtUtil;
        this.referenceTokenService = referenceTokenService;
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Login using username+password, email+password, phone+password, or phone+OTP")
    public ResponseEntity<?> login(@RequestBody Map<String, String> requestBody, HttpServletRequest request) {
        String username = requestBody.get("username");
        String phoneNumber = requestBody.get("phoneNumber");
        String email = requestBody.get("email");
        String password = requestBody.get("password");

        try {
            return authService.handleLoginRequest(username, phoneNumber, email, password);
        } catch (AnyException e) {
            logger.error("Login error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Authentication failed: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected login error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Authentication failed: " + e.getMessage()));
        }
    }

    @GetMapping("/verify")
    @Operation(summary = "Verify user email", description = "Verify a user's email using a token")
    public ResponseEntity<?> verifyUser(
            @RequestParam("email") String email,
            @RequestParam("token") String token
    ) {
        if (email == null || token == null || email.trim().isEmpty() || token.trim().isEmpty()) {
            logger.warn("Missing email or token in verify request");
            return ResponseEntity.badRequest().body(Map.of("error", "Email and token are required"));
        }

        try {
            return authService.verifyUser(email, token);
        } catch (AnyException e) {
            logger.error("Email verification failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected email verification error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Email verification failed: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP for login", description = "Verify OTP sent to phone for login")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> requestBody, HttpServletRequest request) {
        String phoneNumber = requestBody.get("phoneNumber");
        String otp = requestBody.get("otp");
        if(validateNull.isNullOrEmpty(phoneNumber)|| validateNull.isNullOrEmpty(otp)) {
            logger.warn("Missing phone number or OTP in verify-otp request");
            return ResponseEntity.badRequest().body(Map.of("error", "Phone number and OTP are required"));
        }
        try {
            boolean isVerified = otpService.verifyLoginOtp(phoneNumber, otp);
            if (!isVerified) {
                logger.warn("Invalid OTP or phone number: {}", phoneNumber);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid OTP or phone number"));
            }
            otpService.deleteOtpByIdentifier(phoneNumber);
            logger.info("Login OTP verified successfully for phone: {}", phoneNumber);
            return ResponseEntity.ok(Map.of("message", "OTP verified successfully for login"));
        } catch (Exception e) {
            logger.error("OTP verification failed for login: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Something went wrong during OTP verification: " + e.getMessage()));
        }
    }

    @PostMapping("/forget-password")
    @Operation(summary = "Initiate password reset", description = "Send OTP to phone for password reset")
    public ResponseEntity<?> forgetPassword(@RequestBody Map<String, String> requestBody) {
        String phoneNumber = requestBody.get("phoneNumber");

        if (validateNull.isNullOrEmpty(phoneNumber)) {
            logger.warn("Missing phone number in forget-password request");
            return ResponseEntity.badRequest().body(Map.of("error", "phoneNumber is required"));
        }

        try {
            return authService.forgetPassword(phoneNumber);
        } catch (AnyException e) {
            logger.error("Forget password request failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected forget password error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Forget password failed: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-otp-for-reset")
    @Operation(summary = "Verify OTP for password reset", description = "Verify OTP sent to email for password reset")
    public ResponseEntity<?> verifyResetOtp(@RequestBody Map<String, String> requestBody, HttpServletRequest request) {
        String phoneNumber = requestBody.get("phoneNumber");
        String otp = requestBody.get("otp");
         if(validateNull.isNullOrEmpty(phoneNumber)||validateNull.isNullOrEmpty(otp)) {
            logger.warn("Missing phoneNumber or OTP in verify-otp-for-reset request");
            return ResponseEntity.badRequest().body(Map.of("error", "phoneNumber and OTP are required"));
        }

        try {
            boolean isVerified = otpService.verifyOtp(phoneNumber, otp); // Uses FORGOT_PASSWORD purpose
            if (!isVerified) {
                logger.warn("Invalid OTP or phoneNumber for reset: {}", phoneNumber);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid OTP or email"));
            }
            otpService.deleteOtpByIdentifier(phoneNumber);
            logger.info("Reset OTP verified successfully for phoneNumber: {}", phoneNumber);
            return ResponseEntity.ok(Map.of("message", "OTP verified successfully for password reset"));
        } catch (Exception e) {
            logger.error("Reset OTP verification failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Something went wrong during OTP verification: " + e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Reset password using Phone and new password after OTP verification")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> requestBody, HttpServletRequest request) {
        String phoneNumber = requestBody.get("phoneNumber");
        String newPassword = requestBody.get("newPassword");
        if (validateNull.isNullOrEmpty(phoneNumber) || validateNull.isNullOrEmpty(newPassword)) {
        logger.warn("Missing phoneNumber or new password in reset-password request");
            return ResponseEntity.badRequest().body(Map.of("error", "phoneNumber and new password are required"));
        }
        if (!PasswordUtil.isValidPassword(newPassword)) {
            logger.warn("Invalid password format for phone number: {}", phoneNumber);
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be 8+ chars, include uppercase, lowercase, digit, and special char"));
        }
        try {
            return authService.resetPassword(phoneNumber, newPassword);
        } catch (AnyException e) {
            logger.error("Password reset failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected password reset error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Password reset failed: " + e.getMessage()));
        }
    }

    @GetMapping("/validateReferenceToken")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Validate reference token", description = "Validate a reference token and return JWT")
    public ResponseEntity<?> validateReferenceToken(@AuthenticationPrincipal UserPrinciple userPrinciple,
                                                    @RequestParam String referenceToken, HttpServletRequest request) {
        if (userPrinciple == null || !userPrinciple.checkAccountStatus()) {
            logger.warn("User not authenticated or account not active/verified for validateReferenceToken");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        }
        if (referenceToken == null || referenceToken.trim().isEmpty()) {
            logger.warn("Missing reference token in validateReferenceToken request");
            return ResponseEntity.badRequest().body(Map.of("error", "Reference token is required"));
        }

        String jwt = referenceTokenService.getJwtFromReferenceToken(referenceToken);
        if (jwt == null) {
            logger.warn("Invalid or expired reference token: {}", referenceToken);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired reference token"));
        }
        logger.info("Reference token validated successfully for user ID: {}", userPrinciple.getUserId());
        return ResponseEntity.ok(Map.of("jwtToken", jwt));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "User logout", description = "Invalidate reference token and log out user")
    public ResponseEntity<?> logout(@AuthenticationPrincipal UserPrinciple userPrinciple,
                                    @RequestBody Map<String, String> requestBody, HttpServletRequest request) {
        if (userPrinciple == null || !userPrinciple.checkAccountStatus()) {
            logger.warn("User not authenticated or account not active/verified for logout");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        }
        String referenceToken = requestBody.get("referenceToken");
        if (referenceToken == null || referenceToken.trim().isEmpty()) {
            logger.warn("Missing reference token in logout request");
            return ResponseEntity.badRequest().body(Map.of("error", "Reference token is required"));
        }

        String jwt = referenceTokenService.getJwtFromReferenceToken(referenceToken);
        if (jwt == null) {
            logger.warn("Invalid reference token: {}", referenceToken);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid reference token"));
        }

        String username = jwtUtil.extractUsername(jwt);
        if (username == null || !username.equals(userPrinciple.getUsername())) {
            logger.warn("Invalid JWT token for reference token: {}", referenceToken);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid JWT token"));
        }

        referenceTokenService.invalidateReferenceToken(referenceToken);
        logger.info("User logged out successfully: {}", username);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}