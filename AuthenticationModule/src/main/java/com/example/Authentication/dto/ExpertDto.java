package com.example.Authentication.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpertDto {

    private Long id;
    private Long userId;

    private String userName;
    private String userEmail;
    private String userRole;

    @NotBlank(message = "Expertise field is required")
    private String field;

    @NotBlank(message = "Experience is required")
    private String experience_years;

    private String organization;
    private String profileImage_path;

    @NotBlank(message = "Verification Id Required")
    private String uploadId_path;

    private boolean isVerified;
    private boolean pendingReview;

    private String rejectionReason;

    private String createdAt;
    private String updatedAt;

    public String getStatusText() {
        if (isVerified) {
            return "APPROVED";
        } else if (pendingReview) {
            return "PENDING";
        } else {
            return "REJECTED";
        }
    }
}