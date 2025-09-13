package com.example.agriconnect.Controller;


import com.example.Authentication.Components.UserPrinciple;
import com.example.agriconnect.Service.CropDiseaseService;
import com.example.common.Model.CropDisease;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/diseases")
public class CropDiseaseRestController {

    @Autowired
    private CropDiseaseService cropDiseaseService;

    // Get Disease Dashboard Data
    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getDiseaseDashboard(
            @AuthenticationPrincipal UserPrinciple userPrinciples,
            @RequestParam(value = "lang", defaultValue = "en") String lang,
            @RequestParam(value = "cropType", required = false) String cropType,
            @RequestParam(value = "cause", required = false) String cause,
            @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(value = "size", defaultValue = "10") @Min(1) int size) {

        try {
            Long userId = userPrinciples.getUserId();
            Pageable pageable = PageRequest.of(page, size);
            Page<CropDisease> diseasePage = getDiseasePage(lang, cropType, cause, userId, pageable);
            List<CropDisease> allDiseases = diseasePage.getContent();
            List<String> cropTypes = "hi".equalsIgnoreCase(lang)
                    ? cropDiseaseService.findDistinctCropTypesHiByUserId(userId)
                    : cropDiseaseService.findDistinctCropTypesByUserId(userId);

            long criticalDiseases = allDiseases.stream()
                    .filter(this::isCriticalDisease)
                    .count();

            Map<String, Object> response = new HashMap<>();
            response.put("diseases", allDiseases);
            response.put("cropTypes", cropTypes);
            response.put("totalPages", diseasePage.getTotalPages());
            response.put("currentPage", page);
            response.put("totalElements", diseasePage.getTotalElements());
            response.put("criticalDiseases", criticalDiseases);
            response.put("lang", lang);
            response.put("userId", userId);

            if (allDiseases.isEmpty() && page == 0) {
                response.put("error", "No disease tracking data found. Please add some disease records first.");
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Error loading disease dashboard: " + e.getMessage()));
        }
    }

    // Get Disease Details
    @GetMapping("/details/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getDiseaseDetails(
            @AuthenticationPrincipal UserPrinciple userPrinciples,
            @PathVariable Long id,
            @RequestParam(value = "lang", defaultValue = "en") String lang) {

        try {
            Long userId = userPrinciples.getUserId();
            Optional<CropDisease> diseaseOpt = cropDiseaseService.findByIdAndUserId(id, userId);

            if (diseaseOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Disease not found or access denied."));
            }

            CropDisease disease = diseaseOpt.get();
            Map<String, Object> response = new HashMap<>();
            populateDiseaseAttributes(response, disease, lang);
            response.put("imagePath", disease.getImagePath());
            response.put("lang", lang);
            response.put("userId", userId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Error loading disease details: " + e.getMessage()));
        }
    }

    private Page<CropDisease> getDiseasePage(String lang, String cropType, String cause, Long userId, Pageable pageable) {
        boolean isHindi = "hi".equalsIgnoreCase(lang);
        if (cropType != null && !cropType.isEmpty() && cause != null && !cause.isEmpty()) {
            return isHindi
                    ? cropDiseaseService.findByCropTypeHiAndCauseHiContainingIgnoreCaseAndUserId(cropType, cause, userId, pageable)
                    : cropDiseaseService.findByCropTypeEnAndCauseEnContainingIgnoreCaseAndUserId(cropType, cause, userId, pageable);
        } else if (cropType != null && !cropType.isEmpty()) {
            return isHindi
                    ? cropDiseaseService.findByCropTypeHiAndUserId(cropType, userId, pageable)
                    : cropDiseaseService.findByCropTypeAndUserId(cropType, userId, pageable);
        } else if (cause != null && !cause.isEmpty()) {
            return isHindi
                    ? cropDiseaseService.findByCauseHiContainingIgnoreCaseAndUserId(cause, userId, pageable)
                    : cropDiseaseService.findByCauseEnContainingIgnoreCaseAndUserId(cause, userId, pageable);
        }
        return cropDiseaseService.findAllDiseasesByUserId(userId, pageable);
    }

    private boolean isCriticalDisease(CropDisease d) {
        String symptoms = (d.getSymptomsEn() != null ? d.getSymptomsEn() : "").toLowerCase();
        String cause = (d.getCauseEn() != null ? d.getCauseEn() : "").toLowerCase();
        return symptoms.contains("death") || symptoms.contains("severe") ||
                symptoms.contains("wilting") || cause.contains("virus") ||
                symptoms.contains("rot") || symptoms.contains("blight");
    }

    private void populateDiseaseAttributes(Map<String, Object> response, CropDisease disease, String lang) {
        if ("hi".equalsIgnoreCase(lang)) {
            response.put("diseaseName", disease.getNameHi());
            response.put("cause", disease.getCauseHi());
            response.put("symptoms", disease.getSymptomsHi());
            response.put("suggestion", disease.getSuggestionHi());
            response.put("cropType", disease.getCropTypeHi());
        } else {
            response.put("diseaseName", disease.getNameEn());
            response.put("cause", disease.getCauseEn());
            response.put("symptoms", disease.getSymptomsEn());
            response.put("suggestion", disease.getSuggestionEn());
            response.put("cropType", disease.getCropTypeEn());
        }
    }
}