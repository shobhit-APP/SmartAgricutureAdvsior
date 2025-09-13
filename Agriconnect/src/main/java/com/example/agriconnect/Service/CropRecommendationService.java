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
@Slf4j
@Service
public class CropRecommendationService {
    @Autowired
    private TranslateToHindi translateToHindi;

    @Value("${flask.api.url:http://localhost:8080/recommend}")
    private String flaskApiUrl1;

    @Autowired
    private CropRecommendationRepo cropRecommendationRepo;

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

    public List<CropRecommendation> GetSavedRecommendCropByUserId(Long userId) {
        return cropRecommendationRepo.findByUserDetails1UserId(userId);
    }

    public Set<String> getUniqueCrops(Long userId) {
        return cropRecommendationRepo.findDistinctPredictedCropsByUserId(userId);
    }

    public List<CropRecommendation> getRecommendations(String crop, Long userId) {
        return cropRecommendationRepo.findByUserIdAndPredictedCrop(userId, crop);
    }

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
