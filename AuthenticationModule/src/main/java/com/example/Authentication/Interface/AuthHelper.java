package com.example.Authentication.Interface;

import com.example.common.Model.UserDetails1;

public interface AuthHelper {
    enum LoginMethod { EMAIL, PHONE, USERNAME }
    LoginMethod determineLoginMethod(String username, String email, String phoneNumber);
    String maskEmail(String email);
    String maskPhoneNumber(String phoneNumber);
    UserDetails1 authenticateUser(LoginMethod method, String username, String email, String phoneNumber, String password);
    String loginWithEmail(String email);
    String loginWithPhone(String phoneNumber);
    UserDetails1 findUser(LoginMethod loginMethod, String username, String phoneNumber, String email);
    void verifyUser(UserDetails1 user);
}