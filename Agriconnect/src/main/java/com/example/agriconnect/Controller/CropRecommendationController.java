package com.example.agriconnect.Controller;

import com.example.Authentication.Components.UserPrinciple;
import com.example.Authentication.repository.UserRepo;
import com.example.common.Model.CropRecommendation;
import com.example.common.Model.UserDetails1;
import com.example.agriconnect.Service.CropRecommendationService;
import com.example.common.Exception.AnyException;
import com.example.common.util.PexelsImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@Tag(name = "Crop Recommendation API", description = "Endpoints for generating and retrieving crop recommendations")
public class CropRecommendationController {

    private final CropRecommendationService cropRecommendationService;
    private final UserRepo userRepo;
    private final PexelsImageService pexelsImageService;

    public CropRecommendationController(CropRecommendationService cropRecommendationService,
                                        UserRepo userRepo,
                                        PexelsImageService pexelsImageService) {
        this.cropRecommendationService = cropRecommendationService;
        this.userRepo = userRepo;
        this.pexelsImageService = pexelsImageService;
    }

    @PostMapping("/recommend")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Generate crop recommendation",
            description = "Generates a crop recommendation based on provided crop details for the authenticated user, including an image URL for the recommended crop."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Crop recommendation generated successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid crop recommendation data",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized access - authentication required",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Server error processing recommendation",
                    content = @Content(schema = @Schema(implementation = Map.class))
            )
    })
    public ResponseEntity<?> getRecommendation(
            @Parameter(description = "Crop recommendation details", required = true)
            @RequestBody CropRecommendation cropRecommendation,
            @AuthenticationPrincipal UserPrinciple userPrinciples) {

        // Check if user is authenticated
        if (userPrinciples == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        Long userId = userPrinciples.getUserId();
        try {
            // Fetch user details from repository
            UserDetails1 userDetails1 = userRepo.findByUserId(userId);
            if (userDetails1 == null) {
                throw new AnyException(HttpStatus.NOT_FOUND.value(), "User not found");
            }

            // Associate user details with recommendation
            cropRecommendation.setUserDetails1(userDetails1);

            // Generate crop recommendation
            Map<String, Object> recommendationResult =
                    cropRecommendationService.GetRecommendation(cropRecommendation);

            // Fetch image URL for the recommended crop
            String cropName = recommendationResult.get("predicted_crop").toString();
            String imageUrl = pexelsImageService.fetchImageUrl(cropName);

            recommendationResult.put("image_url", imageUrl);

            // Return success response with recommendation and message
            return ResponseEntity.ok(Map.of(
                    "message", "Crop recommendation generated successfully!",
                    "recommendationResult", recommendationResult
            ));

        } catch (AnyException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing crop recommendation for userId {}: {}", userId, e.getMessage(), e);
            throw new AnyException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to process crop recommendation: " + e.getMessage());
        }
    }

    @GetMapping("/dashboard1")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get saved crop recommendations",
            description = "Retrieves saved crop recommendations for the authenticated user, optionally filtered by crop name, along with unique crops."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved saved recommendations",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized access - authentication required",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No recommendations found for the user",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Server error fetching recommendations",
                    content = @Content(schema = @Schema(implementation = Map.class))
            )
    })
    public ResponseEntity<?> getSavedRecommendation(
            @Parameter(description = "Crop name to filter recommendations (optional)", example = "Wheat")
            @RequestParam(required = false) String crop,
            @AuthenticationPrincipal UserPrinciple userPrinciples) {

        // Check if user is authenticated
        if (userPrinciples == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        Long userId = userPrinciples.getUserId();
        try {
            // Fetch all saved recommendations for the user
            List<CropRecommendation> recommendationList =
                    cropRecommendationService.GetSavedRecommendCropByUserId(userId);

            // Fetch unique crop names for the user
            Set<String> uniqueCrops = cropRecommendationService.getUniqueCrops(userId);

            // Filter recommendations by crop if provided
            List<CropRecommendation> displayedRecommendations =
                    (crop != null && !crop.isEmpty())
                            ? cropRecommendationService.getRecommendations(crop, userId)
                            : recommendationList;

            // Check if recommendations exist
            if (displayedRecommendations.isEmpty()) {
                throw new AnyException(HttpStatus.NOT_FOUND.value(),
                        "No crop recommendations found for this user");
            }

            // Return response with recommendations and unique crops
            return ResponseEntity.ok(Map.of(
                    "recommendations", displayedRecommendations,
                    "uniqueCrops", uniqueCrops
            ));

        } catch (AnyException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching recommendations for userId {}: {}", userId, e.getMessage(), e);
            throw new AnyException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to fetch crop recommendations: " + e.getMessage());
        }
    }
}