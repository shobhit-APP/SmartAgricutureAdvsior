
package com.example.agriconnect.Service;

import com.example.common.Exception.AnyException;
import com.example.common.Model.Crop;
import com.example.agriconnect.Repository.cropPriceRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Service
public class MarketServices {
    private static final Logger log = LoggerFactory.getLogger(MarketServices.class);

    @Value("${flask.api.url2:http://localhost:8081/predict}")
    private String flaskApiUrl2;

    @Autowired
    private cropPriceRepo repository;

    public Map<String, Object> getPrediction(Crop crop) {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("state", crop.getState());
        requestMap.put("district", crop.getDistrict());
        requestMap.put("market", crop.getMarket());
        requestMap.put("crop_name", crop.getCropName());
        requestMap.put("arrival_date", crop.getArrivalDate() != null ? crop.getArrivalDate().toString() : null);
        requestMap.put("min_price", crop.getMinPrice());
        requestMap.put("max_price", crop.getMaxPrice());

        Map<String, Object> response;
        try {
            response = restTemplate.postForObject(flaskApiUrl2, requestMap, Map.class);
            if (response != null) {
                if (response.containsKey("predicted_price_ensemble")) {
                    crop.setSuggestedPriceSecond((Double) response.get("predicted_price_ensemble"));
                }
                if (response.containsKey("predicted_price_nn")) {
                    crop.setSuggestedPriceThird((Double) response.get("predicted_price_nn"));
                }
                if (response.containsKey("predicted_price_xgb")) {
                    crop.setSuggestedPrice((Double) response.get("predicted_price_xgb"));
                }

                // Add values back to response
                response.put("suggested_price_first", crop.getSuggestedPriceSecond());
                response.put("suggested_price_second", crop.getSuggestedPriceThird());
                response.put("suggested_price_third", crop.getSuggestedPrice());

                // Calculate best
                double best = Math.max(crop.getSuggestedPrice(),
                        Math.max(crop.getSuggestedPriceSecond(), crop.getSuggestedPriceThird()));
                response.put("BEST", best);

                // Logs
                log.info("State: {}", crop.getState());
                log.info("District: {}", crop.getDistrict());
                log.info("Market: {}", crop.getMarket());
                log.info("Crop Name: {}", crop.getCropName());
                log.info("Arrival Date: {}", crop.getArrivalDate());
                log.info("Min Price: {}", crop.getMinPrice());
                log.info("Max Price: {}", crop.getMaxPrice());
                log.info("Suggested Price: {}", crop.getSuggestedPrice());

                crop.setBestPrice(best);
                repository.save(crop);
            }
        } catch (ResourceAccessException e) {
            log.error("Error accessing Flask URL: {}", flaskApiUrl2, e);
            throw new AnyException(HttpStatus.SERVICE_UNAVAILABLE.value(), "Failed to access Flask service.");
        } catch (HttpClientErrorException e) {
            log.error("Client error from Flask API: {}", e.getStatusCode(), e);
            throw new AnyException(HttpStatus.BAD_REQUEST.value(), "Invalid request to Flask service.");
        } catch (Exception e) {
            log.error("Unexpected error occurred: ", e);
            throw new AnyException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unexpected error during market price prediction.");
        }
        return response;
    }

    public List<Crop> GetAllMarketDetailsById(Long userId) {
        return repository.findByUserDetails1UserId(userId);
    }

    public List<Crop> findByStateAndUserId(String state, Long userId) {
        return repository.findByStateAndUserDetails1UserId(state, userId);
    }

    public Set<String> getUniqueState(Long userId) {
        return repository.findDistinctStatesByUserId(userId);
    }
}
