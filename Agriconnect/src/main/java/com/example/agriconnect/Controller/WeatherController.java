package com.example.agriconnect.Controller;

import com.example.common.util.WeatherHelper;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
@Slf4j
@RestController
@RequestMapping("/api")
public class WeatherController {
    private WeatherHelper weatherHelper;
    @GetMapping("/weather")
    @Operation(summary = "Get weather data", description = "Retrieve weather data for given coordinates")
    public ResponseEntity<?> getWeatherData(
            @RequestParam(value = "lat", required = false) Double latitude,
            @RequestParam(value = "lon", required = false) Double longitude,
            @RequestParam(value = "lang", defaultValue = "en") String lang) {
        log.info("Weather API call: lat={}, lon={}, lang={}", latitude, longitude, lang);
        try {
            return weatherHelper.createWeatherApiResponse(latitude, longitude, lang);
        } catch (Exception e) {
            log.error("Weather API call failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving weather data: " + e.getMessage()));
        }
    }
    @GetMapping("/weather/advice")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSingleCropAdvice(
            @RequestParam(value = "lat", required = false) Double latitude,
            @RequestParam(value = "lon", required = false) Double longitude,
            @RequestParam(value = "lang", defaultValue = "en") String lang,
            @RequestParam(value = "crop") String cropName) {

        log.info("API call for single crop advice - Crop: {}, Lat: {}, Lon: {}, Lang: {}", cropName, latitude, longitude, lang);
        return weatherHelper.createSingleCropApiResponse(latitude, longitude, lang, cropName);
    }
}
