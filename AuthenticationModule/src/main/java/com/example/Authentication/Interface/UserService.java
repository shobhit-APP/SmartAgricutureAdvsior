package com.example.Authentication.Interface;

import com.example.Authentication.dto.UserDTO;
import com.example.Authentication.dto.UserRegistrationDto;
import com.example.common.Model.UserDetails1;

public interface UserService {
    void register(UserRegistrationDto registrationDto);
    void updateUser(UserDetails1 user,String Username,String UserEmail,String ContactNumber);
    boolean changePassword(Long userId, String currentPassword, String newPassword);

    boolean resetPassword(String phoneNumber, String newPassword);
    String GenerateToken(Long userId, String email);
    boolean deactivateAccount(Long userId, String confirmPassword);
    boolean softDeleteAccount(Long userId, String confirmPassword);
    boolean reactivateAccount(String username, String password);
    boolean checkPassword(String enteredPassword, Long userId);
    boolean checkEmail(String email);
    boolean checkPhoneNumber(String phone);
    boolean checkByUserName(String username);
    boolean sendVerificationEmail(Long userId, String email, String context);
    UserDetails1 loginWithPassword(String loginKey, String password);
    boolean requestPhoneLoginOtp(String phone);
    UserDetails1 verifyPhoneOtpAndGetUser(String phone, String otp);
    boolean forgotPasswordRequest(String email);
    boolean resetPasswordWithOtp(String email, String otp, String newPassword);
    public UserDTO getUserProfile(Long userId);
}