package com.example.Authentication.Interface;
import org.springframework.http.ResponseEntity;

public interface AuthService {
    ResponseEntity<?> handleLoginRequest(String username, String phoneNumber, String email, String password);
    ResponseEntity<?> loginWithPhoneAndOtp(String phoneNumber);
    ResponseEntity<?> forgetPassword(String phoneNumber);
    ResponseEntity<?> resetPassword(String phoneNumber, String newPassword);
    ResponseEntity<?> verifyUser(String email, String token);
}