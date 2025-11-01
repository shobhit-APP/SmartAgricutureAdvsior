package com.example.Authentication.Service;
import com.example.Authentication.Interface.EmailServiceInterface;
import com.example.Authentication.Interface.OtpService;
import com.example.Authentication.Interface.UserService;
import com.example.Authentication.Model.PasswordResetToken;
import com.example.Authentication.UTIL.validateNull;
import com.example.Authentication.dto.UserDTO;
import com.example.Authentication.dto.UserRegistrationDto;
import com.example.Authentication.enums.OtpPurpose;
import com.example.Authentication.repository.PasswordResetTokenRepository;
import com.example.Authentication.repository.UserRepo;
import com.example.common.Exception.AnyException;
import com.example.common.Model.UserDetails1;
import com.example.common.util.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Service implementation for managing user-related operations such as registration, authentication,
 * password management, and account status updates. Implements the {@link UserService} interface.
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private EmailServiceInterface emailService;

    @Autowired
    private OtpService otpService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private validateNull validateNull;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    /**
     * Registers a new user with the provided registration details.
     * Validates the email, phone number, username, and password before saving the user to the database.
     * Sends a verification email upon successful registration.
     *
     * @param registrationDto The {@link UserRegistrationDto} containing user registration details.
     * @throws AnyException If the registration details are invalid, already exist, or an error occurs during processing.
     */
    @Override
    @Transactional
    public void register(UserRegistrationDto registrationDto) {
        if (registrationDto == null) {
            throw new AnyException(HttpStatus.BAD_REQUEST.value(), "Registration details cannot be null");
        }

        if (!checkEmail(registrationDto.getUserEmail())) {
            throw new AnyException(HttpStatus.CONFLICT.value(), "Email is already registered");
        }

        if (!checkPhoneNumber(registrationDto.getContactNumber())) {
            throw new AnyException(HttpStatus.CONFLICT.value(), "Phone number is already registered");
        }

        if (!checkByUserName(registrationDto.getUsername())) {
            throw new AnyException(HttpStatus.CONFLICT.value(), "Username is already registered");
        }
        try {
            UserDetails1 userDetails = new UserDetails1();
            userDetails.setUsername(registrationDto.getUsername());
            userDetails.setFullname(registrationDto.getFullname());
            userDetails.setUserEmail(registrationDto.getUserEmail());
            userDetails.setContactNumber(registrationDto.getContactNumber());
            userDetails.setRole(UserDetails1.UserRole.valueOf(registrationDto.getRole()));
            if (!PasswordUtil.isValidPassword(registrationDto.getUserPassword())) {
                throw new AnyException(HttpStatus.BAD_REQUEST.value(), "Password must be 8+ chars, include uppercase, lowercase, digit, and special char");
            }
            userDetails.setUserPassword(passwordEncoder.encode(registrationDto.getUserPassword()));
            userDetails.setVerificationStatus(UserDetails1.VerificationStatus.Pending);
            userDetails.setStatus(UserDetails1.UserStatus.Inactive);

            UserDetails1 user = userRepo.save(userDetails);
            boolean isSend = sendVerificationEmail(user.getUserId(), registrationDto.getUserEmail(),registrationDto.getContactNumber(), "registration");
            if (!isSend) {
                throw new AnyException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to Send Email Try Again And Make Sure You Enter a Valid Email Address");
            }
        } catch (Exception e) {
            log.error("Registration failed: {}", e.getMessage(), e);
            throw new AnyException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to save data to database");
        }
    }

    /**
     * Checks if the provided email is available (not already registered).
     *
     * @param email The email to check.
     * @return {@code true} if the email is available, {@code false} otherwise.
     */
    @Override
    public boolean checkEmail(String email) {
        return Optional.ofNullable(email)
                .map(e -> !userRepo.existsByUserEmail(e))
                .orElse(false);
    }

    /**
     * Checks if the provided phone number is available (not already registered).
     *
     * @param phone The phone number to check.
     * @return {@code true} if the phone number is available, {@code false} otherwise.
     */
    @Override
    public boolean checkPhoneNumber(String phone) {
        return Optional.ofNullable(phone)
                .map(p -> !userRepo.existsByContactNumber(p))
                .orElse(false);
    }

    /**
     * Checks if the provided username is available (not already registered).
     *
     * @param username The username to check.
     * @return {@code true} if the username is available, {@code false} otherwise.
     */
    @Override
    public boolean checkByUserName(String username) {
        return Optional.ofNullable(username)
                .map(u -> !userRepo.existsByusername(u))
                .orElse(false);
    }

    /**
     * Sends a verification email with a unique token to the specified email address.
     *
     * @param userId  The ID of the user to verify.
     * @param email   The email address to send the verification link to.
     * @param context The context of the verification (e.g., "registration").
     * @return {@code true} if the email was sent successfully, {@code false} otherwise.
     */
    public boolean sendVerificationEmail(Long userId, String email, String phone, String context) {
        try {
            String token = GenerateToken(userId, email);
            String link = String.format(
                    "https://smartagriadvisior.com/api/verify?context=%s&email=%s&token=%s",
                    URLEncoder.encode(context, StandardCharsets.UTF_8),
                    URLEncoder.encode(email, StandardCharsets.UTF_8),
                    URLEncoder.encode(token, StandardCharsets.UTF_8)
            );

            if ("login".equalsIgnoreCase(context)) {
                emailService.sendVerificationLink(email, link);
                log.info("Login verification link sent to {} (context: login)", email);
            } else {
                String otp = otpService.generateAndStoreOtp(email, OtpPurpose.REGISTRATION);
                emailService.sendVerificationLink(email, link);
                if (phone != null && !phone.isBlank()) {
                   emailService.sendOtp(phone, otp);
                }
                log.info("Verification email + OTP sent to {} (context: {})", email, context);
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to send verification email/OTP to {}: {}", email, e.getMessage(), e);
            return false;
        }
    }



    /**
     * Finds a user by their ID.
     *
     * @param userId The ID of the user to find.
     * @return The {@link UserDetails1} object if found, or {@code null} if not found.
     */
    public UserDetails1 findByUserId(Long userId) {
        return userRepo.findById(userId).orElse(null);
    }

    /**
     * Finds a user by their username.
     *
     * @param username The username of the user to find.
     * @return The {@link UserDetails1} object if found, or {@code null} if not found.
     */
    public UserDetails1 findByUsername(String username) {
        return userRepo.findByUsername(username);
    }

    /**
     * Updates the user's username, email, and contact number.
     *
     * @param user          The {@link UserDetails1} object to update.
     * @param username      The new username.
     * @param userEmail     The new email address.
     * @param contactNumber The new contact number.
     */
    @Transactional
    public void updateUser(UserDetails1 user, String username, String userEmail, String contactNumber) {
        user.setUsername(username);
        user.setUserEmail(userEmail);
        user.setContactNumber(contactNumber);
        userRepo.save(user);
    }

    /**
     * Updates the user's password after validation.
     *
     * @param user        The {@link UserDetails1} object to update.
     * @param newPassword The new password to set.
     * @return {@code true} if the password was updated successfully, {@code false} otherwise.
     */
    @Transactional
    private boolean updateUserPassword(UserDetails1 user, String newPassword) {
        if (user == null || validateNull.isNullOrEmpty(newPassword)) {
            return false;
        }
        user.setUserPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);
        return true;
    }

    /**
     * Changes the user's password after verifying the current password.
     *
     * @param userId         The ID of the user.
     * @param currentPassword The current password for verification.
     * @param newPassword     The new password to set.
     * @return {@code true} if the password was changed successfully, {@code false} otherwise.
     */
    @Override
    public boolean changePassword(Long userId, String currentPassword, String newPassword) {
        UserDetails1 user = findByUserId(userId);
        if (user != null && passwordEncoder.matches(currentPassword, user.getUserPassword())) {
            return updateUserPassword(user, newPassword);
        }
        return false;
    }

    /**
     * Resets the user's password using their phone number.
     *
     * @param phoneNumber The phone number of the user.
     * @param newPassword The new password to set.
     * @return {@code true} if the password was reset successfully, {@code false} otherwise.
     */
    @Override
    @Transactional
    public boolean resetPassword(String phoneNumber, String newPassword,UserDetails1 user) {
        if (user != null) {
            return updateUserPassword(user, newPassword);
        }
        return false;
    }

    /**
     * Deactivates the user's account after verifying their password.
     *
     * @param userId         The ID of the user.
     * @param confirmPassword The password to verify the user.
     * @return {@code true} if the account was deactivated successfully, {@code false} otherwise.
     */
    @Override
    @Transactional
    public boolean deactivateAccount(Long userId, String confirmPassword) {
        UserDetails1 user = findByUserId(userId);
        if (user != null && passwordEncoder.matches(confirmPassword, user.getUserPassword())) {
            user.setStatus(UserDetails1.UserStatus.Inactive);
            userRepo.save(user);
            return true;
        }
        return false;
    }

    /**
     * Soft deletes the user's account after verifying their password.
     *
     * @param userId         The ID of the user.
     * @param confirmPassword The password to verify the user.
     * @return {@code true} if the account was soft deleted successfully, {@code false} otherwise.
     */
    @Transactional
    @Override
    public boolean softDeleteAccount(Long userId, String confirmPassword) {
        UserDetails1 user = findByUserId(userId);
        if (user != null && passwordEncoder.matches(confirmPassword, user.getUserPassword())) {
            user.setStatus(UserDetails1.UserStatus.Deleted);
            userRepo.save(user);
            return true;
        }
        return false;
    }

    /**
     * Reactivates an inactive user account after verifying their credentials.
     *
     * @param username The username of the user.
     * @param password The password to verify the user.
     * @return {@code true} if the account was reactivated successfully, {@code false} otherwise.
     */
    @Override
    @Transactional
    public boolean reactivateAccount(String username, String password) {
        UserDetails1 user = findByUsername(username);
        if (user != null && user.getStatus() == UserDetails1.UserStatus.Inactive &&
                passwordEncoder.matches(password, user.getUserPassword())) {
            user.setStatus(UserDetails1.UserStatus.Active);
            userRepo.save(user);
            return true;
        }
        return false;
    }

    /**
     * Verifies if the provided password matches the user's stored password.
     *
     * @param enteredPassword The password to verify.
     * @param userId         The ID of the user.
     * @return {@code true} if the password matches, {@code false} otherwise.
     */
    @Override
    public boolean checkPassword(String enteredPassword, Long userId) {
        UserDetails1 user = findByUserId(userId);
        return user != null && passwordEncoder.matches(enteredPassword, user.getUserPassword());
    }

    /**
     * Authenticates a user using their login key (username, email, or phone number) and password.
     *
     * @param loginKey The login key (username, email, or phone number).
     * @param password The password to verify.
     * @return The {@link UserDetails1} object if authentication is successful, or {@code null} if it fails.
     */
    @Override
    public UserDetails1 loginWithPassword(String loginKey, String password) {
        UserDetails1 user = (UserDetails1) userRepo.findByUsername(loginKey);
        if (user == null) {
            user = userRepo.findByUserEmail(loginKey);
        }
        if (user == null) {
            user = userRepo.findByContactNumber(loginKey);
        }
        if (user == null || user.getVerificationStatus() != UserDetails1.VerificationStatus.Verified ||
                user.getStatus() == UserDetails1.UserStatus.Deleted) {
            return null;
        }
        if (passwordEncoder.matches(password, user.getUserPassword())) {
            return user;
        }
        return null;
    }

    /**
     * Requests an OTP for phone-based login.
     *
     * @param phone The phone number to send the OTP to.
     * @return {@code true} if the OTP request was successful, {@code false} otherwise.
     */
    @Override
    public boolean requestPhoneLoginOtp(String phone) {
        try {
            if (!userRepo.existsByContactNumber(phone)) {
                return false;
            }
            String otp = otpService.generateAndStoreOtp(phone, OtpPurpose.LOGIN);
            // Assume SMS service sends OTP
            return true;
        } catch (Exception e) {
            log.error("Failed to request phone login OTP: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Verifies a phone OTP and returns the associated user.
     *
     * @param phone The phone number associated with the OTP.
     * @param otp   The OTP to verify.
     * @return The {@link UserDetails1} object if the OTP is valid, or {@code null} if verification fails.
     */
    @Override
    public UserDetails1 verifyPhoneOtpAndGetUser(String phone, String otp) {
        try {
            boolean valid = otpService.verifyOtp(phone, otp, OtpPurpose.LOGIN);
            if (!valid) return null;

            UserDetails1 user = userRepo.findByContactNumber(phone);
            if (user == null) return null;

            otpService.deleteOtpByIdentifier(phone);
            return user;
        } catch (Exception e) {
            log.error("Phone OTP verification failed: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Requests a password reset OTP for the specified email.
     *
     * @param email The email address to send the OTP to.
     * @return {@code true} if the OTP request was successful, {@code false} otherwise.
     */
    @Override
    @Transactional
    public boolean forgotPasswordRequest(String email) {
        try {
            String otp = otpService.generateAndStoreOtp(email, OtpPurpose.FORGOT_PASSWORD);
            return emailService.sendOtp(email, otp);
        } catch (Exception e) {
            log.error("Failed to send password reset OTP: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Resets the user's password using an OTP.
     *
     * @param email      The email address associated with the OTP.
     * @param otp        The OTP to verify.
     * @param newPassword The new password to set.
     * @return {@code true} if the password was reset successfully, {@code false} otherwise.
     */
    @Override
    @Transactional
    public boolean resetPasswordWithOtp(String email, String otp, String newPassword) {
        try {
            boolean valid = otpService.verifyOtp(email, otp, OtpPurpose.FORGOT_PASSWORD);
            if (!valid) return false;

            UserDetails1 user = userRepo.findByUserEmail(email);
            if (user == null || user.getStatus() == UserDetails1.UserStatus.Deleted) {
                return false;
            }

            user.setUserPassword(passwordEncoder.encode(newPassword));
            userRepo.save(user);
            otpService.deleteOtpByIdentifier(email);
            emailService.sendPasswordUpdateConfirmation(email);
            return true;
        } catch (Exception e) {
            log.error("Password reset failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Retrieves the user profile for the specified user ID.
     *
     * @param userId The ID of the user.
     * @return The {@link UserDTO} containing the user profile details.
     * @throws AnyException If the user is not found.
     */
    @Override
    @Transactional
    public UserDTO getUserProfile(Long userId) {
        UserDetails1 userDetails = userRepo.findByUserId(userId);
        if (userDetails == null) {
            throw new AnyException(HttpStatus.NOT_FOUND.value(), "User not found");
        }
        return UserMapper.toUserResponseDTO(userDetails);
    }

    @Override
    public UserDetails1 findById(Long userId) {
        return userRepo.findByUserId(userId);
    }

    /**
     * Generates a password reset token for the specified user and email.
     *
     * @param userId The ID of the user.
     * @param email  The email address associated with the user.
     * @return The generated token as a string.
     */
    @Override
    @Transactional
    public String GenerateToken(Long userId, String email) {
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUserId(userId);
        resetToken.setExpiryDate(Instant.now().plus(1, ChronoUnit.HOURS));
        passwordResetTokenRepository.save(resetToken);
        return token;
    }
}