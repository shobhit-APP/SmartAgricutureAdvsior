package com.example.Authentication.Controller;

import com.example.Authentication.Components.UserPrinciple;
import com.example.Authentication.Interface.AuthHelper;
import com.example.Authentication.Interface.UserService;
import com.example.Authentication.UTIL.validateNull;
import com.example.Authentication.dto.UserDTO;
import com.example.Authentication.dto.UserRegistrationDto;
import com.example.Authentication.repository.UserRepo;
import com.example.common.Exception.AnyException;
import com.example.common.Model.UserDetails1;
import com.example.common.util.PasswordUtil;
import com.example.common.util.WeatherHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "User Management API", description = "Endpoints for user registration, profile management, and account operations")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private AuthHelper authHelper;

    @Autowired
    private validateNull validateNull;

    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Registers a new user with the provided details and sends an OTP to the email for verification."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User registered successfully, OTP sent to email",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid registration data",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Server error during registration",
                    content = @Content(schema = @Schema(implementation = Map.class))
            )
    })
    public ResponseEntity<?> register(
            @Parameter(description = "User registration details", required = true)
            @Valid @RequestBody UserRegistrationDto registrationDto) {
        try {
            // Register user and initiate OTP verification
            userService.register(registrationDto);
            log.info("User registration initiated for email: {}", registrationDto.getUserEmail());

            // Mask email for response
            String maskedEmail = authHelper.maskEmail(registrationDto.getUserEmail());

            return ResponseEntity.ok(Map.of(
                    "message", "User registered successfully. Please verify your email with the OTP sent to " + maskedEmail,
                    "email", maskedEmail
            ));
        } catch (AnyException e) {
            log.error("Registration failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during registration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Registration failed: " + e.getMessage()));
        }
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get user profile",
            description = "Retrieves the profile details of the authenticated user."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserDTO.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized access - user not authenticated or account not active",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Server error retrieving profile",
                    content = @Content(schema = @Schema(implementation = Map.class))
            )
    })
    public ResponseEntity<?> getProfile(
            @AuthenticationPrincipal UserPrinciple userPrinciple) {
        // Validate user authentication and account status
        if (userPrinciple == null || !userPrinciple.checkAccountStatus()) {
            log.warn("User not authenticated or account not active/verified for profile request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not authenticated"));
        }
        try {
            // Retrieve user profile
            UserDTO response = userService.getUserProfile(userPrinciple.getUserId());
            log.info("Profile retrieved for user ID: {}", userPrinciple.getUserId());
            return ResponseEntity.ok(response);
        } catch (AnyException e) {
            log.warn("Profile retrieval failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error while retrieving profile: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving profile: " + e.getMessage()));
        }
    }

    @PostMapping("/profile/update-info")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Update user profile",
            description = "Updates the username, email, and contact number of the authenticated user."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Profile updated successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing or invalid profile details",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized access - user not authenticated or account not active",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Server error updating profile",
                    content = @Content(schema = @Schema(implementation = Map.class))
            )
    })
    public ResponseEntity<?> updateProfileInfo(
            @AuthenticationPrincipal UserPrinciple userPrinciple,
            @Parameter(description = "Profile details (username, userEmail, contactNumber)", required = true)
            @RequestBody Map<String, String> requestBody) {
        String username = requestBody.get("username");
        String userEmail = requestBody.get("userEmail");
        String contactNumber = requestBody.get("contactNumber");

        // Validate user authentication and account status
        if (userPrinciple == null || !userPrinciple.checkAccountStatus()) {
            log.warn("User not authenticated or account not active/verified for update profile request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        }
        // Validate input parameters
        if (validateNull.isNullOrEmpty(username) || validateNull.isNullOrEmpty(userEmail) || validateNull.isNullOrEmpty(contactNumber)) {
            log.warn("Missing profile details for user ID: {}", userPrinciple.getUserId());
            return ResponseEntity.badRequest().body(Map.of("error", "Username, email, and contact number are required"));
        }
        try {
            // Fetch user details
            UserDetails1 user = userRepo.findByUserId(userPrinciple.getUserId());
            if (user == null) {
                log.warn("User not found for ID: {}", userPrinciple.getUserId());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
            }
            // Update user profile
            userService.updateUser(user, username, userEmail, contactNumber);
            log.info("Profile updated successfully for user ID: {}", userPrinciple.getUserId());
            return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
        } catch (Exception e) {
            log.error("Profile update failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error updating profile: " + e.getMessage()));
        }
    }

    @PostMapping("/profile/change-password")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Change user password",
            description = "Changes the password of the authenticated user after validating the current password."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Password changed successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing or invalid password fields, or passwords do not match",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized access - user not authenticated or incorrect current password",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Server error changing password",
                    content = @Content(schema = @Schema(implementation = Map.class))
            )
    })
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal UserPrinciple userPrinciple,
            @Parameter(description = "Password details (currentPassword, newPassword, confirmPassword)", required = true)
            @RequestBody Map<String, String> requestBody) {
        String currentPassword = requestBody.get("currentPassword");
        String newPassword = requestBody.get("newPassword");
        String confirmPassword = requestBody.get("confirmPassword");

        // Validate user authentication and account status
        if (userPrinciple == null || !userPrinciple.checkAccountStatus()) {
            log.warn("User not authenticated or account not active/verified for change password request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        }
        // Validate input parameters
        if (validateNull.isNullOrEmpty(currentPassword) || validateNull.isNullOrEmpty(newPassword) || validateNull.isNullOrEmpty(confirmPassword)) {
            log.warn("Missing password fields for user ID: {}", userPrinciple.getUserId());
            return ResponseEntity.badRequest().body(Map.of("error", "All password fields are required"));
        }
        // Validate new password format
        if (!PasswordUtil.isValidPassword(newPassword)) {
            log.warn("Invalid new password format for user ID: {}", userPrinciple.getUserId());
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be 8+ chars, include uppercase, lowercase, digit, and special char"));
        }
        // Validate password match
        if (!newPassword.equals(confirmPassword)) {
            log.warn("New password and confirm password do not match for user ID: {}", userPrinciple.getUserId());
            return ResponseEntity.badRequest().body(Map.of("error", "New password and confirm password do not match"));
        }

        try {
            // Change user password
            boolean success = userService.changePassword(userPrinciple.getUserId(), currentPassword, newPassword);
            if (success) {
                log.info("Password changed successfully for user ID: {}", userPrinciple.getUserId());
                return ResponseEntity.ok(Map.of(
                        "message", "Password changed successfully",
                        "details", "Your password has been updated. Please use the new password for future logins."
                ));
            } else {
                log.warn("Incorrect current password for user ID: {}", userPrinciple.getUserId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Incorrect current password"));
            }
        } catch (Exception e) {
            log.error("Password change failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error changing password: " + e.getMessage()));
        }
    }

    @PostMapping("/profile/deactivate")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Deactivate user account",
            description = "Deactivates the authenticated user's account after password confirmation."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Account deactivated successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing confirm password",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized access - user not authenticated or incorrect password",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Server error deactivating account",
                    content = @Content(schema = @Schema(implementation = Map.class))
            )
    })
    public ResponseEntity<?> deactivateAccount(
            @AuthenticationPrincipal UserPrinciple userPrinciple,
            @Parameter(description = "Confirm password for deactivation", required = true)
            @RequestBody Map<String, String> requestBody) {
        String confirmPassword = requestBody.get("confirmPassword");

        // Validate user authentication and account status
        if (userPrinciple == null || !userPrinciple.checkAccountStatus()) {
            log.warn("User not authenticated or account not active/verified for deactivate account request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        }
        // Validate input parameter
        if (confirmPassword == null || confirmPassword.trim().isEmpty()) {
            log.warn("missing confirm password for user ID: {}", userPrinciple.getUserId());
            return ResponseEntity.badRequest().body(Map.of("error", "Confirm password is required"));
        }

        try {
            // Deactivate user account
            boolean success = userService.deactivateAccount(userPrinciple.getUserId(), confirmPassword);
            if (success) {
                log.info("Account deactivated successfully for user ID: {}", userPrinciple.getUserId());
                return ResponseEntity.ok(Map.of("message", "Account deactivated successfully"));
            } else {
                log.warn("Incorrect password for deactivation for user ID: {}", userPrinciple.getUserId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Incorrect password"));
            }
        } catch (Exception e) {
            log.error("Account deactivation failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error deactivating account: " + e.getMessage()));
        }
    }

    @PostMapping("/profile/delete")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Soft delete user account",
            description = "Soft deletes the authenticated user's account after password confirmation."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Account soft-deleted successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing confirm password",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized access - user not authenticated or incorrect password",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Server error soft-deleting account",
                    content = @Content(schema = @Schema(implementation = Map.class))
            )
    })
    public ResponseEntity<?> softDeleteAccount(
            @AuthenticationPrincipal UserPrinciple userPrinciple,
            @Parameter(description = "Confirm password for soft deletion", required = true)
            @RequestBody Map<String, String> requestBody) {
        String confirmPassword = requestBody.get("confirmPassword");

        // Validate user authentication and account status
        if (userPrinciple == null || !userPrinciple.checkAccountStatus()) {
            log.warn("User not authenticated or account not active/verified for soft delete account request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        }
        // Validate input parameter
        if (confirmPassword == null || confirmPassword.trim().isEmpty()) {
            log.warn("Missing confirmed password for user ID: {}", userPrinciple.getUserId());
            return ResponseEntity.badRequest().body(Map.of("error", "Confirm password is required"));
        }

        try {
            // Soft delete user account
            boolean success = userService.softDeleteAccount(userPrinciple.getUserId(), confirmPassword);
            if (success) {
                log.info("Account soft-deleted successfully for user ID: {}", userPrinciple.getUserId());
                return ResponseEntity.ok(Map.of("message", "Account soft-deleted successfully"));
            } else {
                log.warn("Incorrect password for soft deletion for user ID: {}", userPrinciple.getUserId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Incorrect password"));
            }
        } catch (Exception e) {
            log.error("Account soft deletion failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error soft-deleting account: " + e.getMessage()));
        }
    }

    @PostMapping("/profile/activate")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Activate user account",
            description = "Reactivates a deactivated user account after password confirmation."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Account activated successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing confirm password",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized access - user not authenticated or incorrect password",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Server error activating account",
                    content = @Content(schema = @Schema(implementation = Map.class))
            )
    })
    public ResponseEntity<?> activateAccount(
            @AuthenticationPrincipal UserPrinciple userPrinciple,
            @Parameter(description = "Confirm password for account activation", required = true)
            @RequestBody Map<String, String> requestBody) {
        String confirmPassword = requestBody.get("confirmPassword");

        // Validate user authentication and account status
        if (userPrinciple == null || !userPrinciple.checkAccountStatus()) {
            log.warn("User not authenticated or account not active/verified for activate account request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        }
        // Validate input parameter
        if (confirmPassword == null || confirmPassword.trim().isEmpty()) {
            log.warn("Missing confirm password for user ID: {}", userPrinciple.getUserId());
            return ResponseEntity.badRequest().body(Map.of("error", "Confirm password is required"));
        }

        try {
            // Reactivate user account
            boolean success = userService.reactivateAccount(userPrinciple.getUsername(), confirmPassword);
            if (success) {
                log.info("Account activated successfully for user ID: {}", userPrinciple.getUserId());
                return ResponseEntity.ok(Map.of("message", "Account activated successfully"));
            } else {
                log.warn("Incorrect password for activation for user ID: {}", userPrinciple.getUserId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Incorrect password"));
            }
        } catch (Exception e) {
            log.error("Account activation failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error activating account: " + e.getMessage()));
        }
    }
}