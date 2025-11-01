package com.smartagriculture.community.Service;

import com.example.Authentication.Components.UserPrinciple;
import com.example.Authentication.Model.Expert;
import com.example.Authentication.repository.ExpertRepository;
import com.example.common.Exception.AnyException;
import com.smartagriculture.community.Interface.SoilReportService;
import com.smartagriculture.community.Model.SoilParameter;
import com.smartagriculture.community.Model.SoilReport;
import com.smartagriculture.community.Repository.SoilParameterRepository;
import com.smartagriculture.community.Repository.SoilReportRepository;
import com.smartagriculture.community.dto.SoilParameterDto;
import com.smartagriculture.community.dto.SoilReportDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SoilReportServiceImpl implements SoilReportService {

    private final SoilReportRepository reportRepository;
    private final SoilParameterRepository parameterRepository;
    private final ExpertRepository expertRepository;

    private SoilReportDto convertToDto(SoilReport report) {
        List<SoilParameterDto> paramDtos = null;

        if (report.getParameters() != null && !report.getParameters().isEmpty()) {
            paramDtos = report.getParameters().stream()
                    .map(this::convertParameterToDto)
                    .collect(Collectors.toList());
        }

        return SoilReportDto.builder()
                .id(report.getId())
                .reporterName(report.getReporterName())
                .designation(report.getDesignation())
                .region(report.getRegion())
                .soilType(report.getSoilType())
                .remarks(report.getRemarks())
                .parameters(paramDtos)
                .reportDate(report.getReportDate() != null ?
                        report.getReportDate().toString() : null)
                .build();
    }

    private SoilParameterDto convertParameterToDto(SoilParameter param) {
        return SoilParameterDto.builder()
                .id(param.getId())
                .ph(param.getPh())
                .nitrogen(param.getNitrogen())
                .phosphorus(param.getPhosphorus())
                .potassium(param.getPotassium())
                .temperature(param.getTemperature())
                .humidity(param.getHumidity())
                .rainfall(param.getRainfall())
                .organicCarbon(param.getOrganicCarbon())
                .sulfur(param.getSulfur())
                .zinc(param.getZinc())
                .iron(param.getIron())
                .manganese(param.getManganese())
                .copper(param.getCopper())
                .soilTexture(param.getSoilTexture())
                .moistureContent(param.getMoistureContent())
                .build();
    }

    @Override
    public SoilReportDto createReport(SoilReportDto dto) {
        UserPrinciple currentUser = (UserPrinciple) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        Long userId = currentUser.getUserId();
        String role = currentUser.getUserRole().toString();

        if (dto.getRegion() == null || dto.getRegion().trim().isEmpty()) {
            throw new AnyException(400, "Region is required");
        }
        if (dto.getSoilType() == null || dto.getSoilType().trim().isEmpty()) {
            throw new AnyException(400, "Soil type is required");
        }

        String reporterName = currentUser.getUsername();
        String designation = "Farmer";

        if ("EXPERT".equalsIgnoreCase(role)) {
            Expert expert = expertRepository.findByUserId(userId)
                    .orElseThrow(() -> new AnyException(404, "Expert profile not found"));
            designation = expert.getField();
        }

        SoilReport report = SoilReport.builder()
                .reporterName(reporterName)
                .designation(designation)
                .region(dto.getRegion())
                .soilType(dto.getSoilType())
                .remarks(dto.getRemarks())
                .UserId(userId)
                .build();

        report = reportRepository.save(report);

        if (dto.getParameters() != null && !dto.getParameters().isEmpty()) {
            SoilReport finalReport = report;
            List<SoilParameter> parameters = dto.getParameters().stream()
                    .map(paramDto -> toParameterEntity(paramDto, finalReport))
                    .collect(Collectors.toList());
            parameterRepository.saveAll(parameters);
            report.setParameters(parameters);
        }

        SoilReport savedReport = reportRepository.save(report);
        return convertToDto(savedReport);
    }

    @Override
    public SoilReportDto getReportById(Long id) {
        SoilReport report = reportRepository.findById(id)
                .orElseThrow(() -> new AnyException(404, "Soil report not found with id: " + id));

        return convertToDto(report);
    }

    @Override
    public List<SoilReportDto> getAllReports() {
        return reportRepository.findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<SoilReportDto> searchByRegion(String region) {
        if (region == null || region.trim().isEmpty()) {
            throw new AnyException(400, "Region cannot be empty");
        }

        return reportRepository.findByRegionContainingIgnoreCase(region)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<SoilReportDto> searchByReporter(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new AnyException(400, "Reporter name cannot be empty");
        }

        return reportRepository.findByReporterNameContainingIgnoreCase(name)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public SoilReportDto updateReport(Long id, SoilReportDto dto) {
        SoilReport report = reportRepository.findById(id)
                .orElseThrow(() -> new AnyException(404, "Soil report not found with id: " + id));

        UserPrinciple currentUser = (UserPrinciple) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        Long userId = currentUser.getUserId();
        String role = currentUser.getUserRole().toString();

        if (!report.getUserId().equals(userId)) {
            throw new AnyException(403, "You can only update your own reports");
        }

        if (dto.getRegion() == null || dto.getRegion().trim().isEmpty()) {
            throw new AnyException(400, "Region is required");
        }
        if (dto.getSoilType() == null || dto.getSoilType().trim().isEmpty()) {
            throw new AnyException(400, "Soil type is required");
        }

        String designation = "Farmer";
        if ("EXPERT".equalsIgnoreCase(role)) {
            Expert expert = expertRepository.findByUserId(userId)
                    .orElseThrow(() -> new AnyException(404, "Expert profile not found"));
            designation = expert.getField();
        }

        report.setReporterName(currentUser.getUsername());
        report.setDesignation(designation);
        report.setRegion(dto.getRegion());
        report.setSoilType(dto.getSoilType());
        report.setRemarks(dto.getRemarks());

        if (report.getParameters() != null) {
            parameterRepository.deleteAll(report.getParameters());
        }

        if (dto.getParameters() != null && !dto.getParameters().isEmpty()) {
            List<SoilParameter> newParams = dto.getParameters().stream()
                    .map(paramDto -> toParameterEntity(paramDto, report))
                    .collect(Collectors.toList());
            parameterRepository.saveAll(newParams);
            report.setParameters(newParams);
        } else {
            report.setParameters(null);
        }

        SoilReport updatedReport = reportRepository.save(report);
        return convertToDto(updatedReport);
    }

    @Override
    public void deleteReport(Long id) {
        SoilReport report = reportRepository.findById(id)
                .orElseThrow(() -> new AnyException(404, "Soil report not found with id: " + id));

        UserPrinciple currentUser = (UserPrinciple) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        Long userId = currentUser.getUserId();

        if (!report.getUserId().equals(userId)) {
            throw new AnyException(403, "You can only delete your own reports");
        }

        reportRepository.delete(report);
    }

    private SoilParameter toParameterEntity(SoilParameterDto dto, SoilReport report) {
        return SoilParameter.builder()
                .ph(dto.getPh())
                .nitrogen(dto.getNitrogen())
                .phosphorus(dto.getPhosphorus())
                .potassium(dto.getPotassium())
                .temperature(dto.getTemperature())
                .humidity(dto.getHumidity())
                .rainfall(dto.getRainfall())
                .organicCarbon(dto.getOrganicCarbon())
                .sulfur(dto.getSulfur())
                .zinc(dto.getZinc())
                .iron(dto.getIron())
                .manganese(dto.getManganese())
                .copper(dto.getCopper())
                .soilTexture(dto.getSoilTexture())
                .moistureContent(dto.getMoistureContent())
                .soilReport(report)
                .build();
    }
}
