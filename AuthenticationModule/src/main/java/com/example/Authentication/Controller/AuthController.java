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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@Tag(name = "Authentication API", description = "Endpoints for user authentication, OTP verification, password reset, and token management")
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
    @Operation(
            summary = "User login",
            description = "Authenticates a user using username+password, email+password, phone+password, or phone+OTP. Returns a JWT token on success."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful, returns JWT token",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid login credentials or missing parameters",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Server error during authentication",
                    content = @Content(schema = @Schema(implementation = Map.class))
            )
    })
    public ResponseEntity<?> login(
            @Parameter(description = "Login credentials (username, email, phoneNumber, password)", required = true)
            @RequestBody Map<String, String> requestBody,
            HttpServletRequest request) {
        String username = requestBody.get("username");
        String phoneNumber = requestBody.get("phoneNumber");
        String email = requestBody.get("email");
        String password = requestBody.get("password");

        try {
            // Handle login request with provided credentials
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
    @Operation(
            summary = "Verify user email",
            description = "Verifies a user's email address using a token sent to the email."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Email verified successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing or invalid email/token",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Server error during email verification",
                    content = @Content(schema = @Schema(implementation = Map.class))
            )
    })
    public ResponseEntity<?> verifyUser(
            @Parameter(description = "Email address to verify", required = true, example = "user@example.com")
            @RequestParam("email") String email,
            @Parameter(description = "Verification token", required = true, example = "abc123")
            @RequestParam("token") String token) {
        // Validate email and token presence
        if (email == null || token == null || email.trim().isEmpty() || token.trim().isEmpty()) {
            logger.warn("Missing email or token in verify request");
            return ResponseEntity.badRequest().body(Map.of("error", "Email and token are required"));
        }

        try {
            // Verify user email using AuthService
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
    @Operation(
            summary = "Verify OTP for login",
            description = "Verifies an OTP sent to a phone number for user login authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OTP verified successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing phone number or OTP",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid OTP or phone number",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Server error during OTP verification",
                    content = @Content(schema = @Schema(implementation = Map.class))
            )
    })
    public ResponseEntity<?> verifyOtp(
            @Parameter(description = "Phone number and OTP for verification", required = true)
            @RequestBody Map<String, String> requestBody,
            HttpServletRequest request) {
        String phoneNumber = requestBody.get("phoneNumber");
        String otp = requestBody.get("otp");
        // Validate input parameters
        if (validateNull.isNullOrEmpty(phoneNumber) || validateNull.isNullOrEmpty(otp)) {
            logger.warn("Missing phone number or OTP in verify-otp request");
            return ResponseEntity.badRequest().body(Map.of("error", "Phone number and OTP are required"));
        }
        try {
            // Verify OTP for login
            boolean isVerified = otpService.verifyLoginOtp(phoneNumber, otp);
            if (!isVerified) {
                logger.warn("Invalid OTP or phone number: {}", phoneNumber);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid OTP or phone number"));
            }
            // Delete OTP after successful verification
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
    @Operation(
            summary = "Initiate password reset",
            description = "Sends an OTP to the specified phone number to initiate the password reset process."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Password reset OTP sent successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing phone number or invalid request",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Server error during password reset initiation",
                    content = @Content(schema = @Schema(implementation = Map.class))
            )
    })
    public ResponseEntity<?> forgetPassword(
            @Parameter(description = "Phone number for password reset", required = true)
            @RequestBody Map<String, String> requestBody) {
        String phoneNumber = requestBody.get("phoneNumber");

        // Validate phone number presence
        if (validateNull.isNullOrEmpty(phoneNumber)) {
            logger.warn("Missing phone number in forget-password request");
            return ResponseEntity.badRequest().body(Map.of("error", "phoneNumber is required"));
        }

        try {
            // Initiate password reset by sending OTP
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
    @Operation(
            summary = "Verify OTP for password reset",
            description = "Verifies an OTP sent to a phone number for password reset."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OTP verified successfully for password reset",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing phone number or OTP",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid OTP or phone number",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Server error during OTP verification",
                    content = @Content(schema = @Schema(implementation = Map.class))
            )
    })
    public ResponseEntity<?> verifyResetOtp(
            @Parameter(description = "Phone number and OTP for password reset verification", required = true)
            @RequestBody Map<String, String> requestBody,
            HttpServletRequest request) {
        String phoneNumber = requestBody.get("phoneNumber");
        String otp = requestBody.get("otp");
        // Validate input parameters
        if (validateNull.isNullOrEmpty(phoneNumber) || validateNull.isNullOrEmpty(otp)) {
            logger.warn("Missing phoneNumber or OTP in verify-otp-for-reset request");
            return ResponseEntity.badRequest().body(Map.of("error", "phoneNumber and OTP are required"));
        }

        try {
            // Verify OTP for password reset
            boolean isVerified = otpService.verifyOtp(phoneNumber, otp);
            if (!isVerified) {
                logger.warn("Invalid OTP or phoneNumber for reset: {}", phoneNumber);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid OTP or phone number"));
            }
            // Delete OTP after successful verification
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
    @Operation(
            summary = "Reset password",
            description = "Resets the user password using the phone number and new password after OTP verification."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Password reset successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing phone number, invalid password format, or invalid request",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Server error during password reset",
                    content = @Content(schema = @Schema(implementation = Map.class))
            )
    })
    public ResponseEntity<?> resetPassword(
            @Parameter(description = "Phone number and new password for reset", required = true)
            @RequestBody Map<String, String> requestBody,
            HttpServletRequest request) {
        String phoneNumber = requestBody.get("phoneNumber");
        String newPassword = requestBody.get("newPassword");
        // Validate input parameters
        if (validateNull.isNullOrEmpty(phoneNumber) || validateNull.isNullOrEmpty(newPassword)) {
            logger.warn("Missing phoneNumber or new password in reset-password request");
            return ResponseEntity.badRequest().body(Map.of("error", "phoneNumber and new password are required"));
        }
        // Validate password format
        if (!PasswordUtil.isValidPassword(newPassword)) {
            logger.warn("Invalid password format for phone number: {}", phoneNumber);
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be 8+ chars, include uppercase, lowercase, digit, and special char"));
        }
        try {
            // Reset password using AuthService
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
    @Operation(
            summary = "Validate reference token",
            description = "Validates a reference token for an authenticated user and returns a JWT token."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Reference token validated successfully, JWT returned",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing or invalid reference token",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized access - user not authenticated or account not active",
                    content = @Content(schema = @Schema(implementation = Map.class))
            )
    })
    public ResponseEntity<?> validateReferenceToken(
            @AuthenticationPrincipal UserPrinciple userPrinciple,
            @Parameter(description = "Reference token to validate", required = true, example = "ref123")
            @RequestParam String referenceToken,
            HttpServletRequest request) {
        // Validate user authentication and account status
        if (userPrinciple == null || !userPrinciple.checkAccountStatus()) {
            logger.warn("User not authenticated or account not active/verified for validateReferenceToken");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        }
        // Validate reference token presence
        if (referenceToken == null || referenceToken.trim().isEmpty()) {
            logger.warn("Missing reference token in validateReferenceToken request");
            return ResponseEntity.badRequest().body(Map.of("error", "Reference token is required"));
        }

        // Validate reference token and retrieve JWT
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
    @Operation(
            summary = "User logout",
            description = "Invalidates a reference token to log out the authenticated user."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User logged out successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing or invalid reference token",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized access - user not authenticated or invalid JWT",
                    content = @Content(schema = @Schema(implementation = Map.class))
            )
    })
    public ResponseEntity<?> logout(
            @AuthenticationPrincipal UserPrinciple userPrinciple,
            @Parameter(description = "Reference token to invalidate", required = true)
            @RequestBody Map<String, String> requestBody,
            HttpServletRequest request) {
        // Validate user authentication and account status
        if (userPrinciple == null || !userPrinciple.checkAccountStatus()) {
            logger.warn("User not authenticated or account not active/verified for logout");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        }
        String referenceToken = requestBody.get("referenceToken");
        // Validate reference token presence
        if (referenceToken == null || referenceToken.trim().isEmpty()) {
            logger.warn("Missing reference token in logout request");
            return ResponseEntity.badRequest().body(Map.of("error", "Reference token is required"));
        }

        // Validate JWT associated with reference token
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

        // Invalidate reference token for logout
        referenceTokenService.invalidateReferenceToken(referenceToken);
        logger.info("User logged out successfully: {}", username);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}