package com.example.agriconnect.Controller;

import com.example.common.util.WeatherHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@Tag(name = "Weather API", description = "Endpoints for retrieving weather data and crop-specific weather advice")
public class WeatherController {

    private final WeatherHelper weatherHelper;

    @Autowired
    public WeatherController(WeatherHelper weatherHelper) {
        this.weatherHelper = weatherHelper;
    }

    @GetMapping("/weather")
    @Operation(
            summary = "Get weather data",
            description = "Retrieves weather data for the specified latitude and longitude coordinates, with support for language-specific responses."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Weather data retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid coordinates or request parameters",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Server error retrieving weather data",
                    content = @Content(schema = @Schema(implementation = Map.class))
            )
    })
    public ResponseEntity<?> getWeatherData(
            @Parameter(description = "Latitude coordinate for weather data (optional)", example = "19.0760")
            @RequestParam(value = "lat", required = false) Double latitude,
            @Parameter(description = "Longitude coordinate for weather data (optional)", example = "72.8777")
            @RequestParam(value = "lon", required = false) Double longitude,
            @Parameter(description = "Language for the response (e.g., 'en' for English, 'hi' for Hindi)", example = "en")
            @RequestParam(value = "lang", defaultValue = "en") String lang) {
        log.info("Weather API call: lat={}, lon={}, lang={}", latitude, longitude, lang);
        try {
            // Fetch weather data using WeatherHelper
            return weatherHelper.createWeatherApiResponse(latitude, longitude, lang);
        } catch (Exception e) {
            log.error("Weather API call failed: {}", e.getMessage(), e);
            // Return error response if weather data retrieval fails
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving weather data: " + e.getMessage()));
        }
    }

    @GetMapping("/weather/advice")
    @Operation(
            summary = "Get crop-specific weather advice",
            description = "Retrieves weather-based advice for a specific crop, using latitude and longitude coordinates and language-specific responses."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Crop-specific weather advice retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid coordinates, crop name, or request parameters",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Server error retrieving crop advice",
                    content = @Content(schema = @Schema(implementation = Map.class))
            )
    })
    public ResponseEntity<Map<String, Object>> getSingleCropAdvice(
            @Parameter(description = "Latitude coordinate for weather data (optional)", example = "19.0760")
            @RequestParam(value = "lat", required = false) Double latitude,
            @Parameter(description = "Longitude coordinate for weather data (optional)", example = "72.8777")
            @RequestParam(value = "lon", required = false) Double longitude,
            @Parameter(description = "Language for the response (e.g., 'en' for English, 'hi' for Hindi)", example = "en")
            @RequestParam(value = "lang", defaultValue = "en") String lang,
            @Parameter(description = "Name of the crop for which advice is requested", required = true, example = "Wheat")
            @RequestParam(value = "crop") String cropName) {

        log.info("API call for single crop advice - Crop: {}, Lat: {}, Lon: {}, Lang: {}", cropName, latitude, longitude, lang);
        // Fetch crop-specific weather advice using WeatherHelper
        return weatherHelper.createSingleCropApiResponse(latitude, longitude, lang, cropName);
    }
}