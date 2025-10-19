package com.example.agriconnect.Controller;

import com.example.Authentication.Components.UserPrinciple;
import com.example.agriconnect.Service.CropDiseaseService;
import com.example.common.Model.CropDisease;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/diseases")
@Tag(name = "Crop Disease API", description = "Endpoints for managing crop disease data")
public class CropDiseaseRestController {

    @Autowired
    private CropDiseaseService cropDiseaseService;

    // Get Disease Dashboard Data
    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get crop disease dashboard",
            description = "Retrieves a paginated list of crop diseases for the authenticated user, filtered by language, crop type, and cause. Includes metadata like total pages and critical disease count."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved disease dashboard data",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters or server error",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized access - authentication required",
                    content = @Content
            )
    })
    public ResponseEntity<Map<String, Object>> getDiseaseDashboard(
            @AuthenticationPrincipal UserPrinciple userPrinciples,
            @Parameter(description = "Language for disease data (e.g., 'en' for English, 'hi' for Hindi)", example = "en")
            @RequestParam(value = "lang", defaultValue = "en") String lang,
            @Parameter(description = "Filter by crop type (optional)", example = "Wheat")
            @RequestParam(value = "cropType", required = false) String cropType,
            @Parameter(description = "Filter by disease cause (optional)", example = "Fungus")
            @RequestParam(value = "cause", required = false) String cause,
            @Parameter(description = "Page number for pagination (zero-based)", example = "0")
            @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Number of records per page", example = "10")
            @RequestParam(value = "size", defaultValue = "10") @Min(1) int size) {

        // Fetch user ID from authenticated principal
        try {
            Long userId = userPrinciples.getUserId();
            Pageable pageable = PageRequest.of(page, size);
            // Retrieve paginated disease data based on filters
            Page<CropDisease> diseasePage = getDiseasePage(lang, cropType, cause, userId, pageable);
            List<CropDisease> allDiseases = diseasePage.getContent();
            // Fetch distinct crop types for the user, based on language
            List<String> cropTypes = "hi".equalsIgnoreCase(lang)
                    ? cropDiseaseService.findDistinctCropTypesHiByUserId(userId)
                    : cropDiseaseService.findDistinctCropTypesByUserId(userId);

            // Count diseases with critical symptoms or causes
            long criticalDiseases = allDiseases.stream()
                    .filter(this::isCriticalDisease)
                    .count();

            // Build response map with disease data and metadata
            Map<String, Object> response = new HashMap<>();
            response.put("diseases", allDiseases);
            response.put("cropTypes", cropTypes);
            response.put("totalPages", diseasePage.getTotalPages());
            response.put("currentPage", page);
            response.put("totalElements", diseasePage.getTotalElements());
            response.put("criticalDiseases", criticalDiseases);
            response.put("lang", lang);
            response.put("userId", userId);

            // Handle case where no data is found on the first page
            if (allDiseases.isEmpty() && page == 0) {
                response.put("error", "No disease tracking data found. Please add some disease records first.");
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Return error response if an exception occurs
            return ResponseEntity.badRequest().body(Map.of("error", "Error loading disease dashboard: " + e.getMessage()));
        }
    }

    // Get Disease Details
    @GetMapping("/details/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get details of a specific crop disease",
            description = "Retrieves detailed information about a crop disease by its ID for the authenticated user, with language-specific attributes."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved disease details",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Disease not found or access denied",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or server error",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized access - authentication required",
                    content = @Content
            )
    })
    public ResponseEntity<Map<String, Object>> getDiseaseDetails(
            @AuthenticationPrincipal UserPrinciple userPrinciples,
            @Parameter(description = "ID of the crop disease to retrieve", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Language for disease data (e.g., 'en' for English, 'hi' for Hindi)", example = "en")
            @RequestParam(value = "lang", defaultValue = "en") String lang) {

        // Fetch disease details for the authenticated user
        try {
            Long userId = userPrinciples.getUserId();
            Optional<CropDisease> diseaseOpt = cropDiseaseService.findByIdAndUserId(id, userId);

            // Check if disease exists and user has access
            if (diseaseOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Disease not found or access denied."));
            }

            // Build response with language-specific disease attributes
            CropDisease disease = diseaseOpt.get();
            Map<String, Object> response = new HashMap<>();
            populateDiseaseAttributes(response, disease, lang);
            response.put("imagePath", disease.getImagePath());
            response.put("lang", lang);
            response.put("userId", userId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Return error response if an exception occurs
            return ResponseEntity.badRequest().body(Map.of("error", "Error loading disease details: " + e.getMessage()));
        }
    }

    // Helper method to fetch paginated disease data based on filters
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

    // Helper method to determine if a disease is critical
    private boolean isCriticalDisease(CropDisease d) {
        String symptoms = (d.getSymptomsEn() != null ? d.getSymptomsEn() : "").toLowerCase();
        String cause = (d.getCauseEn() != null ? d.getCauseEn() : "").toLowerCase();
        return symptoms.contains("death") || symptoms.contains("severe") ||
                symptoms.contains("wilting") || cause.contains("virus") ||
                symptoms.contains("rot") || symptoms.contains("blight");
    }

    // Helper method to populate language-specific disease attributes
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