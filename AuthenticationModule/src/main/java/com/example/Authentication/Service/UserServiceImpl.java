package com.example.Authentication.Service;
import com.example.Authentication.Interface.AuthHelper;
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

    @Override
    public void register(UserRegistrationDto registrationDto) {
        if (registrationDto == null) {
            throw new AnyException(HttpStatus.BAD_REQUEST.value(), "Registration details cannot be null");
        }

        if (!checkEmail(registrationDto.getUserEmail())) {
            throw new AnyException(HttpStatus.CONFLICT.value(), "Email is already registered");
        }

        if (!checkPhoneNumber(registrationDto.getContactNumber())) {
            throw new AnyException(HttpStatus.CONFLICT.value(),"Phone number is already registered");
        }

        if (!checkByUserName(registrationDto.getUsername())) {
            throw new AnyException(HttpStatus.CONFLICT.value(),"Username is already registered");
        }
        try {
            UserDetails1 userDetails = new UserDetails1();
            userDetails.setUsername(registrationDto.getUsername());
            userDetails.setFullname(registrationDto.getFullname());
            userDetails.setUserEmail(registrationDto.getUserEmail());
            userDetails.setContactNumber(registrationDto.getContactNumber());
            if (!PasswordUtil.isValidPassword(registrationDto.getUserPassword())) {
                throw new AnyException(HttpStatus.BAD_REQUEST.value(), "Password must be 8+ chars, include uppercase, lowercase, digit, and special char");
            }
            userDetails.setUserPassword(passwordEncoder.encode(registrationDto.getUserPassword()));
            userDetails.setVerificationStatus(UserDetails1.VerificationStatus.Pending);
            userDetails.setStatus(UserDetails1.UserStatus.Inactive);

            UserDetails1 user = userRepo.save(userDetails);
            boolean IsSend = sendVerificationEmail(user.getUserId(),registrationDto.getUserEmail(),"registration");
            if(!IsSend)
            {
                throw new AnyException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to Send Email Try Again And Make Sure You Enter a Valid Email Address");
            }
        } catch (Exception e) {
            log.error("Registration failed: {}", e.getMessage(), e);
            throw new AnyException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to save data to database");
        }
    }

    @Override
    public boolean checkEmail(String email) {
        return Optional.ofNullable(email)
                .map(e -> !userRepo.existsByUserEmail(e))
                .orElse(false);
    }

    @Override
    public boolean checkPhoneNumber(String phone) {
        return Optional.ofNullable(phone)
                .map(p -> !userRepo.existsByContactNumber(p))
                .orElse(false);
    }

    @Override
    public boolean checkByUserName(String username) {
        return Optional.ofNullable(username)
                .map(u -> !userRepo.existsByusername(u))
                .orElse(false);
    }

    public boolean sendVerificationEmail(Long userId, String email, String context) {
        try {
            String token = GenerateToken(userId, email);
            String link = String.format(
                    "https://smartagriadvisior.com/api/verify?context=%s&email=%s&token=%s",
                    URLEncoder.encode(context, StandardCharsets.UTF_8),
                    URLEncoder.encode(email, StandardCharsets.UTF_8),
                    URLEncoder.encode(token, StandardCharsets.UTF_8)
            );

            // Send email
            emailService.sendVerificationLink(email, link);
            log.info("Verification email sent for {} with context: {}", email, context);
            return true;
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", email, e.getMessage(), e);
            return false;
        }
    }



    public UserDetails1 findByUserId(Long userId) {
        return userRepo.findById(userId).orElse(null);
    }

    public UserDetails1 findByUsername(String username) {
        return userRepo.findByUsername(username);
    }

    public void updateUser(UserDetails1 user,String username,String userEmail,String contactNumber) {
        user.setUsername(username);
        user.setUserEmail(userEmail);
        user.setContactNumber(contactNumber);
        userRepo.save(user);
    }

    private boolean updateUserPassword(UserDetails1 user, String newPassword) {
        if (user == null || validateNull.isNullOrEmpty(newPassword)) {
            return false;
        }
        user.setUserPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);
        return true;
    }

    @Override
    public boolean changePassword(Long userId, String currentPassword, String newPassword) {
        UserDetails1 user = findByUserId(userId);
        if (user != null && passwordEncoder.matches(currentPassword, user.getUserPassword())) {
            return updateUserPassword(user, newPassword);
        }
        return false;
    }
    @Override
    public boolean resetPassword(String phoneNumber, String newPassword) {
        UserDetails1 user = userRepo.findByContactNumber(phoneNumber);
        if (user != null) {
            return updateUserPassword(user, newPassword);
        }
        return false;
    }

    @Override
    public boolean deactivateAccount(Long userId, String confirmPassword) {
        UserDetails1 user = findByUserId(userId);
        if (user != null && passwordEncoder.matches(confirmPassword, user.getUserPassword())) {
            user.setStatus(UserDetails1.UserStatus.Inactive);
            userRepo.save(user);
            return true;
        }
        return false;
    }

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

    @Override
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

    @Override
    public boolean checkPassword(String enteredPassword, Long userId) {
        UserDetails1 user = findByUserId(userId);
        return user != null && passwordEncoder.matches(enteredPassword, user.getUserPassword());
    }

    @Override
    public UserDetails1 loginWithPassword(String loginKey, String password) {
        UserDetails1 user = (UserDetails1) userRepo.findByUsername(loginKey);
        if (user == null) {
            user = userRepo.findByUserEmail(loginKey).stream().findFirst().orElse(null);
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

    @Override
    public UserDetails1 verifyPhoneOtpAndGetUser(String phone, String otp) {
        try {
            boolean valid = otpService.verifyOtp( phone, otp, OtpPurpose.LOGIN);
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

    @Override
    public boolean forgotPasswordRequest(String email) {
        try {
            String otp = otpService.generateAndStoreOtp(email, OtpPurpose.FORGOT_PASSWORD);
            return emailService.sendOtp(email, otp);
        } catch (Exception e) {
            log.error("Failed to send password reset OTP: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean resetPasswordWithOtp(String email, String otp, String newPassword) {
        try {
            boolean valid = otpService.verifyOtp(email, otp, OtpPurpose.FORGOT_PASSWORD);
            if (!valid) return false;

            UserDetails1 user = userRepo.findByUserEmail(email).stream().findFirst().orElse(null);
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

    @Override
    public UserDTO getUserProfile(Long userId) {
        UserDetails1 userDetails = userRepo.findByUserId(userId);
        if (userDetails == null) {
            throw new AnyException(HttpStatus.NOT_FOUND.value(), "User not found");
        }
        return UserMapper.toUserResponseDTO(userDetails);
    }
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