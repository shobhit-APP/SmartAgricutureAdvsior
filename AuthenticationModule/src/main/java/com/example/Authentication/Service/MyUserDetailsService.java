package com.example.Authentication.Service;

import com.example.Authentication.Components.UserPrinciple;
import com.example.common.Model.UserDetails1;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class MyUserDetailsService implements UserDetailsService {

    @Autowired
    private UserServiceImpl userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (!StringUtils.hasText(username)) {
            log.warn("loadUserByUsername: Empty or null username provided");
            throw new UsernameNotFoundException("Username must not be empty");
        }

        UserDetails1 user = userService.findByUsername(username);
        if (user == null) {
            log.warn("loadUserByUsername: User not found: {}", username);
            throw new UsernameNotFoundException("User with username '" + username + "' was not found");
        }

        log.debug("loadUserByUsername: Successfully loaded user: {}", username);
        return new UserPrinciple(user);
    }
}