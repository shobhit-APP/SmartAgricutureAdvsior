package com.smartagriculture.community.Repository;


import com.smartagriculture.community.Model.SoilReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SoilReportRepository extends JpaRepository<SoilReport, Long> {
    List<SoilReport> findByRegionContainingIgnoreCase(String region);
    List<SoilReport> findByReporterNameContainingIgnoreCase(String name);
}