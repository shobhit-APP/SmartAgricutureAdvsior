package com.example.agriconnect.Controller;

import com.example.Authentication.Components.UserPrinciple;
import com.example.agriconnect.Service.CropPriceFacade;
import com.example.agriconnect.Service.MarketServices;
import com.example.common.Model.Crop;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api")
@Tag(name = "Crop Price API", description = "Endpoints for retrieving crop price data, predictions, and CSRF token")
public class CropPriceController {
    @Autowired
    private MarketServices services;

    @Autowired
    private HttpServletRequest request;

    private final CropPriceFacade cropPriceFacade;

    public CropPriceController(CropPriceFacade cropPriceFacade) {
        this.cropPriceFacade = cropPriceFacade;
    }

    @GetMapping("/csrf_token")
    @Operation(
            summary = "Get CSRF token",
            description = "Retrieves the CSRF token for use in subsequent requests requiring CSRF protection."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "CSRF token retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CsrfToken.class))
            )
    })
    public CsrfToken getCsrfToken(HttpServletRequest request) {
        // Retrieve CSRF token from the request attribute
        return (CsrfToken) request.getAttribute("_csrf");
    }

    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get crop price dashboard",
            description = "Retrieves market details for crop prices for the authenticated user, optionally filtered by state."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved market dashboard data",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized access - authentication required",
                    content = @Content(schema = @Schema(implementation = Map.class))
            )
    })
    public ResponseEntity<?> getAllMarketDetails(
            @AuthenticationPrincipal UserPrinciple userPrinciples,
            @Parameter(description = "State to filter market data (optional)", example = "Maharashtra")
            @RequestParam(required = false) String state) {
        // Check if user is authenticated
        if (userPrinciples == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        // Fetch market dashboard data via facade
        return cropPriceFacade.getDashboard(userPrinciples, state);
    }

    @GetMapping("/predict")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get crop price prediction page",
            description = "Retrieves crop price prediction data based on optional latitude and longitude coordinates."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved prediction page data",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized access - authentication required",
                    content = @Content
            )
    })
    public ResponseEntity<?> pricePredictionModel(
            @Parameter(description = "Latitude coordinate for prediction (optional)", example = "19.0760")
            @RequestParam(value = "lat", required = false) Double latitude,
            @Parameter(description = "Longitude coordinate for prediction (optional)", example = "72.8777")
            @RequestParam(value = "lon", required = false) Double longitude) {
        // Fetch prediction page data via facade
        return cropPriceFacade.getPredictionPage(latitude, longitude);
    }

    @PostMapping("/predict")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Predict crop price",
            description = "Submits crop details to predict its price for the authenticated user."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Crop price predicted successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid crop data",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized access - authentication required",
                    content = @Content
            )
    })
    public ResponseEntity<?> predict(
            @AuthenticationPrincipal UserPrinciple userPrinciples,
            @Parameter(description = "Crop details for price prediction", required = true)
            @Valid @RequestBody Crop crop) {
        // Check if user is authenticated
        if (userPrinciples == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }
        // Predict crop price via facade
        return cropPriceFacade.predictCrop(userPrinciples, crop);
    }
}