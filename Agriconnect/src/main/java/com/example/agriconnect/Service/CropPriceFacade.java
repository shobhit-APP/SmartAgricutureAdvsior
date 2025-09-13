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

@Slf4j
@Service
public class CropPriceFacade {

    private final MarketServices services;
    private final UserRepo userRepo;
    private final LocationHelper locationHelper;

    public CropPriceFacade(MarketServices services, UserRepo userRepo, LocationHelper locationHelper) {
        this.services = services;
        this.userRepo = userRepo;
        this.locationHelper = locationHelper;
    }

    /** ✅ Dashboard API */
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

    /** ✅ Prediction Page API */
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
            log.error("❌ Error in pricePredictionModel: {}", e.getMessage(), e);
            throw new AnyException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to load prediction page: " + e.getMessage());
        }
    }

    /** ✅ Prediction Result API */
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
