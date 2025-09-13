package com.example.agriconnect.Service;

import com.example.Authentication.Components.UserPrinciple;
import com.example.Authentication.repository.UserRepo;
import com.example.common.Exception.AnyException;
import com.example.common.Model.Crop;
import com.example.common.Model.LocationMapping;
import com.example.common.Model.UserDetails1;
import com.example.common.util.LocationHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Facade service for managing crop price-related operations, coordinating interactions with market services,
 * user data, and location helpers. Provides endpoints for retrieving market dashboard data, setting up
 * the crop price prediction page, and generating crop price predictions for authenticated users.
 */
@Slf4j
@Service
public class CropPriceFacade {

    private final MarketServices services;
    private final UserRepo userRepo;
    private final LocationHelper locationHelper;

    /**
     * Constructs a new {@code CropPriceFacade} with the specified dependencies.
     *
     * @param services        the {@link MarketServices} for retrieving market data and predictions
     * @param userRepo        the {@link UserRepo} for accessing user data
     * @param locationHelper  the {@link LocationHelper} for resolving location-based information
     */
    public CropPriceFacade(MarketServices services, UserRepo userRepo, LocationHelper locationHelper) {
        this.services = services;
        this.userRepo = userRepo;
        this.locationHelper = locationHelper;
    }

    /**
     * Retrieves market dashboard data for an authenticated user, optionally filtered by state.
     * Returns a map containing the page title, unique states, and market data associated with the user.
     *
     * @param userPrinciples the authenticated user's details
     * @param state         the state to filter market data by (optional)
     * @return a {@link ResponseEntity} containing a map with the page title, unique states, and market data
     * @throws AnyException if no market data is available (HTTP 404)
     */
    public ResponseEntity<?> getDashboard(UserPrinciple userPrinciples, String state) {
        Long userId = userPrinciples.getUserId();
        List<Crop> allData = services.GetAllMarketDetailsById(userId);
        Set<String> uniqueStates = services.getUniqueState(userId);

        List<Crop> filteredData = (state != null && !state.isEmpty())
                ? services.findByStateAndUserId(state, userId)
                : allData;

        if (allData.isEmpty() && filteredData.isEmpty()) {
            throw new AnyException(HttpStatus.NOT_FOUND.value(), "No market data available");
        }

        return ResponseEntity.ok(Map.of(
                "pageTitle", "Farmer's Market Dashboard",
                "state", uniqueStates,
                "allMarketData", allData
        ));
    }

    /**
     * Prepares data for the crop price prediction page, including location information based on provided
     * latitude and longitude, and lists of states, districts, and markets.
     *
     * @param latitude  the latitude for location resolution
     * @param longitude the longitude for location resolution
     * @return a {@link ResponseEntity} containing a map with the page title, default location, and lists of states, districts, and markets
     * @throws AnyException if location resolution fails (HTTP 500)
     */
    public ResponseEntity<?> getPredictionPage(Double latitude, Double longitude) {
        try {
            LocationMapping defaultLocation = locationHelper.resolveLocation(latitude, longitude);

            return ResponseEntity.ok(Map.of(
                    "pageTitle", "Crop Price Prediction",
                    "defaultLocation", defaultLocation,
                    "states", locationHelper.GetStates(),
                    "districts", locationHelper.GetDistricts(),
                    "markets", locationHelper.GetMarkets()
            ));

        } catch (Exception e) {
            log.error("Error in pricePredictionModel: {}", e.getMessage(), e);
            throw new AnyException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to load prediction page: " + e.getMessage());
        }
    }

    /**
     * Predicts the price of a crop for an authenticated user and returns the prediction result.
     * Associates the crop with the user and delegates the prediction to the market services.
     *
     * @param userPrinciples the authenticated user's details
     * @param crop          the {@link Crop} object containing details for the prediction
     * @return a {@link ResponseEntity} containing a map with a success message and the prediction result
     * @throws AnyException if the user is not found (HTTP 404) or prediction fails (HTTP 500)
     */
    public ResponseEntity<?> predictCrop(UserPrinciple userPrinciples, Crop crop) {
        Long userId = userPrinciples.getUserId();
        try {
            UserDetails1 userDetails1 = userRepo.findByUserId(userId);
            if (userDetails1 == null) {
                throw new AnyException(HttpStatus.NOT_FOUND.value(), "User not found");
            }
            crop.setUserDetails1(userDetails1);
        } catch (AnyException e) {
            throw e; // rethrow if already our custom exception
        } catch (Exception e) {
            log.error("Error fetching user details for userId {}: {}", userId, e.getMessage());
            throw new AnyException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to fetch user details: " + e.getMessage());
        }

        try {
            Map<String, Object> predictionResult = services.getPrediction(crop);
            return ResponseEntity.ok(Map.of(
                    "message", "Crop price predicted successfully!",
                    "predictionResult", predictionResult
            ));
        } catch (Exception e) {
            log.error("Error while predicting crop price: {}", e.getMessage(), e);
            throw new AnyException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to predict crop price: " + e.getMessage());
        }
    }
}