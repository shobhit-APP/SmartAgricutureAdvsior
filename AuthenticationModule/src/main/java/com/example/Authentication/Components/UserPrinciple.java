package com.example.Authentication.Components;

import com.example.common.Model.UserDetails1;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;

@Component
@AllArgsConstructor
@NoArgsConstructor
public class UserPrinciple implements UserDetails {
    @Getter
    private  Long userId;
    private String username;
    private String fullName;
    private String password;
    private UserDetails1.UserStatus status;
    private UserDetails1.VerificationStatus verificationStatus;

    // Constructor for database-driven authentication (using UserDetails1)
    public UserPrinciple(UserDetails1 user) {
        this.userId = user.getUserId();
        this.username = user.getUsername();
        this.fullName = user.getFullname();
        this.password = user.getUserPassword();
        this.status = user.getStatus();
        this.verificationStatus = user.getVerificationStatus();
    }

    // Constructor for JWT-driven authentication (using claims from UserContext)
    public UserPrinciple(Long userId, String username, String fullName,
                         UserDetails1.UserStatus status, UserDetails1.VerificationStatus verificationStatus) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.password = null; // Not needed for JWT
        this.status = status;
        this.verificationStatus = verificationStatus;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList(); // Add roles if needed
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }
    /**
     * Custom method to check if user is Active and Verified
     */
    public boolean checkAccountStatus() {
        return userId != null
                && status == UserDetails1.UserStatus.Active
                && verificationStatus == UserDetails1.VerificationStatus.Verified;
    }

    @Override
    public boolean isAccountNonExpired() {
        return status != UserDetails1.UserStatus.Deleted;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserDetails1.UserStatus.Inactive
                && status != UserDetails1.UserStatus.Deleted;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Passwords never expire in this setup
    }

    @Override
    public boolean isEnabled() {
        return status == UserDetails1.UserStatus.Active;
    }
}