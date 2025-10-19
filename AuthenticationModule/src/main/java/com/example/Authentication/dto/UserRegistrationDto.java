package com.example.Authentication.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user registration data.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRegistrationDto {

    /**
     * Username for the user (required, 3-20 characters).
     */
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    private String username;

    /**
     * Full name of the user (required).
     */
    @NotBlank(message = "Full name is required")
    private String fullname;

    /**
     * Password for the user (required, strong password rules apply).
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String userPassword;

    /**
     * Email address of the user (required, must be valid).
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String userEmail;

    /**
     * Contact number of the user (required, 10-digit Indian phone number).
     */
    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Contact number must be a valid 10-digit Indian number")
    private String contactNumber;

    @NotBlank(message = "User role is required")
    private String role; // FARMER or EXPERT
}