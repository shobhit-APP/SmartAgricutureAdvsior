package com.smartagriculture.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoilParameterDto {

    private Long id;

    @NotNull(message = "pH value is required")
    private Double ph;

    @NotNull(message = "Nitrogen value is required")
    private Double nitrogen;

    @NotNull(message = "Phosphorus value is required")
    private Double phosphorus;

    @NotNull(message = "Potassium value is required")
    private Double potassium;

    @NotNull(message = "Temperature value is required")
    private Float temperature;

    @NotNull(message = "Humidity value is required")
    private Float humidity;

    @NotNull(message = "Rainfall value is required")
    private Float rainfall;

    @NotNull(message = "Organic carbon value is required")
    private Double organicCarbon;

    private Double sulfur;
    private Double zinc;
    private Double iron;
    private Double manganese;
    private Double copper;

    @NotBlank(message = "Soil texture is required")
    private String soilTexture;

    @NotNull(message = "Moisture content is required")
    private Double moistureContent;

    private Long soilReportId; // foreign key
}
