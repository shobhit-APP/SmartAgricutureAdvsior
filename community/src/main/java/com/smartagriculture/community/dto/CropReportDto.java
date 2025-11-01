package com.smartagriculture.community.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CropReportDto {

    private Long id;

    @NotBlank(message = "Reporter name is required")
    private String reporterName;

    @NotBlank(message = "Designation is required")
    private String designation;

    @NotBlank(message = "Crop name is required")
    private String cropName;

    @NotBlank(message = "Region is required")
    private String region;

    @NotBlank(message = "Crop health description is required")
    private String cropHealth;

    @Positive(message = "Estimated yield must be positive")
    private Double estimatedYield;

    private String expertRemarks;
    private String imageUrl;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private String createdAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private String updatedAt;
}
