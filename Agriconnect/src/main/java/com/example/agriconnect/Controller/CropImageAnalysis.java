package com.example.agriconnect.Controller;

import com.example.Authentication.Components.UserPrinciple;
import com.example.agriconnect.Service.CropImageAnalysisService;
import com.example.common.Exception.AnyException;
import com.example.common.util.GeminiValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Crop Image Analysis API", description = "Endpoints for analyzing crop images, checking analysis status, and cleaning up old files")
public class CropImageAnalysis {

    @Autowired
    private CropImageAnalysisService cropImageAnalysisService;

    @Autowired
    private GeminiValidationService geminiValidationService;

    @PostMapping("/analyzeImage")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Analyze a crop image",
            description = "Uploads and analyzes a crop image to detect diseases or issues, returning analysis results for the authenticated user."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Image analyzed successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid or empty image file, or analysis failed",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized access - authentication required",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Server error during image processing or analysis",
                    content = @Content(schema = @Schema(implementation = Map.class))
            )
    })
    public ResponseEntity<?> analyzeImage(
            @Parameter(description = "Image file to analyze (e.g., JPEG, PNG)", required = true)
            @RequestParam("file") MultipartFile file,
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
    @Operation(
            summary = "Get analysis status",
            description = "Retrieves the status of a previous image analysis by filename for the authenticated user."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Analysis status retrieved successfully",
                    content = @Content(schema = @Schema(type = "string"))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized access - authentication required",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Server error retrieving analysis status",
                    content = @Content(schema = @Schema(type = "string"))
            )
    })
    public ResponseEntity<String> getAnalysisStatus(
            @Parameter(description = "Filename of the analyzed image", required = true, example = "crop_image.jpg")
            @PathVariable String filename) {
        try {
            // Retrieve analysis status for the given filename
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
    @Operation(
            summary = "Clean up old analysis files",
            description = "Deletes image analysis files older than the specified number of days. Restricted to admin users."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Old files cleaned up successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized access - authentication required",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - admin role required",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Server error during file cleanup",
                    content = @Content(schema = @Schema(implementation = Map.class))
            )
    })
    public ResponseEntity<?> cleanupOldFiles(
            @Parameter(description = "Number of days to determine age of files to delete", required = true, example = "30")
            @PathVariable int days) {
        try {
            // Execute cleanup of old files
            cropImageAnalysisService.cleanupOldFiles(days);
            return ResponseEntity.ok(createSuccessResponse());
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
    private Map<String, Object> createSuccessResponse() {
        Map<String, Object> successResponse = new HashMap<>();
        successResponse.put("success", true);
        successResponse.put("message", "Old files cleanup executed.");
        return successResponse;
    }
}