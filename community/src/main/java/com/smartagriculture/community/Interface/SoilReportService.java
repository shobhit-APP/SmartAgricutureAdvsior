package com.smartagriculture.community.Interface;



import com.smartagriculture.community.dto.SoilReportDto;

import java.util.List;

public interface SoilReportService {

    SoilReportDto createReport(SoilReportDto dto);

    SoilReportDto getReportById(Long id);

    List<SoilReportDto> getAllReports();

    List<SoilReportDto> searchByRegion(String region);

    List<SoilReportDto> searchByReporter(String name);

    SoilReportDto updateReport(Long id, SoilReportDto dto);

    void deleteReport(Long id);
}