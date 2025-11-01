package com.smartagriculture.community.Controller;

import com.smartagriculture.community.Interface.SoilReportService;
import com.smartagriculture.community.dto.SoilReportDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/soil-reports")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated() and hasAnyRole('USER', 'ADMIN', 'EXPERT')")
public class SoilReportController {

    private final SoilReportService soilReportService;

    @PostMapping
    public ResponseEntity<SoilReportDto> createReport(
            @Valid @RequestBody SoilReportDto dto) {
        SoilReportDto report = soilReportService.createReport(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(report);
    }

    @GetMapping
    public ResponseEntity<List<SoilReportDto>> getAllReports() {
        return ResponseEntity.ok(soilReportService.getAllReports());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SoilReportDto> getReport(@PathVariable Long id) {
        return ResponseEntity.ok(soilReportService.getReportById(id));
    }

    @GetMapping("/search/region")
    public ResponseEntity<List<SoilReportDto>> searchByRegion(
            @RequestParam String q) {
        return ResponseEntity.ok(soilReportService.searchByRegion(q));
    }

    @GetMapping("/search/reporter")
    public ResponseEntity<List<SoilReportDto>> searchByReporter(
            @RequestParam String q) {
        return ResponseEntity.ok(soilReportService.searchByReporter(q));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SoilReportDto> updateReport(
            @PathVariable Long id,
            @Valid @RequestBody SoilReportDto dto) {
        return ResponseEntity.ok(soilReportService.updateReport(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable Long id) {
        soilReportService.deleteReport(id);
        return ResponseEntity.noContent().build();
    }
}