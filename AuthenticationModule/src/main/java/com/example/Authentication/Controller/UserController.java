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
@Tag(name = "User Management", description = "User profile and account operations")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private WeatherHelper weatherHelper;

    @Autowired
    private AuthHelper authHelper;
    @Autowired
    private validateNull validateNull;


    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Register a user and send OTP to email for verification")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationDto registrationDto) {
        try {
            userService.register(registrationDto);
            log.info("User registration initiated for email: {}", registrationDto.getUserEmail());

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
    @Operation(summary = "Get user profile", description = "Retrieve profile details of the authenticated user")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal UserPrinciple userPrinciple) {
        if (userPrinciple == null || !userPrinciple.checkAccountStatus()) {
            log.warn("User not authenticated or account not active/verified for profile request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not authenticated"));
        }
        try {
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
    @Operation(summary = "Update user profile", description = "Update username, email, and contact number of the authenticated user")
    public ResponseEntity<?> updateProfileInfo(@AuthenticationPrincipal UserPrinciple userPrinciple,
                                               @RequestBody Map<String, String> requestBody) {
        String username = requestBody.get("username");
        String userEmail = requestBody.get("userEmail");
        String contactNumber = requestBody.get("contactNumber");

        if (userPrinciple == null || !userPrinciple.checkAccountStatus()) {
            log.warn("User not authenticated or account not active/verified for update profile request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        }
        if (validateNull.isNullOrEmpty(username)|| validateNull.isNullOrEmpty(userEmail) || validateNull.isNullOrEmpty(contactNumber)) {
            log.warn("Missing profile details for user ID: {}", userPrinciple.getUserId());
            return ResponseEntity.badRequest().body(Map.of("error", "Username, email, and contact number are required"));
        }
        try {
            UserDetails1 user = userRepo.findByUserId(userPrinciple.getUserId());
            if (user == null) {
                log.warn("User not found for ID: {}", userPrinciple.getUserId());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
            }
            userService.updateUser(user,username,userEmail,contactNumber);
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
    @Operation(summary = "Change user password", description = "Change the password of the authenticated user")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal UserPrinciple userPrinciple,
                                            @RequestBody Map<String, String> requestBody) {
        String currentPassword = requestBody.get("currentPassword");
        String newPassword = requestBody.get("newPassword");
        String confirmPassword = requestBody.get("confirmPassword");

        if (userPrinciple == null || !userPrinciple.checkAccountStatus()) {
            log.warn("User not authenticated or account not active/verified for change password request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        }
        if(validateNull.isNullOrEmpty(currentPassword)||validateNull.isNullOrEmpty(newPassword)||validateNull.isNullOrEmpty(confirmPassword)) {
            log.warn("Missing password fields for user ID: {}", userPrinciple.getUserId());
            return ResponseEntity.badRequest().body(Map.of("error", "All password fields are required"));
        }
        if (!PasswordUtil.isValidPassword(newPassword)) {
            log.warn("Invalid new password format for user ID: {}", userPrinciple.getUserId());
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be 8+ chars, include uppercase, lowercase, digit, and special char"));
        }

        if (!newPassword.equals(confirmPassword)) {
            log.warn("New password and confirm password do not match for user ID: {}", userPrinciple.getUserId());
            return ResponseEntity.badRequest().body(Map.of("error", "New password and confirm password do not match"));
        }

        try {
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
    @Operation(summary = "Deactivate user account", description = "Deactivate the authenticated user's account")
    public ResponseEntity<?> deactivateAccount(@AuthenticationPrincipal UserPrinciple userPrinciple,
                                               @RequestBody Map<String, String> requestBody) {
        String confirmPassword = requestBody.get("confirmPassword");

        if (userPrinciple == null || !userPrinciple.checkAccountStatus()) {
            log.warn("User not authenticated or account not active/verified for deactivate account request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        }
        if (confirmPassword == null || confirmPassword.trim().isEmpty()) {
            log.warn("Missing confirm password for user ID: {}", userPrinciple.getUserId());
            return ResponseEntity.badRequest().body(Map.of("error", "Confirm password is required"));
        }

        try {
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
    @Operation(summary = "Soft delete user account", description = "Soft delete the authenticated user's account")
    public ResponseEntity<?> softDeleteAccount(@AuthenticationPrincipal UserPrinciple userPrinciple,
                                               @RequestBody Map<String, String> requestBody) {
        String confirmPassword = requestBody.get("confirmPassword");

        if (userPrinciple == null || !userPrinciple.checkAccountStatus()) {
            log.warn("User not authenticated or account not active/verified for soft delete account request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        }
        if (confirmPassword == null || confirmPassword.trim().isEmpty()) {
            log.warn("Missing confirmed password for user ID: {}", userPrinciple.getUserId());
            return ResponseEntity.badRequest().body(Map.of("error", "Confirm password is required"));
        }

        try {
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
    @Operation(summary = "Activate user account", description = "Reactivate a deactivated user account")
    public ResponseEntity<?> activateAccount(@AuthenticationPrincipal UserPrinciple userPrinciple,
                                             @RequestBody Map<String, String> requestBody) {
        String confirmPassword = requestBody.get("confirmPassword");

        if (userPrinciple == null || !userPrinciple.checkAccountStatus()) {
            log.warn("User not authenticated or account not active/verified for activate account request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        }
        if (confirmPassword == null || confirmPassword.trim().isEmpty()) {
            log.warn("missing confirm password for user ID: {}", userPrinciple.getUserId());
            return ResponseEntity.badRequest().body(Map.of("error", "Confirm password is required"));
        }

        try {
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