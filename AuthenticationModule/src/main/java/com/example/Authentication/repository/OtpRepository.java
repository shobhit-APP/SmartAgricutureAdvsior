package com.example.Authentication.repository;

import com.example.Authentication.Model.Otpdata;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otpdata, Long> {
    Optional<Otpdata> findByPhoneNumberAndPurpose(String phoneNumber, String purpose);
    Optional<Otpdata> findByEmailAndPurpose(String email, String purpose);
    void deleteByPhoneNumber(String phoneNumber);
    void deleteByEmail(String email);
}