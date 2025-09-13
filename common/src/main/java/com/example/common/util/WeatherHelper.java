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

/**
 * Utility class for handling weather-related operations, including fetching weather data and generating crop advice.
 * Provides methods to normalize coordinates, create API responses, and handle fallback scenarios for weather data retrieval.
 */
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
     * Normalizes the provided latitude and longitude coordinates, using default values if null.
     *
     * @param latitude  The latitude coordinate, or null to use the default (Basti, Uttar Pradesh).
     * @param longitude The longitude coordinate, or null to use the default (Basti, Uttar Pradesh).
     * @return An array containing the normalized latitude and longitude coordinates.
     */
    public double[] getNormalizedCoordinates(Double latitude, Double longitude) {
        double lat = latitude != null ? latitude : DEFAULT_LATITUDE;
        double lon = longitude != null ? longitude : DEFAULT_LONGITUDE;
        return new double[]{lat, lon};
    }

    /**
     * Creates an empty weather data template with default values.
     *
     * @return A {@link Map} containing default weather data fields (e.g., city, temperature, humidity).
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
     * Fetches weather data for the specified coordinates and language, with fallback handling.
     * Attempts to retrieve live weather data, falling back to default/cached data or an empty template if errors occur.
     *
     * @param latitude  The latitude coordinate, or null to use the default (Basti, Uttar Pradesh).
     * @param longitude The longitude coordinate, or null to use the default (Basti, Uttar Pradesh).
     * @param lang      The language code for localized weather data.
     * @return A {@link WeatherResult} containing the weather data, location, success status, and optional error message.
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
     * Creates an API response for weather data based on the provided coordinates and language.
     *
     * @param latitude  The latitude coordinate, or null to use the default (Basti, Uttar Pradesh).
     * @param longitude The longitude coordinate, or null to use the default (Basti, Uttar Pradesh).
     * @param lang      The language code for localized weather data.
     * @return A {@link ResponseEntity} containing the weather data, location, and success status.
     *         Returns HTTP 200 for success or fallback, and HTTP 500 for errors.
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

    /**
     * Creates an API response with crop-specific advice based on weather data.
     *
     * @param latitude  The latitude coordinate, or null to use the default (Basti, Uttar Pradesh).
     * @param longitude The longitude coordinate, or null to use the default (Basti, Uttar Pradesh).
     * @param lang      The language code for localized advice.
     * @param cropName  The name of the crop for which to generate advice.
     * @return A {@link ResponseEntity} containing the crop advice, location, and success status.
     *         Returns HTTP 200 for success or fallback, and HTTP 500 for errors.
     * @throws AnyException If an error occurs while generating crop advice.
     */
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
     * Inner class to encapsulate weather data results, including success status and error information.
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

        /**
         * Constructs a WeatherResult instance.
         *
         * @param weatherData   The weather data map.
         * @param location      The location string (e.g., "Basti, Uttar Pradesh").
         * @param success       Indicates if the operation was successful.
         * @param errorMessage  The error message, if any.
         * @param isError       Indicates if the result represents an error state.
         */
        private WeatherResult(Map<String, Object> weatherData, String location,
                              boolean success, String errorMessage, boolean isError) {
            this.weatherData = weatherData;
            this.location = location;
            this.success = success;
            this.errorMessage = errorMessage;
            this.isError = isError;
        }

        /**
         * Creates a successful WeatherResult instance.
         *
         * @param weatherData The weather data map.
         * @param location    The location string.
         * @return A {@link WeatherResult} representing a successful operation.
         */
        public static WeatherResult success(Map<String, Object> weatherData, String location) {
            return new WeatherResult(weatherData, location, true, null, false);
        }

        /**
         * Creates a fallback WeatherResult instance for when live data is unavailable.
         *
         * @param weatherData  The fallback weather data map.
         * @param location     The location string.
         * @param error        The error message explaining the fallback.
         * @return A {@link WeatherResult} representing a fallback operation.
         */
        public static WeatherResult fallback(Map<String, Object> weatherData, String location, String error) {
            return new WeatherResult(weatherData, location, false, error, false);
        }

        /**
         * Creates an error WeatherResult instance for when the service is unavailable.
         *
         * @param weatherData  The empty or default weather data map.
         * @param location     The location string.
         * @param error        The error message explaining the failure.
         * @return A {@link WeatherResult} representing an error state.
         */
        public static WeatherResult error(Map<String, Object> weatherData, String location, String error) {
            return new WeatherResult(weatherData, location, false, error, true);
        }

        /**
         * Checks if the result represents an error state.
         *
         * @return {@code true} if the result is an error, {@code false} otherwise.
         */
        public boolean isError() { return isError; }
    }
}