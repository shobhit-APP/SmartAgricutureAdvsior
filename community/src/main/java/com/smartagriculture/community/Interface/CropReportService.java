package com.smartagriculture.community.Interface;

import com.smartagriculture.community.dto.CropReportDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CropReportService {
    CropReportDto createReport(CropReportDto dto, MultipartFile image, Long userId);
    CropReportDto updateReport(Long id, CropReportDto dto, MultipartFile image, Long userId);
    CropReportDto getReportByIdForUser(Long id, Long userId);
    List<CropReportDto> getAllReportsByUserId(Long userId);
    List<CropReportDto> searchByCropForUser(String cropName, Long userId);
    List<CropReportDto> searchByRegionForUser(String region, Long userId);
    void deleteReport(Long id, Long userId);
    CropReportDto getCropReportById(Long id);
    List<CropReportDto> getListOfCropReportById(Long userId);
}
