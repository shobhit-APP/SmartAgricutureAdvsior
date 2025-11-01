package com.example.Authentication.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "experts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @NotBlank(message = "Expertise field is required")
    @Column(nullable = false)
    private String field;

    @NotBlank(message = "Experience is required")
    @Column(nullable = false)
    private String experience_years;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "organization")
    private String organization;


    // Store Cloudinary URL or local file path
    @Column(name = "profile_image_path")
    private String profileImage_path;

    // Store Cloudinary URL or local file path
    @Column(name = "upload_id_path")
    private String uploadId_path;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    private boolean pendingReview;

}
