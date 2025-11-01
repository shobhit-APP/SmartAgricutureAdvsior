package com.smartagriculture.community.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "soil_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoilReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Reporter name is required")
    @Column(name = "reporter_name", nullable = false)
    private String reporterName;

    @NotBlank(message = "Designation is required")
    @Column(nullable = false)
    private String designation; // Farmer / Expert / Lab Technician

    @NotBlank(message = "Region is required")
    @Column(nullable = false)
    private String region;

    @NotBlank(message = "Soil type is required")
    @Column(name = "soil_type", nullable = false)
    private String soilType; // e.g., Clay, Sandy, Loamy, Silt, Peaty

    @Column(name = "userId", nullable = false)
    private Long UserId;

    @OneToMany(mappedBy = "soilReport", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SoilParameter> parameters;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Column(name = "report_date", nullable = false, updatable = false)
    private LocalDateTime reportDate;

    @PrePersist
    protected void onCreate() {
        reportDate = LocalDateTime.now();
    }
}
