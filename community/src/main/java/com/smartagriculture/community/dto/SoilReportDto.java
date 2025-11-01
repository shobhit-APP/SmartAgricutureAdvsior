package com.smartagriculture.community.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoilReportDto {

    private Long id;

    private String reporterName;

    private String designation;

    @NotBlank(message = "Region is required")
    private String region;

    @NotBlank(message = "Soil type is required")
    private String soilType;

    private List<SoilParameterDto> parameters;

    private String remarks;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private String reportDate;
}
