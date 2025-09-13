package com.example.agriconnect.Controller;

import com.example.Authentication.Components.UserPrinciple;
import com.example.Authentication.repository.UserRepo;
import com.example.common.Model.CropRecommendation;
import com.example.common.Model.UserDetails1;
import com.example.agriconnect.Service.CropRecommendationService;
import com.example.common.Exception.AnyException;
import com.example.common.util.PexelsImageService;
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
@RequestMapping("/api")
public class CropRecommendationController {

    private final CropRecommendationService cropRecommendationService;
    private  final UserRepo userRepo;
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
    public ResponseEntity<?> getRecommendation(
            @RequestBody CropRecommendation cropRecommendation,
            @AuthenticationPrincipal UserPrinciple userPrinciples) {

        if (userPrinciples == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        Long userId = userPrinciples.getUserId();
        try {
            UserDetails1 userDetails1 = userRepo.findByUserId(userId);
            if (userDetails1 == null) {
                throw new AnyException(HttpStatus.NOT_FOUND.value(), "User not found");
            }

            cropRecommendation.setUserDetails1(userDetails1);

            Map<String, Object> recommendationResult =
                    cropRecommendationService.GetRecommendation(cropRecommendation);

            String cropName = recommendationResult.get("predicted_crop").toString();
            String imageUrl = pexelsImageService.fetchImageUrl(cropName);

            recommendationResult.put("image_url", imageUrl);

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
    public ResponseEntity<?> getSavedRecommendation(
            @RequestParam(required = false) String crop,
            @AuthenticationPrincipal UserPrinciple userPrinciples) {

        if (userPrinciples == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        Long userId = userPrinciples.getUserId();
        try {
            List<CropRecommendation> recommendationList =
                    cropRecommendationService.GetSavedRecommendCropByUserId(userId);

            Set<String> uniqueCrops = cropRecommendationService.getUniqueCrops(userId);

            List<CropRecommendation> displayedRecommendations =
                    (crop != null && !crop.isEmpty())
                            ? cropRecommendationService.getRecommendations(crop, userId)
                            : recommendationList;

            if (displayedRecommendations.isEmpty()) {
                throw new AnyException(HttpStatus.NOT_FOUND.value(),
                        "No crop recommendations found for this user");
            }

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
