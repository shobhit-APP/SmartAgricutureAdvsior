package com.example.Authentication.Interface;

import com.example.common.Model.UserDetails1;
import org.springframework.http.ResponseEntity;

public interface JwTService {
    ResponseEntity<?> generateAuthResponseForUser(UserDetails1 user);
}
