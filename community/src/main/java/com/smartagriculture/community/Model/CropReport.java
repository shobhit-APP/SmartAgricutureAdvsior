package com.smartagriculture.community.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "crop_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CropReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Reporter name is required")
    @Column(name = "reporter_name", nullable = false)
    private String reporterName;

    @NotBlank(message = "Designation is required")
    @Column(nullable = false)
    private String designation; // Farmer / Expert / Lab Technician

    @NotBlank(message = "Crop name is required")
    @Column(name = "crop_name", nullable = false)
    private String cropName;

    @NotBlank(message = "Region is required")
    @Column(nullable = false)
    private String region;

    // ✅ fixed: changed UserId → userId for proper JPA field recognition
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotBlank(message = "Crop health description is required")
    @Column(name = "crop_health", nullable = false, length = 500)
    private String cropHealth;

    @Positive(message = "Estimated yield must be positive")
    @Column(name = "estimated_yield")
    private Double estimatedYield;

    @Column(name = "expert_remarks", length = 1000)
    private String expertRemarks;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
