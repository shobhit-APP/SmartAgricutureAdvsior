package com.smartagriculture.community.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "soil_parameters")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoilParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @NotNull(message = "pH value is required")
    @Column(name = "ph_value", nullable = false)
    private Double ph;

    @NotNull(message = "Nitrogen value is required")
    @Column(nullable = false)
    private Double nitrogen;

    @NotNull(message = "Phosphorus value is required")
    @Column(nullable = false)
    private Double phosphorus;

    @NotNull(message = "Potassium value is required")
    @Column(nullable = false)
    private Double potassium;

    @NotNull(message = "temperature value is required")
    @Column(name = "temperature")
    private float temperature;

    @NotNull(message = "humidity value is required")
    @Column(name = "humidity")
    private float humidity;

    @NotNull(message = "rainfall value is required")
    @Column(name = "rainfall")
    private float rainfall;

    @NotNull(message = "Organic carbon value is required")
    @Column(name = "organic_carbon", nullable = false)
    private Double organicCarbon;

    @Column(name = "sulfur")
    private Double sulfur;

    @Column(name = "zinc")
    private Double zinc;

    @Column(name = "iron")
    private Double iron;

    @Column(name = "manganese")
    private Double manganese;

    @Column(name = "copper")
    private Double copper;


    @NotBlank(message = "Soil texture is required")
    @Column(name = "soil_texture", nullable = false)
    private String soilTexture;

    @NotNull(message = "Moisture content is required")
    @Column(name = "moisture_content", nullable = false)
    private Double moistureContent;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "soil_report_id")
    private SoilReport soilReport;
}
