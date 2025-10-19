package com.example.common.Model;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import java.util.List;

@Entity
@Table(name = "user_details") // Table name clean aur snake_case
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDetails1 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "fullname", nullable = false)
    private String fullname;

    @Column(name = "password", nullable = false)
    private String userPassword;

    @Column(name = "email", unique = true, nullable = false)
    private String userEmail;

    @Column(name = "contact_number", nullable = false, unique = true)
    private String contactNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status; // default inactive

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    private VerificationStatus verificationStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;  // NEW FIELD (Farmer / Expert)

    @OneToMany(mappedBy = "userDetails1", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Crop> cropList;

    @OneToMany(mappedBy = "userDetails1", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<CropRecommendation> cropRecommendation;

    public enum UserStatus {
        Active, Inactive, Deleted,Blocked;

        @JsonCreator
        public static UserStatus fromString(String value) {
            return UserStatus.valueOf(value.trim().toUpperCase());
        }
    }

    public enum UserRole {
        FARMER, EXPERT;

        @JsonCreator
        public static UserRole fromString(String value) {
            return UserRole.valueOf(value.toUpperCase());
        }
    }
    public enum VerificationStatus {
        Verified, Pending, Rejected;

        @JsonCreator
        public static VerificationStatus fromString(String value) {
            return VerificationStatus.valueOf(value.trim().toUpperCase());
        }
    }

}
