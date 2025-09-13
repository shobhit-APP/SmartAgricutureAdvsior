package com.example.Authentication.Service;

import com.example.Authentication.dto.UserDTO;
import com.example.common.Model.UserDetails1;

public class UserMapper {

    public static UserDTO toUserResponseDTO(UserDetails1 userDetails) {
        if (userDetails == null) {
            return null;
        }

        return new UserDTO(
                userDetails.getUserId(),
                userDetails.getUserEmail(),
                userDetails.getUsername(),
                userDetails.getFullname(),
                userDetails.getContactNumber(),
                userDetails.getVerificationStatus() != null ? userDetails.getVerificationStatus().name() : null,
                userDetails.getStatus() != null ? userDetails.getStatus().name() : null
        );
    }
}
