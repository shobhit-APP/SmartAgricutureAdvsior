package com.smartagriculture.community.Controller;

import com.example.Authentication.Components.UserPrinciple;
import com.smartagriculture.community.Interface.CropReportService;
import com.smartagriculture.community.dto.CropReportDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/crop-reports")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated() and hasAnyRole('USER', 'ADMIN', 'EXPERT')")
public class CropReportController {

    private final CropReportService cropReportService;
    @PostMapping
    public ResponseEntity<CropReportDto> createReport(
            @Valid @RequestPart("report") CropReportDto dto,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal UserPrinciple userPrinciples) {

        Long userId = userPrinciples.getUserId();
        CropReportDto created = cropReportService.createReport(dto, image, userId);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<CropReportDto>> getAllReports(
            @AuthenticationPrincipal UserPrinciple userPrinciples) {

        Long userId = userPrinciples.getUserId();
        List<CropReportDto> reports = cropReportService.getAllReportsByUserId(userId);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CropReportDto> getReportById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrinciple userPrinciples) {

        Long userId = userPrinciples.getUserId();
        CropReportDto report = cropReportService.getReportByIdForUser(id, userId);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/search")
    public ResponseEntity<List<CropReportDto>> searchReports(
            @RequestParam(required = false) String crop,
            @RequestParam(required = false) String region,
            @AuthenticationPrincipal UserPrinciple userPrinciples) {

        Long userId = userPrinciples.getUserId();
        List<CropReportDto> results;

        if (crop != null && !crop.isEmpty()) {
            results = cropReportService.searchByCropForUser(crop, userId);
        } else if (region != null && !region.isEmpty()) {
            results = cropReportService.searchByRegionForUser(region, userId);
        } else {
            results = cropReportService.getAllReportsByUserId(userId);
        }

        return ResponseEntity.ok(results);
    }
    @PutMapping("/{id}")
    public ResponseEntity<CropReportDto> updateReport(
            @PathVariable Long id,
            @Valid @RequestPart("report") CropReportDto dto,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal UserPrinciple userPrinciples) {

        Long userId = userPrinciples.getUserId();
        CropReportDto updated = cropReportService.updateReport(id, dto, image, userId);
        return ResponseEntity.ok(updated);
    }

    // ✅ Delete report (only if owned by logged-in user)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrinciple userPrinciples) {

        Long userId = userPrinciples.getUserId();
        cropReportService.deleteReport(id, userId);
        return ResponseEntity.noContent().build();
    }
}
