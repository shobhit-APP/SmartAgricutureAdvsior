package com.example.Authentication.Interface;


import com.example.Authentication.enums.OtpPurpose;

public interface OtpService {
    String generateAndStoreOtp(String identifier, OtpPurpose purpose);
    boolean verifyOtp(String identifier, String otpEntered, OtpPurpose purpose);
    boolean verifyLoginOtp(String phoneNumber, String otpEntered);
    boolean verifyOtp(String phoneNumber, String otpEntered);
    void deleteOtpByIdentifier(String identifier); // Handles both phone and email
    void sendOtp(String toPhone, String otp);
}