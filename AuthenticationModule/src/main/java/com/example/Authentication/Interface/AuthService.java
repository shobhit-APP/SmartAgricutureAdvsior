package com.example.Authentication.Interface;
import org.springframework.http.ResponseEntity;

public interface AuthService {
    ResponseEntity<?> handleLoginRequest(String username, String phoneNumber, String email, String password);
    ResponseEntity<?> loginWithPhoneAndOtp(String phoneNumber);
    ResponseEntity<?> forgetPassword(String email);
    ResponseEntity<?> resetPassword(String email, String newPassword);
    ResponseEntity<?> verifyUser(String email, String token,String  otp);
}