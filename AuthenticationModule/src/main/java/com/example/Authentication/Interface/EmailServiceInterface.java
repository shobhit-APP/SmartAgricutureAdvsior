package com.example.Authentication.Interface;

public interface EmailServiceInterface {

    /**
     * Check if email is valid and exists in DB
     */
    boolean checkEmail(String email);

    /**
     * Send OTP for password reset or verification
     */
    boolean sendOtp(String email, String otp);

    /**
     * Send confirmation email after password update
     */
    void sendPasswordUpdateConfirmation(String email);

    /**
     * Send verification link for account activation
     */
    void sendVerificationLink(String email, String verificationLink);
}
