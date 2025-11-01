package com.smartagriculture.community.Repository;


import com.smartagriculture.community.Model.CropReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CropReportRepository extends JpaRepository<CropReport, Long> {

    List<CropReport> findByUserId(Long userId);

    List<CropReport> findByCropNameContainingIgnoreCaseAndUserId(String cropName, Long userId);

    List<CropReport> findByRegionContainingIgnoreCaseAndUserId(String region, Long userId);
}
