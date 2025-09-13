package com.example.Authentication.Model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "otpdata")
public class Otpdata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String phoneNumber;
    private String email;
    private String otp;
    private String purpose;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private ZonedDateTime expiryTime;

    public Otpdata(String phoneNumber, String email, String otp, String purpose) {
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.otp = otp;
        this.purpose = purpose;
        this.expiryTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).plus(5, ChronoUnit.MINUTES);
    }

    public boolean isExpired() {
        return ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).isAfter(expiryTime);
    }
}