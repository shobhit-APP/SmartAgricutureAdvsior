package com.example.common.util;

import com.example.common.Exception.AnyException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class GeminiApiHelper {

    private static final Logger logger = LoggerFactory.getLogger(GeminiApiHelper.class);

    @Value("${api.key}")
    private String geminiApiKey;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiApiHelper(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Calls the Gemini API with a text prompt and optional generation configuration.
     * @param prompt The text prompt to send to the Gemini API.
     * @param generationConfig Optional configuration for response format (e.g., response_mime_type).
     * @return The text response from the Gemini API.
     * @throws AnyException If the API call fails or the response is invalid.
     */
    public String callGeminiApi(String prompt, Map<String, Object> generationConfig) throws AnyException {
        try {
            // Build request body
            Map<String, Object> contents = Map.of("parts", List.of(Map.of("text", prompt)));
            Map<String, Object> requestBodyMap = Map.of(
                    "contents", List.of(contents),
                    "generationConfig", generationConfig != null ? generationConfig : Map.of()
            );

            String requestBody = objectMapper.writeValueAsString(requestBodyMap);
            logger.debug("Gemini API Request: {}", requestBody);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            // Make API call

            String apiUrl = GEMINI_API_URL + "?key=" + geminiApiKey;
            String responseBody = restTemplate.postForObject(apiUrl, entity, String.class);

            if (responseBody == null) {
                throw new AnyException(204, "Gemini API returned empty response");
            }

            // Parse response
            JsonNode geminiResponse = objectMapper.readTree(responseBody);
            JsonNode candidates = geminiResponse.path("candidates");
            if (candidates.isEmpty()) {
                throw new AnyException(204, "No candidates returned by Gemini API");
            }

            String resultText = candidates.get(0).path("content").path("parts").get(0).path("text").asText();
            logger.debug("gemini API Response: {}", resultText);
            return resultText;

        } catch (AnyException ex) {
            throw ex;
        } catch (Exception e) {
            logger.error("Gemini API call failed", e);
            throw new AnyException(500, "Failed to call Gemini API");
        }
    }

    /**
     * Calls the Gemini API with a text prompt and inline data (e.g., image).
     * @param textPrompt The text prompt to send.
     * @param inlineData The inline data (e.g., base64-encoded image).
     * @return The text response from the Gemini API.
     * @throws AnyException If the API call fails or the response is invalid.
     */
    public String callGeminiApiWithInlineData(String textPrompt, Map<String, Object> inlineData) throws AnyException {
        try {
            // Build request body
            Map<String, Object> parts = Map.of(
                    "parts", List.of(
                            Map.of("text", textPrompt),
                            Map.of("inline_data", inlineData)
                    )
            );
            Map<String, Object> requestBodyMap = Map.of("contents", List.of(parts));

            String requestBody = objectMapper.writeValueAsString(requestBodyMap);
            logger.debug("Gemini API Request with Inline Data: {}", requestBody);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            // Make API call
            String apiUrl = GEMINI_API_URL + "?key=" + geminiApiKey;
            String responseBody = restTemplate.postForObject(apiUrl, entity, String.class);

            if (responseBody == null) {
                throw new AnyException(204, "Gemini API returned empty response");
            }

            // Parse response
            JsonNode geminiResponse = objectMapper.readTree(responseBody);
            JsonNode candidates = geminiResponse.path("candidates");
            if (candidates.isEmpty()) {
                throw new AnyException(204, "No Gemini candidates");
            }

            String resultText = candidates.get(0).path("content").path("parts").get(0).path("text").asText();
            logger.debug("Gemini API Response: {}", resultText);
            return resultText;

        } catch (AnyException ex) {
            throw ex;
        } catch (Exception e) {
            logger.error("Gemini API call with inline data failed", e);
            throw new AnyException(500, "Failed to call Gemini API with inline data");
        }
    }

    /**
     * Simplified method for text-only calls with JSON response format
     */
    public String callGeminiApiForJson(String prompt) throws AnyException {
        Map<String, Object> generationConfig = Map.of("response_mime_type", "application/json");
        return callGeminiApi(prompt, generationConfig);
    }
}