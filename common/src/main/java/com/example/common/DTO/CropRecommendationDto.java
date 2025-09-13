package com.example.common.DTO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * DTO for representing crop recommendation data based on soil and environmental factors.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CropRecommendationDto {

    /**
     * Nitrogen level in soil (required, non-negative).
     */
    @Min(value = 0, message = "Nitrogen level must be non-negative")
    private Double N;

    /**
     * Phosphorus level in soil (required, non-negative).
     */
    @Min(value = 0, message = "Phosphorus level must be non-negative")
    private Double P;

    /**
     * Potassium level in soil (required, non-negative).
     */
    @Min(value = 0, message = "Potassium level must be non-negative")
    private Double K;

    /**
     * Temperature in Celsius (required, within realistic agricultural range).
     */
    @Min(value = -50, message = "Temperature must be at least -50°C")
    @Max(value = 60, message = "Temperature must not exceed 60°C")
    private Double temperature;

    /**
     * Humidity percentage (required, 0-100%).
     */
    @Min(value = 0, message = "Humidity must be non-negative")
    @Max(value = 100, message = "Humidity must not exceed 100%")
    private Double humidity;

    /**
     * Soil pH value (required, within realistic agricultural range).
     */
    @Min(value = 0, message = "pH must be non-negative")
    @Max(value = 14, message = "pH must not exceed 14")
    private Double ph;

    /**
     * Rainfall in mm (required, non-negative).
     */
    @Min(value = 0, message = "Rainfall must be non-negative")
    private Double rainfall;

    /**
     * Predicted crop name in English (required).
     */
    @NotBlank(message = "Predicted crop name is required")
    private String predictedCrop;

    /**
     * Predicted crop name in Hindi (required).
     */
    @NotBlank(message = "Predicted crop name in Hindi is required")
    private String predictedCropHindi;

    /**
     * Description of the crop in Hindi (required).
     */
    @NotNull(message = "Hindi description is required")
    private String hindiDescription;

    /**
     * Description of the crop in English (required).
     */
    @NotNull(message = "English description is required")
    private String englishDescription;

}