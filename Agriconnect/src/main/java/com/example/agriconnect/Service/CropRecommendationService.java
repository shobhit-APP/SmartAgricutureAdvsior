package com.example.agriconnect.Service;

import com.example.common.Exception.AnyException;
import com.example.common.Model.CropInfo;
import com.example.common.Model.CropRecommendation;
import com.example.agriconnect.Repository.CropRecommendationRepo;
import com.example.common.util.TranslateToHindi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service class for managing crop recommendations based on soil and environmental parameters.
 * Integrates with a Flask API to predict suitable crops, enhances predictions with bilingual
 * (English and Hindi) crop information, and manages storage and retrieval of recommendations
 * for users. Provides utility methods for accessing unique crops and determining water needs.
 */
@Slf4j
@Service
public class CropRecommendationService {

    @Autowired
    private TranslateToHindi translateToHindi;

    @Value("${flask.api.url:http://localhost:8080/recommend}")
    private String flaskApiUrl1;

    @Autowired
    private CropRecommendationRepo cropRecommendationRepo;

    /**
     * Generates a crop recommendation based on soil and environmental parameters by calling a Flask API.
     * Enhances the prediction with bilingual (English and Hindi) crop information, saves the recommendation
     * to the database, and includes water needs based on rainfall.
     *
     * @param cropRecommendation the {@link CropRecommendation} object containing soil (N, P, K, pH)
     *                           and environmental (temperature, humidity, rainfall) parameters
     * @return a {@link Map} containing the predicted crop, bilingual crop information, water needs,
     *         and other response data from the Flask API
     * @throws AnyException if the Flask API is unavailable (HTTP 503), the request is invalid (HTTP 400),
     *                      or an unexpected error occurs (HTTP 500)
     */
    public Map<String, Object> GetRecommendation(CropRecommendation cropRecommendation) {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("N", cropRecommendation.getN());
        requestMap.put("P", cropRecommendation.getP());
        requestMap.put("K", cropRecommendation.getK());
        requestMap.put("temperature", cropRecommendation.getTemperature());
        requestMap.put("humidity", cropRecommendation.getHumidity());
        requestMap.put("rainfall", cropRecommendation.getRainfall());
        requestMap.put("ph", cropRecommendation.getPh());

        log.info("Request Map: {}", requestMap);

        Map<String, Object> responses;
        try {
            responses = restTemplate.postForObject(flaskApiUrl1, requestMap, Map.class);

            if (responses != null && responses.containsKey("predicted_crop")) {
                String predictedCrop = (String) responses.get("predicted_crop");
                cropRecommendation.setPredictedCrop(predictedCrop);

                // Get CropInfo including Hindi name and both descriptions
                CropInfo cropInfo = translateToHindi.getCropInfo(predictedCrop);

                cropRecommendation.setPredictedCropHindi(cropInfo.getHindiName());
                cropRecommendation.setHindiDescription(cropInfo.getHindiDescription());
                cropRecommendation.setEnglishDescription(cropInfo.getEnglishDescription());

                // Add extra info to response map
                responses.put("hindi_name", cropInfo.getHindiName());
                responses.put("hindi_description", cropInfo.getHindiDescription());
                responses.put("english_description", cropInfo.getEnglishDescription());
                responses.put("temperature", requestMap.get("temperature"));

                String waterNeeds = WaterNeeds(cropRecommendation.getRainfall());
                responses.put("water_needs", waterNeeds);

                cropRecommendationRepo.save(cropRecommendation);

                log.info("Predicted Crop: {}", predictedCrop);
                log.info("Hindi Name: {}", cropInfo.getHindiName());
                log.info("Hindi Description: {}", cropInfo.getHindiDescription());
                log.info("English Description: {}", cropInfo.getEnglishDescription());
            }
        } catch (ResourceAccessException e) {
            log.error("Error accessing Flask URL: {}", flaskApiUrl1, e);
            throw new AnyException(HttpStatus.SERVICE_UNAVAILABLE.value(), "Failed to access Flask service.");
        } catch (HttpClientErrorException e) {
            log.error("Client error from Flask API: {}", e.getStatusCode(), e);
            throw new AnyException(HttpStatus.BAD_REQUEST.value(), "Invalid request to Flask service.");
        } catch (Exception e) {
            log.error("Unexpected error occurred: ", e);
            throw new AnyException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unexpected error during crop recommendation.");
        }
        return responses;
    }

    /**
     * Retrieves all saved crop recommendations for a specific user.
     *
     * @param userId the ID of the user whose crop recommendations are to be retrieved
     * @return a {@link List} of {@link CropRecommendation} objects associated with the user
     */
    public List<CropRecommendation> GetSavedRecommendCropByUserId(Long userId) {
        return cropRecommendationRepo.findByUserDetails1UserId(userId);
    }

    /**
     * Retrieves a set of unique crop names recommended for a specific user.
     *
     * @param userId the ID of the user whose unique recommended crops are to be retrieved
     * @return a {@link Set} of distinct crop names
     */
    public Set<String> getUniqueCrops(Long userId) {
        return cropRecommendationRepo.findDistinctPredictedCropsByUserId(userId);
    }

    /**
     * Retrieves crop recommendations for a specific user and crop.
     *
     * @param crop   the name of the crop to filter recommendations by
     * @param userId the ID of the user whose recommendations are to be retrieved
     * @return a {@link List} of {@link CropRecommendation} objects matching the crop and user
     */
    public List<CropRecommendation> getRecommendations(String crop, Long userId) {
        return cropRecommendationRepo.findByUserIdAndPredictedCrop(userId, crop);
    }

    /**
     * Determines the water needs for a crop based on rainfall.
     *
     * @param rainfall the rainfall value in millimeters
     * @return a string indicating water needs ("High" for <200 mm, "Medium" for 200-800 mm, "Low" for >800 mm)
     */
    private String WaterNeeds(float rainfall) {
        if (rainfall < 200) {
            return "High";
        } else if (rainfall >= 200 && rainfall <= 800) {
            return "Medium";
        } else {
            return "Low";
        }
    }
}