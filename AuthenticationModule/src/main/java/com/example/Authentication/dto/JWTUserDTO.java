package com.example.Authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JWTUserDTO {
    private Long userId;
    private String username;
    private String fullname;
    private String status;
    private String verificationStatus;
    private String UserRole;
}
