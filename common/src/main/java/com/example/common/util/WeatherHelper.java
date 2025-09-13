package com.example.common.util;

import com.example.common.Exception.AnyException;
import com.example.common.Service.WeatherService;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Component
public class WeatherHelper {

    private static final Logger logger = LoggerFactory.getLogger(WeatherHelper.class);

    @Autowired
    private WeatherService weatherService;

    // Default coordinates for Basti, Uttar Pradesh
    private static final double DEFAULT_LATITUDE = 26.8067;
    private static final double DEFAULT_LONGITUDE = 82.7567;
    private static final String DEFAULT_LOCATION = "Basti, Uttar Pradesh";

    /**
     * Get normalized coordinates (use defaults if null)
     */
    public double[] getNormalizedCoordinates(Double latitude, Double longitude) {
        double lat = latitude != null ? latitude : DEFAULT_LATITUDE;
        double lon = longitude != null ? longitude : DEFAULT_LONGITUDE;
        return new double[]{lat, lon};
    }

    /**
     * Get empty weather data template
     */
    public Map<String, Object> getEmptyWeatherData() {
        Map<String, Object> emptyData = new HashMap<>();
        emptyData.put("city", "");
        emptyData.put("country", "");
        emptyData.put("temperature", 0);
        emptyData.put("humidity", 0);
        emptyData.put("rainProbability", 0);
        emptyData.put("weatherMain", "Loading...");
        emptyData.put("windSpeed", 0);
        emptyData.put("cropAdvice", new ArrayList<>());
        return emptyData;
    }

    /**
     * Get weather data with proper error handling
     */
    public WeatherResult getWeatherData(Double latitude, Double longitude, String lang) {
        double[] coords = getNormalizedCoordinates(latitude, longitude);
        double lat = coords[0];
        double lon = coords[1];

        logger.info("Fetching weather for coordinates - Lat: {}, Lon: {}, Lang: {}", lat, lon, lang);

        try {
            Map<String, Object> weatherData = weatherService.getWeatherForecast(lat, lon, lang);
            String location = weatherData.get("city") + ", " + weatherData.get("country");

            return WeatherResult.success(weatherData, location);

        } catch (Exception e) {
            logger.error("Error fetching weather data: {}", e.getMessage());

            try {
                // Try to get default/cached data
                Map<String, Object> defaultData = weatherService.getDefaultWeatherData(lang);
                return WeatherResult.fallback(defaultData, DEFAULT_LOCATION, "Failed to fetch current weather data");

            } catch (Exception ex) {
                logger.error("Error getting default weather data: {}", ex.getMessage());
                // Return empty data as last resort
                Map<String, Object> emptyData = getEmptyWeatherData();
                return WeatherResult.error(emptyData, DEFAULT_LOCATION, "Service temporarily unavailable");
            }
        }
    }

    /**
     * Create API response for weather data
     */
    public ResponseEntity<Map<String, Object>> createWeatherApiResponse(Double latitude, Double longitude, String lang) {
        WeatherResult result = getWeatherData(latitude, longitude, lang);

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("weatherData", result.getWeatherData());
        response.put("location", result.getLocation());

        if (!result.isSuccess()) {
            response.put("error", result.getErrorMessage());
        }

        return result.isError() ?
                ResponseEntity.status(500).body(response) :
                ResponseEntity.ok(response);
    }
    public ResponseEntity<Map<String, Object>> createSingleCropApiResponse(
            Double latitude, Double longitude, String lang, String cropName) {
        WeatherResult result = getWeatherData(latitude, longitude, lang);

        Map<String, Object> response = new HashMap<>();
        response.put("location", result.getLocation());

        if (result.isSuccess()) {
            try {
                Map<String, String> cropAdvice = weatherService.generateSingleCropAdvice(
                        (double) result.getWeatherData().get("temperature"),
                        (int) result.getWeatherData().get("humidity"),
                        ((Number) result.getWeatherData().get("rainProbability")).doubleValue() / 100, // converting % to probability
                        (String) result.getWeatherData().get("weatherMain"),
                        lang,
                        cropName
                );
                response.put("success", true);
                response.put("cropAdvice", cropAdvice);
            } catch (AnyException e) {
                response.put("success", false);
                response.put("error", e.getMessage());
            }
        } else {
            response.put("success", false);
            response.put("error", result.getErrorMessage());
        }

        return result.isError() ?
                ResponseEntity.status(500).body(response) :
                ResponseEntity.ok(response);
    }
    /**
     * Weather result wrapper class
     */
    public static class WeatherResult {
        // Getters
        @Getter
        private final Map<String, Object> weatherData;
        @Getter
        private final String location;
        @Getter
        private final boolean success;
        @Getter
        private final String errorMessage;
        private final boolean isError;

        private WeatherResult(Map<String, Object> weatherData, String location,
                              boolean success, String errorMessage, boolean isError) {
            this.weatherData = weatherData;
            this.location = location;
            this.success = success;
            this.errorMessage = errorMessage;
            this.isError = isError;
        }

        public static WeatherResult success(Map<String, Object> weatherData, String location) {
            return new WeatherResult(weatherData, location, true, null, false);
        }

        public static WeatherResult fallback(Map<String, Object> weatherData, String location, String error) {
            return new WeatherResult(weatherData, location, false, error, false);
        }

        public static WeatherResult error(Map<String, Object> weatherData, String location, String error) {
            return new WeatherResult(weatherData, location, false, error, true);
        }

        public boolean isError() { return isError; }
    }
}