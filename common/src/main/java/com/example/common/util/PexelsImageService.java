package com.example.common.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PexelsImageService {

    private static final String PEXELS_BASE_URL = "https://api.pexels.com/v1/search";

    private static final Logger logger = LoggerFactory.getLogger(PexelsImageService.class);

    @Value("${pexels.api.key}")
    private String apiKey;

    public String fetchImageUrl(String cropName) {
        if (apiKey == null || apiKey.isEmpty()) {
            logger.error("Pexels API key is not configured.");
            return null;
        }

        try {
            String url = PEXELS_BASE_URL + "?query=" +
                    URLEncoder.encode(cropName, StandardCharsets.UTF_8) +
                    "&per_page=1";

            RestTemplate restTemplate = new RestTemplate();

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", apiKey);

            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

            org.springframework.http.ResponseEntity<Map> response = restTemplate.exchange(url,
                    org.springframework.http.HttpMethod.GET, entity, Map.class);

            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("photos")) {
                Object photosObj = body.get("photos");

                if (photosObj instanceof java.util.List<?> photos) {

                    if (!photos.isEmpty() && photos.get(0) instanceof Map<?, ?> firstPhoto) {

                        Object srcObj = firstPhoto.get("src");
                        if (srcObj instanceof Map<?, ?> src) {
                            Object mediumUrl = src.get("medium");
                            return mediumUrl != null ? mediumUrl.toString() : null;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching image URL from Pexels API for crop: {}", cropName, e);
        }
        return null; // No image found
    }
}
