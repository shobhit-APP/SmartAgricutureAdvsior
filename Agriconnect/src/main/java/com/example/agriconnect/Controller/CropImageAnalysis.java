package com.example.agriconnect.Controller;
import com.example.Authentication.Components.UserPrinciple;
import com.example.agriconnect.Service.CropImageAnalysisService;
import com.example.common.Exception.AnyException;
import com.example.common.util.GeminiValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
public class CropImageAnalysis {

    @Autowired
    private CropImageAnalysisService cropImageAnalysisService;

    @Autowired
    private GeminiValidationService geminiValidationService;

    @PostMapping("/analyzeImage")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> analyzeImage(@RequestParam("file") MultipartFile file,
                                          @AuthenticationPrincipal UserPrinciple userPrinciples) {
        // Check if file is empty
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(createErrorResponse("File upload is empty. Please provide an image."));
        }

        try {
            // Validate crop image using GeminiValidationService
            boolean isValid = geminiValidationService.isValidCropImage(file);
            if (!isValid) {
                return ResponseEntity.badRequest().body(createErrorResponse("Invalid crop image. Please upload a valid crop image."));
            }

            // Analyze image using service (returns Map<String, Object>)
            Map<String, Object> result = cropImageAnalysisService.analyzeImage(file, userPrinciples.getUserId());

            // Check for errors in the service response
            if (Boolean.FALSE.equals(result.get("success"))) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse(result.getOrDefault("error", "Image analysis failed.").toString()));
            }

            // Return successful response
            return ResponseEntity.ok(result);

        } catch (AnyException e) {
            log.error("Custom exception during image analysis: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(createErrorResponse(e.getMessage()));
        } catch (IOException e) {
            log.error("IO Exception during image analysis", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("File processing error. Please check the file and try again."));
        } catch (Exception e) {
            log.error("Unexpected exception during image analysis", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Unexpected error occurred during image analysis."));
        }
    }

    @GetMapping("/analysisStatus/{filename}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> getAnalysisStatus(@PathVariable String filename) {
        try {
            String status = cropImageAnalysisService.getAnalysisStatus(filename);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Error retrieving analysis status for filename {}: {}", filename, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("error.image_analysis_status_failed");
        }
    }

    @DeleteMapping("/cleanupOldFiles/{days}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> cleanupOldFiles(@PathVariable int days) {
        try {
            cropImageAnalysisService.cleanupOldFiles(days);
            return ResponseEntity.ok(createSuccessResponse("Old files cleanup executed."));
        } catch (Exception e) {
            log.error("Error during cleanup of old files: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to clean up old files."));
        }
    }

    // Helper method to create standardized error response
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", message);
        return errorResponse;
    }

    // Helper method to create standardized success response
    private Map<String, Object> createSuccessResponse(String message) {
        Map<String, Object> successResponse = new HashMap<>();
        successResponse.put("success", true);
        successResponse.put("message", message);
        return successResponse;
    }
}