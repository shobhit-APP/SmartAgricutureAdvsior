package com.example.Authentication.repository;

import com.example.common.Model.UserDetails1;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Collection;

@Repository
public interface UserRepo extends JpaRepository<UserDetails1, Long> {
    UserDetails1 findByUsername(String username);
    void deleteByUsername(String username);
    boolean existsByUserEmail(String email);
    boolean existsByContactNumber(String phoneNumber);
    UserDetails1 findByUserEmail(String email);
    UserDetails1 findByContactNumber(String phoneNumber);
    boolean existsByusername(String u);
    UserDetails1 findByUserId(Long userId);
}
