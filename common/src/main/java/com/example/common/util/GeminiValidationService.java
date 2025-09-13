package com.example.common.util;

import com.example.common.Exception.AnyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * Service class for validating crop images using the Gemini API.
 * Processes uploaded images to determine if they depict a crop or plant by interacting with the Gemini API.
 */
@Service
public class GeminiValidationService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiValidationService.class);

    private final GeminiApiHelper geminiApiHelper;

    /**
     * Constructs a GeminiValidationService instance with the specified Gemini API helper.
     *
     * @param geminiApiHelper The helper class for interacting with the Gemini API.
     */
    public GeminiValidationService(GeminiApiHelper geminiApiHelper) {
        this.geminiApiHelper = geminiApiHelper;
    }

    /**
     * Validates whether an uploaded image depicts a crop or plant using the Gemini API.
     * Converts the image to Base64 format and sends it to the Gemini API with a prompt to determine
     * if the image represents a crop or plant.
     *
     * @param file The uploaded image file to validate.
     * @return {@code true} if the image is identified as a crop or plant, {@code false} otherwise.
     * @throws AnyException If the file is empty, invalid, or if the Gemini API call fails.
     */
    public boolean isValidCropImage(MultipartFile file) throws AnyException {
        try {
            if (file == null || file.isEmpty()) {
                throw new AnyException(HttpStatus.BAD_REQUEST.value(), "Uploaded file is empty");
            }

            logger.info("Validating image with Gemini API...");
            byte[] imageBytes = file.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String mimeType = file.getContentType() != null ? file.getContentType() : "image/jpeg";

            // Build inline data for request
            Map<String, Object> inlineData = Map.of(
                    "mime_type", mimeType,
                    "data", base64Image
            );

            String textPrompt = "Is this image a crop or plant? Answer YES or NO.";
            String resultText = geminiApiHelper.callGeminiApiWithInlineData(textPrompt, inlineData);

            logger.info("Gemini validation result: {}", resultText);
            return resultText.trim().equalsIgnoreCase("YES") || resultText.toUpperCase().contains("YES");

        } catch (AnyException e) {
            logger.error("Validation error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Failed to validate image with Gemini", e);
            throw new AnyException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to validate crop image");
        }
    }
}